package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    // Questo metodo serve per salvare un utente
    public void save(User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    // Questo metodo serve per trovare un utente dato il suo username
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Questo metodo serve per trovare un utente dato il suo id
    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // Questo metodo serve per salvare un utente
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // Questo metodo serve per trovare un utente data la sua email
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}