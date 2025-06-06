package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.Payment;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import it.uniroma3.siwprogetto.repository.CartItemRepository;
import it.uniroma3.siwprogetto.repository.PaymentRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    public void addSubscriptionToCart(Long subscriptionId, User user) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

        CartItem item = new CartItem();
        item.setSubscription(subscription);
        item.setUser(user);
        item.setQuantity(1);
        item.setProduct(null); // Nessun prodotto associato
        cartItemRepository.save(item);
    }

    public void updateQuantity(Long itemId, int quantity, User user) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Non autorizzato");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    public void removeFromCart(Long itemId, User user) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento non trovato"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Non autorizzato");
        }
        cartItemRepository.delete(item);
    }

    public BigDecimal calculateSubtotal(User user) {
        return cartItemRepository.findByUser(user).stream()
                .map(item -> {
                    BigDecimal price = BigDecimal.ZERO;
                    if (item.getSubscription() != null) {
                        price = BigDecimal.valueOf(item.getSubscription().getPrice());
                        // Apply discount if valid
                        if (item.getSubscription().getDiscount() != null &&
                                item.getSubscription().getDiscountExpiry() != null &&
                                item.getSubscription().getDiscountExpiry().isAfter(LocalDate.now())) {
                            BigDecimal discount = new BigDecimal(item.getSubscription().getDiscount().toString())
                                    .divide(new BigDecimal("100"), 10, BigDecimal.ROUND_HALF_UP);
                            price = price.multiply(BigDecimal.ONE.subtract(discount));
                        }
                    } else if (item.getProduct() != null) {
                        price = item.getProduct().getPrice();
                    }
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void checkoutSubscriptions(User user, String transactionId) {
        BigDecimal total = calculateSubtotal(user);

        // Save payment record
        Payment payment = new Payment(user, total, transactionId, "SUCCESS");
        paymentRepository.save(payment);

        List<CartItem> items = cartItemRepository.findByUser(user);
        for (CartItem item : items) {
            if (item.getSubscription() != null) {
                // Activate subscription with auto-renewal
                UserSubscription userSubscription = userService.subscribeUserToDealer(user.getId(), item.getSubscription().getId());
                userSubscription.setAutoRenew(true); // Enable auto-renewal
                userSubscriptionRepository.save(userSubscription);
            }
            // Handle products, if present
            if (item.getProduct() != null && item.getSubscription() != null) {
                Product product = item.getProduct();
                product.setIsFeatured(true);
                product.setFeaturedUntil(LocalDate.now().plusDays(item.getSubscription().getDurationDays()).atStartOfDay());
                productRepository.save(product);
            }
        }
        // Clear the cart
        cartItemRepository.deleteAll(items);
    }
}