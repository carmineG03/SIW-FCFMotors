package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.*;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ImageRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.service.DealerService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.ResponseEntity.*;

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

    @Autowired
    private ImageRepository imageRepository;

    @GetMapping("/manutenzione/dealer")
    @Transactional
    public String redirectDealerPage(Model model) {
        logger.debug("Accessing /rest/manutenzione/dealer");
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

        @PostMapping("/api/dealers")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> createOrUpdateDealer(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean isUpdate,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
    
        logger.info("POST /rest/api/dealers - name: {}, isUpdate: {}, images: {}",
                name, isUpdate, images != null ? images.size() : 0);
    
        if (!StringUtils.hasText(name)) {
            logger.warn("Nome concessionario mancante");
            return badRequest().body(Map.of("message", "Il nome del concessionario è obbligatorio"));
        }
    
        try {
            Dealer dealer = new Dealer();
            dealer.setName(name.trim());
            dealer.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            dealer.setAddress(StringUtils.hasText(address) ? address.trim() : null);
            dealer.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
            dealer.setEmail(StringUtils.hasText(email) ? email.trim() : null);
    
            List<Image> imageEntities = new ArrayList<>();
            
            // Per creazione (non update), le immagini sono obbligatorie
            if (!Boolean.TRUE.equals(isUpdate)) {
                if (images == null || images.isEmpty()) {
                    logger.info("No images provided for dealer creation: {}", name);
                    return badRequest().body(Map.of("message", "Seleziona almeno un'immagine per il concessionario"));
                }
            }
            
            // Processa le immagini se presenti
            if (images != null && !images.isEmpty()) {
                if (images.size() > 4) {
                    logger.warn("Superato il limite di 4 immagini: {}", images.size());
                    return badRequest().body(Map.of("message", "Massimo 4 immagini per il concessionario"));
                }
                
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setDealer(dealer);
                        imageEntities.add(img);
                    } else {
                        logger.warn("Empty file received: {}", file.getOriginalFilename());
                    }
                }
            }
            
            dealer.setImages(imageEntities);
    
            Dealer savedDealer = dealerService.saveDealer(dealer, Boolean.TRUE.equals(isUpdate));
            logger.info("Dealer saved successfully: id={}, name={}, images={}", 
                       savedDealer.getId(), savedDealer.getName(), savedDealer.getImages().size());
            
            // IMPORTANTE: Assicurati che la risposta contenga l'ID del dealer
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Concessionario creato con successo!");
            response.put("id", savedDealer.getId());
            response.put("name", savedDealer.getName());
            response.put("images", savedDealer.getImages().stream().map(Image::getId).toList());
            
            return ok(response);
            
        } catch (Exception e) {
            logger.error("Errore salvataggio dealer: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    @PutMapping("/api/dealers/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateDealer(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        logger.info("PUT /rest/api/dealers/{} - name: {}, images: {}", id, name, images != null ? images.size() : 0);

        if (!StringUtils.hasText(name)) {
            logger.warn("Nome concessionario mancante");
            return badRequest().body(Map.of("message", "Il nome del concessionario è obbligatorio"));
        }

        try {
            Dealer dealer = dealerService.findById(id);
            if (dealer == null) {
                logger.warn("Dealer not found: id={}", id);
                return status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Concessionario non trovato"));
            }

            dealer.setName(name.trim());
            dealer.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            dealer.setAddress(StringUtils.hasText(address) ? address.trim() : null);
            dealer.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
            dealer.setEmail(StringUtils.hasText(email) ? email.trim() : null);

            if (images != null && !images.isEmpty()) {
                if (images.size() > 4) {
                    logger.warn("Superato il limite di 4 immagini: {}", images.size());
                    return ResponseEntity.badRequest().body(Map.of("message", "Massimo 4 immagini per il concessionario"));
                }
                List<Image> imageEntities = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setDealer(dealer);
                        imageEntities.add(img);
                    } else {
                        logger.warn("Empty file received: {}", file.getOriginalFilename());
                    }
                }
                dealer.setImages(imageEntities);
            }

            Dealer savedDealer = dealerService.saveDealer(dealer, true);
            logger.info("Dealer updated: id={}, name={}, images={}", savedDealer.getId(), savedDealer.getName(), savedDealer.getImages().size());
            return ok(Map.of(
                    "id", savedDealer.getId(),
                    "name", savedDealer.getName(),
                    "images", savedDealer.getImages().stream().map(Image::getId).toList()
            ));
        } catch (Exception e) {
            logger.error("Errore aggiornamento dealer: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    @PostMapping("/api/products")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> addProduct(
            @RequestParam String model,
            @RequestParam String price,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) Integer mileage,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) Integer featuredUntil,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        logger.info("POST /rest/api/products - model: {}, images: {}", model, images != null ? images.size() : 0);

        if (!StringUtils.hasText(model) || !StringUtils.hasText(price)) {
            return badRequest().body(Map.of("message", "Modello e prezzo sono obbligatori"));
        }

        try {
            BigDecimal priceValue = new BigDecimal(price);
            Product product = new Product();
            product.setModel(model.trim());
            product.setBrand(StringUtils.hasText(brand) ? brand.trim() : null);
            product.setCategory(StringUtils.hasText(category) ? category.trim() : null);
            product.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            product.setPrice(priceValue);
            product.setMileage(mileage);
            product.setYear(year);
            product.setFuelType(StringUtils.hasText(fuelType) ? fuelType.trim() : null);
            product.setTransmission(StringUtils.hasText(transmission) ? transmission.trim() : null);
            product.setIsFeatured(isFeatured != null ? isFeatured : false);

            List<Image> imageEntities = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                if (images.size() > 10) {
                    return badRequest().body(Map.of("message", "Massimo 10 immagini per prodotto"));
                }
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setProduct(product);
                        imageEntities.add(img);
                    } else {
                        logger.warn("Empty file received: {}", file.getOriginalFilename());
                    }
                }
            } else {
                logger.info("No images provided for product: {}", model);
                return badRequest().body(Map.of("message", "Seleziona almeno un'immagine per il prodotto"));
            }
            product.setImages(imageEntities);

            if (Boolean.TRUE.equals(isFeatured) && featuredUntil != null && featuredUntil > 0) {
                product.setIsFeatured(true);
                product.setFeaturedUntil(LocalDateTime.now().plusDays(featuredUntil));
            }

            Product savedProduct = dealerService.addProduct(product);
            return ok(Map.of(
                    "id", savedProduct.getId(),
                    "model", savedProduct.getModel(),
                    "images", savedProduct.getImages().stream().map(Image::getId).toList()
            ));
        } catch (NumberFormatException e) {
            return badRequest().body(Map.of("message", "Prezzo non valido"));
        } catch (Exception e) {
            logger.error("Errore salvataggio prodotto: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    @PutMapping("/api/products/{productId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) Integer mileage,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) Integer featuredUntil,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        logger.info("Received PUT /rest/api/products/{} with model: {}, images: {}", productId, model, images != null ? images.size() : 0);

        try {
            Product existingProduct = dealerService.findProductById(productId);
            if (existingProduct == null) {
                logger.warn("Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Prodotto non trovato"));
            }

            if (StringUtils.hasText(model)) existingProduct.setModel(model.trim());
            if (StringUtils.hasText(brand)) existingProduct.setBrand(brand.trim());
            if (StringUtils.hasText(category)) existingProduct.setCategory(category.trim());
            if (StringUtils.hasText(description)) existingProduct.setDescription(description.trim());
            if (StringUtils.hasText(price)) existingProduct.setPrice(new BigDecimal(price));
            if (StringUtils.hasText(fuelType)) existingProduct.setFuelType(fuelType.trim());
            if (StringUtils.hasText(transmission)) existingProduct.setTransmission(transmission.trim());
            if (mileage != null) existingProduct.setMileage(mileage);
            if (year != null) existingProduct.setYear(year);
            if (isFeatured != null) existingProduct.setIsFeatured(isFeatured);

            if (images != null && !images.isEmpty()) {
                if (images.size() > 10) {
                    return badRequest().body(Map.of("message", "Massimo 10 immagini per prodotto"));
                }
                List<Image> imageEntities = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setProduct(existingProduct);
                        imageEntities.add(img);
                    } else {
                        logger.warn("Empty file received: {}", file.getOriginalFilename());
                    }
                }
                existingProduct.setImages(imageEntities);
            }

            if (isFeatured != null) {
                existingProduct.setIsFeatured(isFeatured);
                if (isFeatured && featuredUntil != null && featuredUntil > 0) {
                    existingProduct.setFeaturedUntil(LocalDateTime.now().plusDays(featuredUntil));
                } else if (!isFeatured) {
                    existingProduct.setFeaturedUntil(null);
                }
            }

            Product updatedProduct = dealerService.updateProduct(productId, existingProduct);

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
            response.put("images", updatedProduct.getImages() != null
                    ? updatedProduct.getImages().stream().map(Image::getId).toList()
                    : List.of());
            response.put("highlighted", updatedProduct.isFeatured());
            response.put("highlightExpiration", updatedProduct.getFeaturedUntil() != null ? updatedProduct.getFeaturedUntil().toString() : null);

            return ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid price format: {}", price);
            return badRequest().body(Map.of("message", "Formato del prezzo non valido"));
        } catch (Exception e) {
            logger.error("Unexpected error updating product: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    @GetMapping("/api/dealers")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> findDealers(@RequestParam(required = false) String query) {
        logger.info("Received GET /rest/api/dealers with query: '{}'", query);
        try {
            List<Dealer> dealers = dealerService.findByLocation(query);
            List<Map<String, Object>> response = dealers.stream().map(dealer -> {
                Map<String, Object> dealerMap = new HashMap<>();
                dealerMap.put("id", dealer.getId() != null ? dealer.getId() : 0L);
                dealerMap.put("name", dealer.getName() != null ? dealer.getName() : "");
                dealerMap.put("description", dealer.getDescription() != null ? dealer.getDescription() : "");
                dealerMap.put("address", dealer.getAddress() != null ? dealer.getAddress() : "");
                dealerMap.put("phone", dealer.getPhone() != null ? dealer.getPhone() : "");
                dealerMap.put("email", dealer.getEmail() != null ? dealer.getEmail() : "");
                List<Long> imageIds = (dealer.getImages() != null)
                        ? dealer.getImages().stream().map(Image::getId).toList()
                        : List.of();
                dealerMap.put("images", imageIds);
                return dealerMap;
            }).toList();
            logger.info("Returning {} dealers", response.size());
            return ok(response);
        } catch (Exception e) {
            logger.error("Error fetching dealers with query '{}': {}", query, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/dealers/create")
    @Transactional
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
    @Transactional
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

    @GetMapping("/api/products/{productId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        logger.info("Received GET /rest/api/products/{}", productId);
        try {
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND)
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
            List<Long> imageIds = (product.getImages() != null)
                    ? product.getImages().stream().map(Image::getId).toList()
                    : List.of();
            response.put("images", imageIds);
            response.put("highlighted", product.isFeatured());
            response.put("highlightExpiration", product.getFeaturedUntil() != null ? product.getFeaturedUntil().toString() : null);

            logger.info("Product retrieved successfully: id={}, model={}", product.getId(), product.getModel());
            return ok(response);
        } catch (Exception e) {
            logger.error("Error fetching product: id={}, error={}", productId, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @DeleteMapping("/api/products/{productId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        logger.info("Received DELETE /rest/api/products/{}", productId);
        try {
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Prodotto non trovato"));
            }

            dealerService.deleteProduct(productId);
            logger.info("Product deleted successfully: id={}", productId);
            return ok(Map.of("message", "Prodotto eliminato con successo"));
        } catch (Exception e) {
            logger.error("Error deleting product: id={}, error={}", productId, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/dealers")
    @Transactional(readOnly = true)
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
    public String deleteDealer(@PathVariable("id") Long id, 
                            RedirectAttributes redirectAttributes, 
                            Authentication authentication) {
        logger.info("Received GET /rest/manutenzione/dealer/delete_dealer/{}", id);
        
        try {
            // Verifica che l'utente sia autorizzato
            String currentUsername = authentication.getName();
            Dealer dealer = dealerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Concessionario non trovato"));
            
            if (!dealer.getOwner().getUsername().equals(currentUsername)) {
                logger.warn("Utente {} non autorizzato a eliminare il concessionario {}", currentUsername, id);
                redirectAttributes.addFlashAttribute("errorMessage", "Non sei autorizzato a eliminare questo concessionario");
                return "redirect:/rest/dealers/manage";
            }

            // Chiama il service per eliminare il dealer
            dealerService.deleteDealer(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Concessionario eliminato con successo.");
            return "redirect:/"; // Torna alla home
            
        } catch (Exception e) {
            logger.error("Error deleting dealer: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione del concessionario: " + e.getMessage());
            return "redirect:/rest/dealers/manage";
        }
    }   

    @PostMapping("/api/products/{productId}/highlight")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> highlightProduct(@PathVariable Long productId, @RequestBody Map<String, String> payload) {
        logger.info("Received POST /rest/api/products/{}/highlight with payload: {}", productId, payload);
        String durationStr = payload.get("duration");

        if (!StringUtils.hasText(durationStr)) {
            logger.warn("Validation failed: Highlight duration is missing or empty");
            return badRequest().body(Map.of("message", "La durata dell'evidenza è obbligatoria"));
        }

        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                logger.warn("Validation failed: Highlight duration must be positive");
                return badRequest().body(Map.of("message", "La durata deve essere un numero positivo"));
            }

            Product highlightedProduct = dealerService.highlightProduct(productId, duration);
            logger.info("Product highlighted successfully: id={}, model={}", highlightedProduct.getId(), highlightedProduct.getModel());

            Map<String, Object> response = new HashMap<>();
            response.put("id", highlightedProduct.getId());
            response.put("model", highlightedProduct.getModel());
            response.put("highlighted", highlightedProduct.isFeatured());
            response.put("highlightExpiration", highlightedProduct.getFeaturedUntil() != null ? highlightedProduct.getFeaturedUntil().toString() : null);

            return ok(response);
        } catch (NumberFormatException e) {
            logger.error("Invalid duration format: {}", durationStr);
            return badRequest().body(Map.of("message", "Formato della durata non valido"));
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error highlighting product: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @PostMapping("/api/products/{productId}/remove-highlight")
    @ResponseBody
    @Transactional
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

            return ok(response);
        } catch (IllegalStateException e) {
            logger.error("State error: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing highlight: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/dealer/quote-requests/{dealerId}")
    @Transactional
    public String viewQuoteRequests(@PathVariable Long dealerId, Model model) {
        logger.info("Accessing quote requests for dealer ID: {}", dealerId);
        try {
            Dealer dealer = dealerRepository.findById(dealerId)
                    .orElseThrow(() -> new IllegalStateException("Dealer non trovato"));

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
    @Transactional
    public String respondToQuoteRequest(@PathVariable Long requestId, @RequestParam String responseMessage, RedirectAttributes redirectAttributes) {
        logger.info("Responding to quote request ID: {}", requestId);
        QuoteRequest quoteRequest = new QuoteRequest();
        try {
            quoteRequest = quoteRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Richiesta di preventivo non trovata"));

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Dealer authenticatedDealer = dealerService.findByOwner();
            if (authenticatedDealer == null || !authenticatedDealer.getId().equals(quoteRequest.getDealer().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Accesso non autorizzato.");
                return "redirect:/rest/dealers/manage";
            }

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

            quoteRequest.setStatus("RESPONDED");
            quoteRequest.setResponseMessage(responseMessage);
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
    @Transactional
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
            return ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/api/users/featured-limit")
    @ResponseBody
    @Transactional(readOnly = true)
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
                return status(HttpStatus.BAD_REQUEST)
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
            return ok(response);
        } catch (IllegalStateException e) {
            logger.error("Errore nello stato: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Errore durante il recupero del limite di evidenza: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server"));
        }
    }

    @GetMapping("/dealers/{id}")
    @Transactional(readOnly = true)
    public String showDealerDetailPage(@PathVariable("id") Long id,
                                       @RequestParam(value = "productId", required = false) Long productId,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        logger.info("Accessing dealer detail page for dealer ID: {}", id);
        try {
            Dealer dealer = dealerService.findById(id);
            if (dealer == null) {
                logger.error("Dealer not found: id={}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Concessionario non trovato.");
                return "redirect:/dealers";
            }
            List<Product> products = dealerService.getProductsByDealerOwner(dealer);
            logger.info("Found {} products for dealer ID: {}", products.size(), id);
            model.addAttribute("dealer", dealer);
            model.addAttribute("products", products);
            return "dealer_detail";
        } catch (Exception e) {
            logger.error("Error loading dealer detail page: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore nel caricamento della pagina del concessionario.");
            return "redirect:/dealers";
        }
    }

    @GetMapping("/api/images/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return imageRepository.findById(id)
                .map(image -> ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .body(image.getData()))
                .orElseGet(() -> notFound().build());
    }
}