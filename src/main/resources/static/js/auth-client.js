(function () {
  var ACCESS_KEY = 'notelify-access-token';
  var REFRESH_KEY = 'notelify-refresh-token';
  var USERNAME_KEY = 'notelify-username';
  var REFRESH_WINDOW_SECONDS = 30;

  function getAccessToken() {
    return localStorage.getItem(ACCESS_KEY);
  }

  function getRefreshToken() {
    return localStorage.getItem(REFRESH_KEY);
  }

  function setSession(data) {
    if (!data || !data.accessToken || !data.refreshToken) {
      return;
    }
    localStorage.setItem(ACCESS_KEY, data.accessToken);
    localStorage.setItem(REFRESH_KEY, data.refreshToken);
    if (data.username) {
      localStorage.setItem(USERNAME_KEY, data.username);
    }
  }

  function clearSession() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USERNAME_KEY);
  }

  function decodeJwtPayload(token) {
    if (!token) return null;
    try {
      var parts = token.split('.');
      if (parts.length < 2) return null;
      var base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      var normalized = base64 + '='.repeat((4 - (base64.length % 4 || 4)) % 4);
      return JSON.parse(atob(normalized));
    } catch (e) {
      return null;
    }
  }

  function tokenExpiringSoon(token, withinSeconds) {
    var payload = decodeJwtPayload(token);
    if (!payload || !payload.exp) return true;
    var nowSeconds = Math.floor(Date.now() / 1000);
    return payload.exp <= (nowSeconds + withinSeconds);
  }

  function refreshSession() {
    var refreshToken = getRefreshToken();
    if (!refreshToken) {
      return Promise.resolve(false);
    }
    return fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: refreshToken })
    }).then(function (res) {
      return res.json().catch(function () {
        return { success: false };
      }).then(function (data) {
        if (!res.ok || !data.success) {
          clearSession();
          return false;
        }
        setSession(data);
        return true;
      });
    }).catch(function () {
      return false;
    });
  }

  function getUsableAccessToken() {
    var accessToken = getAccessToken();
    if (!accessToken) {
      return refreshSession().then(function (ok) {
        return ok ? getAccessToken() : null;
      });
    }
    if (!tokenExpiringSoon(accessToken, REFRESH_WINDOW_SECONDS)) {
      return Promise.resolve(accessToken);
    }
    return refreshSession().then(function (ok) {
      if (ok) {
        return getAccessToken();
      }
      clearSession();
      return null;
    });
  }

  function authFetch(url, options) {
    var reqOptions = options || {};
    return getUsableAccessToken().then(function (accessToken) {
      if (!accessToken) {
        throw new Error('AUTH_REQUIRED');
      }

      var headers = new Headers(reqOptions.headers || {});
      headers.set('Authorization', 'Bearer ' + accessToken);
      var request = Object.assign({}, reqOptions, { headers: headers });

      return fetch(url, request).then(function (res) {
        if (res.status !== 401) {
          return res;
        }
        return refreshSession().then(function (ok) {
          if (!ok) {
            throw new Error('AUTH_REQUIRED');
          }
          var retriedHeaders = new Headers(reqOptions.headers || {});
          retriedHeaders.set('Authorization', 'Bearer ' + getAccessToken());
          var retriedRequest = Object.assign({}, reqOptions, { headers: retriedHeaders });
          return fetch(url, retriedRequest).then(function (retriedResponse) {
            if (retriedResponse.status === 401) {
              clearSession();
              throw new Error('AUTH_REQUIRED');
            }
            return retriedResponse;
          });
        });
      });
    });
  }

  function logout() {
    var refreshToken = getRefreshToken();
    var promise = Promise.resolve();
    if (refreshToken) {
      promise = fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: refreshToken })
      }).catch(function () {
        return null;
      });
    }
    return promise.finally(function () {
      clearSession();
    });
  }

  window.NotelifyAuth = {
    setSession: setSession,
    clearSession: clearSession,
    authFetch: authFetch,
    refreshSession: refreshSession,
    logout: logout,
    getUsername: function () { return localStorage.getItem(USERNAME_KEY) || ''; }
  };
})();