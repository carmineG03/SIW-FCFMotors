package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.CartItemRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private List<CartItem> savedItems = new ArrayList<>(); // Simulazione salvataggio per dopo

    public List<CartItem> getCartItems() {
        return cartItemRepository.findAll();
    }

    public List<CartItem> getSavedItems() {
        return savedItems;
    }

    public void addToCart(Long productId) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isPresent()) {
            CartItem item = new CartItem();
            item.setProduct(product.get());
            item.setQuantity(1);
            cartItemRepository.save(item);
        } else {
            throw new IllegalArgumentException("Prodotto non trovato con ID: " + productId);
        }
    }

    public void updateQuantity(Long itemId, int quantity) {
        Optional<CartItem> item = cartItemRepository.findById(itemId);
        if (item.isPresent()) {
            CartItem cartItem = item.get();
            if (quantity <= 0) {
                cartItemRepository.delete(cartItem); // Rimuovi se la quantità è 0
            } else {
                cartItem.setQuantity(quantity);
                cartItemRepository.save(cartItem);
            }
        } else {
            throw new IllegalArgumentException("Elemento del carrello non trovato con ID: " + itemId);
        }
    }

    public void removeFromCart(Long itemId) {
        Optional<CartItem> item = cartItemRepository.findById(itemId);
        if (item.isPresent()) {
            cartItemRepository.deleteById(itemId);
        } else {
            throw new IllegalArgumentException("Elemento del carrello non trovato con ID: " + itemId);
        }
    }

    public void saveForLater(Long itemId) {
        Optional<CartItem> item = cartItemRepository.findById(itemId);
        if (item.isPresent()) {
            savedItems.add(item.get());
            cartItemRepository.deleteById(itemId);
        } else {
            throw new IllegalArgumentException("Elemento del carrello non trovato con ID: " + itemId);
        }
    }

    public void addBackToCart(Long itemId) {
        CartItem itemToAdd = null;
        for (CartItem item : savedItems) {
            if (item.getId().equals(itemId)) {
                itemToAdd = item;
                break;
            }
        }
        if (itemToAdd != null) {
            savedItems.remove(itemToAdd);
            cartItemRepository.save(itemToAdd);
        } else {
            throw new IllegalArgumentException("Elemento salvato non trovato con ID: " + itemId);
        }
    }

    public double calculateSubtotal() {
        return cartItemRepository.findAll().stream()
                .mapToDouble(item -> {
                    BigDecimal price = item.getProduct().getPrice();
                    BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                    return price.multiply(quantity).doubleValue();
                })
                .sum();
    }

    public double calculateTotal() {
        return calculateSubtotal(); // Aggiungi logica per spedizione se necessario
    }

    public List<Product> getSuggestedProducts() {
        return productRepository.findAll().stream()
                .filter(p -> !cartItemRepository.findAll().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(p.getId())))
                .limit(4)
                .toList();
    }
}