package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import it.uniroma3.siwprogetto.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


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

    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
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
    }

    public void cancelSubscription(Long userSubscriptionId, Long userId) {
        UserSubscription userSubscription = userSubscriptionRepository.findById(userSubscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

        if (!userSubscription.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Non autorizzato a cancellare questo abbonamento");
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
}