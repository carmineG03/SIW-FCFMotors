package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;

@Entity
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int quantity;
    @ManyToOne
    @JoinColumn(nullable = true)
    private Product product;
    @ManyToOne
    private User user;
    @ManyToOne
    private Subscription subscription;

    public CartItem() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription2) { this.subscription = subscription2; }
}