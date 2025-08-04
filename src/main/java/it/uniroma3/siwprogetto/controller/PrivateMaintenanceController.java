package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/private")
public class PrivateMaintenanceController {

    private static final Logger logger = LoggerFactory.getLogger(PrivateMaintenanceController.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    /**
     * Mostra la pagina di manutenzione per utenti privati.
     * Se l'utente non ha auto, mostra la pagina per aggiungere un'auto.
     * Se l'utente ha un'auto, mostra la pagina per modificarla.
     */
    @GetMapping("/maintenance")
    public String showMaintenancePage(Authentication authentication, Model model) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Product> userProducts = productRepository.findBySellerId(user.getId());

            if (userProducts.isEmpty()) {
                model.addAttribute("product", new Product());
                return "add_car";
            } else {
                Product product = userProducts.get(0);
                model.addAttribute("product", product);
                return "edit_car";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Errore durante il caricamento della pagina di manutenzione: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Aggiunge una nuova auto per l'utente privato.
     * Richiede almeno un'immagine da caricare.
     */
    @PostMapping("/add")
    public String addCar(@ModelAttribute Product product, @RequestParam("images") List<MultipartFile> images, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            List<Product> userProducts = productRepository.findBySellerId(user.getId());

            if (!userProducts.isEmpty()) {
                return "redirect:/private/maintenance?error=already_has_car";
            }

            product.setSeller(user);
            product.setSellerType("PRIVATE");

            if (images == null || images.isEmpty()) {
                return "redirect:/private/maintenance?error=no_images";
            }

            List<Image> imageEntities = new ArrayList<>();
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    Image img = new Image();
                    img.setData(file.getBytes());
                    img.setContentType(file.getContentType());
                    img.setProduct(product);
                    imageEntities.add(img);
                }
            }
            product.setImages(imageEntities);

            productRepository.save(product);
            return "redirect:/private/maintenance?success=car_added";
        } catch (Exception e) {
            logger.error("Error adding car: {}", e.getMessage(), e);
            return "redirect:/private/maintenance?error=add_failed";
        }
    }

    /**
     * Modifica l'auto esistente dell'utente privato.
     * Se vengono fornite nuove immagini, queste sostituiscono quelle esistenti.
     */
    @PostMapping("/edit/{id}")
    public String editCar(@PathVariable Long id, @ModelAttribute Product updatedProduct, @RequestParam(value = "images", required = false) List<MultipartFile> images, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

            if (!existingProduct.getSeller().getId().equals(user.getId())) {
                return "redirect:/private/maintenance?error=not_authorized";
            }

            existingProduct.setDescription(updatedProduct.getDescription());
            existingProduct.setPrice(updatedProduct.getPrice());
            existingProduct.setCategory(updatedProduct.getCategory());
            existingProduct.setBrand(updatedProduct.getBrand());
            existingProduct.setModel(updatedProduct.getModel());
            existingProduct.setMileage(updatedProduct.getMileage());
            existingProduct.setYear(updatedProduct.getYear());
            existingProduct.setFuelType(updatedProduct.getFuelType());
            existingProduct.setTransmission(updatedProduct.getTransmission());

            if (images != null && !images.isEmpty()) {
                List<Image> imageEntities = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        Image img = new Image();
                        img.setData(file.getBytes());
                        img.setContentType(file.getContentType());
                        img.setProduct(existingProduct);
                        imageEntities.add(img);
                    }
                }
                existingProduct.setImages(imageEntities);
            }

            productRepository.save(existingProduct);
            return "redirect:/private/maintenance?success=car_updated";
        } catch (Exception e) {
            logger.error("Error updating car: {}", e.getMessage(), e);
            return "redirect:/private/maintenance?error=update_failed";
        }
    }

    /**
     * Elimina l'auto dell'utente privato.
     */
    @PostMapping("/delete/{id}")
    public String deleteCar(@PathVariable Long id, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));

            if (!product.getSeller().getId().equals(user.getId())) {
                return "redirect:/private/maintenance?error=not_authorized";
            }

            productRepository.delete(product);
            return "redirect:/private/maintenance?success=car_deleted";
        } catch (Exception e) {
            logger.error("Error deleting car: {}", e.getMessage(), e);
            return "redirect:/private/maintenance?error=delete_failed";
        }
    }
}