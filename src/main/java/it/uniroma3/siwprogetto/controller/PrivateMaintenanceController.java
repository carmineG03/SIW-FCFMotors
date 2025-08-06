package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ImageRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
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

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;


    @GetMapping("/maintenance")
    @Transactional
    public String showMaintenancePage(Authentication authentication, Model model) {
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/login";
            }

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
            logger.error("Error loading maintenance page: {}", e.getMessage(), e);
            return "redirect:/login";
        }
    }

    @PostMapping("/add")
    @Transactional
    public String addCar(HttpServletRequest request, Authentication authentication) {
        logger.info("=== STARTING ADD CAR PROCESS ===");
        logger.info("User: {}", authentication.getName());

        try {
            // Trova l'utente
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.error("User not found");
                return "redirect:/private/maintenance?error=user_not_found";
            }

            // Verifica se ha già un'auto
            List<Product> userProducts = productRepository.findBySellerId(user.getId());
            if (!userProducts.isEmpty()) {
                logger.warn("User already has a car");
                return "redirect:/private/maintenance?error=already_has_car";
            }

            // Estrai parametri dalla richiesta
            String brand = request.getParameter("brand");
            String model = request.getParameter("model");
            String category = request.getParameter("category");
            String priceStr = request.getParameter("price");
            String yearStr = request.getParameter("year");
            String mileageStr = request.getParameter("mileage");
            String fuelType = request.getParameter("fuelType");
            String transmission = request.getParameter("transmission");
            String description = request.getParameter("description");

            logger.info("Received parameters: brand={}, model={}, category={}, price={}, year={}, mileage={}, fuel={}, transmission={}",
                    brand, model, category, priceStr, yearStr, mileageStr, fuelType, transmission);

            // Validazioni
            if (brand == null || brand.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_brand";
            }
            if (model == null || model.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_model";
            }
            if (category == null || category.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_category";
            }
            if (fuelType == null || fuelType.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_fuel_type";
            }
            if (transmission == null || transmission.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_transmission";
            }
            if (description == null || description.trim().isEmpty()) {
                return "redirect:/private/maintenance?error=missing_description";
            }

            // Validazione prezzo
            BigDecimal price;
            try {
                price = new BigDecimal(priceStr);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    return "redirect:/private/maintenance?error=invalid_price";
                }
            } catch (Exception e) {
                return "redirect:/private/maintenance?error=invalid_price";
            }

            // Validazione anno
            Integer year;
            try {
                year = Integer.parseInt(yearStr);
                if (year < 1900 || year > 2030) {
                    return "redirect:/private/maintenance?error=invalid_year";
                }
            } catch (Exception e) {
                return "redirect:/private/maintenance?error=invalid_year";
            }

            // Validazione chilometraggio
            Integer mileage;
            try {
                mileage = Integer.parseInt(mileageStr);
                if (mileage < 0) {
                    return "redirect:/private/maintenance?error=invalid_mileage";
                }
            } catch (Exception e) {
                return "redirect:/private/maintenance?error=invalid_mileage";
            }

            // Gestione immagini
            String[] imageFiles = request.getParameterValues("images");
            if (imageFiles == null || imageFiles.length == 0) {
                // Prova con multipart
                if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                    org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest =
                            (org.springframework.web.multipart.MultipartHttpServletRequest) request;

                    List<MultipartFile> files = multipartRequest.getFiles("images");
                    if (files.isEmpty() || files.get(0).isEmpty()) {
                        return "redirect:/private/maintenance?error=no_images";
                    }

                    // Crea il prodotto
                    Product product = new Product();
                    product.setBrand(brand.trim());
                    product.setModel(model.trim());
                    product.setCategory(category.trim());
                    product.setPrice(price);
                    product.setYear(year);
                    product.setMileage(mileage);
                    product.setFuelType(fuelType.trim());
                    product.setTransmission(transmission.trim());
                    product.setDescription(description.trim());
                    product.setSeller(user);
                    product.setSellerType("PRIVATE");

                    // Processa le immagini
                    List<Image> images = new ArrayList<>();
                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            try {
                                String contentType = file.getContentType();
                                if (contentType != null && contentType.startsWith("image/")) {
                                    if (file.getSize() <= 5 * 1024 * 1024) { // 5MB
                                        Image img = new Image();
                                        img.setData(file.getBytes());
                                        img.setContentType(contentType);
                                        img.setProduct(product);
                                        images.add(img);

                                        logger.info("Processed image: {}", file.getOriginalFilename());
                                    }
                                }
                            } catch (IOException e) {
                                logger.error("Error processing image: {}", e.getMessage());
                            }
                        }
                    }

                    if (images.isEmpty()) {
                        return "redirect:/private/maintenance?error=no_valid_images";
                    }

                    product.setImages(images);

                    // Salva il prodotto
                    Product savedProduct = productRepository.save(product);
                    logger.info("Product saved successfully with ID: {}", savedProduct.getId());

                    return "redirect:/private/maintenance?success=car_added";
                } else {
                    return "redirect:/private/maintenance?error=no_images";
                }
            }

            return "redirect:/private/maintenance?error=add_failed";

        } catch (Exception e) {
            logger.error("Error in addCar: ", e);
            return "redirect:/private/maintenance?error=add_failed";
        }
    }

     @PostMapping("/edit/{id}")
    @Transactional
    public String editCar(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        logger.info("Editing car {} for user: {}", id, authentication.getName());

        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/private/maintenance?error=user_not_found";
            }

            Product existingProduct = productRepository.findById(id).orElse(null);
            if (existingProduct == null || !existingProduct.getSeller().getId().equals(user.getId())) {
                return "redirect:/private/maintenance?error=not_authorized";
            }

            // Aggiorna tutti i campi dal form
            String brand = request.getParameter("brand");
            String model = request.getParameter("model");
            String category = request.getParameter("category");
            String priceStr = request.getParameter("price");
            String yearStr = request.getParameter("year");
            String mileageStr = request.getParameter("mileage");
            String fuelType = request.getParameter("fuelType");
            String transmission = request.getParameter("transmission");
            String description = request.getParameter("description");

            // Aggiorna tutti i campi
            if (brand != null && !brand.trim().isEmpty()) {
                existingProduct.setBrand(brand.trim());
            }
            if (model != null && !model.trim().isEmpty()) {
                existingProduct.setModel(model.trim());
            }
            if (category != null && !category.trim().isEmpty()) {
                existingProduct.setCategory(category.trim());
            }
            if (fuelType != null && !fuelType.trim().isEmpty()) {
                existingProduct.setFuelType(fuelType.trim());
            }
            if (transmission != null && !transmission.trim().isEmpty()) {
                existingProduct.setTransmission(transmission.trim());
            }
            if (description != null && !description.trim().isEmpty()) {
                existingProduct.setDescription(description.trim());
            }

            if (priceStr != null) {
                try {
                    BigDecimal price = new BigDecimal(priceStr);
                    if (price.compareTo(BigDecimal.ZERO) > 0) {
                        existingProduct.setPrice(price);
                    }
                } catch (Exception e) {
                    logger.warn("Invalid price format: {}", priceStr);
                }
            }

            if (yearStr != null) {
                try {
                    Integer year = Integer.parseInt(yearStr);
                    if (year >= 1900 && year <= 2030) {
                        existingProduct.setYear(year);
                    }
                } catch (Exception e) {
                    logger.warn("Invalid year format: {}", yearStr);
                }
            }

            if (mileageStr != null) {
                try {
                    Integer mileage = Integer.parseInt(mileageStr);
                    if (mileage >= 0) {
                        existingProduct.setMileage(mileage);
                    }
                } catch (Exception e) {
                    logger.warn("Invalid mileage format: {}", mileageStr);
                }
            }

            // CORREZIONE: Gestione nuove immagini migliorata
            if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest = 
                    (org.springframework.web.multipart.MultipartHttpServletRequest) request;
                
                List<MultipartFile> newImages = multipartRequest.getFiles("newImages");
                logger.info("Found {} new images in request", newImages != null ? newImages.size() : 0);
                
                if (newImages != null && !newImages.isEmpty()) {
                    // Verifica che almeno un file non sia vuoto
                    boolean hasValidFiles = newImages.stream().anyMatch(file -> !file.isEmpty());
                    
                    if (hasValidFiles) {
                        logger.info("Processing new images - clearing existing ones");
                        
                        // Elimina le immagini esistenti dal database
                        if (existingProduct.getImages() != null && !existingProduct.getImages().isEmpty()) {
                            imageRepository.deleteAll(existingProduct.getImages());
                            existingProduct.getImages().clear();
                        }
                        
                        // Processa le nuove immagini
                        List<Image> images = new ArrayList<>();
                        for (MultipartFile file : newImages) {
                            if (!file.isEmpty()) {
                                try {
                                    String contentType = file.getContentType();
                                    if (contentType != null && contentType.startsWith("image/")) {
                                        if (file.getSize() <= 5 * 1024 * 1024) { // 5MB
                                            Image img = new Image();
                                            img.setData(file.getBytes());
                                            img.setContentType(contentType);
                                            img.setProduct(existingProduct);
                                            images.add(img);
                                            
                                            logger.info("Processed new image: {} ({} bytes)", 
                                                file.getOriginalFilename(), file.getSize());
                                        } else {
                                            logger.warn("Image too large: {} ({} bytes)", 
                                                file.getOriginalFilename(), file.getSize());
                                        }
                                    } else {
                                        logger.warn("Invalid content type: {} for file {}", 
                                            contentType, file.getOriginalFilename());
                                    }
                                } catch (IOException e) {
                                    logger.error("Error processing new image {}: {}", 
                                        file.getOriginalFilename(), e.getMessage());
                                }
                            }
                        }
                        
                        if (!images.isEmpty()) {
                            existingProduct.setImages(images);
                            logger.info("Set {} new images to product", images.size());
                        } else {
                            logger.warn("No valid images found in upload");
                        }
                    } else {
                        logger.info("No valid files found in new images upload");
                    }
                } else {
                    logger.info("No new images to process");
                }
            } else {
                logger.info("Request is not multipart - no image processing");
            }

            // Salva il prodotto aggiornato
            Product savedProduct = productRepository.save(existingProduct);
            logger.info("Product updated successfully. Images count: {}", 
                savedProduct.getImages() != null ? savedProduct.getImages().size() : 0);
            
            return "redirect:/private/maintenance?success=car_updated";

        } catch (Exception e) {
            logger.error("Error editing car: ", e);
            return "redirect:/private/maintenance?error=update_failed";
        }
    }

    // Metodo per eliminare singole immagini via AJAX
    @PostMapping("/images/delete/{imageId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> deleteImage(@PathVariable Long imageId, Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return ResponseEntity.status(403).body("Non autorizzato");
            }

            // Trova l'immagine e verifica che appartenga all'utente
            Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null || !image.getProduct().getSeller().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Non autorizzato");
            }

            imageRepository.delete(image);
            return ResponseEntity.ok("Immagine eliminata");

        } catch (Exception e) {
            logger.error("Error deleting image: ", e);
            return ResponseEntity.status(500).body("Errore interno");
        }
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteCar(@PathVariable Long id, Authentication authentication) {
        logger.info("Deleting car {} for user: {}", id, authentication.getName());

        try {
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                return "redirect:/private/maintenance?error=user_not_found";
            }

            Product product = productRepository.findById(id).orElse(null);
            if (product == null || !product.getSeller().getId().equals(user.getId())) {
                return "redirect:/private/maintenance?error=not_authorized";
            }

            // NUOVO: Elimina prima tutti i quote requests associati
            List<QuoteRequest> quoteRequests = quoteRequestRepository.findByProductId(id);
            if (!quoteRequests.isEmpty()) {
                logger.info("Deleting {} quote requests for product {}", quoteRequests.size(), id);
                quoteRequestRepository.deleteAll(quoteRequests);
            }
            // Elimina prima le immagini associate
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageRepository.deleteAll(product.getImages());
                logger.info("Deleted {} images for product {}", product.getImages().size(), id);
            }

            // Elimina il prodotto
            productRepository.delete(product);
            logger.info("Product {} deleted successfully", id);

            return "redirect:/private/maintenance?success=car_deleted";

        } catch (Exception e) {
            logger.error("Error deleting car: ", e);
            return "redirect:/private/maintenance?error=delete_failed";
        }
    }
}