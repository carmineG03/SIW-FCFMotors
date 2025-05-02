package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @GetMapping("/cart")
    public String showCartPage(Model model, Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        if (!isAuthenticated || principal == null) {
            model.addAttribute("cartItems", List.of());
            model.addAttribute("cartCount", 0);
            model.addAttribute("subtotal", 0.0);
        } else {
            User user = userService.findByUsername(principal.getName());
            List<CartItem> cartItems = cartService.getCartItems(user);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartCount", cartItems.size());
            model.addAttribute("subtotal", cartService.calculateSubtotal(user));
        }

        return "cart";
    }

    @PostMapping("/cart/add-subscription")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSubscriptionToCart(@RequestParam Long subscriptionId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            cartService.addSubscriptionToCart(subscriptionId, user);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/cart/update/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(@PathVariable Long itemId, @RequestBody Map<String, Integer> body, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            int quantity = body.get("quantity");
            cartService.updateQuantity(itemId, quantity, user);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/cart/remove/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Long itemId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            cartService.removeFromCart(itemId, user);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/cart/checkout-subscriptions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkoutSubscriptions(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            cartService.checkoutSubscriptions(user);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}