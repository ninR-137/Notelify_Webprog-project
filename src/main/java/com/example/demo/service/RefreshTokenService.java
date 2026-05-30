package com.example.demo.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

	private final Map<String, RefreshTokenEntry> activeRefreshTokens = new ConcurrentHashMap<>();

	public void store(String token, String email, Instant expiresAt) {
		activeRefreshTokens.put(token, new RefreshTokenEntry(email, expiresAt, false));
	}

	public boolean isValid(String token, String email) {
		RefreshTokenEntry entry = activeRefreshTokens.get(token);
		if (entry == null || entry.revoked()) {
			return false;
		}
		if (!entry.email().equals(email)) {
			return false;
		}
		if (Instant.now().isAfter(entry.expiresAt())) {
			activeRefreshTokens.remove(token);
			return false;
		}
		return true;
	}

	public void revoke(String token) {
		RefreshTokenEntry entry = activeRefreshTokens.get(token);
		if (entry == null) {
			return;
		}
		activeRefreshTokens.put(token, new RefreshTokenEntry(entry.email(), entry.expiresAt(), true));
	}

	private record RefreshTokenEntry(String email, Instant expiresAt, boolean revoked) {
	}
}