package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.*;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.service.DealerService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rest")
public class DealerController {

    private static final Logger logger = LoggerFactory.getLogger(DealerController.class);

    @Autowired
    private DealerService dealerService;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;



    @PostMapping("/api/dealers")
    @ResponseBody
    public ResponseEntity<?> createOrUpdateDealer(@RequestBody Map<String, String> payload) {
        logger.info("Received POST /rest/api/dealers with payload: {}", payload);
        String name = payload.get("name");
        String description = payload.get("description");
        String address = payload.get("address");
        String phone = payload.get("phone");
        String email = payload.get("email");
        String imagePath = payload.get("imagePath");
        String isUpdate = payload.get("isUpdate");
        boolean updateFlag = "true".equalsIgnoreCase(isUpdate);
        String trimmedName = (name != null) ? name.trim() : null;

        if (!StringUtils.hasText(trimmedName)) {
            logger.warn("Validation failed: Dealer name is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il nome del concessionario è obbligatorio"));
        }

        try {
            Dealer dealer = new Dealer();
            dealer.setName(trimmedName);
            dealer.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            dealer.setAddress(StringUtils.hasText(address) ? address.trim() : null);
            dealer.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
            dealer.setEmail(StringUtils.hasText(email) ? email.trim() : null);
            dealer.setImagePath(StringUtils.hasText(imagePath) ? imagePath.trim() : null);

            logger.info("Calling dealerService.saveDealer for dealer: {}, isUpdate={}", dealer.getName(), updateFlag);
            Dealer savedDealer = dealerService.saveDealer(dealer, updateFlag);
            logger.info("Dealer processed successfully: id={}, name={}", savedDealer.getId(), savedDealer.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedDealer.getId());
            response.put("name", savedDealer.getName());
            response.put("description", savedDealer.getDescription() != null ? savedDealer.getDescription() : "");
            response.put("address", savedDealer.getAddress() != null ? savedDealer.getAddress() : "");
            response.put("phone", savedDealer.getPhone() != null ? savedDealer.getPhone() : "");
            response.put("email", savedDealer.getEmail() != null ? savedDealer.getEmail() : "");
            response.put("imagePath", savedDealer.getImagePath() != null ? savedDealer.getImagePath() : "");

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error saving dealer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/api/dealers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> findDealers(@RequestParam(required = false) String query) {
        logger.info("Received GET /rest/api/dealers with query: '{}'", query);
        try {
            List<Dealer> dealers = dealerService.findByLocation(query);
            List<Map<String, Object>> response = dealers.stream().map(dealer -> {
                logger.debug("Processing dealer: id={}, name={}",
                        dealer.getId() != null ? dealer.getId() : "null",
                        dealer.getName() != null ? dealer.getName() : "null");
                Map<String, Object> dealerMap = new HashMap<>();
                dealerMap.put("id", dealer.getId() != null ? dealer.getId() : 0L);
                dealerMap.put("name", dealer.getName() != null ? dealer.getName() : "");
                dealerMap.put("description", dealer.getDescription() != null ? dealer.getDescription() : "");
                dealerMap.put("address", dealer.getAddress() != null ? dealer.getAddress() : "");
                dealerMap.put("phone", dealer.getPhone() != null ? dealer.getPhone() : "");
                dealerMap.put("email", dealer.getEmail() != null ? dealer.getEmail() : "");
                dealerMap.put("imagePath", dealer.getImagePath() != null ? dealer.getImagePath() : "");
                return dealerMap;
            }).toList();
            logger.info("Returning {} dealers", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching dealers with query '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/manutenzione/dealer")
    public String redirectDealerPage(Model model) {
        logger.debug("Accessing /manutenzione/dealer");
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("Authenticated user: name={}", username);
            Dealer dealer = dealerService.findByOwner();
            if (dealer != null) {
                logger.info("Dealer found for user '{}': id={}, name={}", username, dealer.getId(), dealer.getName());
                return "redirect:/rest/dealers/manage";
            } else {
                logger.warn("No dealer found for user '{}', redirecting to create page", username);
                return "redirect:/rest/dealers/create";
            }
        } catch (Exception e) {
            logger.error("Error redirecting dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "redirect:/rest/dealers/create";
        }
    }

    @GetMapping("/dealers/create")
    public String showCreateDealerPage(Model model) {
        logger.debug("Accessing create dealer page");
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("Authenticated user: name={}", username);
            Dealer dealer = dealerService.findByOwner();
            if (dealer != null) {
                logger.info("Dealer already exists for user '{}': id={}, name={}", username, dealer.getId(), dealer.getName());
                return "redirect:/rest/dealers/manage";
            }

            model.addAttribute("dealer", new Dealer());
            return "create_dealer";
        } catch (Exception e) {
            logger.error("Error loading create dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "create_dealer";
        }
    }

    @GetMapping("/dealers/manage")
    public String showManageDealerPage(Model model) {
        logger.debug("Accessing manage dealer page");
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("Authenticated user: name={}", username);
            Dealer dealer = dealerService.findByOwner();
            if (dealer == null) {
                logger.warn("No dealer found for user '{}', redirecting to create page", username);
                return "redirect:/rest/dealers/create";
            }

            List<Product> products = dealerService.getProductsByDealer();
            logger.info("Dealer found for user '{}': id={}, name={}, products={}", username, dealer.getId(), dealer.getName(), products.size());
            model.addAttribute("dealer", dealer);
            model.addAttribute("products", products);
            return "manage_dealer";
        } catch (Exception e) {
            logger.error("Error loading manage dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "redirect:/rest/dealers/create";
        }
    }

    @PostMapping("/api/products")
    @ResponseBody
    public ResponseEntity<?> addProduct(@RequestBody Map<String, String> payload) {
        logger.info("Received POST /rest/api/products for product: {}", payload.get("model"));String model = payload.get("model");
        String brand = payload.get("brand");
        String category = payload.get("category");
        String description = payload.get("description");
        String priceStr = payload.get("price");
        String imageUrl = payload.get("imageUrl");
        String fuelType = payload.get("fuelType");
        String transmission = payload.get("transmission");
        String mileageStr = payload.get("mileage");
        String yearStr = payload.get("year");
        String isFeaturedStr = payload.get("isFeatured");
        String featuredUntilStr = payload.get("featuredUntil");

        String trimmedModel = (model != null) ? model.trim() : null;

        if (!StringUtils.hasText(trimmedModel)) {
            logger.warn("Validation failed: Product model is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il modello del prodotto è obbligatorio"));
        }

        if (!StringUtils.hasText(priceStr)) {
            logger.warn("Validation failed: Product price is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il prezzo del prodotto è obbligatorio"));
        }

        if (!priceStr.matches("\\d+(\\.\\d{1,2})?")) {
            logger.warn("Invalid price format: {}", priceStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Il prezzo deve essere un numero valido (es. 20000.00)"));
        }

        try {
            BigDecimal price = new BigDecimal(priceStr);
            Product product = new Product();
            product.setModel(trimmedModel);
            product.setBrand(StringUtils.hasText(brand) ? brand.trim() : null);
            product.setCategory(StringUtils.hasText(category) ? category.trim() : null);
            product.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            product.setPrice(price);
            product.setMileage(StringUtils.hasText(mileageStr) ? Integer.parseInt(mileageStr) : null);
            product.setYear(StringUtils.hasText(yearStr) ? Integer.parseInt(yearStr) : null);
            product.setFuelType(StringUtils.hasText(fuelType) ? fuelType.trim() : null);
            product.setTransmission(StringUtils.hasText(transmission) ? transmission.trim() : null);
            product.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);

            boolean highlighted = StringUtils.hasText(isFeaturedStr) ? Boolean.parseBoolean(isFeaturedStr) : false;
            product.setIsFeatured(highlighted);
            if (highlighted && StringUtils.hasText(featuredUntilStr)) {
                int highlightDuration = Integer.parseInt(featuredUntilStr);
                if (highlightDuration <= 0) {
                    logger.warn("Invalid highlight duration: {}", featuredUntilStr);
                    return ResponseEntity.badRequest().body(Map.of("message", "La durata deve essere positiva"));
                }
                product.setFeaturedUntil(LocalDateTime.now().plusDays(highlightDuration));
            }

            logger.info("Calling dealerService.addProduct for product: {}", product.getModel());
            long startTime = System.currentTimeMillis();
            Product savedProduct = dealerService.addProduct(product);
            logger.info("Product added successfully: id={}, model={}, took {} ms",
                    savedProduct.getId(), savedProduct.getModel(), System.currentTimeMillis() - startTime);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProduct.getId());
            response.put("model", savedProduct.getModel());
            response.put("brand", savedProduct.getBrand() != null ? savedProduct.getBrand() : "");
            response.put("category", savedProduct.getCategory() != null ? savedProduct.getCategory() : "");
            response.put("description", savedProduct.getDescription() != null ? savedProduct.getDescription() : "");
            response.put("price", savedProduct.getPrice());
            response.put("mileage", savedProduct.getMileage());
            response.put("year", savedProduct.getYear());
            response.put("fuelType", savedProduct.getFuelType() != null ? savedProduct.getFuelType() : "");
            response.put("transmission", savedProduct.getTransmission() != null ? savedProduct.getTransmission() : "");
            response.put("imageUrl", savedProduct.getImageUrl() != null ? savedProduct.getImageUrl() : "");
            response.put("highlighted", savedProduct.isFeatured());
            response.put("highlightExpiration", savedProduct.getFeaturedUntil() != null ? savedProduct.getFeaturedUntil().toString() : null);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid price or highlight duration format: price={}, duration={}", priceStr, featuredUntilStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Formato del prezzo o della durata non valido"));
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/api/products/{productId}")
    @ResponseBody
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        logger.info("Received GET /rest/api/products/{}", productId);
        try {
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("Product not found: id={}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Prodotto non trovato"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", product.getId());
            response.put("model", product.getModel());
            response.put("brand", product.getBrand() != null ? product.getBrand() : "");
            response.put("category", product.getCategory() != null ? product.getCategory() : "");
            response.put("description", product.getDescription() != null ? product.getDescription() : "");
            response.put("price", product.getPrice());
            response.put("mileage", product.getMileage());
            response.put("year", product.getYear());
            response.put("fuelType", product.getFuelType() != null ? product.getFuelType() : "");
            response.put("transmission", product.getTransmission() != null ? product.getTransmission() : "");
            response.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "");
            response.put("highlighted", product.isFeatured());
            response.put("highlightExpiration", product.getFeaturedUntil() != null ? product.getFeaturedUntil().toString() : null);

            logger.info("Product retrieved successfully: id={}, model={}", product.getId(), product.getModel());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching product: id={}, error={}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @PutMapping("/api/products/{productId}")
    @ResponseBody
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @RequestBody Map<String, String> payload) {
        logger.info("Received PUT /rest/api/products/{} with payload: {}", productId, payload);
        String model = payload.get("model");
        String brand = payload.get("brand");
        String category = payload.get("category");
        String description = payload.get("description");
        String priceStr = payload.get("price");
        String imageUrl = payload.get("imageUrl");
        String fuelType = payload.get("fuelType");
        String transmission = payload.get("transmission");
        String mileageStr = payload.get("mileage");
        String yearStr = payload.get("year");
        String highlightedStr = payload.get("isFeatured");
        String highlightDurationStr = payload.get("featuredUntil");

        String trimmedModel = (model != null) ? model.trim() : null;

        if (!StringUtils.hasText(trimmedModel)) {
            logger.warn("Validation failed: Product model is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il modello del prodotto è obbligatorio"));
        }

        if (!StringUtils.hasText(priceStr)) {
            logger.warn("Validation failed: Product price is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il prezzo del prodotto è obbligatorio"));
        }

        try {
            BigDecimal price = new BigDecimal(priceStr);
            Product product = new Product();
            product.setModel(trimmedModel);
            product.setBrand(StringUtils.hasText(brand) ? brand.trim() : null);
            product.setCategory(StringUtils.hasText(category) ? category.trim() : null);
            product.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            product.setPrice(price);
            product.setMileage(StringUtils.hasText(mileageStr) ? Integer.parseInt(mileageStr) : null);
            product.setYear(StringUtils.hasText(yearStr) ? Integer.parseInt(yearStr) : null);
            product.setFuelType(StringUtils.hasText(fuelType) ? fuelType.trim() : null);
            product.setTransmission(StringUtils.hasText(transmission) ? transmission.trim() : null);
            product.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);

            boolean highlighted = Boolean.parseBoolean(highlightedStr);
            product.setIsFeatured(highlighted);
            if (highlighted && StringUtils.hasText(highlightDurationStr)) {
                int highlightDuration = Integer.parseInt(highlightDurationStr);
                product.setFeaturedUntil(LocalDateTime.now().plusDays(highlightDuration));
            } else if (!highlighted) {
                product.setFeaturedUntil(null);
            }

            logger.info("Calling dealerService.updateProduct for product: id={}, model={}", productId, product.getModel());
            Product updatedProduct = dealerService.updateProduct(productId, product);
            logger.info("Product updated successfully: id={}, model={}", updatedProduct.getId(), updatedProduct.getModel());

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedProduct.getId());
            response.put("model", updatedProduct.getModel());
            response.put("brand", updatedProduct.getBrand() != null ? updatedProduct.getBrand() : "");
            response.put("category", updatedProduct.getCategory() != null ? updatedProduct.getCategory() : "");
            response.put("description", updatedProduct.getDescription() != null ? updatedProduct.getDescription() : "");
            response.put("price", updatedProduct.getPrice());
            response.put("mileage", updatedProduct.getMileage());
            response.put("year", updatedProduct.getYear());
            response.put("fuelType", updatedProduct.getFuelType() != null ? updatedProduct.getFuelType() : "");
            response.put("transmission", updatedProduct.getTransmission() != null ? updatedProduct.getTransmission() : "");
            response.put("imageUrl", updatedProduct.getImageUrl() != null ? updatedProduct.getImageUrl() : "");
            response.put("highlighted", updatedProduct.isFeatured());
            response.put("highlightExpiration", updatedProduct.getFeaturedUntil() != null ? updatedProduct.getFeaturedUntil().toString() : null);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid price or highlight duration format: price={}, duration={}", priceStr, highlightDurationStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Formato del prezzo o della durata non valido"));
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @DeleteMapping("/api/products/{productId}")
    @ResponseBody
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        logger.info("Received DELETE /rest/api/products/{}", productId);
        try {
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("Product not found: id={}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Prodotto non trovato"));
            }

            dealerService.deleteProduct(productId);
            logger.info("Product deleted successfully: id={}", productId);
            return ResponseEntity.ok(Map.of("message", "Prodotto eliminato con successo"));
        } catch (Exception e) {
            logger.error("Error deleting product: id={}, error={}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/dealers")
    public String showDealersPage(Model model) {
        logger.info("Accessing /dealers page");
        try {
            List<Dealer> dealers = dealerService.findAll();
            model.addAttribute("dealers", dealers);

            return "dealers";
        } catch (Exception e) {
            logger.error("Error loading dealers page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento della pagina dei concessionari.");
            return "dealers";
        }
    }

    @GetMapping("/manutenzione/dealer/delete_dealer/{id}")
    public String deleteDealer(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Received GET /rest/manutenzione/dealer/delete_dealer/{}", id);
        try {
            dealerService.deleteDealer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Concessionario eliminato con successo.");
            return "redirect:/rest/dealers/create";
        } catch (IllegalArgumentException e) {
            logger.warn("Error deleting dealer: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rest/dealers/create";
        } catch (IllegalStateException e) {
            logger.error("State error deleting dealer: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Impossibile eliminare il concessionario: " + e.getMessage());
            return "redirect:/rest/dealers/create";
        } catch (Exception e) {
            logger.error("Unexpected error deleting dealer: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione del concessionario.");
            return "redirect:/rest/dealers/create";
        }
    }

    @PostMapping("/api/products/{productId}/highlight")
    @ResponseBody
    public ResponseEntity<?> highlightProduct(@PathVariable Long productId, @RequestBody Map<String, String> payload) {
        logger.info("Received POST /rest/api/products/{}/highlight with payload: {}", productId, payload);
        String durationStr = payload.get("duration");

        if (!StringUtils.hasText(durationStr)) {
            logger.warn("Validation failed: Highlight duration is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "La durata dell'evidenza è obbligatoria"));
        }

        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                logger.warn("Validation failed: Highlight duration must be positive");
                return ResponseEntity.badRequest().body(Map.of("message", "La durata deve essere un numero positivo"));
            }

            Product highlightedProduct = dealerService.highlightProduct(productId, duration);
            logger.info("Product highlighted successfully: id={}, model={}", highlightedProduct.getId(), highlightedProduct.getModel());

            Map<String, Object> response = new HashMap<>();
            response.put("id", highlightedProduct.getId());
            response.put("model", highlightedProduct.getModel());
            response.put("highlighted", highlightedProduct.isFeatured());
            response.put("highlightExpiration", highlightedProduct.getFeaturedUntil() != null ? highlightedProduct.getFeaturedUntil().toString() : null);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid duration format: {}", durationStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Formato della durata non valido"));
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error highlighting product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @PostMapping("/api/products/{productId}/remove-highlight")
    @ResponseBody
    public ResponseEntity<?> removeHighlight(@PathVariable Long productId) {
        logger.info("Received POST /rest/api/products/{}/remove-highlight", productId);
        try {
            Product updatedProduct = dealerService.removeHighlight(productId);
            logger.info("Highlight removed successfully: id={}, model={}", updatedProduct.getId(), updatedProduct.getModel());

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedProduct.getId());
            response.put("model", updatedProduct.getModel());
            response.put("highlighted", updatedProduct.isFeatured());
            response.put("highlightExpiration", updatedProduct.getFeaturedUntil() != null ? updatedProduct.getFeaturedUntil().toString() : null);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing highlight: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/dealer/quote-requests/{dealerId}")
    public String viewQuoteRequests(@PathVariable Long dealerId, Model model) {
        logger.info("Accessing quote requests for dealer ID: {}", dealerId);
        try {
            // Verifica che il dealer esista
            Dealer dealer = dealerRepository.findById(dealerId)
                    .orElseThrow(() -> new IllegalStateException("Dealer non trovato"));

            // Verifica che l'utente autenticato sia il proprietario del dealer
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Dealer authenticatedDealer = dealerService.findByOwner();
            if (authenticatedDealer == null || !authenticatedDealer.getId().equals(dealerId)) {
                model.addAttribute("errorMessage", "Accesso non autorizzato.");
                return "redirect:/rest/dealers/manage";
            }

            List<QuoteRequest> quoteRequests = quoteRequestRepository.findByDealerId(dealerId);
            model.addAttribute("quoteRequests", quoteRequests);
            model.addAttribute("dealerId", dealerId);
            return "quote_requests";
        } catch (IllegalStateException e) {
            logger.error("Error loading quote requests for dealer ID {}: {}", dealerId, e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/rest/dealers/manage";
        } catch (Exception e) {
            logger.error("Unexpected error loading quote requests for dealer ID {}: {}", dealerId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento delle richieste di preventivo.");
            return "redirect:/rest/dealers/manage";
        }
    }

    @PostMapping("/dealer/quote-requests/respond/{requestId}")
    public String respondToQuoteRequest(@PathVariable Long requestId, @RequestParam String responseMessage, RedirectAttributes redirectAttributes) {
        logger.info("Responding to quote request ID: {}", requestId);
        QuoteRequest quoteRequest = new QuoteRequest();
        try {
            quoteRequest = quoteRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Richiesta di preventivo non trovata"));

            // Verifica che l'utente autenticato sia il proprietario del dealer
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Dealer authenticatedDealer = dealerService.findByOwner();
            if (authenticatedDealer == null || !authenticatedDealer.getId().equals(quoteRequest.getDealer().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Accesso non autorizzato.");
                return "redirect:/rest/dealers/manage";
            }

            // Invia email all'utente
            SimpleMailMessage message = new SimpleMailMessage();
            Product product = quoteRequest.getProduct();
            String productDisplayName = (product.getBrand() != null ? product.getBrand() + " " : "") +
                    (product.getModel() != null ? product.getModel() : "");
            message.setTo(quoteRequest.getUserEmail());
            message.setSubject("Risposta alla tua richiesta di preventivo - FCF Motors");
            message.setText("Gentile cliente,\n\nAbbiamo ricevuto la tua richiesta di preventivo per il prodotto: " +
                    productDisplayName + ".\n\nRisposta:\n" + responseMessage +
                    "\n\nGrazie per aver scelto FCF Motors!\nIl team FCF Motors");
            message.setFrom("info@fcfmotors.com");
            mailSender.send(message);

            // Aggiorna lo stato della richiesta
            quoteRequest.setStatus("RESPONDED");
            quoteRequestRepository.save(quoteRequest);

            redirectAttributes.addFlashAttribute("successMessage", "Risposta inviata con successo!");
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();
        } catch (IllegalStateException e) {
            logger.error("Error responding to quote request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();
        } catch (Exception e) {
            logger.error("Unexpected error responding to quote request: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'invio della risposta.");
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();
        }
    }

    @PostMapping("/products/{id}/set-featured")
    public ResponseEntity<Map<String, Object>> setProductFeatured(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato"));
            if (!product.getSeller().equals(user)) {
                throw new IllegalArgumentException("Non autorizzato a modificare questo prodotto");
            }
            if (!productService.canAddFeaturedCar(user, product)) {
                throw new IllegalArgumentException("Limite massimo di macchine in evidenza raggiunto");
            }
            productService.setProductFeatured(id, true, LocalDateTime.now().plusDays(30));
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/api/users/featured-limit")
    @ResponseBody
    public ResponseEntity<?> getFeaturedLimit() {
        logger.info("Received GET /rest/api/users/featured-limit");
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.debug("Authenticated user: {}", username);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Utente non trovato: {}", username);
                        return new IllegalStateException("Utente non trovato");
                    });

            Subscription subscription = user.getSubscription();
            if (subscription == null) {
                logger.warn("Nessun abbonamento trovato per l'utente: {}", username);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Nessun abbonamento associato. Contatta l'assistenza."));
            }

            int maxFeaturedProducts = subscription.getMaxFeaturedCars();
            logger.debug("Subscription details for user {}: id={}, maxFeaturedProducts={}",
                    username, subscription.getId(), maxFeaturedProducts);
            if (maxFeaturedProducts <= 0) {
                logger.warn("maxFeaturedProducts non valido per l'utente {}: {}", username, maxFeaturedProducts);
            }
            long currentFeaturedCount = productRepository.countBySellerAndIsFeaturedTrue(user);
            logger.info("Featured limit for user {}: currentFeaturedCount={}, maxFeaturedProducts={}",
                    username, currentFeaturedCount, maxFeaturedProducts);

            Map<String, Object> response = new HashMap<>();
            response.put("currentFeaturedCount", currentFeaturedCount);
            response.put("maxFeaturedProducts", maxFeaturedProducts);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.error("Errore nello stato: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Errore durante il recupero del limite di evidenza: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }
}
