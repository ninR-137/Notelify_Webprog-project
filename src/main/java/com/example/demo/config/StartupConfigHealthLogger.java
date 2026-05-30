package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StartupConfigHealthLogger {

	private static final Logger log = LoggerFactory.getLogger(StartupConfigHealthLogger.class);

	@Value("${spring.mail.host:}")
	private String mailHost;

	@Value("${spring.mail.port:}")
	private String mailPort;

	@Value("${spring.mail.username:}")
	private String mailUsername;

	@Value("${spring.mail.password:}")
	private String mailPassword;

	@Value("${app.mail.from:}")
	private String appMailFrom;

	@Value("${app.jwt.secret:}")
	private String jwtSecret;

	@PostConstruct
	public void logConfigHealth() {
		boolean mailConfigured = isPresent(mailHost)
			&& isPresent(mailPort)
			&& isPresent(mailUsername)
			&& isPresent(mailPassword)
			&& isPresent(appMailFrom);

		boolean jwtConfigured = isPresent(jwtSecret) && jwtSecret.length() >= 32;

		log.info(
			"Config health check - mailConfigured={}, jwtConfigured={}, mailHostSet={}, mailPortSet={}, mailUserSet={}, mailPassSet={}, appMailFromSet={}, jwtSecretLength={}",
			mailConfigured,
			jwtConfigured,
			isPresent(mailHost),
			isPresent(mailPort),
			isPresent(mailUsername),
			isPresent(mailPassword),
			isPresent(appMailFrom),
			jwtSecret == null ? 0 : jwtSecret.length()
		);

		if (!mailConfigured) {
			log.warn("Mail configuration is incomplete. OTP and forgot-password email delivery may fail.");
		}
		if (!jwtConfigured) {
			log.warn("JWT secret is missing or too short. Set APP_JWT_SECRET to a strong value (32+ chars).");
		}
	}

	private boolean isPresent(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
