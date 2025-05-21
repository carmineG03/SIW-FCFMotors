package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quote_requests")
public class QuoteRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = true)
	private User user;

	@ManyToOne
	@JoinColumn(name = "dealer_id", nullable = true)
	private Dealer dealer;

	private LocalDateTime requestDate;

	private String status; // PENDING, RESPONDED

	private String userEmail;

	private String requestType;

	private String recipientEmail;


	@Column(columnDefinition = "TEXT")
	private String responseMessage; // Nuovo campo per il messaggio di risposta

	// Getters e Setters
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Product getProduct() { return product; }
	public void setProduct(Product product) { this.product = product; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public Dealer getDealer() { return dealer; }
	public void setDealer(Dealer dealer) { this.dealer = dealer; }
	public LocalDateTime getRequestDate() { return requestDate; }
	public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getUserEmail() { return userEmail; }
	public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
	public String getResponseMessage() { return responseMessage; }
	public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
	public String getRequestType() { return requestType; }
	public void setRequestType(String requestType) { this.requestType = requestType; }
	public String getRecipientEmail() { return recipientEmail; }
	public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

}