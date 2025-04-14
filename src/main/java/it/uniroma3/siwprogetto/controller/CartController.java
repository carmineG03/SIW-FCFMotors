package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import it.uniroma3.siwprogetto.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.uniroma3.siwprogetto.model.Subscription;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @GetMapping("/cart")
    public String showCartPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        List<CartItem> cartItems = cartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartCount", cartItems.size());
        model.addAttribute("subtotal", cartService.calculateSubtotal());

        return "cart";
    }

    @GetMapping("/add-subscription/{productId}")
    public String showAddSubscriptionPage(@PathVariable Long productId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<CartItem> cartItems = cartService.getCartItems();

        model.addAttribute("product", product);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("cartCount", cartItems.size());

        return "add-subscription";
    }

    @PostMapping("/cart/add-subscription/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSubscriptionToCart(@PathVariable Long productId, @RequestParam Long subscriptionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.addSubscriptionToCart(productId, subscriptionId);
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
    public ResponseEntity<Map<String, Object>> updateQuantity(@PathVariable Long itemId, @RequestBody Map<String, Integer> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            int quantity = body.get("quantity");
            cartService.updateQuantity(itemId, quantity);
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
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.removeFromCart(itemId);
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
    public ResponseEntity<Map<String, Object>> checkoutSubscriptions() {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.checkoutSubscriptions();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}