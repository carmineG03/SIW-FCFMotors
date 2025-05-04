package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.AdminService;
import it.uniroma3.siwprogetto.service.DealerService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.security.Principal;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	@Autowired
	private AdminService adminService;

	@Autowired
	private UserService userService;

	@Autowired
	private DealerService dealerService;

	// Mostra la pagina di manutenzione
	@GetMapping("/maintenance")
	public String showMaintenancePage(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("products", adminService.findAllProducts());
		model.addAttribute("dealers", adminService.findAllDealers());
		model.addAttribute("users", adminService.findAllUsers());
		return "admin-maintenance";
	}

	// Mostra il form di modifica di un prodotto
	@GetMapping("/product/{id}/edit")
	public String showEditProductForm(@PathVariable Long id, Model model) {
		Product product = adminService.findProductById(id)
				.orElseThrow(() -> {
					logger.error("Product not found: id={}", id);
					return new IllegalStateException("Prodotto non trovato");
				});
		model.addAttribute("product", product);
		return "admin-product-edit";
	}

	// Modifica un prodotto
	@PostMapping("/product/{id}/update")
	public String updateProduct(@PathVariable Long id, @ModelAttribute Product updatedProduct, BindingResult result, Model model) {
		if (result.hasErrors()) {
			model.addAttribute("errorMessage", "Errore nei dati del prodotto");
			return "admin-product-edit";
		}
		logger.debug("Updating product: id={}", id);
		try {
			adminService.updateProduct(id, updatedProduct);
			model.addAttribute("successMessage", "Prodotto aggiornato con successo");
		} catch (Exception e) {
			logger.error("Error updating product: id={}", id, e);
			model.addAttribute("errorMessage", "Errore durante l'aggiornamento del prodotto");
			return "admin-product-edit";
		}
		return "redirect:/admin/maintenance";
	}

	// Elimina un prodotto
	@PostMapping("/product/{id}/delete")
	public String deleteProduct(@PathVariable Long id, Model model) {
		logger.debug("Deleting product: id={}", id);
		try {
			adminService.deleteProduct(id);
			model.addAttribute("successMessage", "Prodotto eliminato con successo");
		} catch (Exception e) {
			logger.error("Error deleting product: id={}", id, e);
			model.addAttribute("errorMessage", "Errore durante l'eliminazione del prodotto");
		}
		return "redirect:/admin/maintenance";
	}

	// Mostra il form di modifica di un concessionario
	@GetMapping("/dealer/{id}/edit")
	public String showEditDealerForm(@PathVariable Long id, Model model) {
		Dealer dealer = adminService.findDealerById(id)
				.orElseThrow(() -> {
					logger.error("Dealer not found: id={}", id);
					return new IllegalStateException("Concessionario non trovato");
				});
		model.addAttribute("dealer", dealer);
		return "admin-dealer-edit";
	}

	// Modifica un concessionario
	@PostMapping("/dealer/{id}/update")
	public String updateDealer(@PathVariable Long id, @ModelAttribute Dealer updatedDealer, BindingResult result, Model model) {
		if (result.hasErrors()) {
			model.addAttribute("errorMessage", "Errore nei dati del concessionario");
			return "admin-dealer-edit";
		}
		logger.debug("Updating dealer: id={}", id);
		try {
			adminService.updateDealer(id, updatedDealer);
			model.addAttribute("successMessage", "Concessionario aggiornato con successo");
		} catch (Exception e) {
			logger.error("Error updating dealer: id={}", id, e);
			model.addAttribute("errorMessage", "Errore durante l'aggiornamento del concessionario");
			return "admin-dealer-edit";
		}
		return "redirect:/admin/maintenance";
	}

	// Elimina un concessionario
	@PostMapping("/dealer/{id}/delete")
	public String deleteDealer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		logger.debug("Deleting dealer: id={}", id);
		try {
			dealerService.deleteDealer(id);
			redirectAttributes.addFlashAttribute("successMessage", "Concessionario eliminato con successo");
		} catch (IllegalArgumentException e) {
			logger.error("Invalid dealer ID or not found: id={}", id, e);
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		} catch (IllegalStateException e) {
			logger.error("Error deleting dealer: id={}", id, e);
			redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione del concessionario: " + e.getMessage());
		} catch (Exception e) {
			logger.error("Unexpected error deleting dealer: id={}", id, e);
			redirectAttributes.addFlashAttribute("errorMessage", "Errore imprevisto durante l'eliminazione del concessionario");
		}
		return "redirect:/admin/maintenance";
	}

	// Modifica un utente
	@PostMapping("/user/{id}/update")
	public String updateUser(@PathVariable Long id, @ModelAttribute User updatedUser, BindingResult result, Model model) {
		if (result.hasErrors()) {
			model.addAttribute("errorMessage", "Errore nei dati dell'utente");
			return "admin-maintenance";
		}
		logger.debug("Updating user: id={}", id);
		try {
			adminService.updateUser(id, updatedUser);
			model.addAttribute("successMessage", "Utente aggiornato con successo");
		} catch (Exception e) {
			logger.error("Error updating user: id={}", id, e);
			model.addAttribute("errorMessage", "Errore durante l'aggiornamento dell'utente");
		}
		return "redirect:/admin/maintenance";
	}

	// Elimina un utente
	@PostMapping("/user/{id}/delete")
	public String deleteUser(@PathVariable Long id, Model model) {
		logger.debug("Deleting user: id={}", id);
		try {
			adminService.deleteUser(id);
			model.addAttribute("successMessage", "Utente eliminato con successo");
		} catch (Exception e) {
			logger.error("Error deleting user: id={}", id, e);
			model.addAttribute("errorMessage", "Errore durante l'eliminazione dell'utente");
		}
		return "redirect:/admin/maintenance";
	}
}