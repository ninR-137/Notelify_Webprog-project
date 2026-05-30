package com.example.demo.controller;

import java.time.Instant;

import com.example.demo.dto.auth.AuthResponse;
import com.example.demo.dto.auth.ForgotPasswordRequest;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LogoutRequest;
import com.example.demo.dto.auth.RefreshRequest;
import com.example.demo.dto.auth.SendOtpRequest;
import com.example.demo.dto.auth.TokenPair;
import com.example.demo.dto.auth.VerifyOtpRequest;
import com.example.demo.service.AppUserService;
import com.example.demo.service.EmailOtpService;
import com.example.demo.service.JwtService;
import com.example.demo.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

	private final EmailOtpService emailOtpService;
	private final AppUserService appUserService;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final AuthenticationManager authenticationManager;

	public AuthApiController(
		EmailOtpService emailOtpService,
		AppUserService appUserService,
		JwtService jwtService,
		RefreshTokenService refreshTokenService,
		AuthenticationManager authenticationManager
	) {
		this.emailOtpService = emailOtpService;
		this.appUserService = appUserService;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/send-otp")
	public ResponseEntity<AuthResponse> sendOtp(@RequestBody SendOtpRequest request) {
		if (request == null || isBlank(request.email()) || isBlank(request.username()) || isBlank(request.password())) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Please complete all fields."));
		}
		if (!request.email().contains("@")) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Please enter a valid email address."));
		}
		if (request.username().trim().length() < 3) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Username must be at least 3 characters."));
		}
		if (request.password().length() < 6) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Password must be at least 6 characters."));
		}
		if (appUserService.exists(request.email())) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "An account with this email already exists."));
		}

		try {
			emailOtpService.sendSignupOtp(request.email(), request.username(), request.password());
		} catch (RuntimeException ex) {
			return ResponseEntity.status(503).body(new AuthResponse(false, "OTP email service is unavailable. Please try again later."));
		}
		return ResponseEntity.ok(new AuthResponse(true, "OTP sent to your email."));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
		if (request == null || isBlank(request.email()) || isBlank(request.otp())) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Email and OTP are required."));
		}
		EmailOtpService.SignupDraft draft = emailOtpService.verifySignupOtp(request.email(), request.otp().trim());
		if (draft == null) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Invalid or expired OTP."));
		}

		try {
			appUserService.register(request.email(), draft.username(), draft.password());
		} catch (IllegalStateException ex) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, ex.getMessage()));
		}

		TokenPair tokens = issueTokens(request.email());
		return ResponseEntity.ok(new AuthResponse(
			true,
			"Email verified.",
			tokens.accessToken(),
			tokens.refreshToken(),
			appUserService.displayNameFor(request.email())
		));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
		if (request == null || isBlank(request.email()) || isBlank(request.password())) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Email and password are required."));
		}

		try {
			Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
			);
			if (!authentication.isAuthenticated()) {
				throw new BadCredentialsException("Invalid credentials");
			}
		} catch (BadCredentialsException ex) {
			return ResponseEntity.status(401).body(new AuthResponse(false, "Invalid email or password."));
		}

		TokenPair tokens = issueTokens(request.email());
		return ResponseEntity.ok(new AuthResponse(
			true,
			"Login successful.",
			tokens.accessToken(),
			tokens.refreshToken(),
			appUserService.displayNameFor(request.email())
		));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
		if (request == null || isBlank(request.refreshToken())) {
			return ResponseEntity.status(401).body(new AuthResponse(false, "Refresh token is required."));
		}

		String email = jwtService.extractSubjectForType(request.refreshToken(), "refresh");
		if (email == null || !refreshTokenService.isValid(request.refreshToken(), email)) {
			return ResponseEntity.status(401).body(new AuthResponse(false, "Refresh token is invalid or expired."));
		}

		refreshTokenService.revoke(request.refreshToken());
		TokenPair tokens = issueTokens(email);
		return ResponseEntity.ok(new AuthResponse(
			true,
			"Token refreshed.",
			tokens.accessToken(),
			tokens.refreshToken(),
			appUserService.displayNameFor(email)
		));
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logout(@RequestBody LogoutRequest request) {
		if (request != null && !isBlank(request.refreshToken())) {
			refreshTokenService.revoke(request.refreshToken());
		}
		return ResponseEntity.ok(new AuthResponse(true, "Logged out successfully."));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		if (request == null || isBlank(request.email()) || !request.email().contains("@")) {
			return ResponseEntity.badRequest().body(new AuthResponse(false, "Please enter a valid email address."));
		}
		try {
			emailOtpService.sendPasswordResetEmail(request.email());
		} catch (RuntimeException ex) {
			return ResponseEntity.status(503).body(new AuthResponse(false, "Password reset email service is unavailable. Please try again later."));
		}
		return ResponseEntity.ok(new AuthResponse(true, "Password reset email sent."));
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private TokenPair issueTokens(String emailInput) {
		String email = emailInput.trim().toLowerCase();
		String username = appUserService.displayNameFor(email);
		String accessToken = jwtService.issueAccessToken(email, username);
		String refreshToken = jwtService.issueRefreshToken(email);
		Instant refreshExpiry = jwtService.extractExpiration(refreshToken);
		refreshTokenService.store(refreshToken, email, refreshExpiry);
		return new TokenPair(accessToken, refreshToken);
	}
}