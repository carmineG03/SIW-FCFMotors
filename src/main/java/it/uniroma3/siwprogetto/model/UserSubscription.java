package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class UserSubscription {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private User user;

	@ManyToOne
	private Subscription subscription;

	private LocalDate startDate;
	private LocalDate expiryDate;

	private boolean active;

	// Getters e Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public Subscription getSubscription() { return subscription; }
	public void setSubscription(Subscription subscription) { this.subscription = subscription; }
	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
	public LocalDate getExpiryDate() { return expiryDate; }
	public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	// Method to check if the subscription is expired
	public boolean isExpired() {
		return expiryDate != null && LocalDate.now().isAfter(expiryDate);
	}
}