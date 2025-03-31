package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void save(User user) {
        if ((userRepository.findByEmail(user.getEmail()) != null) || userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Email e username già registrati");
        }
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email già registrata");
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username già in uso");
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            throw new RuntimeException("Le password non corrispondono");
        }

        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        // Imposta un ruolo di default
        user.setRolesString("USER");
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String generateResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token valido per 1 ora
            userRepository.save(user);
            return token;
        }
        throw new RuntimeException("User not found");
    }

    public boolean isResetTokenValid(String token) {
        User user = userRepository.findByResetToken(token);
        return user != null && user.getResetTokenExpiry() != null && LocalDateTime.now().isBefore(user.getResetTokenExpiry());
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token);
        if (user != null && LocalDateTime.now().isBefore(user.getResetTokenExpiry())) {
            user.setPassword(bCryptPasswordEncoder.encode(newPassword));
            user.setResetToken(null); // Invalida il token
            user.setResetTokenExpiry(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid or expired token");
        }
    }
}