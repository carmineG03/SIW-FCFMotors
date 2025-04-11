package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private CartService cartService;

    @GetMapping("/user-login")
    public String login(Model model) {
        // Aggiungi lo stato di autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Aggiungi il conteggio del carrello
        model.addAttribute("cartCount", cartService.getCartItems().size());

        return "login";
    }

    @GetMapping("/user-register")
    public String userRegister(Model model) {
        // Aggiungi lo stato di autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Aggiungi il conteggio del carrello
        model.addAttribute("cartCount", cartService.getCartItems().size());

        return "register";
    }
}