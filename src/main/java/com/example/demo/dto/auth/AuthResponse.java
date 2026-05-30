package com.example.demo.dto.auth;

public record AuthResponse(
	boolean success,
	String message,
	String accessToken,
	String refreshToken,
	String username
) {
	public AuthResponse(boolean success, String message) {
		this(success, message, null, null, null);
	}
}