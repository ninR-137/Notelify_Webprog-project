package com.example.demo.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailOtpService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String APP_NAME = "Notelify";

	private final JavaMailSender mailSender;
	private final HttpClient httpClient;
	private final String fromEmail;
	private final String mailApiKey;
	private final String mailApiUrl;
	private final Duration otpTtl;
	private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
	private final Map<String, SignupDraft> signupDrafts = new ConcurrentHashMap<>();

	public EmailOtpService(
		JavaMailSender mailSender,
		@Value("${app.mail.from}") String fromEmail,
		@Value("${app.mail.api-key:}") String mailApiKey,
		@Value("${app.mail.api-url:https://api.resend.com/emails}") String mailApiUrl,
		@Value("${app.otp.ttl-minutes:5}") long otpTtlMinutes
	) {
		this.mailSender = mailSender;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
		this.fromEmail = fromEmail;
		this.mailApiKey = mailApiKey == null ? "" : mailApiKey.trim();
		this.mailApiUrl = mailApiUrl;
		this.otpTtl = Duration.ofMinutes(otpTtlMinutes);
	}

	public String createSignupOtp(String email, String username, String password) {
		String normalizedEmail = normalize(email);
		String otp = generateOtp();
		otpStore.put(normalizedEmail, new OtpEntry(otp, Instant.now().plus(otpTtl)));
		signupDrafts.put(normalizedEmail, new SignupDraft(username, password));
		return otp;
	}

	public void sendSignupOtpEmail(String email, String username, String otp) {
		String subject = APP_NAME + " verification code: " + otp;
		String html = buildOtpEmailHtml(username, otp, otpTtl.toMinutes());
		sendHtmlEmail(normalize(email), subject, html);
	}

	public String sendSignupOtp(String email, String username, String password) {
		String otp = createSignupOtp(email, username, password);
		sendSignupOtpEmail(email, username, otp);
		return otp;
	}

	public SignupDraft verifySignupOtp(String email, String otp) {
		String normalizedEmail = normalize(email);
		OtpEntry entry = otpStore.get(normalizedEmail);
		if (entry == null) {
			return null;
		}
		if (Instant.now().isAfter(entry.expiresAt())) {
			otpStore.remove(normalizedEmail);
			signupDrafts.remove(normalizedEmail);
			return null;
		}
		boolean valid = entry.otp().equals(otp);
		if (!valid) {
			return null;
		}

		otpStore.remove(normalizedEmail);
		return signupDrafts.remove(normalizedEmail);
	}

	public void sendPasswordResetEmail(String email) {
		String normalizedEmail = normalize(email);
		String subject = APP_NAME + " password reset request";
		String html = buildResetEmailHtml(normalizedEmail);
		sendHtmlEmail(normalizedEmail, subject, html);
	}

	private String generateOtp() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}

	private String normalize(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	private void sendHtmlEmail(String to, String subject, String html) {
		if (!mailApiKey.isBlank()) {
			sendHtmlEmailViaApi(to, subject, html);
			return;
		}
		sendHtmlEmailViaSmtp(to, subject, html);
	}

	private void sendHtmlEmailViaSmtp(String to, String subject, String html) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
			helper.setFrom(fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
		} catch (MessagingException ex) {
			throw new IllegalStateException("Failed to send email", ex);
		}
	}

	private void sendHtmlEmailViaApi(String to, String subject, String html) {
		String payload = "{" +
			"\"from\":\"" + escapeJson(fromEmail) + "\"," +
			"\"to\":[\"" + escapeJson(to) + "\"]," +
			"\"subject\":\"" + escapeJson(subject) + "\"," +
			"\"html\":\"" + escapeJson(html) + "\"" +
		"}";

		HttpRequest request = HttpRequest.newBuilder(URI.create(mailApiUrl))
			.timeout(Duration.ofSeconds(15))
			.header("Authorization", "Bearer " + mailApiKey)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(payload))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return;
			}
			throw new IllegalStateException("Failed to send email via HTTPS API. Status=" + status);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to send email via HTTPS API", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Email send was interrupted", ex);
		}
	}

	private String escapeJson(String input) {
		return input
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	private String buildOtpEmailHtml(String username, String otp, long expiresMinutes) {
		String safeName = (username == null || username.isBlank()) ? "there" : username;
		return """
			<!doctype html>
			<html>
			  <body style=\"margin:0;padding:0;background:#f6f7fb;font-family:'Segoe UI',Arial,sans-serif;color:#111827;\">
			    <table role=\"presentation\" width=\"100%%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px;\">
			      <tr>
			        <td align=\"center\">
			          <table role=\"presentation\" width=\"100%%\" style=\"max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;\">
			            <tr>
			              <td style=\"background:linear-gradient(135deg,#5a4af4,#6f38d9);padding:28px 32px;color:#ffffff;\">
			                <div style=\"font-size:14px;letter-spacing:0.06em;text-transform:uppercase;opacity:0.9;\">Notelify Security</div>
			                <h1 style=\"margin:10px 0 0;font-size:28px;line-height:1.2;\">Verify your email</h1>
			              </td>
			            </tr>
			            <tr>
			              <td style=\"padding:30px 32px;\">
			                <p style=\"margin:0 0 14px;font-size:16px;line-height:1.6;\">Hi %s,</p>
			                <p style=\"margin:0 0 20px;font-size:16px;line-height:1.6;color:#374151;\">Use this one-time code to finish creating your account:</p>
			                <div style=\"margin:0 0 20px;padding:14px 18px;border:1px dashed #8b7cf6;border-radius:12px;background:#f4f2ff;text-align:center;\">
			                  <span style=\"font-size:34px;font-weight:800;letter-spacing:0.24em;color:#4c3ed9;\">%s</span>
			                </div>
			                <p style=\"margin:0 0 8px;font-size:14px;color:#4b5563;\">This code expires in %d minutes.</p>
			                <p style=\"margin:0;font-size:13px;color:#6b7280;\">If you did not request this, you can safely ignore this email.</p>
			              </td>
			            </tr>
			            <tr>
			              <td style=\"padding:18px 32px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;\">
			                Sent by Notelify • Keep this email for your records
			              </td>
			            </tr>
			          </table>
			        </td>
			      </tr>
			    </table>
			  </body>
			</html>
			""".formatted(escapeHtml(safeName), escapeHtml(otp), expiresMinutes);
	}

	private String buildResetEmailHtml(String email) {
		return """
			<!doctype html>
			<html>
			  <body style=\"margin:0;padding:0;background:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;color:#111827;\">
			    <table role=\"presentation\" width=\"100%%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px;\">
			      <tr>
			        <td align=\"center\">
			          <table role=\"presentation\" width=\"100%%\" style=\"max-width:620px;background:#ffffff;border:1px solid #d1d5db;border-radius:16px;overflow:hidden;\">
			            <tr>
			              <td style=\"padding:28px 32px;border-bottom:1px solid #e5e7eb;\">
			                <h1 style=\"margin:0;font-size:26px;line-height:1.25;\">Password reset requested</h1>
			              </td>
			            </tr>
			            <tr>
			              <td style=\"padding:28px 32px;\">
			                <p style=\"margin:0 0 12px;font-size:15px;line-height:1.6;\">We received a request to reset the password for:</p>
			                <p style=\"margin:0 0 20px;font-size:16px;font-weight:700;color:#1f2937;\">%s</p>
			                <p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;color:#374151;\">If this was you, continue from the app flow to complete your reset. If this was not you, no action is needed.</p>
			                <div style=\"padding:12px 14px;border-radius:10px;background:#f9fafb;border:1px solid #e5e7eb;font-size:13px;color:#4b5563;\">
			                  For security, we never include passwords in email and never ask for your app key.
			                </div>
			              </td>
			            </tr>
			            <tr>
			              <td style=\"padding:18px 32px;background:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;color:#6b7280;\">
			                Notelify Account Protection
			              </td>
			            </tr>
			          </table>
			        </td>
			      </tr>
			    </table>
			  </body>
			</html>
			""".formatted(escapeHtml(email));
	}

	private String escapeHtml(String input) {
		return input
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private record OtpEntry(String otp, Instant expiresAt) {
	}

	public record SignupDraft(String username, String password) {
	}
}