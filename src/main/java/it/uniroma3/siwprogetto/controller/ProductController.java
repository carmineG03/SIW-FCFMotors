package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manutenzione")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @GetMapping("/prodotti")
    public String showMaintenancePage(Model model) {
        model.addAttribute("products", productService.findAll());

        // Aggiungi lo stato di autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Aggiungi il conteggio del carrello
        //model.addAttribute("cartCount", cartService.getCartItems().size());

        return "maintenance";
    }
}