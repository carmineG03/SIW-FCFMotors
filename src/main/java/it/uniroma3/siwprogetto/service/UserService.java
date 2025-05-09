package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import it.uniroma3.siwprogetto.repository.*;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailService emailService;


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

        // Cambia il ruolo a DEALER
        user.setRolesString("DEALER");

        // Crea una nuova sottoscrizione attiva
        UserSubscription userSubscription = new UserSubscription();
        userSubscription.setUser(user);
        userSubscription.setSubscription(subscription);
        userSubscription.setStartDate(LocalDate.now());
        userSubscription.setExpiryDate(LocalDate.now().plusDays(subscription.getDurationDays()));

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
        return userSubscriptionRepository.findByUserId(userId);
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
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Password non corretta");
        }

        // Invia mail di conferma cancellazione
        try {
            emailService.sendAccountDeletionEmail(user.getEmail(), user.getUsername());
        } catch (MessagingException e) {
            System.err.println("Errore durante l'invio della mail di cancellazione: " + e.getMessage());
        }

        // Cancella le informazioni aggiuntive
        accountInformationRepository.findByUser(user).ifPresent(accountInformationRepository::delete);

        // Resto del codice come sopra
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(user.getId());
        userSubscriptionRepository.deleteAll(subscriptions);
        List<Product> products = productRepository.findBySeller(user);
        productRepository.deleteAll(products);
        dealerRepository.findByOwner(user).ifPresent(dealer -> {
            try {
                dealerService.deleteDealer(dealer.getId());
            } catch (IllegalArgumentException ignored) {
            }
        });
        userRepository.delete(user);
    }

}