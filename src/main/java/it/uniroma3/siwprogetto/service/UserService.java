package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.*;
import it.uniroma3.siwprogetto.repository.*;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer per gestione completa degli utenti del sistema automotive
 * 
 * Responsabilità:
 * - Registrazione utenti con validazioni business e unicità
 * - Reset password sicuro con token temporanei
 * - Gestione subscription lifecycle completo (attivazione, rinnovo, cancellazione)
 * - Upgrade/downgrade ruoli utente (USER -> DEALER -> PRIVATE)
 * - Eliminazione account con cascade cleanup completo
 * - Job schedulato per rinnovi automatici subscription
 * - Integration con EmailService per notifiche transazionali
 * 
 * Business Model:
 * - User = account principale con credenziali e permissions
 * - UserSubscription = istanza attiva subscription con auto-renewal
 * - Roles = USER (base), DEALER (concessionario), PRIVATE (venditore privato)
 * - Cascade deletions = cleanup completo dipendenze su account deletion
 * 
 * Pattern Service vantaggi:
 * - Business logic centralizzata per user lifecycle
 * - Security integration con password encoding
 * - Scheduling integration per automated processes
 * - Email notifications per user engagement
 * - Transaction management per consistenza dati complessa
 */
@Service
public class UserService {

    /**
     * Repository per accesso dati utenti
     * Core entity per authentication e authorization
     */
    private final UserRepository userRepository;

    /**
     * Password encoder per sicurezza credenziali
     * BCrypt o configurazione Spring Security
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Service per email transazionali
     * Onboarding, notifications, lifecycle events
     */
    private final EmailService emailService;

    /**
     * Logger SLF4J per audit trail e debugging user operations
     * Compliance e troubleshooting operazioni critiche
     */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Repository per piani subscription disponibili
     * Catalogo subscription per attivazioni
     */
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Repository per subscription utente attive
     * Gestione istanze subscription con expiry/renewal
     */
    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    /**
     * Repository per prodotti automotive
     * Cleanup prodotti su role changes e deletions
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Repository per dealer entities
     * Gestione concessionari associati agli utenti
     */
    @Autowired
    private DealerRepository dealerRepository;

    /**
     * Service specializzato per operazioni dealer
     * Delegation per eliminazioni cascade complesse
     */
    @Autowired
    private DealerService dealerService;

    /**
     * Repository per informazioni account aggiuntive
     * Profile information e metadati utente
     */
    @Autowired
    private AccountInformationRepository accountInformationRepository;

    /**
     * Repository per richieste preventivo
     * Cleanup conversazioni su account deletion
     */
    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    /**
     * Repository per audit trail pagamenti
     * Storico transazioni per compliance
     */
    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Repository per elementi carrello acquisti
     * Cleanup carrello su account deletion
     */
    @Autowired
    private CartItemRepository cartItemRepository;

    /**
     * Constructor injection per dependencies core
     * Immutability pattern per thread safety
     * 
     * @param userRepository Repository per operazioni user
     * @param passwordEncoder Encoder per sicurezza password
     * @param emailService Service per notifiche email
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registra nuovo utente con validazioni complete business rules
     * 
     * Business Rules Validation:
     * - Email uniqueness (global system constraint)
     * - Username uniqueness (global system constraint)
     * - Password confirmation match (UX validation)
     * - Default USER role assignment
     * - Welcome email automatic dispatch
     * 
     * @param user Oggetto User con dati registrazione
     * @throws RuntimeException Se validazioni falliscono o duplicati trovati
     * 
     * Validation Priority Order:
     * 1. Email + Username duplicates = error specifico
     * 2. Email duplicate = error email-specific
     * 3. Username duplicate = error username-specific
     * 4. Password mismatch = UX error
     * 
     * Post-Registration Flow:
     * - Password encoding con BCrypt/configured algorithm
     * - Role assignment "USER" (default permission level)
     * - Database persistence transactional
     * - Welcome email dispatch (non-blocking failure)
     * 
     * Email Integration:
     * - Welcome email immediate dispatch
     * - Registration confirmation
     * - Onboarding guidance
     * - Non-blocking failure (registration success anche se email fails)
     */
    @Transactional
    public void save(User user) {
        logger.debug("Registering new user: username={}, email={}", user.getUsername(), user.getEmail());

        // Comprehensive duplicate validation
        if ((userRepository.findByEmail(user.getEmail()).isPresent()) && 
            (userRepository.findByUsername(user.getUsername()).isPresent())) {
            logger.error("Both email and username already exist: email={}, username={}", 
                    user.getEmail(), user.getUsername());
            throw new RuntimeException("Email e username già registrati");
        }
        
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.error("Email already exists: {}", user.getEmail());
            throw new RuntimeException("Email già registrata");
        }
        
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.error("Username already exists: {}", user.getUsername());
            throw new RuntimeException("Username già in uso");
        }
        
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            logger.error("Password confirmation mismatch for user: {}", user.getUsername());
            throw new RuntimeException("Le password non corrispondono");
        }

        // Security processing
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRolesString("USER"); // Default role assignment
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: id={}, username={}, email={}", 
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        // Welcome email dispatch (non-blocking)
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            logger.info("Welcome email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
            // Non-blocking: registration success anche con email failure
        }
    }

    /**
     * Trova utente per username con exception handling
     * 
     * @param username Username da cercare (case-sensitive)
     * @return User se trovato
     * @throws RuntimeException Se utente non trovato
     * 
     * Utilizzi:
     * - Authentication flows
     * - User profile loading
     * - Authorization checks
     * - Business logic che richiede user existence guarantee
     */
    @Transactional
    public User findByUsername(String username) {
        logger.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new RuntimeException("Utente non trovato con username: " + username);
                });
    }

    /**
     * Trova utente per ID con exception handling
     * 
     * @param id ID univoco utente
     * @return User se trovato
     * @throws RuntimeException Se utente non trovato
     * 
     * Use Cases:
     * - Profile management operations
     * - Admin operations su user specifico
     * - Relationship loading (foreign key operations)
     */
    @Transactional
    public User getUser(Long id) {
        logger.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new RuntimeException("Utente non trovato con ID: " + id);
                });
    }

    /**
     * Salva o aggiorna utente esistente
     * 
     * @param user Oggetto User da persistere
     * @return User salvato con ID generato/aggiornato
     * 
     * Transaction Scope:
     * - Insert per nuovi utenti (ID null)
     * - Update per utenti esistenti (ID present)
     * - Optimistic locking per concurrent updates
     */
    @Transactional
    public User saveUser(User user) {
        logger.debug("Saving user: id={}, username={}", user.getId(), user.getUsername());
        User savedUser = userRepository.save(user);
        logger.info("User saved: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    /**
     * Trova utente per email con exception handling
     * 
     * @param email Email da cercare (case-sensitive)
     * @return User se trovato
     * @throws RuntimeException Se utente non trovato
     * 
     * Use Cases:
     * - Password reset flows
     * - Email-based login alternative
     * - Contact/communication features
     */
    @Transactional
    public User findByEmail(String email) {
        logger.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found with email: {}", email);
                    return new RuntimeException("Utente non trovato con email: " + email);
                });
    }

    /**
     * Genera token sicuro per reset password con expiry
     * 
     * Security Features:
     * - UUID random token generation (cryptographically secure)
     * - 1-hour expiry window per security
     * - Single-use token (cleared dopo utilizzo)
     * - Database persistence per validation
     * 
     * @param email Email utente per reset password
     * @return Token generato per invio email
     * @throws RuntimeException Se utente con email non trovato
     * 
     * Reset Password Flow:
     * 1. User request reset via email
     * 2. Token generation e database storage
     * 3. Email dispatch con reset link
     * 4. Token validation su reset form submission
     * 5. Password change e token invalidation
     * 
     * Security Considerations:
     * - Token expiry per brute force mitigation
     * - Single email per token (no multiple active tokens)
     * - Token invalidation dopo successful reset
     */
    @Transactional
    public String generateResetToken(String email) {
        logger.debug("Generating reset token for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Cannot generate reset token, user not found with email: {}", email);
                    return new RuntimeException("Utente non trovato con email: " + email);
                });
        
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1-hour expiry window
        userRepository.save(user);
        
        logger.info("Reset token generated for user: id={}, email={}, expires_at={}", 
                user.getId(), email, user.getResetTokenExpiry());
        return token;
    }

    /**
     * Valida token reset password per expiry e existence
     * 
     * Validation Rules:
     * - Token must exist in database
     * - Associated user must exist
     * - Current time must be before expiry
     * - Token not already consumed/cleared
     * 
     * @param token Token da validare
     * @return true se token valido e non scaduto, false otherwise
     * 
     * Security Logic:
     * - Database lookup per token existence
     * - Expiry time comparison con current timestamp
     * - Null safety per token expiry field
     */
    @Transactional
    public boolean isResetTokenValid(String token) {
        logger.debug("Validating reset token: {}", token);
        
        User user = userRepository.findByResetToken(token);
        boolean isValid = user != null && 
                         user.getResetTokenExpiry() != null && 
                         LocalDateTime.now().isBefore(user.getResetTokenExpiry());
        
        logger.debug("Reset token validation result: token={}, valid={}", token, isValid);
        return isValid;
    }

    /**
     * Resetta password utente con token validation e cleanup
     * 
     * Security Validation:
     * - Token existence e expiry validation
     * - Current timestamp before expiry check
     * - Token consumption (one-time use)
     * 
     * @param token Token reset da validare
     * @param newPassword Nuova password in plaintext
     * @throws RuntimeException Se token non valido o scaduto
     * 
     * Reset Process:
     * 1. Token validation (existence + expiry)
     * 2. Password encoding con configured algorithm
     * 3. Token invalidation (null + expiry clear)
     * 4. User persistence con new password
     * 
     * Post-Reset State:
     * - Password aggiornata e encodata
     * - Reset token cleared (null)
     * - Reset expiry cleared (null)
     * - User può fare login con nuova password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        logger.debug("Resetting password with token: {}", token);
        
        User user = userRepository.findByResetToken(token);
        if (user != null && LocalDateTime.now().isBefore(user.getResetTokenExpiry())) {
            // Password reset processing
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null); // Token consumption
            user.setResetTokenExpiry(null); // Expiry cleanup
            userRepository.save(user);
            
            logger.info("Password reset successfully for user: id={}, email={}", 
                    user.getId(), user.getEmail());
        } else {
            logger.error("Invalid or expired reset token: {}", token);
            throw new RuntimeException("Token non valido o scaduto");
        }
    }

    /**
     * Attiva subscription dealer per utente con role upgrade
     * 
     * Business Logic Complex:
     * - User role upgrade da USER a DEALER
     * - UserSubscription instance creation con expiry calculation
     * - Auto-renewal enabled di default per convenience
     * - Email confirmation dispatch per transparency
     * 
     * @param userId ID dell'utente da sottoscrivere
     * @param subscriptionId ID del piano subscription da attivare
     * @return UserSubscription creata e attiva
     * @throws IllegalArgumentException Se utente o subscription non trovati
     * 
     * Subscription Activation Flow:
     * 1. User e Subscription loading con validation
     * 2. User.subscription assignment (current active)
     * 3. User role upgrade a "DEALER"
     * 4. UserSubscription instance creation
     * 5. Expiry date calculation (now + subscription.durationDays)
     * 6. Auto-renewal activation per user convenience
     * 7. Database persistence (User + UserSubscription)
     * 8. Email confirmation dispatch
     * 
     * Data Model Relations:
     * - User.subscription = current active subscription (foreign key)
     * - User.rolesString = permission level upgrade
     * - UserSubscription = instance con dates e auto-renewal settings
     * 
     * Email Integration:
     * - Subscription confirmation immediate dispatch
     * - Details: subscription name, dates, terms
     * - Non-blocking failure pattern
     */
    @Transactional
    public UserSubscription subscribeUserToDealer(Long userId, Long subscriptionId) {
        logger.debug("Subscribing user to dealer: userId={}, subscriptionId={}", userId, subscriptionId);
        
        // Entity loading con validation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found for subscription: userId={}", userId);
                    return new IllegalArgumentException("Utente non trovato");
                });
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> {
                    logger.error("Subscription not found: subscriptionId={}", subscriptionId);
                    return new IllegalArgumentException("Abbonamento non trovato");
                });

        // User upgrade processing
        user.setSubscription(subscription); // Current active subscription
        user.setRolesString("DEALER"); // Role upgrade per permissions

        // UserSubscription instance creation
        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setSubscription(subscription);
        userSubscription.setStartDate(LocalDate.now());
        LocalDate expiryDate = LocalDate.now().plusDays(subscription.getDurationDays());
        userSubscription.setExpiryDate(expiryDate);
        userSubscription.setActive(true); // Immediately active
        userSubscription.setAutoRenew(true); // Default convenience

        // Database persistence
        UserSubscription savedSubscription = userSubscriptionRepository.save(userSubscription);
        userRepository.save(user);
        
        logger.info("User subscribed to dealer: userId={}, subscriptionId={}, expiryDate={}", 
                userId, subscriptionId, expiryDate);

        // Email confirmation dispatch
        try {
            emailService.sendSubscriptionConfirmationEmail(
                    user.getEmail(),
                    user.getUsername(),
                    subscription.getName(),
                    userSubscription.getStartDate(),
                    userSubscription.getExpiryDate()
            );
            logger.info("Subscription confirmation email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send subscription confirmation email to {}: {}", 
                    user.getEmail(), e.getMessage());
            // Non-blocking: subscription success anche con email failure
        }
        
        return savedSubscription;
    }

    /**
     * Cancella subscription utente con role downgrade e cleanup
     * 
     * Authorization Validation:
     * - Solo owner della subscription può cancellarla
     * - User ID match con subscription.user.id
     * - Exception per unauthorized attempts
     * 
     * @param userSubscriptionId ID della subscription da cancellare
     * @param userId ID dell'utente richiedente (authorization)
     * @throws IllegalArgumentException Se subscription non trovata o non autorizzata
     * 
     * Cancellation Process Multi-Step:
     * 1. UserSubscription loading con validation
     * 2. Authorization check (subscription.user.id == userId)
     * 3. Subscription deactivation (active=false, autoRenew=false, expiryDate=now)
     * 4. Email notification dispatch
     * 5. Check remaining active subscriptions per user
     * 6. If no remaining active: role downgrade + dealer cleanup
     * 
     * Role Downgrade Logic:
     * - Se nessuna subscription attiva rimanente
     * - User.rolesString = "USER" (permission downgrade)
     * - User.subscription = null (clear current active)
     * - Dealer deletion cascade (se presente)
     * - Products deletion cascade (tutti i prodotti seller)
     * 
     * Business Impact:
     * - Loss of dealer privileges immediate
     * - Dealer profile deletion irreversible
     * - Products removal from catalog
     * - Revenue impact per business
     */
    @Transactional
    public void cancelSubscription(Long userSubscriptionId, Long userId) {
        logger.debug("Cancelling subscription: userSubscriptionId={}, userId={}", userSubscriptionId, userId);
        
        // Subscription loading con validation
        UserSubscription userSubscription = userSubscriptionRepository.findById(userSubscriptionId)
                .orElseThrow(() -> {
                    logger.error("UserSubscription not found: id={}", userSubscriptionId);
                    return new IllegalArgumentException("Abbonamento non trovato");
                });

        // Authorization validation
        if (!userSubscription.getUser().getId().equals(userId)) {
            logger.error("Unauthorized subscription cancellation: userSubscriptionId={}, userId={}, ownerId={}", 
                    userSubscriptionId, userId, userSubscription.getUser().getId());
            throw new IllegalArgumentException("Non autorizzato a cancellare questo abbonamento");
        }

        // Subscription deactivation
        userSubscription.setAutoRenew(false); // Prevent future renewals
        userSubscription.setActive(false); // Immediate deactivation
        userSubscription.setExpiryDate(LocalDate.now()); // Immediate expiry
        userSubscriptionRepository.save(userSubscription);
        
        logger.info("Subscription cancelled: id={}, userId={}", userSubscriptionId, userId);

        // Email notification dispatch
        try {
            emailService.sendSubscriptionCancellationEmail(
                    userSubscription.getUser().getEmail(),
                    userSubscription.getUser().getUsername(),
                    userSubscription.getSubscription().getName()
            );
            logger.info("Subscription cancellation email sent to: {}", userSubscription.getUser().getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send subscription cancellation email to {}: {}", 
                    userSubscription.getUser().getEmail(), e.getMessage());
        }

        // Role downgrade logic
        User user = userSubscription.getUser();
        List<UserSubscription> remainingActive = userSubscriptionRepository.findByUserAndActive(user, true);
        
        if (remainingActive.isEmpty()) {
            logger.info("No remaining active subscriptions for user {}, performing role downgrade", userId);
            
            // Role and subscription clearance
            user.setRolesString("USER");
            user.setSubscription(null);
            userRepository.save(user);
            
            // Dealer cleanup cascade
            dealerRepository.findByOwner(user).ifPresent(dealer -> {
                try {
                    dealerService.deleteDealer(dealer.getId());
                    logger.info("Dealer deleted for user: userId={}, dealerId={}", userId, dealer.getId());
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to delete dealer for user {}: {}", userId, e.getMessage());
                }
            });

            // Products cleanup
            List<Product> products = productRepository.findBySeller(user);
            productRepository.deleteAll(products);
            logger.info("Deleted {} products for user {}", products.size(), userId);
        } else {
            logger.info("User {} still has {} active subscriptions", userId, remainingActive.size());
        }
    }

    /**
     * Recupera subscription attive non scadute per utente
     * 
     * Business Logic Filtering:
     * - Database query per active=true
     * - In-memory filtering per !isExpired()
     * - Double validation per data consistency
     * 
     * @param userId ID dell'utente per cui cercare subscription attive
     * @return Lista UserSubscription attive e non scadute
     * @throws IllegalArgumentException Se utente non trovato
     * 
     * Filtering Strategy:
     * 1. Database pre-filter: active=true
     * 2. Application filter: !isExpired() per current date check
     * 3. Stream processing per functional approach
     * 
     * Use Cases:
     * - User dashboard subscription status
     * - Authorization checks per dealer features
     * - Billing e renewal notifications
     */
    @Transactional
    public List<UserSubscription> getActiveSubscriptions(Long userId) {
        logger.debug("Getting active subscriptions for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId);
                    return new IllegalArgumentException("Utente non trovato");
                });
        
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserAndActive(user, true)
                .stream()
                .filter(userSubscription -> !userSubscription.isExpired()) // Additional expiry check
                .collect(Collectors.toList());
        
        logger.debug("Found {} active non-expired subscriptions for user {}", activeSubscriptions.size(), userId);
        return activeSubscriptions;
    }

    /**
     * Recupera tutti i piani subscription disponibili per acquisto
     * 
     * @return Lista completa Subscription dal catalogo
     * 
     * Use Cases:
     * - Subscription selection UI
     * - Pricing page popolamento
     * - Admin configuration interface
     */
    @Transactional
    public List<Subscription> getAvailableSubscriptions() {
        logger.debug("Getting all available subscriptions");
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        logger.debug("Found {} available subscriptions", subscriptions.size());
        return subscriptions;
    }

    /**
     * Aggiorna ruolo utente con validation e persistence
     * 
     * @param user Oggetto User da aggiornare
     * @param newRole Nuovo ruolo da assegnare (USER, DEALER, PRIVATE)
     * 
     * Role Management:
     * - Role string assignment senza validation (flexible)
     * - Database persistence immediate
     * - No cascade operations (manual role management)
     * 
     * Use Cases:
     * - Admin role management
     * - Upgrade/downgrade flows
     * - Permission level adjustments
     */
    @Transactional
    public void updateUserRole(User user, String newRole) {
        logger.debug("Updating user role: userId={}, currentRole={}, newRole={}", 
                user.getId(), user.getRolesString(), newRole);
        
        user.setRolesString(newRole);
        userRepository.save(user);
        
        logger.info("User role updated: userId={}, newRole={}", user.getId(), newRole);
    }

    /**
     * Rimuove ruolo PRIVATE e elimina prodotti associati
     * 
     * Business Rules Validation:
     * - User deve avere ruolo PRIVATE corrente
     * - Exception se role check fallisce
     * - Cascade deletion prodotti seller
     * - Role downgrade a USER
     * 
     * @param user Oggetto User da cui rimuovere ruolo PRIVATE
     * @throws IllegalStateException Se utente non ha ruolo PRIVATE
     * 
     * PRIVATE Role Removal Process:
     * 1. Current role validation (must contain "PRIVATE")
     * 2. All user products deletion (cascade cleanup)
     * 3. Role downgrade a "USER"
     * 4. Database persistence
     * 
     * Business Impact:
     * - Loss of private selling privileges
     * - All products removed from catalog
     * - Revenue impact per seller
     * - Irreversible operation (data loss)
     */
    @Transactional
    public void removePrivateRoleAndCar(User user) {
        logger.debug("Removing PRIVATE role and cars for user: userId={}, currentRole={}", 
                user.getId(), user.getRolesString());
        
        // Role validation
        if (!user.getRolesString().contains("PRIVATE")) {
            logger.error("User does not have PRIVATE role: userId={}, role={}", 
                    user.getId(), user.getRolesString());
            throw new IllegalStateException("L'utente non ha il ruolo PRIVATO.");
        }

        // Products cascade deletion
        List<Product> car = productRepository.findBySeller(user);
        productRepository.deleteAll(car);
        logger.info("Deleted {} products for PRIVATE user: userId={}", car.size(), user.getId());
        
        // Role downgrade
        user.setRolesString("USER");
        userRepository.save(user);
        
        logger.info("PRIVATE role removed for user: userId={}", user.getId());
    }

    /**
     * Elimina account utente con password validation e cascade cleanup completo
     * 
     * ATTENZIONE: Operazione irreversibile con data loss esteso
     * - GDPR compliance per right to be forgotten
     * - Password validation per security
     * - Cascade cleanup di TUTTE le entity associate
     * - Email confirmation per transparency
     * 
     * @param user Oggetto User da eliminare
     * @param password Password corrente per validation security
     * @throws IllegalArgumentException Se password non corrisponde
     * 
     * Security Validation:
     * - Password match con encoded password database
     * - PasswordEncoder.matches() per sicurezza
     * - Exception immediate se validation fallisce
     * 
     * Deletion Process Multi-Phase (order important):
     * 1. Password validation security check
     * 2. UserSubscriptions cascade deletion
     * 3. AccountInformation deletion (profile data)
     * 4. Dealer deletion via service delegation (cascade products)
     * 5. Remaining products deletion (non-dealer products)
     * 6. QuoteRequests deletion (conversations)
     * 7. Payments deletion (transaction history)
     * 8. CartItems deletion (shopping cart)
     * 9. User entity deletion (final)
     * 10. Account deletion email dispatch
     * 
     * GDPR Compliance:
     * - Complete data removal per right to be forgotten
     * - Audit trail per compliance documentation
     * - Email confirmation per transparency
     * - Irreversible process per regulation requirements
     * 
     * Error Handling:
     * - Partial failure tolerance (warn + continue)
     * - Critical failures = exception propagation
     * - Detailed logging per troubleshooting
     */
    @Transactional
    public void deleteUser(User user, String password) {
        logger.info("Attempting to delete user: userId={}, username={}", user.getId(), user.getUsername());
        
        // Security validation
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Invalid password for user deletion: userId={}", user.getId());
            throw new IllegalArgumentException("Password non corretta");
        }

        // Phase 1: UserSubscriptions cascade deletion
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(user.getId());
        userSubscriptionRepository.deleteAll(subscriptions);
        logger.info("Deleted {} subscriptions for user {}", subscriptions.size(), user.getId());

        // Phase 2: AccountInformation deletion
        accountInformationRepository.findByUser(user).ifPresent(accountInfo -> {
            accountInformationRepository.delete(accountInfo);
            logger.info("Deleted account information for user {}", user.getId());
        });

        // Phase 3: Dealer deletion via service delegation (cascade products)
        dealerRepository.findByOwner(user).ifPresent(dealer -> {
            try {
                dealerService.deleteDealer(dealer.getId());
                logger.info("Deleted dealer for user: userId={}, dealerId={}", user.getId(), dealer.getId());
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to delete dealer for user {}: {}", user.getId(), e.getMessage());
            }
        });

        // Phase 4: Remaining products deletion (non-dealer products)
        List<Product> products = productRepository.findBySeller(user);
        productRepository.deleteAll(products);
        logger.info("Deleted {} remaining products for user {}", products.size(), user.getId());

        // Phase 5: QuoteRequests deletion (conversations)
        List<QuoteRequest> quoteRequests = quoteRequestRepository.findByUserId(user.getId());
        quoteRequestRepository.deleteAll(quoteRequests);
        logger.info("Deleted {} quote requests for user {}", quoteRequests.size(), user.getId());

        // Phase 6: Payments deletion (transaction history)
        List<Payment> payments = paymentRepository.findByUser(user);
        paymentRepository.deleteAll(payments);
        logger.info("Deleted {} payments for user {}", payments.size(), user.getId());

        // Phase 7: CartItems deletion (shopping cart)
        List<CartItem> items = cartItemRepository.findByUser(user);
        cartItemRepository.deleteAll(items);
        logger.info("Deleted {} cart items for user {}", items.size(), user.getId());

        // Phase 8: User entity deletion (final)
        userRepository.delete(user);

        // Phase 9: Account deletion email confirmation
        try {
            emailService.sendAccountDeletionEmail(user.getEmail(), user.getUsername());
            logger.info("Sent account deletion email to {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Failed to send account deletion email to {}: {}", user.getEmail(), e.getMessage());
        }

        logger.info("User {} deleted successfully", user.getId());
    }

    /**
     * Job schedulato per rinnovo automatico subscription e cleanup scadute
     * 
     * Scheduling Configuration:
     * - Cron: "0 0 0 * * ?" = Daily execution alle 00:00
     * - @Scheduled annotation per Spring scheduling
     * - @Transactional per consistency batch operations
     * 
     * Business Logic Complex:
     * - Trova subscription che scadono oggi
     * - Auto-renewal processing per subscription abilitate
     * - Deactivation per subscription senza auto-renewal
     * - Role downgrade per users senza subscription attive
     * - Email notifications per transparency
     * - Dealer cleanup cascade per users downgraded
     * 
     * Performance Considerations:
     * - Batch processing per efficiency
     * - saveAndFlush() per intermediate commits
     * - Distinct user collection per role checking
     * - Exception tolerance per individual failures
     * 
     * Process Flow:
     * 1. Find expiring subscriptions (active + expiryDate = today)
     * 2. For each subscription: auto-renewal OR deactivation
     * 3. Email notifications per subscription action
     * 4. Collect affected users (distinct)
     * 5. For each user: check remaining active subscriptions
     * 6. If no active remaining: role downgrade + dealer cleanup
     * 7. Email notifications per role changes
     * 
     * Auto-Renewal Logic:
     * - subscription.renew() = extend expiryDate
     * - Keep active=true e autoRenew=true
     * - Email confirmation per renewal
     * 
     * Deactivation Logic:
     * - active=false (immediate effect)
     * - Email notification per expiry
     * 
     * Role Downgrade Cascade:
     * - User.rolesString = "USER"
     * - User.subscription = null
     * - Dealer deletion via service delegation
     * - Products deletion per role loss
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily execution at midnight
    @Transactional
    public void renewSubscriptions() {
        logger.info("START: Processing subscriptions at {}", LocalDateTime.now());
        
        // Phase 1: Find expiring subscriptions
        List<UserSubscription> expiringSubscriptions = userSubscriptionRepository.findByActiveAndExpiryDate(true, LocalDate.now());
        logger.info("Found {} subscriptions expiring today", expiringSubscriptions.size());

        // Phase 2: Process each expiring subscription
        for (UserSubscription subscription : expiringSubscriptions) {
            User user = subscription.getUser();
            logger.info("Processing subscription ID: {}, userId: {}, expiryDate: {}, autoRenew: {}",
                    subscription.getId(), user.getId(), subscription.getExpiryDate(), subscription.isAutoRenew());

            if (subscription.isAutoRenew()) {
                // Auto-renewal processing
                logger.info("Renewing subscription ID: {}", subscription.getId());
                subscription.renew(); // Extend expiry date
                userSubscriptionRepository.saveAndFlush(subscription);
                logger.info("Subscription ID: {} renewed until {}", subscription.getId(), subscription.getExpiryDate());

                // Renewal email notification
                try {
                    emailService.sendSubscriptionRenewalEmail(
                            user.getEmail(),
                            user.getUsername(),
                            subscription.getSubscription().getName(),
                            subscription.getExpiryDate()
                    );
                    logger.info("Sent renewal email to {}", user.getEmail());
                } catch (MessagingException e) {
                    logger.error("Failed to send renewal email to {}: {}", user.getEmail(), e.getMessage());
                }
            } else {
                // Deactivation processing
                logger.info("Deactivating expired subscription ID: {}", subscription.getId());
                subscription.setActive(false);
                userSubscriptionRepository.saveAndFlush(subscription);
                logger.info("Subscription ID: {} set to inactive", subscription.getId());

                // Expiry email notification
                try {
                    emailService.sendSubscriptionCancellationEmail(
                            user.getEmail(),
                            user.getUsername(),
                            subscription.getSubscription().getName()
                    );
                    logger.info("Sent expiration email to {}", user.getEmail());
                } catch (MessagingException e) {
                    logger.error("Failed to send expiration email to {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }

        // Phase 3: Role downgrade processing per affected users
        List<User> usersToCheck = expiringSubscriptions.stream()
                .map(UserSubscription::getUser)
                .distinct() // Avoid duplicate processing
                .collect(Collectors.toList());
        logger.info("Checking {} users for active subscriptions", usersToCheck.size());

        for (User user : usersToCheck) {
            List<UserSubscription> remainingActive = userSubscriptionRepository.findByUserAndActive(user, true);
            logger.info("User {} has {} active subscriptions", user.getId(), remainingActive.size());
            
            if (remainingActive.isEmpty()) {
                // Role downgrade cascade processing
                logger.info("No active subscriptions for user {}, reverting to USER role", user.getId());
                user.setRolesString("USER");
                user.setSubscription(null);
                userRepository.saveAndFlush(user);
                logger.info("User {} role updated to USER and subscription cleared", user.getId());

                // Dealer cleanup cascade
                dealerRepository.findByOwner(user).ifPresent(dealer -> {
                    try {
                        dealerService.deleteDealer(dealer.getId());
                        logger.info("Deleted dealer for user {}", user.getId());
                    } catch (IllegalArgumentException e) {
                        logger.warn("Failed to delete dealer for user {}: {}", user.getId(), e.getMessage());
                    }
                });

                // Products cleanup
                List<Product> products = productRepository.findBySeller(user);
                productRepository.deleteAll(products);
                logger.info("Deleted {} products for user {}", products.size(), user.getId());
            } else {
                logger.info("User {} still has active subscriptions", user.getId());
            }
        }

        logger.info("END: Finished processing subscriptions at {}", LocalDateTime.now());
    }
}