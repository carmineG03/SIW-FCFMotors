package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/private")
public class PrivateMaintenanceController {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserService userService;

	@GetMapping("/maintenance")
	public String showMaintenancePage(Authentication authentication, Model model) {
		try {
			User user = userService.findByUsername(authentication.getName());
			List<Product> userProducts = productRepository.findBySellerId(user.getId());

			if (userProducts.isEmpty()) {
				model.addAttribute("product", new Product());
				return "add_car";
			} else {
				model.addAttribute("product", userProducts.get(0));
				return "edit_car";
			}
		} catch (Exception e) {
			model.addAttribute("errorMessage", "Errore durante il caricamento della pagina di manutenzione: " + e.getMessage());
			return "error";
		}
	}

	@PostMapping("/add")
	public String addCar(@ModelAttribute Product product, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		List<Product> userProducts = productRepository.findBySellerId(user.getId());

		if (!userProducts.isEmpty()) {
			return "redirect:/private/maintenance?error=already_has_car";
		}

		product.setSeller(user);
		product.setSellerType("PRIVATE");

		if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
			product.setImageUrl("/image/default-car.jpg");
		}

		productRepository.save(product);
		return "redirect:/private/maintenance?success=car_added";
	}

	@PostMapping("/edit/{id}")
	public String editCar(@PathVariable Long id, @ModelAttribute Product updatedProduct, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		Product existingProduct = productRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

		if (!existingProduct.getSeller().getId().equals(user.getId())) {
			return "redirect:/private/maintenance?error=not_authorized";
		}

		existingProduct.setName(updatedProduct.getName());
		existingProduct.setDescription(updatedProduct.getDescription());
		existingProduct.setPrice(updatedProduct.getPrice());
		existingProduct.setCategory(updatedProduct.getCategory());
		existingProduct.setBrand(updatedProduct.getBrand());
		existingProduct.setModel(updatedProduct.getModel());
		existingProduct.setMileage(updatedProduct.getMileage());
		existingProduct.setYear(updatedProduct.getYear());
		existingProduct.setFuelType(updatedProduct.getFuelType());
		existingProduct.setTransmission(updatedProduct.getTransmission());
		existingProduct.setImageUrl(updatedProduct.getImageUrl() != null && !updatedProduct.getImageUrl().isEmpty()
				? updatedProduct.getImageUrl()
				: "/image/default-car.jpg");

		productRepository.save(existingProduct);
		return "redirect:/private/maintenance?success=car_updated";
	}

	@PostMapping("/delete/{id}")
	public String deleteCar(@PathVariable Long id, Authentication authentication) {
		User user = userService.findByUsername(authentication.getName());
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

		if (!product.getSeller().getId().equals(user.getId())) {
			return "redirect:/private/maintenance?error=not_authorized";
		}

		productRepository.delete(product);
		return "redirect:/private/maintenance?success=car_deleted";
	}
}