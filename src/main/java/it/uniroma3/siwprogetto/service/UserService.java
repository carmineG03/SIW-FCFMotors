    package it.uniroma3.siwprogetto.service;
    
    import it.uniroma3.siwprogetto.model.*;
    import it.uniroma3.siwprogetto.repository.*;
    import jakarta.mail.MessagingException;
    import jakarta.transaction.Transactional;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.stereotype.Service;
    
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;
    import java.util.stream.Collectors;

    @Service
    public class UserService {
        private final UserRepository userRepository;
        private final BCryptPasswordEncoder bCryptPasswordEncoder;
        private final EmailService emailService;
        private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    
        @Autowired
        private SubscriptionRepository subscriptionRepository;
    
        @Autowired
        private UserSubscriptionRepository userSubscriptionRepository;
    
        @Autowired
        private ProductRepository productRepository;
    
        @Autowired
        private DealerRepository dealerRepository;
    
        @Autowired
        private DealerService dealerService;
    
        @Autowired
        private AccountInformationRepository accountInformationRepository;

        @Autowired
        private QuoteRequestRepository quoteRequestRepository;
    
        public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, EmailService emailService) {
            this.userRepository = userRepository;
            this.bCryptPasswordEncoder = bCryptPasswordEncoder;
            this.emailService = emailService;
        }
    
        public void save(User user) {
            if ((userRepository.findByEmail(user.getEmail()).isPresent()) && (userRepository.findByUsername(user.getUsername()).isPresent())) {
                throw new RuntimeException("Email e username già registrati");
            }
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("Email già registrata");
            }
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                throw new RuntimeException("Username già in uso");
            }
            if (!user.getPassword().equals(user.getConfirmPassword())) {
                throw new RuntimeException("Le password non corrispondono");
            }
    
            user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
            user.setRolesString("USER");
            userRepository.save(user);
    
            // Invia mail di benvenuto
            try {
                emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            } catch (MessagingException e) {
                // Log dell'errore, ma non interrompere la registrazione
                System.err.println("Errore durante l'invio della mail di benvenuto: " + e.getMessage());
            }
        }
    
        public User findByUsername(String username) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato con username: " + username));
        }
    
        public User getUser(Long id) {
            return userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + id));
        }
    
        public User saveUser(User user) {
            return userRepository.save(user);
        }
    
        public User findByEmail(String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato con email: " + email));
        }
    
        public String generateResetToken(String email) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato con email: " + email));
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            return token;
        }
    
        public boolean isResetTokenValid(String token) {
            User user = userRepository.findByResetToken(token);
            return user != null && user.getResetTokenExpiry() != null && LocalDateTime.now().isBefore(user.getResetTokenExpiry());
        }
    
        public void resetPassword(String token, String newPassword) {
            User user = userRepository.findByResetToken(token);
            if (user != null && LocalDateTime.now().isBefore(user.getResetTokenExpiry())) {
                user.setPassword(bCryptPasswordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Token non valido o scaduto");
            }
        }
    
        public void subscribeUserToDealer(Long userId, Long subscriptionId) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

            user.setSubscription(subscription);
            // Cambia il ruolo a DEALER
            user.setRolesString("DEALER");
    
            // Crea una nuova sottoscrizione attiva
            UserSubscription userSubscription = new UserSubscription();
            userSubscription.setUser(user);
            userSubscription.setSubscription(subscription);
            userSubscription.setStartDate(LocalDate.now());
            LocalDate expiryDate = LocalDate.now().plusDays(subscription.getDurationDays());
            userSubscription.setExpiryDate(expiryDate);
            userSubscription.setActive(true);
    
            // Salva le modifiche
            userSubscriptionRepository.save(userSubscription);
            userRepository.save(user);
    
            // Invia mail di conferma sottoscrizione
            try {
                emailService.sendSubscriptionConfirmationEmail(
                        user.getEmail(),
                        user.getUsername(),
                        subscription.getName(),
                        userSubscription.getStartDate(),
                        userSubscription.getExpiryDate()
                );
            } catch (MessagingException e) {
                System.err.println("Errore durante l'invio della mail di conferma sottoscrizione: " + e.getMessage());
            }
        }
    
        public void cancelSubscription(Long userSubscriptionId, Long userId) {
            UserSubscription userSubscription = userSubscriptionRepository.findById(userSubscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));
    
            if (!userSubscription.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Non autorizzato a cancellare questo abbonamento");
            }
    
            // Invia mail di conferma cancellazione abbonamento
            try {
                emailService.sendSubscriptionCancellationEmail(
                        userSubscription.getUser().getEmail(),
                        userSubscription.getUser().getUsername(),
                        userSubscription.getSubscription().getName()
                );
            } catch (MessagingException e) {
                System.err.println("Errore durante l'invio della mail di cancellazione abbonamento: " + e.getMessage());
            }
    
            // Cancella l'abbonamento
            userSubscriptionRepository.delete(userSubscription);
    
            // Aggiorna il ruolo dell'utente a USER
            User user = userSubscription.getUser();
            user.setRolesString("USER");
            userRepository.save(user);
    
            dealerRepository.findByOwner(user).ifPresent(dealer -> {
                try {
                    dealerService.deleteDealer(dealer.getId());
                } catch (IllegalArgumentException ignored) {
                    // Ignora l'eccezione se il dealer non esiste
                }
            });
    
            // Cancella tutti i prodotti creati dall'utente con ruolo DEALER
            List<Product> products = productRepository.findBySeller(user);
            productRepository.deleteAll(products);
        }

        public List<UserSubscription> getActiveSubscriptions(Long userId) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
            return userSubscriptionRepository.findByUserAndActive(user, true)
                    .stream()
                    .filter(userSubscription -> !userSubscription.isExpired())
                    .collect(Collectors.toList());
        }

    
        public List<Subscription> getAvailableSubscriptions() {
            return subscriptionRepository.findAll();
        }
    
        public void updateUserRole(User user, String newRole) {
            user.setRolesString(newRole);
            userRepository.save(user);
        }
    
        public void removePrivateRoleAndCar(User user) {
            // Verifica che l'utente abbia il ruolo PRIVATE
            if (!user.getRolesString().contains("PRIVATE")) {
                throw new IllegalStateException("L'utente non ha il ruolo PRIVATO.");
            }
    
            // Trova l'auto associata all'utente (assumiamo una sola auto per utente privato)
            List<Product> car = productRepository.findBySeller(user);
    
            // Cancella l'auto
            productRepository.deleteAll(car);
    
            // Aggiorna il ruolo a USER
            user.setRolesString("USER");
            userRepository.save(user);
        }

        public void deleteUser(User user, String password) {
            logger.info("Attempting to delete user: {}", user.getId());
            if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("Password non corretta");
            }

            // Check for active subscriptions
            List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserAndActive(user, true);
            if (!activeSubscriptions.isEmpty()) {
                throw new IllegalArgumentException("Impossibile eliminare l'account: hai abbonamenti attivi. Cancella prima gli abbonamenti.");
            }

            // Delete related data
            List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(user.getId());
            userSubscriptionRepository.deleteAll(subscriptions);

            accountInformationRepository.findByUser(user).ifPresent(accountInformationRepository::delete);

            dealerRepository.findByOwner(user).ifPresent(dealer -> {
                try {
                    dealerService.deleteDealer(dealer.getId());
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to delete dealer for user {}: {}", user.getId(), e.getMessage());
                }
            });

            List<Product> products = productRepository.findBySeller(user);
            productRepository.deleteAll(products);

            // Delete quote requests
            List<QuoteRequest> quoteRequests = quoteRequestRepository.findByUserId(user.getId());
            quoteRequestRepository.deleteAll(quoteRequests);
            logger.info("Deleted {} quote requests for user {}", quoteRequests.size(), user.getId());

            // Delete user
            userRepository.delete(user);

            // Send confirmation email
            try {
                emailService.sendAccountDeletionEmail(user.getEmail(), user.getUsername());
                logger.info("Sent account deletion email to {}", user.getEmail());
            } catch (MessagingException e) {
                logger.error("Failed to send account deletion email to {}: {}", user.getEmail(), e.getMessage());
            }

            logger.info("User {} deleted successfully", user.getId());
        }
        @Scheduled(cron = "0 * * * * ?")
        @Transactional
        public void checkAndDeactivateExpiredSubscriptions() {
            logger.info("START: Checking for expired subscriptions at {}", LocalDateTime.now());
            List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByActive(true);
            logger.info("Found {} active subscriptions", activeSubscriptions.size());

            for (UserSubscription userSubscription : activeSubscriptions) {
                boolean isExpired = userSubscription.isExpired();
                logger.info("Checking subscription ID: {}, userId: {}, expiryDate: {}, isExpired: {}, active: {}",
                        userSubscription.getId(), userSubscription.getUser().getId(),
                        userSubscription.getExpiryDate(), isExpired, userSubscription.isActive());
                if (isExpired) {
                    logger.info("Deactivating subscription ID: {}", userSubscription.getId());
                    userSubscription.setActive(false);
                    userSubscriptionRepository.saveAndFlush(userSubscription);
                    logger.info("Subscription ID: {} set to inactive", userSubscription.getId());

                    // Clear subscription from User if no active subscriptions remain
                    User user = userSubscription.getUser();
                    List<UserSubscription> remainingActive = userSubscriptionRepository.findByUserAndActive(user, true);
                    if (remainingActive.isEmpty()) {
                        user.setSubscription(null);
                        user.setRolesString("USER");
                        userRepository.saveAndFlush(user);
                        logger.info("Cleared subscription and set USER role for user {}", user.getId());
                    }

                    try {
                        emailService.sendSubscriptionCancellationEmail(
                                userSubscription.getUser().getEmail(),
                                userSubscription.getUser().getUsername(),
                                userSubscription.getSubscription().getName()
                        );
                        logger.info("Sent expiration email to {}", userSubscription.getUser().getEmail());
                    } catch (MessagingException e) {
                        logger.error("Failed to send expiration email to {}: {}", userSubscription.getUser().getEmail(), e.getMessage());
                    }
                } else {
                    logger.info("Subscription ID: {} is still active", userSubscription.getId());
                }
            }

            // Check users with no active subscriptions
            List<User> usersToCheck = activeSubscriptions.stream()
                    .map(UserSubscription::getUser)
                    .distinct()
                    .collect(Collectors.toList());
            logger.info("Checking {} users for active subscriptions", usersToCheck.size());

            for (User user : usersToCheck) {
                List<UserSubscription> remainingActive = userSubscriptionRepository.findByUserAndActive(user, true);
                logger.info("User {} has {} active subscriptions", user.getId(), remainingActive.size());
                if (remainingActive.isEmpty()) {
                    logger.info("No active subscriptions for user {}, reverting to USER role", user.getId());
                    user.setRolesString("USER");
                    user.setSubscription(null);
                    userRepository.saveAndFlush(user);
                    logger.info("User {} role updated to USER and subscription cleared", user.getId());

                    dealerRepository.findByOwner(user).ifPresent(dealer -> {
                        try {
                            dealerService.deleteDealer(dealer.getId());
                            logger.info("Deleted dealer for user {}", user.getId());
                        } catch (IllegalArgumentException e) {
                            logger.warn("Failed to delete dealer for user {}: {}", user.getId(), e.getMessage());
                        }
                    });

                    List<Product> products = productRepository.findBySeller(user);
                    productRepository.deleteAll(products);
                    logger.info("Deleted {} products for user {}", products.size(), user.getId());
                } else {
                    logger.info("User {} still has active subscriptions: {}", user.getId(), remainingActive);
                }
            }

            logger.info("END: Finished checking expired subscriptions at {}", LocalDateTime.now());
        }
    
    }