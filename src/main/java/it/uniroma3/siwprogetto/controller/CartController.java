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

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            model.addAttribute("originalTotal", BigDecimal.ZERO);
            model.addAttribute("discountedTotal", BigDecimal.ZERO);
            model.addAttribute("discountedPrices", new HashMap<Long, BigDecimal>());
        } else {
            User user = userService.findByUsername(principal.getName());
            List<CartItem> cartItems = cartService.getCartItems(user);

            // Calculate totals and discounted prices
            BigDecimal originalTotal = BigDecimal.ZERO;
            BigDecimal discountedTotal = BigDecimal.ZERO;
            Map<Long, BigDecimal> discountedPrices = new HashMap<>();

            for (CartItem item : cartItems) {
                if (item.getSubscription() != null) {
                    BigDecimal price = BigDecimal.valueOf(item.getSubscription().getPrice());
                    originalTotal = originalTotal.add(price);
                    discountedPrices.put(item.getId(), price); // Default to original price

                    // Apply discount if valid
                    if (item.getSubscription().getDiscount() != null &&
                            item.getSubscription().getDiscountExpiry() != null &&
                            item.getSubscription().getDiscountExpiry().isAfter(LocalDate.now())) {
                        BigDecimal discount = new BigDecimal(item.getSubscription().getDiscount().toString())
                                .divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP);
                        BigDecimal discountedPrice = price.multiply(BigDecimal.ONE.subtract(discount));
                        discountedTotal = discountedTotal.add(discountedPrice);
                        discountedPrices.put(item.getId(), discountedPrice);
                    } else {
                        discountedTotal = discountedTotal.add(price);
                    }
                } else if (item.getProduct() != null) {
                    BigDecimal price = item.getProduct().getPrice();
                    originalTotal = originalTotal.add(price);
                    discountedTotal = discountedTotal.add(price);
                    discountedPrices.put(item.getId(), price);
                }
            }

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartCount", cartItems.size());
            model.addAttribute("originalTotal", originalTotal);
            model.addAttribute("discountedTotal", discountedTotal);
            model.addAttribute("discountedPrices", discountedPrices);
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
    public String initiateCheckout(Principal principal, Model model) {
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            BigDecimal total = cartService.calculateSubtotal(user);
            String transactionId = UUID.randomUUID().toString();
            model.addAttribute("total", total);
            model.addAttribute("transactionId", transactionId);
            return "payment";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/cart/process-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(@RequestParam String transactionId, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Utente non autenticato");
            }
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                throw new IllegalArgumentException("Utente non trovato");
            }
            // Process the checkout with the transaction ID
            cartService.checkoutSubscriptions(user, transactionId);
            response.put("success", true);
            response.put("message", "Pagamento avvenuto con successo");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}