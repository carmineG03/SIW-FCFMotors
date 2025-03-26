package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class AccountController {

    @Autowired
    private UserService userService;

    @GetMapping("/account")
    public String showAccount(Model model, Principal principal) {
        if (principal == null) {
            // Se l'utente non è autenticato, redirigi alla pagina di login
            return "redirect:/login";
        }

        // Recupera l'utente dal servizio usando l'email (principal.getName())
        User user = userService.findByUsername(principal.getName());

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            model.addAttribute("error", "User not found");
        }
        return "account"; // La vista "account"
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        model.addAttribute("isAuthenticated", principal != null);
        return "index"; //Verifica se l'user è autenticato
    }

}
