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

	@Value("${app.mail.api-key:}")
	private String mailApiKey;

	@Value("${app.jwt.secret:}")
	private String jwtSecret;

	@PostConstruct
	public void logConfigHealth() {
		boolean smtpConfigured = isPresent(mailHost)
			&& isPresent(mailPort)
			&& isPresent(mailUsername)
			&& isPresent(mailPassword);

		boolean httpsApiConfigured = isPresent(mailApiKey);

		boolean mailConfigured = isPresent(appMailFrom) && (smtpConfigured || httpsApiConfigured);

		boolean jwtConfigured = isPresent(jwtSecret) && jwtSecret.length() >= 32;

		log.info(
			"Config health check - mailConfigured={}, jwtConfigured={}, smtpConfigured={}, httpsApiConfigured={}, mailHostSet={}, mailPortSet={}, mailUserSet={}, mailPassSet={}, appMailFromSet={}, mailApiKeySet={}, jwtSecretLength= {}",
			mailConfigured,
			jwtConfigured,
			smtpConfigured,
			httpsApiConfigured,
			isPresent(mailHost),
			isPresent(mailPort),
			isPresent(mailUsername),
			isPresent(mailPassword),
			isPresent(appMailFrom),
			isPresent(mailApiKey),
			jwtSecret == null ? 0 : jwtSecret.length()
		);

		if (!mailConfigured) {
			log.warn("Mail configuration is incomplete. Provide APP_MAIL_FROM plus either SMTP settings or APP_MAIL_API_KEY for HTTPS email delivery.");
		}
		if (!jwtConfigured) {
			log.warn("JWT secret is missing or too short. Set APP_JWT_SECRET to a strong value (32+ chars).");
		}
	}

	private boolean isPresent(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
