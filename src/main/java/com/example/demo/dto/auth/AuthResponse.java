package com.example.demo.dto.auth;

public record AuthResponse(
	boolean success,
	String message,
	String accessToken,
	String refreshToken,
	String username,
	String otpCode
) {
	public AuthResponse(boolean success, String message) {
		this(success, message, null, null, null, null);
	}

	public AuthResponse(boolean success, String message, String accessToken, String refreshToken, String username) {
		this(success, message, accessToken, refreshToken, username, null);
	}
}