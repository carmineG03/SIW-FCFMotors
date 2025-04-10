package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.CartItem;
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

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/cart")
    public String showCartPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        List<CartItem> cartItems = cartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartCount", cartItems.size());
        model.addAttribute("subtotal", cartService.calculateSubtotal());
        model.addAttribute("total", cartService.calculateTotal());
        model.addAttribute("suggestedProducts", cartService.getSuggestedProducts());
        model.addAttribute("savedItems", cartService.getSavedItems());

        return "cart";
    }

    @PostMapping("/cart/add/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(@PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.addToCart(productId);
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

    @PostMapping("/cart/save-for-later/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveForLater(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.saveForLater(itemId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/cart/add-back/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addBackToCart(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            cartService.addBackToCart(itemId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}