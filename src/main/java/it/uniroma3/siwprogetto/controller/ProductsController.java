package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.*;
import it.uniroma3.siwprogetto.repository.*;
import it.uniroma3.siwprogetto.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Controller principale per la gestione dei prodotti
 * Gestisce visualizzazione, ricerca, filtri e richieste di preventivo
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
@RequestMapping("/products")
public class ProductsController {

    private static final Logger logger = LoggerFactory.getLogger(ProductsController.class);

    private final ProductService productService;
    
    @Autowired private QuoteRequestRepository quoteRequestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DealerService dealerService;

    public ProductsController(ProductService productService, CartService cartService) {
        this.productService = productService;
    }

    /**
     * Pagina principale dei prodotti con sistema di filtri avanzato
     * Supporta filtri per categoria, marca, prezzo, chilometraggio, anno, carburante, cambio
     * e ricerca testuale
     */
    @GetMapping
    public String showProductsPage(Model model,
                                   @RequestParam(value = "category", required = false) String category,
                                   @RequestParam(value = "brand", required = false) String brand,
                                   @RequestParam(value = "model", required = false) String selectedModel,
                                   @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                                   @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                   @RequestParam(value = "minMileage", required = false) Integer minMileage,
                                   @RequestParam(value = "maxMileage", required = false) Integer maxMileage,
                                   @RequestParam(value = "minYear", required = false) Integer minYear,
                                   @RequestParam(value = "maxYear", required = false) Integer maxYear,
                                   @RequestParam(value = "fuelType", required = false) String fuelType,
                                   @RequestParam(value = "transmission", required = false) String transmission,
                                   @RequestParam(value = "query", required = false) String query) {
        
        logger.debug("Richiesta pagina prodotti con filtri applicati");

        // === NORMALIZZAZIONE PARAMETRI STRINGA ===
        // Rimuove spazi e converte stringhe vuote in null per filtri più efficaci
        category = normalizeString(category);
        brand = normalizeString(brand);
        selectedModel = normalizeString(selectedModel);
        fuelType = normalizeString(fuelType);
        transmission = normalizeString(transmission);
        query = normalizeString(query);

        // === VALIDAZIONE PARAMETRI NUMERICI ===
        // Assicura che i valori numerici siano validi (>= 0)
        minPrice = validateNumericParam(minPrice);
        maxPrice = validateNumericParam(maxPrice);
        minMileage = validateIntegerParam(minMileage);
        maxMileage = validateIntegerParam(maxMileage);
        minYear = validateIntegerParam(minYear);
        maxYear = validateIntegerParam(maxYear);

        // Log parametri per debugging
        logger.debug("Filtri applicati - Category: {}, Brand: {}, Query: {}", category, brand, query);

        // === RICERCA PRODOTTI CON FILTRI ===
        List<Product> products = productService.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission, query);

        // === ORDINAMENTO PRODOTTI ===
        // Priorità: 1) Prodotti in evidenza attivi, 2) Prodotti in evidenza, 3) Ordine alfabetico
        products.sort(Comparator.comparing(Product::isFeaturedActive, Comparator.reverseOrder())
                .thenComparing(Product::isFeatured, Comparator.reverseOrder())
                .thenComparing(Product::getModel, Comparator.nullsLast(String::compareTo)));

        logger.info("Trovati {} prodotti con i filtri applicati", products.size());

        // === PREPARAZIONE DATI PER LA VIEW ===
        // Prodotti filtrati e ordinati
        model.addAttribute("products", products);

        // Valori per dropdown dei filtri
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("brands", productService.findAllBrands());
        model.addAttribute("models", brand != null ? productService.findModelsByBrand(brand) : null);
        model.addAttribute("fuelTypes", productService.findAllFuelTypes());
        model.addAttribute("transmissions", productService.findAllTransmissions());

        // Conserva valori dei filtri per mantenere stato del form
        addFilterAttributesToModel(model, category, brand, selectedModel, minPrice, maxPrice,
                minMileage, maxMileage, minYear, maxYear, fuelType, transmission, query);

        // Stato autenticazione utente
        addAuthenticationStatus(model);

        return "products";
    }

    /**
     * Richiesta preventivo per un prodotto specifico
     * Disponibile solo per utenti autenticati e prodotti venduti da dealer
     */
    @PostMapping("/request-quote/{productId}")
    @Transactional
    public String requestQuote(@PathVariable Long productId, 
                             Authentication authentication, 
                             RedirectAttributes redirectAttributes) {
        
        logger.info("Richiesta preventivo per prodotto ID: {}", productId);

        // === VERIFICA AUTENTICAZIONE ===
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            logger.warn("Tentativo di richiesta preventivo senza autenticazione");
            redirectAttributes.addFlashAttribute("error", "Devi essere loggato per richiedere un preventivo.");
            return "redirect:/login";
        }

        try {
            // === RECUPERO ENTITÀ DAL DATABASE ===
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Utente non trovato: " + username));

            Product product = productService.findById(productId)
                    .orElseThrow(() -> new IllegalStateException("Prodotto non trovato: " + productId));

            // === VALIDAZIONI BUSINESS LOGIC ===
            // Verifica che il prodotto abbia un venditore
            User seller = product.getSeller();
            if (seller == null) {
                logger.warn("Prodotto {} senza venditore associato", productId);
                redirectAttributes.addFlashAttribute("error", "Nessun venditore associato al prodotto.");
                return "redirect:/products";
            }

            // Verifica che il venditore sia un dealer (solo i dealer gestiscono preventivi)
            if (!"DEALER".equalsIgnoreCase(product.getSellerType())) {
                logger.warn("Tentativo preventivo su prodotto non-dealer: {}", productId);
                redirectAttributes.addFlashAttribute("error", "Le richieste di preventivo sono disponibili solo per i prodotti venduti da dealer.");
                return "redirect:/products";
            }

            // Recupera il Dealer associato al venditore
            Dealer dealer = dealerService.findByOwnerUsername(seller.getUsername());
            if (dealer == null) {
                logger.error("Dealer non trovato per username: {}", seller.getUsername());
                redirectAttributes.addFlashAttribute("error", "Nessun dealer associato al venditore.");
                return "redirect:/products";
            }

            // === VERIFICA DUPLICATI ===
            // Evita richieste multiple per lo stesso prodotto dallo stesso utente
            if (quoteRequestRepository.existsByUserIdAndProductIdAndStatus(user.getId(), productId, "PENDING")) {
                logger.warn("Richiesta preventivo duplicata - User: {}, Product: {}", user.getId(), productId);
                redirectAttributes.addFlashAttribute("error", "Hai già una richiesta di preventivo in sospeso per questo prodotto. Attendi una risposta prima di inviarne un'altra.");
                return "redirect:/products";
            }

            // === CREAZIONE RICHIESTA PREVENTIVO ===
            QuoteRequest quoteRequest = createQuoteRequest(user, product, dealer);
            quoteRequestRepository.save(quoteRequest);

            logger.info("Preventivo creato con successo - ID: {}", quoteRequest.getId());
            redirectAttributes.addFlashAttribute("success", "Richiesta di preventivo inviata con successo!");
            return "redirect:/products";

        } catch (Exception e) {
            logger.error("Errore durante creazione preventivo: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Errore interno durante l'invio della richiesta.");
            return "redirect:/products";
        }
    }

    // === METODI HELPER PER PULIZIA CODICE ===

    /**
     * Normalizza stringa: rimuove spazi e converte vuote in null
     */
    private String normalizeString(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }

    /**
     * Valida parametri BigDecimal: deve essere >= 0
     */
    private BigDecimal validateNumericParam(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0 ? value : null;
    }

    /**
     * Valida parametri Integer: deve essere >= 0
     */
    private Integer validateIntegerParam(Integer value) {
        return value != null && value >= 0 ? value : null;
    }

    /**
     * Aggiunge tutti gli attributi dei filtri al model
     */
    private void addFilterAttributesToModel(Model model, String category, String brand, String selectedModel,
                                          BigDecimal minPrice, BigDecimal maxPrice, Integer minMileage,
                                          Integer maxMileage, Integer minYear, Integer maxYear,
                                          String fuelType, String transmission, String query) {
        model.addAttribute("category", category);
        model.addAttribute("brand", brand);
        model.addAttribute("selectedModel", selectedModel);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minMileage", minMileage);
        model.addAttribute("maxMileage", maxMileage);
        model.addAttribute("minYear", minYear);
        model.addAttribute("maxYear", maxYear);
        model.addAttribute("fuelType", fuelType);
        model.addAttribute("transmission", transmission);
        model.addAttribute("query", query);
    }

    /**
     * Aggiunge stato autenticazione al model
     */
    private void addAuthenticationStatus(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);
    }

    /**
     * Crea una nuova richiesta di preventivo
     */
    private QuoteRequest createQuoteRequest(User user, Product product, Dealer dealer) {
        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setProduct(product);
        quoteRequest.setUser(user);
        quoteRequest.setDealer(dealer);
        quoteRequest.setUserEmail(user.getEmail());
        quoteRequest.setRequestDate(LocalDateTime.now());
        quoteRequest.setStatus("PENDING");
        return quoteRequest;
    }

    // === ENDPOINT DI SUPPORTO ===
    
    /**
     * Redirect per ricerche testuali
     */
    @GetMapping("/search")
    public String searchProducts(@RequestParam("query") String query) {
        return "redirect:/products?query=" + query;
    }

    /**
     * Redirect per filtro per marca
     */
    @GetMapping("/brand/{brand}")
    public String getBrand(@PathVariable String brand) {
        return "redirect:/products?brand=" + brand;
    }

    /**
     * Redirect per filtro per categoria
     */
    @GetMapping("/category/{category}")
    public String getCategory(@PathVariable String category) {
        return "redirect:/products?category=" + category;
    }

    /**
     * Dettagli prodotto singolo
     */
    @GetMapping("/{id}")
    public String getProductDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Prodotto non trovato");
            return "redirect:/products";
        }
        
        model.addAttribute("product", product);
        addAuthenticationStatus(model);
        return "product-detail";
    }
}