package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.EmailService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

/**
 * Controller per la gestione del login e recupero password.
 * Gestisce autenticazione, reset password via email e validazione token.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
public class LoginController {

    /** Logger per tracciare le operazioni di login e recovery */
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /** Pattern per validazione email */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /** Lunghezza minima password */
    private static final int MIN_PASSWORD_LENGTH = 8;

    // === DIPENDENZE INIETTATE ===

    /** Servizio per la gestione degli utenti */
    private final UserService userService;

    /** Servizio per l'invio email */
    private final EmailService emailService;

    /**
     * Costruttore con dependency injection.
     * 
     * @param userService Servizio per operazioni utente
     * @param emailService Servizio per invio email
     */
    @Autowired
    public LoginController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // === GESTIONE PAGINA LOGIN ===

    /**
     * Mostra la pagina di login principale.
     * Accessibile a tutti gli utenti non autenticati.
     * 
     * @param model Modello per la vista
     * @return Nome della vista login
     */
    @GetMapping("/login")
    public String showLoginPage(Model model) {
        logger.debug("Accesso alla pagina di login");
        
        // Aggiunge attributi per eventuali messaggi dinamici
        model.addAttribute("pageTitle", "Accedi a FCF Motors");
        
        return "login";
    }

    // === GESTIONE RECUPERO PASSWORD ===

    /**
     * Mostra la pagina per richiedere il reset della password.
     * Permette all'utente di inserire la propria email per ricevere il link di reset.
     * 
     * @param model Modello per la vista
     * @return Nome della vista forgot-password
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        logger.debug("Accesso alla pagina recupero password");
        
        model.addAttribute("pageTitle", "Recupera Password - FCF Motors");
        
        return "forgot-password";
    }

    /**
     * Processa la richiesta di reset password inviando email con link di recovery.
     * Valida l'email e genera un token sicuro per il reset.
     * 
     * @param email Email dell'utente che richiede il reset
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina forgot-password con messaggio
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                       RedirectAttributes redirectAttributes) {
        logger.info("Richiesta reset password per email: {}", email);

        try {
            // Validazione email
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email non può essere vuota");
            }

            email = email.trim().toLowerCase();

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Formato email non valido");
            }

            // Verifica esistenza utente
            User user = userService.findByEmail(email);
            if (user != null) {
                // Genera token sicuro per il reset
                String resetToken = userService.generateResetToken(email);
                
                // Invia email con link di reset
                emailService.sendResetPasswordEmail(email, resetToken);
                
                logger.info("Email di reset password inviata con successo per: {}", email);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Un link per il reset della password è stato inviato al tuo indirizzo email.");
            } else {
                // Per sicurezza, non rivelare se l'email esiste o meno
                logger.warn("Tentativo di reset per email inesistente: {}", email);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Se l'email è registrata nel sistema, riceverai un link per il reset della password.");
            }

            return "redirect:/forgot-password";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione nel reset password: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";

        } catch (Exception e) {
            logger.error("Errore interno durante reset password per email: {}", email, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante l'invio del link di reset. Riprova più tardi.");
            return "redirect:/forgot-password";
        }
    }

    // === GESTIONE RESET PASSWORD ===

    /**
     * Mostra la pagina per inserire la nuova password utilizzando un token valido.
     * Verifica la validità del token prima di mostrare il form.
     * 
     * @param token Token di reset ricevuto via email
     * @param model Modello per la vista
     * @param redirectAttributes Attributi per messaggi flash
     * @return Nome della vista reset-password o redirect al login se token non valido
     */
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token,
                                       Model model, 
                                       RedirectAttributes redirectAttributes) {
        logger.info("Accesso alla pagina reset password con token");

        try {
            // Validazione token
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token di reset non valido");
            }

            // Verifica validità token
            if (!userService.isResetTokenValid(token)) {
                logger.warn("Tentativo di accesso con token non valido o scaduto");
                throw new IllegalArgumentException("Token di reset non valido o scaduto");
            }

            // Popola il modello per la vista
            model.addAttribute("token", token);
            model.addAttribute("pageTitle", "Reimposta Password - FCF Motors");
            model.addAttribute("minPasswordLength", MIN_PASSWORD_LENGTH);

            logger.info("Pagina reset password mostrata con successo");
            return "reset-password";

        } catch (IllegalArgumentException e) {
            logger.error("Token non valido per reset password: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";

        } catch (Exception e) {
            logger.error("Errore interno durante validazione token reset", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante la validazione del token. Richiedi un nuovo reset password.");
            return "redirect:/forgot-password";
        }
    }

    /**
     * Processa il cambio password utilizzando il token di reset valido.
     * Valida la nuova password e aggiorna l'account utente.
     * 
     * @param token Token di reset password
     * @param newPassword Nuova password scelta dall'utente
     * @param confirmPassword Conferma della nuova password
     * @param model Modello per la vista
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect al login con messaggio di successo o al reset in caso di errore
     */
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                     @RequestParam("newPassword") String newPassword,
                                     @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Tentativo di reset password con token");

        try {
            // Validazione token
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token di reset non valido");
            }

            if (!userService.isResetTokenValid(token)) {
                logger.warn("Tentativo di reset con token non valido o scaduto");
                throw new IllegalArgumentException("Token di reset non valido o scaduto. Richiedi un nuovo reset.");
            }

            // Validazione nuova password
            if (newPassword == null || newPassword.trim().isEmpty()) {
                throw new IllegalArgumentException("La nuova password non può essere vuota");
            }

            if (newPassword.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalArgumentException("La password deve essere di almeno " + MIN_PASSWORD_LENGTH + " caratteri");
            }

            // Verifica sicurezza password
            if (!isPasswordStrong(newPassword)) {
                throw new IllegalArgumentException(
                    "La password deve contenere almeno: una lettera maiuscola, una minuscola, un numero");
            }

            // Validazione conferma password se fornita
            if (confirmPassword != null && !newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Le password non coincidono");
            }

            // Esegue il reset della password
            userService.resetPassword(token, newPassword);

            logger.info("Password resettata con successo");
            redirectAttributes.addFlashAttribute("successMessage", 
                "Password resettata con successo! Ora puoi accedere con la nuova password.");

            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            logger.warn("Errore di validazione nel reset password: {}", e.getMessage());
            
            // Ripopola il modello per mostrare nuovamente il form
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "Reimposta Password - FCF Motors");
            model.addAttribute("minPasswordLength", MIN_PASSWORD_LENGTH);
            
            return "reset-password";

        } catch (Exception e) {
            logger.error("Errore interno durante reset password", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante il reset della password. Riprova più tardi o richiedi un nuovo link.");
            return "redirect:/forgot-password";
        }
    }

    // === METODI UTILITY PRIVATI ===

    /**
     * Verifica che la password rispetti i criteri di sicurezza.
     * Controlla presenza di lettere maiuscole, minuscole, numeri.
     * 
     * @param password Password da validare
     * @return true se la password è sufficientemente sicura
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUpperCase && hasLowerCase && hasDigit;
    }
}