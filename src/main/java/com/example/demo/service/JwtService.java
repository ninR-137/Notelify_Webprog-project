package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

	private final String secret;
	private final long accessTokenMinutes;
	private final long refreshTokenDays;
	private SecretKey signingKey;

	public JwtService(
		@Value("${app.jwt.secret}") String secret,
		@Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes,
		@Value("${app.jwt.refresh-token-days:7}") long refreshTokenDays
	) {
		this.secret = secret;
		this.accessTokenMinutes = accessTokenMinutes;
		this.refreshTokenDays = refreshTokenDays;
	}

	@PostConstruct
	void init() {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String issueAccessToken(String email, String username) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(email)
			.claim("typ", "access")
			.claim("name", username)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
			.signWith(signingKey)
			.compact();
	}

	public String issueRefreshToken(String email) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(email)
			.claim("typ", "refresh")
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(refreshTokenDays, ChronoUnit.DAYS)))
			.signWith(signingKey)
			.compact();
	}

	public String extractSubjectForType(String token, String expectedType) {
		Claims claims = parseClaims(token);
		if (claims == null) {
			return null;
		}
		String type = claims.get("typ", String.class);
		if (!expectedType.equals(type)) {
			return null;
		}
		return claims.getSubject();
	}

	public Instant extractExpiration(String token) {
		Claims claims = parseClaims(token);
		if (claims == null || claims.getExpiration() == null) {
			return null;
		}
		return claims.getExpiration().toInstant();
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (JwtException | IllegalArgumentException ex) {
			return null;
		}
	}
}