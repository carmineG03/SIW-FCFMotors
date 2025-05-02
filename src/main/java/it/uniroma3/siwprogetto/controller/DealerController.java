package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.service.DealerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
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

    @PostMapping("/api/dealers")
    @ResponseBody
    public ResponseEntity<?> createOrUpdateDealer(@RequestBody Map<String, String> payload) {
        logger.info("Received POST /rest/api/dealers with payload: {}", payload);
        String name = payload.get("name");
        String description = payload.get("description");
        String address = payload.get("address");
        String contact = payload.get("contact");
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
            dealer.setContact(StringUtils.hasText(contact) ? contact.trim() : null);
            dealer.setImagePath(StringUtils.hasText(imagePath) ? imagePath.trim() : null);

            logger.info("Calling dealerService.saveDealer for dealer: {}, isUpdate={}", dealer.getName(), updateFlag);
            Dealer savedDealer = dealerService.saveDealer(dealer, updateFlag);
            logger.info("Dealer processed successfully: id={}, name={}", savedDealer.getId(), savedDealer.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedDealer.getId());
            response.put("name", savedDealer.getName());
            response.put("description", savedDealer.getDescription() != null ? savedDealer.getDescription() : "");
            response.put("address", savedDealer.getAddress() != null ? savedDealer.getAddress() : "");
            response.put("contact", savedDealer.getContact() != null ? savedDealer.getContact() : "");
            response.put("imagePath", savedDealer.getImagePath() != null ? savedDealer.getImagePath() : "");
            response.put("lat", savedDealer.getLat() != null ? savedDealer.getLat() : 0.0);
            response.put("lng", savedDealer.getLng() != null ? savedDealer.getLng() : 0.0);

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
                dealerMap.put("contact", dealer.getContact() != null ? dealer.getContact() : "");
                dealerMap.put("imagePath", dealer.getImagePath() != null ? dealer.getImagePath() : "");
                dealerMap.put("lat", dealer.getLat() != null ? dealer.getLat() : 0.0);
                dealerMap.put("lng", dealer.getLng() != null ? dealer.getLng() : 0.0);
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
        logger.info("Received POST /rest/api/products with payload: {}", payload);
        String name = payload.get("name");
        String description = payload.get("description");
        String priceStr = payload.get("price");
        String imageUrl = payload.get("imageUrl");
        String trimmedName = (name != null) ? name.trim() : null;

        if (!StringUtils.hasText(trimmedName)) {
            logger.warn("Validation failed: Product name is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il nome del prodotto è obbligatorio"));
        }

        if (!StringUtils.hasText(priceStr)) {
            logger.warn("Validation failed: Product price is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il prezzo del prodotto è obbligatorio"));
        }

        try {
            BigDecimal price = new BigDecimal(priceStr);
            Product product = new Product();
            product.setName(trimmedName);
            product.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            product.setPrice(price);
            product.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);

            logger.info("Calling dealerService.addProduct for product: {}", product.getName());
            Product savedProduct = dealerService.addProduct(product);
            logger.info("Product added successfully: id={}, name={}", savedProduct.getId(), savedProduct.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProduct.getId());
            response.put("name", savedProduct.getName());
            response.put("description", savedProduct.getDescription() != null ? savedProduct.getDescription() : "");
            response.put("price", savedProduct.getPrice());
            response.put("imageUrl", savedProduct.getImageUrl() != null ? savedProduct.getImageUrl() : "");

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid price format: {}", priceStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Formato del prezzo non valido"));
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
            response.put("name", product.getName());
            response.put("description", product.getDescription() != null ? product.getDescription() : "");
            response.put("price", product.getPrice());
            response.put("imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "");

            logger.info("Product retrieved successfully: id={}, name={}", product.getId(), product.getName());
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
        String name = payload.get("name");
        String description = payload.get("description");
        String priceStr = payload.get("price");
        String imageUrl = payload.get("imageUrl");
        String trimmedName = (name != null) ? name.trim() : null;

        if (!StringUtils.hasText(trimmedName)) {
            logger.warn("Validation failed: Product name is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il nome del prodotto è obbligatorio"));
        }

        if (!StringUtils.hasText(priceStr)) {
            logger.warn("Validation failed: Product price is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("message", "Il prezzo del prodotto è obbligatorio"));
        }

        try {
            BigDecimal price = new BigDecimal(priceStr);
            Product product = new Product();
            product.setName(trimmedName);
            product.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            product.setPrice(price);
            product.setImageUrl(StringUtils.hasText(imageUrl) ? imageUrl.trim() : null);

            logger.info("Calling dealerService.updateProduct for product: id={}, name={}", productId, product.getName());
            Product updatedProduct = dealerService.updateProduct(productId, product);
            logger.info("Product updated successfully: id={}, name={}", updatedProduct.getId(), updatedProduct.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedProduct.getId());
            response.put("name", updatedProduct.getName());
            response.put("description", updatedProduct.getDescription() != null ? updatedProduct.getDescription() : "");
            response.put("price", updatedProduct.getPrice());
            response.put("imageUrl", updatedProduct.getImageUrl() != null ? updatedProduct.getImageUrl() : "");

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid price format: {}", priceStr);
            return ResponseEntity.badRequest().body(Map.of("message", "Formato del prezzo non valido"));
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
}