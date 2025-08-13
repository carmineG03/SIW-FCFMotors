package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.*;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.service.DealerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
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

/**
 * Controller REST per la gestione completa dei concessionari
 * Gestisce creazione, modifica, visualizzazione dealer e loro prodotti
 * Include sistema di preventivi, evidenziazione prodotti e gestione immagini
 */
@Controller
@RequestMapping("/rest")
public class DealerController {

    private static final Logger logger = LoggerFactory.getLogger(DealerController.class);

    // === CONFIGURAZIONE LIMITI SISTEMA ===
    /** Numero massimo di immagini consentite per un concessionario */
    private static final int MAX_DEALER_IMAGES = 4;
    
    /** Numero massimo di immagini consentite per singolo prodotto */
    private static final int MAX_PRODUCT_IMAGES = 10;
    
    /** Durata predefinita evidenziazione prodotti (giorni) */
    private static final int DEFAULT_FEATURED_DAYS = 30;
    
    /** Email predefinita per notifiche sistema */
    private static final String SYSTEM_EMAIL = "info@fcfmotors.com";

    // === INJECTION DIPENDENZE ===
    @Autowired private DealerService dealerService;
    @Autowired private DealerRepository dealerRepository;
    @Autowired private QuoteRequestRepository quoteRequestRepository;
    @Autowired private JavaMailSender mailSender;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    /**
     * Endpoint di reindirizzamento principale per dealer
     * Verifica se il dealer esiste e reindirizza alla pagina appropriata
     */
    @GetMapping("/manutenzione/dealer")
    @Transactional
    public String redirectDealerPage(Model model) {
        logger.debug("🔄 Accessing /rest/manutenzione/dealer");
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("❌ No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("✅ Authenticated user: name={}", username);
            
            // === RICERCA DEALER ESISTENTE ===
            Dealer dealer = dealerService.findByOwner();
            if (dealer != null) {
                logger.info("🏪 Dealer found for user '{}': id={}, name={}", username, dealer.getId(), dealer.getName());
                return "redirect:/rest/dealers/manage";
            } else {
                logger.warn("🆕 No dealer found for user '{}', redirecting to create page", username);
                return "redirect:/rest/dealers/create";
            }
            
        } catch (Exception e) {
            logger.error("❌ Error redirecting dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "redirect:/rest/dealers/create";
        }
    }

    /**
     * API per creazione o aggiornamento concessionario
     * Gestisce upload immagini, validazione dati e salvataggio
     */
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

        logger.info("🔨 POST /rest/api/dealers - name: {}, isUpdate: {}, images: {}",
                name, isUpdate, images != null ? images.size() : 0);

        // === VALIDAZIONE NOME OBBLIGATORIO ===
        if (!StringUtils.hasText(name)) {
            logger.warn("❌ Nome concessionario mancante");
            return badRequest().body(Map.of("message", "Il nome del concessionario è obbligatorio"));
        }

        try {
            // === CREAZIONE OGGETTO DEALER ===
            Dealer dealer = new Dealer();
            dealer.setName(name.trim());
            dealer.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            dealer.setAddress(StringUtils.hasText(address) ? address.trim() : null);
            dealer.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
            dealer.setEmail(StringUtils.hasText(email) ? email.trim() : null);

            // === VALIDAZIONE E PROCESSING IMMAGINI ===
            List<Image> imageEntities = new ArrayList<>();

            // Per creazione (non update), le immagini sono obbligatorie
            if (!Boolean.TRUE.equals(isUpdate)) {
                if (images == null || images.isEmpty()) {
                    logger.info("❌ No images provided for dealer creation: {}", name);
                    return badRequest().body(Map.of("message", "Seleziona almeno un'immagine per il concessionario"));
                }
            }

            // Processa le immagini se presenti
            if (images != null && !images.isEmpty()) {
                if (images.size() > MAX_DEALER_IMAGES) {
                    logger.warn("❌ Superato il limite di {} immagini: {}", MAX_DEALER_IMAGES, images.size());
                    return badRequest().body(Map.of("message", "Massimo " + MAX_DEALER_IMAGES + " immagini per il concessionario"));
                }

                // Processing di ogni immagine
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("📸 Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setDealer(dealer);
                        imageEntities.add(img);
                    } else {
                        logger.warn("⚠️ Empty file received: {}", file.getOriginalFilename());
                    }
                }
            }

            dealer.setImages(imageEntities);

            // === SALVATAGGIO DEALER ===
            Dealer savedDealer = dealerService.saveDealer(dealer, Boolean.TRUE.equals(isUpdate));
            logger.info("✅ Dealer saved successfully: id={}, name={}, images={}",
                    savedDealer.getId(), savedDealer.getName(), savedDealer.getImages().size());

            // === PREPARAZIONE RISPOSTA ===
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Concessionario creato con successo!");
            response.put("id", savedDealer.getId());
            response.put("name", savedDealer.getName());
            response.put("images", savedDealer.getImages().stream().map(Image::getId).toList());

            return ok(response);

        } catch (Exception e) {
            logger.error("❌ Errore salvataggio dealer: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    /**
     * API per aggiornamento concessionario esistente
     * Permette modifica di tutti i campi e sostituzione immagini
     */
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

        logger.info("🔄 PUT /rest/api/dealers/{} - name: {}, images: {}", id, name, images != null ? images.size() : 0);

        // === VALIDAZIONE NOME ===
        if (!StringUtils.hasText(name)) {
            logger.warn("❌ Nome concessionario mancante");
            return badRequest().body(Map.of("message", "Il nome del concessionario è obbligatorio"));
        }

        try {
            // === RICERCA DEALER ESISTENTE ===
            Dealer dealer = dealerService.findById(id);
            if (dealer == null) {
                logger.warn("❌ Dealer not found: id={}", id);
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Concessionario non trovato"));
            }

            // === AGGIORNAMENTO CAMPI ===
            dealer.setName(name.trim());
            dealer.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            dealer.setAddress(StringUtils.hasText(address) ? address.trim() : null);
            dealer.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
            dealer.setEmail(StringUtils.hasText(email) ? email.trim() : null);

            // === PROCESSING NUOVE IMMAGINI ===
            if (images != null && !images.isEmpty()) {
                if (images.size() > MAX_DEALER_IMAGES) {
                    logger.warn("❌ Superato il limite di {} immagini: {}", MAX_DEALER_IMAGES, images.size());
                    return ResponseEntity.badRequest().body(Map.of("message", "Massimo " + MAX_DEALER_IMAGES + " immagini per il concessionario"));
                }
                
                List<Image> imageEntities = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("📸 Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setDealer(dealer);
                        imageEntities.add(img);
                    } else {
                        logger.warn("⚠️ Empty file received: {}", file.getOriginalFilename());
                    }
                }
                dealer.setImages(imageEntities);
            }

            // === SALVATAGGIO MODIFICHE ===
            Dealer savedDealer = dealerService.saveDealer(dealer, true);
            logger.info("✅ Dealer updated: id={}, name={}, images={}", savedDealer.getId(), savedDealer.getName(), savedDealer.getImages().size());
            
            return ok(Map.of(
                    "id", savedDealer.getId(),
                    "name", savedDealer.getName(),
                    "images", savedDealer.getImages().stream().map(Image::getId).toList()));
                    
        } catch (Exception e) {
            logger.error("❌ Errore aggiornamento dealer: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    /**
     * API per aggiunta nuovo prodotto al concessionario
     * Gestisce validazione, upload immagini e impostazioni evidenziazione
     */
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

        logger.info("➕ POST /rest/api/products - model: {}, images: {}", model, images != null ? images.size() : 0);

        // === VALIDAZIONE CAMPI OBBLIGATORI ===
        if (!StringUtils.hasText(model) || !StringUtils.hasText(price)) {
            return badRequest().body(Map.of("message", "Modello e prezzo sono obbligatori"));
        }

        try {
            // === VALIDAZIONE E CONVERSIONE PREZZO ===
            BigDecimal priceValue = new BigDecimal(price);
            
            // === CREAZIONE PRODOTTO ===
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

            // === PROCESSING IMMAGINI (OBBLIGATORIE) ===
            List<Image> imageEntities = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                if (images.size() > MAX_PRODUCT_IMAGES) {
                    return badRequest().body(Map.of("message", "Massimo " + MAX_PRODUCT_IMAGES + " immagini per prodotto"));
                }
                
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("📸 Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setProduct(product);
                        imageEntities.add(img);
                    } else {
                        logger.warn("⚠️ Empty file received: {}", file.getOriginalFilename());
                    }
                }
            } else {
                logger.info("❌ No images provided for product: {}", model);
                return badRequest().body(Map.of("message", "Seleziona almeno un'immagine per il prodotto"));
            }
            product.setImages(imageEntities);

            // === GESTIONE EVIDENZIAZIONE ===
            if (Boolean.TRUE.equals(isFeatured) && featuredUntil != null && featuredUntil > 0) {
                product.setIsFeatured(true);
                product.setFeaturedUntil(LocalDateTime.now().plusDays(featuredUntil));
            }

            // === SALVATAGGIO PRODOTTO ===
            Product savedProduct = dealerService.addProduct(product);
            return ok(Map.of(
                    "id", savedProduct.getId(),
                    "model", savedProduct.getModel(),
                    "images", savedProduct.getImages().stream().map(Image::getId).toList()));
                    
        } catch (NumberFormatException e) {
            return badRequest().body(Map.of("message", "Prezzo non valido"));
        } catch (Exception e) {
            logger.error("❌ Errore salvataggio prodotto: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    /**
     * API per aggiornamento prodotto esistente
     * Permette modifica parziale o completa di tutti i campi
     */
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

        logger.info("🔄 Received PUT /rest/api/products/{} with model: {}, images: {}", productId, model, images != null ? images.size() : 0);

        try {
            // === VERIFICA ESISTENZA PRODOTTO ===
            Product existingProduct = dealerService.findProductById(productId);
            if (existingProduct == null) {
                logger.warn("❌ Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));
            }

            // === AGGIORNAMENTO CAMPI TESTUALI ===
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

            // === PROCESSING NUOVE IMMAGINI ===
            if (images != null && !images.isEmpty()) {
                if (images.size() > MAX_PRODUCT_IMAGES) {
                    return badRequest().body(Map.of("message", "Massimo " + MAX_PRODUCT_IMAGES + " immagini per prodotto"));
                }
                
                List<Image> imageEntities = new ArrayList<>();
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        logger.info("📸 Processing image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
                        Image img = dealerService.saveImageFile(file);
                        img.setProduct(existingProduct);
                        imageEntities.add(img);
                    } else {
                        logger.warn("⚠️ Empty file received: {}", file.getOriginalFilename());
                    }
                }
                existingProduct.setImages(imageEntities);
            }

            // === GESTIONE EVIDENZIAZIONE ===
            if (isFeatured != null) {
                existingProduct.setIsFeatured(isFeatured);
                if (isFeatured && featuredUntil != null && featuredUntil > 0) {
                    existingProduct.setFeaturedUntil(LocalDateTime.now().plusDays(featuredUntil));
                } else if (!isFeatured) {
                    existingProduct.setFeaturedUntil(null);
                }
            }

            // === SALVATAGGIO MODIFICHE ===
            Product updatedProduct = dealerService.updateProduct(productId, existingProduct);

            // === PREPARAZIONE RISPOSTA DETTAGLIATA ===
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
            response.put("images", updatedProduct.getImages() != null ? updatedProduct.getImages().stream().map(Image::getId).toList() : List.of());
            response.put("highlighted", updatedProduct.isFeatured());
            response.put("highlightExpiration", updatedProduct.getFeaturedUntil() != null ? updatedProduct.getFeaturedUntil().toString() : null);

            return ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("❌ Invalid price format: {}", price);
            return badRequest().body(Map.of("message", "Formato del prezzo non valido"));
        } catch (Exception e) {
            logger.error("❌ Unexpected error updating product: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Errore interno del server: " + e.getMessage()));
        }
    }

    /**
     * API per ricerca concessionari con filtro località
     * Restituisce lista concessionari con tutte le informazioni
     */
    @GetMapping("/api/dealers")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> findDealers(@RequestParam(required = false) String query) {
        logger.info("🔍 Received GET /rest/api/dealers with query: '{}'", query);
        
        try {
            // === RICERCA DEALERS ===
            List<Dealer> dealers = dealerService.findByLocation(query);
            
            // === MAPPING RESPONSE ===
            List<Map<String, Object>> response = dealers.stream().map(dealer -> {
                Map<String, Object> dealerMap = new HashMap<>();
                dealerMap.put("id", dealer.getId() != null ? dealer.getId() : 0L);
                dealerMap.put("name", dealer.getName() != null ? dealer.getName() : "");
                dealerMap.put("description", dealer.getDescription() != null ? dealer.getDescription() : "");
                dealerMap.put("address", dealer.getAddress() != null ? dealer.getAddress() : "");
                dealerMap.put("phone", dealer.getPhone() != null ? dealer.getPhone() : "");
                dealerMap.put("email", dealer.getEmail() != null ? dealer.getEmail() : "");
                List<Long> imageIds = (dealer.getImages() != null) ? dealer.getImages().stream().map(Image::getId).toList() : List.of();
                dealerMap.put("images", imageIds);
                return dealerMap;
            }).toList();
            
            logger.info("✅ Returning {} dealers", response.size());
            return ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching dealers with query '{}': {}", query, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    /**
     * Pagina creazione concessionario
     * Controlla se dealer già esiste prima di mostrare il form
     */
    @GetMapping("/dealers/create")
    @Transactional
    public String showCreateDealerPage(Model model) {
        logger.debug("📝 Accessing create dealer page");
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("❌ No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("✅ Authenticated user: name={}", username);
            
            // === VERIFICA DEALER NON ESISTENTE ===
            Dealer dealer = dealerService.findByOwner();
            if (dealer != null) {
                logger.info("🔄 Dealer already exists for user '{}': id={}, name={}", username, dealer.getId(), dealer.getName());
                return "redirect:/rest/dealers/manage";
            }

            // === PREPARAZIONE MODEL ===
            model.addAttribute("dealer", new Dealer());
            return "create_dealer";
            
        } catch (Exception e) {
            logger.error("❌ Error loading create dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "create_dealer";
        }
    }

    /**
     * Pagina gestione concessionario esistente
     * Mostra dealer con tutti i suoi prodotti e statistiche
     */
    @GetMapping("/dealers/manage")
    @Transactional
    public String showManageDealerPage(Model model) {
        logger.debug("🏪 Accessing manage dealer page");
        
        try {
            // === VERIFICA AUTENTICAZIONE ===
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("❌ No authenticated user found, redirecting to login");
                model.addAttribute("errorMessage", "Effettua il login per accedere alla pagina del concessionario.");
                return "redirect:/login";
            }

            String username = auth.getName();
            logger.info("✅ Authenticated user: name={}", username);
            
            // === VERIFICA ESISTENZA DEALER ===
            Dealer dealer = dealerService.findByOwner();
            if (dealer == null) {
                logger.warn("❌ No dealer found for user '{}', redirecting to create page", username);
                return "redirect:/rest/dealers/create";
            }

            // === CARICAMENTO PRODOTTI DEALER ===
            List<Product> products = dealerService.getProductsByDealer();
            logger.info("✅ Dealer found for user '{}': id={}, name={}, products={}", username, dealer.getId(), dealer.getName(), products.size());
            
            // === PREPARAZIONE MODEL ===
            model.addAttribute("dealer", dealer);
            model.addAttribute("products", products);
            return "manage_dealer";
            
        } catch (Exception e) {
            logger.error("❌ Error loading manage dealer page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore imprevisto durante il caricamento della pagina.");
            return "redirect:/rest/dealers/create";
        }
    }

    /**
     * API per recupero singolo prodotto con dettagli completi
     */
    @GetMapping("/api/products/{productId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProduct(@PathVariable Long productId) {
        logger.info("📋 Received GET /rest/api/products/{}", productId);
        
        try {
            // === RICERCA PRODOTTO ===
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("❌ Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));
            }

            // === PREPARAZIONE RISPOSTA DETTAGLIATA ===
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
            List<Long> imageIds = (product.getImages() != null) ? product.getImages().stream().map(Image::getId).toList() : List.of();
            response.put("images", imageIds);
            response.put("highlighted", product.isFeatured());
            response.put("highlightExpiration", product.getFeaturedUntil() != null ? product.getFeaturedUntil().toString() : null);

            logger.info("✅ Product retrieved successfully: id={}, model={}", product.getId(), product.getModel());
            return ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error fetching product: id={}, error={}", productId, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Errore interno del server"));
        }
    }

    /**
     * API per eliminazione prodotto
     * Rimuove prodotto e tutte le immagini associate
     */
    @DeleteMapping("/api/products/{productId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        logger.info("🗑️ Received DELETE /rest/api/products/{}", productId);
        
        try {
            // === VERIFICA ESISTENZA PRODOTTO ===
            Product product = dealerService.findProductById(productId);
            if (product == null) {
                logger.warn("❌ Product not found: id={}", productId);
                return status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));
            }

            // === ELIMINAZIONE PRODOTTO ===
            dealerService.deleteProduct(productId);
            logger.info("✅ Product deleted successfully: id={}", productId);
            return ok(Map.of("message", "Prodotto eliminato con successo"));
            
        } catch (Exception e) {
            logger.error("❌ Error deleting product: id={}, error={}", productId, e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Errore interno del server"));
        }
    }

    /**
     * Pagina pubblica lista concessionari
     * Mostra tutti i dealer registrati
     */
    @GetMapping("/dealers")
    @Transactional(readOnly = true)
    public String showDealersPage(Model model) {
        logger.info("📋 Accessing /dealers page");
        
        try {
            // === CARICAMENTO TUTTI I DEALER ===
            List<Dealer> dealers = dealerService.findAll();
            model.addAttribute("dealers", dealers);
            logger.info("✅ Loaded {} dealers", dealers.size());
            return "dealers";
            
        } catch (Exception e) {
            logger.error("❌ Error loading dealers page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento della pagina dei concessionari.");
            return "dealers";
        }
    }

    /**
     * Eliminazione concessionario (solo proprietario autorizzato)
     * Rimuove dealer, prodotti associati e immagini
     */
    @GetMapping("/manutenzione/dealer/delete_dealer/{id}")
    public String deleteDealer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        logger.info("🗑️ Received GET /rest/manutenzione/dealer/delete_dealer/{}", id);

        try {
            // === VERIFICA AUTORIZZAZIONE ===
            String currentUsername = authentication.getName();
            Dealer dealer = dealerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Concessionario non trovato"));

            if (!dealer.getOwner().getUsername().equals(currentUsername)) {
                logger.warn("❌ Utente {} non autorizzato a eliminare il concessionario {}", currentUsername, id);
                redirectAttributes.addFlashAttribute("errorMessage", "Non sei autorizzato a eliminare questo concessionario");
                return "redirect:/rest/dealers/manage";
            }

            // === ELIMINAZIONE DEALER ===
            dealerService.deleteDealer(id);
            logger.info("✅ Dealer deleted successfully: id={}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Concessionario eliminato con successo.");
            return "redirect:/"; // Torna alla home

        } catch (Exception e) {
            logger.error("❌ Error deleting dealer: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione del concessionario: " + e.getMessage());
            return "redirect:/rest/dealers/manage";
        }
    }

    /**
     * API per evidenziazione prodotto con durata personalizzata
     * Mette un prodotto in evidenza per il numero di giorni specificato
     */
    @PostMapping("/api/products/{productId}/highlight")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> highlightProduct(@PathVariable Long productId, @RequestBody Map<String, String> payload) {
        logger.info("🌟 Received POST /rest/api/products/{}/highlight with payload: {}", productId, payload);
        
        String durationStr = payload.get("duration");

        // === VALIDAZIONE DURATA ===
        if (!StringUtils.hasText(durationStr)) {
            logger.warn("❌ Validation failed: Highlight duration is missing or empty");
            return badRequest().body(Map.of("message", "La durata dell'evidenza è obbligatoria"));
        }

        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                logger.warn("❌ Validation failed: Highlight duration must be positive");
                return badRequest().body(Map.of("message", "La durata deve essere un numero positivo"));
            }

            // === EVIDENZIAZIONE PRODOTTO ===
            Product highlightedProduct = dealerService.highlightProduct(productId, duration);
            logger.info("✅ Product highlighted successfully: id={}, model={}", highlightedProduct.getId(), highlightedProduct.getModel());

            // === PREPARAZIONE RISPOSTA ===
            Map<String, Object> response = new HashMap<>();
            response.put("id", highlightedProduct.getId());
            response.put("model", highlightedProduct.getModel());
            response.put("highlighted", highlightedProduct.isFeatured());
            response.put("highlightExpiration", highlightedProduct.getFeaturedUntil() != null ? highlightedProduct.getFeaturedUntil().toString() : null);

            return ok(response);
            
        } catch (NumberFormatException e) {
            logger.error("❌ Invalid duration format: {}", durationStr);
            return badRequest().body(Map.of("message", "Formato della durata non valido"));
        } catch (IllegalStateException e) {
            logger.error("❌ State error: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Unexpected error highlighting product: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Errore interno del server"));
        }
    }

    /**
     * API per rimozione evidenziazione prodotto (metodo alternativo)
     * Rimuove evidenziazione con controlli di autorizzazione
     */
    @PostMapping("/products/{id}/remove-featured")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> removeProductFeatured(@PathVariable Long id) {
        logger.info("🌟❌ POST /rest/products/{}/remove-featured", id);

        Map<String, Object> response = new HashMap<>();

        try {
            // === VERIFICA AUTENTICAZIONE ===
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(username)) {
                response.put("success", false);
                response.put("error", "Non autorizzato");
                return status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // === VERIFICA ESISTENZA UTENTE E PRODOTTO ===
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.put("success", false);
                response.put("error", "Utente non trovato");
                return badRequest().body(response);
            }

            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                response.put("success", false);
                response.put("error", "Prodotto non trovato");
                return status(HttpStatus.NOT_FOUND).body(response);
            }

            // === VERIFICA PROPRIETARIO ===
            if (!product.getSeller().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("error", "Non autorizzato");
                return status(HttpStatus.FORBIDDEN).body(response);
            }

            // === RIMOZIONE EVIDENZA ===
            product.setIsFeatured(false);
            product.setFeaturedUntil(null);
            productRepository.save(product);

            response.put("success", true);
            response.put("message", "Evidenza rimossa con successo!");

            logger.info("✅ Product featured removed: id={}, user={}", id, username);
            return ok(response);

        } catch (Exception e) {
            logger.error("❌ Error removing featured: id={}, error={}", id, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Errore interno");
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API per rimozione evidenziazione prodotto (metodo principale)
     * Utilizza il service per rimuovere evidenziazione
     */
    @PostMapping("/api/products/{productId}/remove-highlight")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> removeHighlight(@PathVariable Long productId) {
        logger.info("🌟❌ Received POST /rest/api/products/{}/remove-highlight", productId);
        
        try {
            // === RIMOZIONE EVIDENZIAZIONE ===
            Product updatedProduct = dealerService.removeHighlight(productId);
            logger.info("✅ Highlight removed successfully: id={}, model={}", updatedProduct.getId(), updatedProduct.getModel());

            // === PREPARAZIONE RISPOSTA ===
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedProduct.getId());
            response.put("model", updatedProduct.getModel());
            response.put("highlighted", updatedProduct.isFeatured());
            response.put("highlightExpiration", updatedProduct.getFeaturedUntil() != null ? updatedProduct.getFeaturedUntil().toString() : null);

            return ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("❌ State error: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Unexpected error removing highlight: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Errore interno del server"));
        }
    }

    /**
     * Pagina visualizzazione preventivi per dealer
     * Solo il proprietario del dealer può accedere ai suoi preventivi
     */
    @GetMapping("/dealer/quote-requests/{dealerId}")
    @Transactional
    public String viewQuoteRequests(@PathVariable Long dealerId, Model model) {
        logger.info("💬 Accessing quote requests for dealer ID: {}", dealerId);
        
        try {
            // === VERIFICA ESISTENZA DEALER ===
            Dealer dealer = dealerRepository.findById(dealerId)
                    .orElseThrow(() -> new IllegalStateException("Dealer non trovato"));

            // === VERIFICA AUTORIZZAZIONE ===
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Dealer authenticatedDealer = dealerService.findByOwner();
            if (authenticatedDealer == null || !authenticatedDealer.getId().equals(dealerId)) {
                logger.warn("❌ Unauthorized access to quote requests for dealer {}", dealerId);
                model.addAttribute("errorMessage", "Accesso non autorizzato.");
                return "redirect:/rest/dealers/manage";
            }

            // === CARICAMENTO PREVENTIVI ===
            List<QuoteRequest> quoteRequests = quoteRequestRepository.findByDealerId(dealerId);
            logger.info("✅ Found {} quote requests for dealer {}", quoteRequests.size(), dealerId);
            
            model.addAttribute("quoteRequests", quoteRequests);
            model.addAttribute("dealerId", dealerId);
            return "quote_requests";
            
        } catch (IllegalStateException e) {
            logger.error("❌ Error loading quote requests for dealer ID {}: {}", dealerId, e.getMessage(), e);
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/rest/dealers/manage";
        } catch (Exception e) {
            logger.error("❌ Unexpected error loading quote requests for dealer ID {}: {}", dealerId, e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento delle richieste di preventivo.");
            return "redirect:/rest/dealers/manage";
        }
    }

    /**
     * Risposta a richiesta di preventivo con invio email
     * Solo il dealer proprietario può rispondere ai suoi preventivi
     */
    @PostMapping("/dealer/quote-requests/respond/{requestId}")
    @Transactional
    public String respondToQuoteRequest(@PathVariable Long requestId, @RequestParam String responseMessage, RedirectAttributes redirectAttributes) {
        logger.info("💬📧 Responding to quote request ID: {}", requestId);
        
        QuoteRequest quoteRequest = new QuoteRequest();
        try {
            // === VERIFICA ESISTENZA PREVENTIVO ===
            quoteRequest = quoteRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Richiesta di preventivo non trovata"));

            // === VERIFICA AUTORIZZAZIONE ===
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Dealer authenticatedDealer = dealerService.findByOwner();
            if (authenticatedDealer == null || !authenticatedDealer.getId().equals(quoteRequest.getDealer().getId())) {
                logger.warn("❌ Unauthorized response to quote request {}", requestId);
                redirectAttributes.addFlashAttribute("errorMessage", "Accesso non autorizzato.");
                return "redirect:/rest/dealers/manage";
            }

            // === INVIO EMAIL RISPOSTA ===
            SimpleMailMessage message = new SimpleMailMessage();
            Product product = quoteRequest.getProduct();
            String productDisplayName = (product.getBrand() != null ? product.getBrand() + " " : "") +
                    (product.getModel() != null ? product.getModel() : "");
            
            message.setTo(quoteRequest.getUserEmail());
            message.setSubject("Risposta alla tua richiesta di preventivo - FCF Motors");
            message.setText("Gentile cliente,\n\nAbbiamo ricevuto la tua richiesta di preventivo per il prodotto: " +
                    productDisplayName + ".\n\nRisposta:\n" + responseMessage +
                    "\n\nGrazie per aver scelto FCF Motors!\nIl team FCF Motors");
            message.setFrom(SYSTEM_EMAIL);
            
            mailSender.send(message);
            logger.info("📧 Response email sent to: {}", quoteRequest.getUserEmail());

            // === AGGIORNAMENTO STATO PREVENTIVO ===
            quoteRequest.setStatus("RESPONDED");
            quoteRequest.setResponseMessage(responseMessage);
            quoteRequestRepository.save(quoteRequest);

            logger.info("✅ Quote request response completed: ID={}", requestId);
            redirectAttributes.addFlashAttribute("successMessage", "Risposta inviata con successo!");
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();

        } catch (IllegalStateException e) {
            logger.error("❌ Error responding to quote request: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();
        } catch (Exception e) {
            logger.error("❌ Unexpected error responding to quote request: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'invio della risposta.");
            return "redirect:/rest/dealer/quote-requests/" + quoteRequest.getDealer().getId();
        }
    }

    /**
     * API per impostazione prodotto in evidenza
     * Controlla limiti abbonamento e mette prodotto in evidenza per durata predefinita
     */
    @PostMapping("/products/{id}/set-featured")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> setProductFeatured(@PathVariable Long id) {
        logger.info("🌟➕ POST /rest/products/{}/set-featured", id);

        Map<String, Object> response = new HashMap<>();

        try {
            // === VERIFICA AUTENTICAZIONE ===
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(username)) {
                response.put("success", false);
                response.put("error", "Non autorizzato - login richiesto");
                return status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // === VERIFICA ESISTENZA UTENTE E PRODOTTO ===
            User user = userRepository.findByUsername(username).orElse(null);
            Product product = productRepository.findById(id).orElse(null);
            
            if (user == null || product == null) {
                response.put("success", false);
                response.put("error", user == null ? "Utente non trovato" : "Prodotto non trovato");
                return user == null ? badRequest().body(response) : status(HttpStatus.NOT_FOUND).body(response);
            }

            // === VERIFICA PROPRIETARIO ===
            if (!product.getSeller().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("error", "Non sei il proprietario di questo prodotto");
                return status(HttpStatus.FORBIDDEN).body(response);
            }

            // === VERIFICA LIMITI EVIDENZIAZIONE ===
            long currentFeaturedCount = productRepository.countBySellerAndIsFeaturedTrue(user);
            int maxFeatured = (user.getSubscription() != null) ? user.getSubscription().getMaxFeaturedCars() : 1;

            if (currentFeaturedCount >= maxFeatured) {
                response.put("success", false);
                response.put("error", "Limite massimo prodotti in evidenza raggiunto (" + maxFeatured + ")");
                return badRequest().body(response);
            }

            // === IMPOSTAZIONE EVIDENZIAZIONE ===
            product.setIsFeatured(true);
            product.setFeaturedUntil(LocalDateTime.now().plusDays(DEFAULT_FEATURED_DAYS));
            productRepository.save(product);

            response.put("success", true);
            response.put("message", "Prodotto messo in evidenza con successo!");

            logger.info("✅ Product set as featured: id={}, user={}, days={}", id, username, DEFAULT_FEATURED_DAYS);
            return ok(response);

        } catch (Exception e) {
            logger.error("❌ Error setting featured: id={}, error={}", id, e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Errore interno: " + e.getMessage());
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API per recupero limiti evidenziazione utente
     * Restituisce limiti attuali e massimi per l'abbonamento
     */
    @GetMapping("/api/users/featured-limit")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFeaturedLimit() {
        logger.info("🔢 Received GET /rest/api/users/featured-limit");
        
        try {
            // === VERIFICA AUTENTICAZIONE E UTENTE ===
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.debug("Authenticated user: {}", username);
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Utente non trovato: {}", username);
                        return new IllegalStateException("Utente non trovato");
                    });

            // === VERIFICA ABBONAMENTO ===
            Subscription subscription = user.getSubscription();
            if (subscription == null) {
                logger.warn("❌ Nessun abbonamento trovato per l'utente: {}", username);
                return status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Nessun abbonamento associato. Contatta l'assistenza."));
            }

            // === CALCOLO LIMITI ===
            int maxFeaturedProducts = subscription.getMaxFeaturedCars();
            logger.debug("Subscription details for user {}: id={}, maxFeaturedProducts={}", username, subscription.getId(), maxFeaturedProducts);
            
            if (maxFeaturedProducts <= 0) {
                logger.warn("⚠️ maxFeaturedProducts non valido per l'utente {}: {}", username, maxFeaturedProducts);
            }
            
            long currentFeaturedCount = productRepository.countBySellerAndIsFeaturedTrue(user);
            logger.info("✅ Featured limit for user {}: currentFeaturedCount={}, maxFeaturedProducts={}", username, currentFeaturedCount, maxFeaturedProducts);

            // === PREPARAZIONE RISPOSTA ===
            Map<String, Object> response = new HashMap<>();
            response.put("currentFeaturedCount", currentFeaturedCount);
            response.put("maxFeaturedProducts", maxFeaturedProducts);
            return ok(response);
            
        } catch (IllegalStateException e) {
            logger.error("❌ Errore nello stato: {}", e.getMessage());
            return status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Errore durante il recupero del limite di evidenza: {}", e.getMessage(), e);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Errore interno del server"));
        }
    }

    /**
     * Pagina dettaglio singolo concessionario
     * Mostra dealer con tutti i suoi prodotti disponibili
     */
    @GetMapping("/dealers/{id}")
    @Transactional(readOnly = true)
    public String showDealerDetailPage(@PathVariable("id") Long id,
            @RequestParam(value = "productId", required = false) Long productId,
            Model model, RedirectAttributes redirectAttributes) {
        logger.info("🔍 Accessing dealer detail page for dealer ID: {}", id);
        
        try {
            // === VERIFICA ESISTENZA DEALER ===
            Dealer dealer = dealerService.findById(id);
            if (dealer == null) {
                logger.error("❌ Dealer not found: id={}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Concessionario non trovato.");
                return "redirect:/dealers";
            }
            
            // === CARICAMENTO PRODOTTI DEALER ===
            List<Product> products = dealerService.getProductsByDealerOwner(dealer);
            logger.info("✅ Found {} products for dealer ID: {}", products.size(), id);
            
            // === PREPARAZIONE MODEL ===
            model.addAttribute("dealer", dealer);
            model.addAttribute("products", products);
            return "dealer_detail";
            
        } catch (Exception e) {
            logger.error("❌ Error loading dealer detail page: id={}, error={}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore nel caricamento della pagina del concessionario.");
            return "redirect:/dealers";
        }
    }
}