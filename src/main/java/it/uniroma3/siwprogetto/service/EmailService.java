package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service layer per gestione invio email transazionali e notifiche
 * 
 * Responsabilità:
 * - Email transazionali sistema (welcome, reset password, etc.)
 * - Notifiche business logic (subscription, messaggi privati)
 * - Template email HTML/plain text
 * - Integration con JavaMailSender Spring
 * - Error handling per delivery failures
 * 
 * Email Categories:
 * - Authentication: reset password, account confirmation
 * - Onboarding: welcome email, account setup
 * - Subscription: conferma, cancellazione, renewal
 * - Messaging: private messages, responses
 * - Compliance: account deletion, GDPR
 * 
 * Pattern Service vantaggi:
 * - Centralizzazione logic invio email
 * - Template management consistency
 * - Error handling standardizzato
 * - Integration testing facilitated
 * - Mock-able per unit tests
 */
@Service
public class EmailService {

    /**
     * Spring JavaMailSender per invio email
     * Configurazione SMTP via application.properties
     * 
     * Features:
     * - MIME message support per HTML
     * - Attachment handling
     * - Connection pooling
     * - SMTP authentication
     */
    private final JavaMailSender mailSender;

    /**
     * Constructor injection per dependency
     * Preferibile a @Autowired per immutability
     * 
     * @param mailSender JavaMailSender configurato da Spring
     */
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Invia email per reset password con token sicuro
     * 
     * Security Features:
     * - Token temporaneo univoco
     * - Link con scadenza implicita
     * - HTTPS endpoint obbligatorio production
     * - No sensitive data in email body
     * 
     * @param email Destinatario email reset
     * @param resetToken Token sicuro per reset
     * @throws MessagingException Se errore invio email
     * 
     * Email Template:
     * - HTML formatted per better UX
     * - Clear call-to-action button/link
     * - Disclaimer per security awareness
     * - Brand consistency con design system
     */
    public void sendResetPasswordEmail(String email, String resetToken) throws MessagingException {
        // Creazione MIME message per HTML support
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Password Reset Request");
        helper.setText(
                "<h3>Reset Your Password</h3>" +
                        "<p>You requested a password reset. Click the link below to reset your password:</p>" +
                        "<a href='http://localhost:8080/reset-password?token=" + resetToken + "'>Reset Password</a>" +
                        "<p>If you didn't request this, please ignore this email.</p>",
                true // HTML content
        );

        mailSender.send(message);
    }

    /**
     * Invia email di benvenuto per nuovi utenti registrati
     * 
     * Onboarding Strategy:
     * - Welcome messaging personalizzato
     * - Call-to-action per primo utilizzo
     * - Contact information per supporto
     * - Link diretti a funzionalità chiave
     * 
     * @param email Email nuovo utente
     * @param username Username per personalizzazione
     * @throws MessagingException Se errore invio email
     */
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

    /**
     * Invia conferma cancellazione account per compliance GDPR
     * 
     * GDPR Compliance:
     * - Conferma azione irreversibile
     * - Dettagli dati eliminati
     * - Contact per supporto emergenza
     * - Audit trail documentazione
     * 
     * @param email Email account cancellato
     * @param username Username per personalizzazione
     * @throws MessagingException Se errore invio email
     */
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

    /**
     * Invia conferma attivazione subscription con dettagli
     * 
     * Business Information:
     * - Nome subscription attivata
     * - Date inizio e scadenza
     * - Link gestione subscription
     * - Contact supporto clienti
     * 
     * @param email Email subscriber
     * @param username Username per personalizzazione
     * @param subscriptionName Nome piano attivato
     * @param startDate Data inizio subscription
     * @param expiryDate Data scadenza subscription
     * @throws MessagingException Se errore invio email
     */
    public void sendSubscriptionConfirmationEmail(String email, String username, String subscriptionName, 
                                                 LocalDate startDate, LocalDate expiryDate) throws MessagingException {
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

    /**
     * Invia conferma cancellazione subscription
     * 
     * @param email Email subscriber
     * @param username Username per personalizzazione
     * @param subscriptionName Nome subscription cancellata
     * @throws MessagingException Se errore invio email
     * 
     * Business Logic:
     * - Conferma azione utente
     * - Info perdita funzionalità
     * - Re-engagement per nuovo subscription
     * - Support contact per assistenza
     */
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

    /**
     * Invia notifica nuovo messaggio privato al venditore
     * 
     * NOTA: SimpleMailMessage invece di MimeMessage
     * - Plain text per semplicità
     * - No HTML formatting needed
     * - Legacy method compatibility
     * 
     * @param recipientEmail Email venditore destinatario
     * @param senderEmail Email utente mittente
     * @param product Prodotto oggetto del messaggio
     * @param message Testo messaggio utente
     * 
     * Messaging System:
     * - Bridge email per comunicazione utente-venditore
     * - Product context per identificazione
     * - Link platform per reply continuazione
     */
    public void sendPrivateMessageEmail(String recipientEmail, String senderEmail, Product product, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(recipientEmail);
        mailMessage.setSubject("New Message About " + product.getBrand() + " " + product.getModel());
        mailMessage.setText("You received a message from " + senderEmail + ":\n\n" +
                "Product: " + product.getBrand() + " " + product.getModel() + "\n" +
                "Message: " + message + "\n\n" +
                "Reply via the platform: [Link to /private/messages]");
        mailSender.send(mailMessage);
    }

    /**
     * Invia notifica risposta a messaggio privato
     * 
     * @param recipientEmail Email utente originale
     * @param responderEmail Email venditore che risponde
     * @param product Prodotto oggetto conversazione
     * @param responseMessage Testo risposta venditore
     * 
     * Conversation Flow:
     * - Notifica reply per continuità conversazione
     * - Product context mantenuto
     * - Platform link per thread completo
     */
    public void sendPrivateMessageResponseEmail(String recipientEmail, String responderEmail, 
                                               Product product, String responseMessage) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(recipientEmail);
        mailMessage.setSubject("Response to Your Message About " + product.getBrand() + " " + product.getModel());
        mailMessage.setText("You received a response from " + responderEmail + ":\n\n" +
                "Product: " + product.getBrand() + " " + product.getModel() + "\n" +
                "Response: " + responseMessage + "\n\n" +
                "Reply via the platform: [Link to /private/messages]");
        mailSender.send(mailMessage);
    }

    /**
     * Invia conferma rinnovo automatico subscription
     * 
     * Auto-Renewal Business:
     * - Conferma subscription rinnovata
     * - Nuova data scadenza
     * - Link gestione auto-renewal
     * - Brand messaging consistency
     * 
     * @param recipientEmail Email subscriber
     * @param username Username per personalizzazione
     * @param subscriptionName Nome subscription rinnovata
     * @param newExpiryDate Nuova data scadenza
     * @throws MessagingException Se errore invio email
     * 
     * UTF-8 Encoding:
     * - Explicit charset per caratteri speciali
     * - Support internationalisation
     * - Compatibility multi-language
     */
    public void sendSubscriptionRenewalEmail(String recipientEmail, String username, 
                                            String subscriptionName, LocalDate newExpiryDate) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setSubject("Conferma Rinnovo Abbonamento - FCF Motors");
        String htmlContent = "<h3>Ciao, " + username + "!</h3>" +
                "<p>Il tuo abbonamento <strong>" + subscriptionName + "</strong> è stato rinnovato con successo.</p>" +
                "<p>Nuova data di scadenza: <strong>" + newExpiryDate + "</strong></p>" +
                "<p>Puoi gestire il tuo abbonamento dalla sezione <a href='http://yourdomain.com/account'>Account</a>.</p>" +
                "<p>Grazie per essere con FCF Motors!</p>" +
                "<p>Il Team FCF Motors</p>";
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}