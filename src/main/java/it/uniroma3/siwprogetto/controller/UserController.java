package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controller per la gestione degli utenti
 * Gestisce registrazione e login degli utenti normali (non dealer)
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
public class UserController {
    
    @Autowired
    private UserService userService;

    /**
     * Mostra il form di registrazione
     * @param model Model per passare dati alla view
     * @return template di registrazione con form vuoto
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Crea un nuovo oggetto User vuoto per il binding del form
        model.addAttribute("user", new User());
        return "register";
    }

    /**
     * Gestisce la registrazione di un nuovo utente
     * @param user Dati utente dal form di registrazione
     * @param model Model per passare messaggi di errore
     * @return redirect a login se successo, altrimenti form registrazione con errore
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            // Tenta di salvare il nuovo utente
            userService.save(user);
            // Se successo, reindirizza alla pagina di login
            return "redirect:/login";
        } catch (RuntimeException e) {
            // Se errore (es. username già esistente), mostra messaggio d'errore
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    /**
     * Mostra form di login alternativo per utenti
     * Evita conflitti con LoginController principale
     * @return template di login
     */
    @GetMapping("/user/login")
    public String showUserLoginForm() {
        return "login";
    }
}