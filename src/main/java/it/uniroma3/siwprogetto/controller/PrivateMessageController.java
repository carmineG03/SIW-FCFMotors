package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.service.QuoteRequestService;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/private/messages")
public class PrivateMessageController {

	@Autowired
	private QuoteRequestService quoteRequestService;

	@Autowired
	private UserService userService;

	@Autowired
	private ProductRepository productRepository;

	@GetMapping
	public String showMessages(Authentication authentication, Model model) {
		User user = userService.findByUsername(authentication.getName());
		List<QuoteRequest> messages = quoteRequestService.getPrivateMessagesForUser(user);
		model.addAttribute("messages", messages);
		return "private_messages";
	}

	@GetMapping("/send/{productId}")
	public String showSendMessageForm(@PathVariable Long productId, Model model) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		if (!"PRIVATE".equals(product.getSellerType())) {
			return "redirect:/products?error=private_messages_only_for_private_sellers";
		}
		model.addAttribute("product", product);
		model.addAttribute("recipientEmail", product.getSeller().getEmail());
		model.addAttribute("quoteRequest", new QuoteRequest());
		return "send_private_message";
	}

	@PostMapping("/send")
	public String sendMessage(@RequestParam Long productId, @RequestParam String message, Authentication authentication) {
		try {
			User user = userService.findByUsername(authentication.getName());
			Product product = productRepository.findById(productId)
					.orElseThrow(() -> new IllegalArgumentException("Product not found"));
			quoteRequestService.createPrivateMessage(user, product, message);
			return "redirect:/private/messages?success=message_sent";
		} catch (IllegalArgumentException e) {
			return "redirect:/products?error=" + e.getMessage();
		}
	}

	@PostMapping("/respond/{id}")
	public String respondToMessage(@PathVariable Long id, @RequestParam String responseMessage, Authentication authentication) {
		try {
			User user = userService.findByUsername(authentication.getName());
			quoteRequestService.respondToPrivateMessage(id, user, responseMessage);
			return "redirect:/private/messages?success=message_responded";
		} catch (IllegalArgumentException e) {
			return "redirect:/private/messages?error=" + e.getMessage();
		}
	}
}