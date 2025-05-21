package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuoteRequestService {

	@Autowired
	private QuoteRequestRepository quoteRequestRepository;

	@Autowired
	private EmailService mailService;

	public QuoteRequest createPrivateMessage(User sender, Product product, String message) {
		if (!"PRIVATE".equals(product.getSellerType())) {
			throw new IllegalArgumentException("Private messages are only for private sellers");
		}

		QuoteRequest quoteRequest = new QuoteRequest();
		quoteRequest.setUser(sender); // Sender of the message
		quoteRequest.setProduct(product);
		quoteRequest.setUserEmail(sender.getEmail());
		quoteRequest.setRecipientEmail(product.getSeller().getEmail()); // Seller's email
		quoteRequest.setRequestType("PRIVATE");
		quoteRequest.setStatus("PENDING");
		quoteRequest.setRequestDate(LocalDateTime.now());
		quoteRequest.setResponseMessage(message);
		quoteRequest.setDealer(null); // Explicitly null for private messages

		quoteRequest = quoteRequestRepository.save(quoteRequest);

		mailService.sendPrivateMessageEmail(
				quoteRequest.getRecipientEmail(),
				sender.getEmail(),
				product,
				message
		);

		return quoteRequest;
	}

	public QuoteRequest respondToPrivateMessage(Long quoteRequestId, User responder, String responseMessage) {
		QuoteRequest quoteRequest = quoteRequestRepository.findById(quoteRequestId)
				.orElseThrow(() -> new IllegalArgumentException("Quote request not found"));

		if (!quoteRequest.getRequestType().equals("PRIVATE") ||
				(!quoteRequest.getUser().getId().equals(responder.getId()) &&
						!quoteRequest.getRecipientEmail().equals(responder.getEmail()))) {
			throw new IllegalArgumentException("Not authorized to respond");
		}

		quoteRequest.setResponseMessage(responseMessage);
		quoteRequest.setStatus("RESPONDED");
		quoteRequest = quoteRequestRepository.save(quoteRequest);

		String recipientEmail = quoteRequest.getUser().getId().equals(responder.getId())
				? quoteRequest.getRecipientEmail()
				: quoteRequest.getUser().getEmail();
		mailService.sendPrivateMessageResponseEmail(
				recipientEmail,
				responder.getEmail(),
				quoteRequest.getProduct(),
				responseMessage
		);

		return quoteRequest;
	}

	public List<QuoteRequest> getPrivateMessagesForUser(User user) {
		return quoteRequestRepository.findPrivateMessagesByUserIdOrEmail(user.getId(), user.getEmail());
	}
}