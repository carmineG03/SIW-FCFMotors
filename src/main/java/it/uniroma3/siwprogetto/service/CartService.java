package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.repository.CartItemRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public List<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    public void addSubscriptionToCart(Long productId, Long subscriptionId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setSubscription(subscription);
        item.setQuantity(1);
        cartItemRepository.save(item);
    }

    public void updateQuantity(Long itemId, int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    public void removeFromCart(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        cartItemRepository.delete(item);
    }

    public double calculateSubtotal() {
        return cartItemRepository.findAll().stream()
            .filter(item -> item.getSubscription() != null)
            .mapToDouble(item -> BigDecimal.valueOf(item.getSubscription().getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())).doubleValue())
            .sum();
    }

    public void checkoutSubscriptions() {
        List<CartItem> items = cartItemRepository.findAll();
        for (CartItem item : items) {
            Product product = item.getProduct();
            product.setIsFeatured(true);
            product.setFeaturedUntil(LocalDateTime.now().plusDays(item.getSubscription().getDurationDays()));
            productRepository.save(product);
        }
        cartItemRepository.deleteAll();
    }
}