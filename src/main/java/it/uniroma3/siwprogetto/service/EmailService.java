package it.uniroma3.siwprogetto.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
public class EmailService {

	private final JavaMailSender mailSender;

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

	public void sendWelcomeEmail(String email, String username) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(email);
		helper.setSubject("Benvenuto su FCF Motors!");
		helper.setText(
				"<h3>Benvenuto, " + username + "!</h3>" +
						"<p>Grazie per esserti registrato su FCF Motors. Siamo entusiasti di averti con noi!</p>" +
						"<p>Esplora il nostro catalogo di auto e trova il veicolo perfetto per te:</p>" +
						"<a href='http://localhost:8080/products'>Trova Auto</a>" +
						"<p>Se hai bisogno di assistenza, contattaci a info@fcfmotors.com.</p>",
				true
		);

		mailSender.send(message);
	}

	public void sendAccountDeletionEmail(String email, String username) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(email);
		helper.setSubject("Conferma Cancellazione Account - FCF Motors");
		helper.setText(
				"<h3>Ciao, " + username + "</h3>" +
						"<p>Il tuo account su FCF Motors è stato cancellato con successo.</p>" +
						"<p>Tutte le tue informazioni personali, abbonamenti e dati associati sono stati rimossi dal nostro sistema.</p>" +
						"<p>Se non hai richiesto questa azione, contattaci immediatamente a info@fcfmotors.com.</p>" +
						"<p>Grazie per aver utilizzato FCF Motors!</p>",
				true
		);

		mailSender.send(message);
	}

	public void sendSubscriptionConfirmationEmail(String email, String username, String subscriptionName, LocalDate startDate, LocalDate expiryDate) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(email);
		helper.setSubject("Conferma Sottoscrizione Abbonamento - FCF Motors");
		helper.setText(
				"<h3>Ciao, " + username + "</h3>" +
						"<p>Hai sottoscritto con successo l'abbonamento <strong>" + subscriptionName + "</strong>.</p>" +
						"<p><strong>Dettagli dell'abbonamento:</strong></p>" +
						"<ul>" +
						"<li>Inizio: " + startDate + "</li>" +
						"<li>Scadenza: " + expiryDate + "</li>" +
						"</ul>" +
						"<p>Ora puoi accedere alle funzionalità esclusive del tuo abbonamento!</p>" +
						"<p>Per gestire il tuo abbonamento, visita <a href='http://localhost:8080/account'>il tuo account</a>.</p>" +
						"<p>Per assistenza, contattaci a info@fcfmotors.com.</p>",
				true
		);

		mailSender.send(message);
	}

	public void sendSubscriptionCancellationEmail(String email, String username, String subscriptionName) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(email);
		helper.setSubject("Conferma Cancellazione Abbonamento - FCF Motors");
		helper.setText(
				"<h3>Ciao, " + username + "</h3>" +
						"<p>L'abbonamento <strong>" + subscriptionName + "</strong> è stato cancellato con successo.</p>" +
						"<p>Non avrai più accesso alle funzionalità associate a questo abbonamento.</p>" +
						"<p>Se desideri sottoscrivere un nuovo abbonamento, visita <a href='http://localhost:8080/subscriptions'>la pagina abbonamenti</a>.</p>" +
						"<p>Per assistenza, contattaci a info@fcfmotors.com.</p>",
				true
		);

		mailSender.send(message);
	}


}
