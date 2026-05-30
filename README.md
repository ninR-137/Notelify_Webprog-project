# Notelify Demo App

Spring Boot web app with a landing page, OTP email verification, JWT authentication with refresh-token rotation, and an authenticated dashboard backed by persisted notes and todos.

## Tech Stack

- Java 17+
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Thymeleaf
- Spring Data JPA (H2)
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
    NoteApiController.java
    TodoApiController.java
  entity/
    NoteEntity.java
    TodoEntity.java
  repository/
    NoteRepository.java
    TodoRepository.java
  security/
    JwtAuthenticationFilter.java
  service/
    AppUserService.java
    EmailOtpService.java
    JwtService.java
    RefreshTokenService.java
    NoteService.java
    TodoService.java
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
    note/
      NoteFlagRequest.java
      NoteResponse.java
      NoteUpsertRequest.java
    todo/
      TodoFlagRequest.java
      TodoResponse.java
      TodoUpsertRequest.java
    user/
      UserProfileResponse.java

src/main/resources
  templates/
    landingpage.html
    dashboard.html
  static/
    notes-app2.png
    js/
      auth-client.js
  application.properties

.env.example
.env (local only, gitignored)
```

## Key Features

- Landing page with dark mode as default.
- Signup OTP sent to email with styled HTML email template.
- Password reset notification email template.
- JWT access tokens and refresh tokens.
- Automatic refresh on the frontend when access token is near expiry or rejected.
- Logout revokes refresh token and clears local session.
- Authenticated dashboard powered by real APIs (no mock dashboard data).
- Persisted notes: create, list, update, favorite, soft-delete, and permanent delete.
- Persisted todos: create, list, update, complete toggle, and delete.
- Day-based todo reminders (for date-only due dates) with dashboard notifications.
- Dashboard is served via Thymeleaf route `/dashboard`.

## Authentication Flow

1. User signs up and requests OTP via /api/auth/send-otp.
2. User verifies OTP via /api/auth/verify-otp.
3. Backend creates user and returns accessToken + refreshToken.
4. Frontend stores tokens in localStorage.
5. Protected calls use Authorization: Bearer accessToken.
6. If token is close to expiry or a 401 occurs, frontend calls /api/auth/refresh.
7. If refresh fails, frontend clears session and forces relogin.
8. If an access token references a missing in-memory user (for example after restart), requests continue unauthenticated and return 401 for protected APIs.

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
- GET /api/notes?view=all|favorites|trash&search=&sort=recent|oldest|a-z|z-a
- POST /api/notes
- PUT /api/notes/{id}
- PATCH /api/notes/{id}/favorite
- PATCH /api/notes/{id}/deleted
- DELETE /api/notes/{id}
- GET /api/todos
- POST /api/todos
- PUT /api/todos/{id}
- PATCH /api/todos/{id}/completed
- DELETE /api/todos/{id}

### Example Payloads

Create/Update note:

```json
{
  "title": "Sprint planning",
  "content": "Finalize backlog and assign owners",
  "category": "Work",
  "tags": ["planning", "sprint"],
  "favorited": false
}
```

Patch note favorite/deleted:

```json
{
  "value": true
}
```

Create/Update todo:

```json
{
  "title": "Prepare release notes",
  "description": "Summarize completed stories and fixes",
  "priority": "high",
  "dueDate": "2026-06-15",
  "reminderDays": 3
}
```

Patch todo completed:

```json
{
  "value": true
}
```

## Configuration

Configuration is read from `src/main/resources/application.properties` and environment-backed values loaded via:

- `spring.config.import=optional:file:.env[.properties]`

Create your local `.env` from `.env.example` and provide real values:

- spring.mail.* for SMTP
- app.mail.from
- app.otp.ttl-minutes
- app.jwt.secret
- app.jwt.access-token-minutes
- app.jwt.refresh-token-days

Important:

- `.env` is gitignored and should never be committed.
- Use a strong random value for `app.jwt.secret`.

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
- Dashboard (after login): http://localhost:8080/dashboard

## Run With Docker

Build image:

```powershell
docker build -t notelify-demo:latest .
```

Run container:

```powershell
docker run --rm -p 8080:8080 --env-file .env notelify-demo:latest
```

Open app:

- http://localhost:8080/

## Deploy On Render (Docker)

This repository includes a production `Dockerfile` that Render can build directly.

1. Push this repo to GitHub.
2. In Render, click `New` -> `Web Service`.
3. Connect your GitHub repository.
4. Choose `Docker` as the runtime.
5. Render will detect and build from `Dockerfile`.
6. Add required environment variables in Render.
7. Deploy.

### Required Render Environment Variables

Set these in Render `Environment`:

- `SPRING_APPLICATION_NAME`
- `APP_MAIL_FROM`
- `APP_MAIL_API_KEY`
- `APP_MAIL_API_URL` (optional, default: `https://api.resend.com/emails`)
- `APP_OTP_TTL_MINUTES`
- `APP_JWT_SECRET`
- `APP_JWT_ACCESS_TOKEN_MINUTES`
- `APP_JWT_REFRESH_TOKEN_DAYS`

Optional SMTP variables (for non-Render-Free environments):

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_SMTP_AUTH`
- `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- `SPRING_MAIL_SMTP_CONNECTION_TIMEOUT`
- `SPRING_MAIL_SMTP_TIMEOUT`
- `SPRING_MAIL_SMTP_WRITE_TIMEOUT`

Render automatically provides `PORT`; the Docker image starts Spring Boot on that port.

### Notes For Render

- Free Render web services cannot send outbound SMTP on ports `25`, `465`, or `587`.
- For Render Free, configure `APP_MAIL_API_KEY` so email is sent via HTTPS API (port `443`).
- The app currently uses in-memory user/auth state, so sessions are reset on restart/redeploy.
- H2 data is not configured for durable production persistence.
- For production-grade persistence, move to a managed database and persistent auth/user storage.

## Current Limitations

- Users and refresh tokens are in-memory only.
- No persistent user registration storage yet.
- Rate limiting and anti-abuse controls are not implemented yet.
- Notes/todos are persisted, but no pagination is implemented yet.
- Because users are in-memory, existing JWTs may no longer map to a user after app restart and will require re-login.

