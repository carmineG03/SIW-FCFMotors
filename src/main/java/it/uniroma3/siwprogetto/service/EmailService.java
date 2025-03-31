package it.uniroma3.siwprogetto.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

	private final JavaMailSender mailSender;

	@Autowired
	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendResetPasswordEmail(String email, String resetToken) throws MessagingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(email);
		helper.setSubject("Password Reset Request");
		helper.setText(
				"<h3>Reset Your Password</h3>" +
						"<p>You requested a password reset. Click the link below to reset your password:</p>" +
						"<a href='http://localhost:8080/reset-password?token=" + resetToken + "'>Reset Password</a>" +
						"<p>If you didn’t request this, please ignore this email.</p>",
				true
		);

		mailSender.send(message);
	}
}
