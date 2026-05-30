# Notelify Demo App

Spring Boot web app with a landing page, OTP email verification, JWT authentication, refresh-token rotation, and a protected profile endpoint.

## Tech Stack

- Java 17+
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Thymeleaf
- Spring Data JPA (H2 for runtime)
- Spring Mail (Gmail SMTP)
- JJWT

## Project Structure

```text
src/main/java/com/example/demo
  config/
    SecurityConfig.java
  controller/
    HomeController.java
    AuthApiController.java
    UserApiController.java
  security/
    JwtAuthenticationFilter.java
  service/
    AppUserService.java
    EmailOtpService.java
    JwtService.java
    RefreshTokenService.java
  dto/
    auth/
      AuthResponse.java
      ForgotPasswordRequest.java
      LoginRequest.java
      LogoutRequest.java
      RefreshRequest.java
      SendOtpRequest.java
      TokenPair.java
      VerifyOtpRequest.java
    user/
      UserProfileResponse.java

src/main/resources
  templates/
    landingpage.html
  static/
    dashboard.html
    notes-app2.png
    js/
      auth-client.js
  application.properties
```

## Key Features

- Landing page with dark mode as default.
- Signup OTP sent to email with styled HTML email template.
- Password reset notification email template.
- JWT access tokens and refresh tokens.
- Automatic refresh on the frontend when access token is near expiry or rejected.
- Logout revokes refresh token and clears local session.

## Authentication Flow

1. User signs up and requests OTP via /api/auth/send-otp.
2. User verifies OTP via /api/auth/verify-otp.
3. Backend creates user and returns accessToken + refreshToken.
4. Frontend stores tokens in localStorage.
5. Protected calls use Authorization: Bearer accessToken.
6. If token is close to expiry or a 401 occurs, frontend calls /api/auth/refresh.
7. If refresh fails, frontend clears session and forces relogin.

## API Summary

### Public

- POST /api/auth/send-otp
- POST /api/auth/verify-otp
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout
- POST /api/auth/forgot-password

### Protected

- GET /api/me

## Configuration

Edit values in src/main/resources/application.properties:

- spring.mail.* for SMTP
- app.mail.from
- app.otp.ttl-minutes
- app.jwt.secret
- app.jwt.access-token-minutes
- app.jwt.refresh-token-days

Security note:
Do not commit real credentials or production JWT secrets. Move them to environment variables or a secret manager before deployment.

## Run Locally

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Run tests:

```powershell
.\mvnw.cmd test
```

Open app:

- http://localhost:8080/

## Current Limitations

- Users and refresh tokens are in-memory only.
- No persistent user registration storage yet.
- Rate limiting and anti-abuse controls are not implemented yet.

