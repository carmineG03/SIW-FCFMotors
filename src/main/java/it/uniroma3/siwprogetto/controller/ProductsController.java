package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.DealerService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductsController {

    private final ProductService productService;
    private final CartService cartService;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerService dealerService;

    public ProductsController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

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
        // Normalize string values
        category = category != null && !category.trim().isEmpty() ? category.trim() : null;
        brand = brand != null && !brand.trim().isEmpty() ? brand.trim() : null;
        selectedModel = selectedModel != null && !selectedModel.trim().isEmpty() ? selectedModel.trim() : null;
        fuelType = fuelType != null && !fuelType.trim().isEmpty() ? fuelType.trim() : null;
        transmission = transmission != null && !transmission.trim().isEmpty() ? transmission.trim() : null;
        query = query != null && !query.trim().isEmpty() ? query.trim() : null; // Fixed: Removed dependency on category

        // Validate numeric parameters
        minPrice = minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0 ? minPrice : null;
        maxPrice = maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0 ? maxPrice : null;
        minMileage = minMileage != null && minMileage >= 0 ? minMileage : null;
        maxMileage = maxMileage != null && maxMileage >= 0 ? maxMileage : null;
        minYear = minYear != null && minYear >= 0 ? minYear : null;
        maxYear = maxYear != null && maxYear >= 0 ? maxYear : null;

        // Log received parameters
        System.out.println("Parametri ricevuti: category=" + category + ", brand=" + brand + ", model=" + selectedModel +
                ", minPrice=" + minPrice + ", maxPrice=" + maxPrice + ", minMileage=" + minMileage +
                ", maxMileage=" + maxMileage + ", minYear=" + minYear + ", maxYear=" + maxYear +
                ", fuelType=" + fuelType + ", transmission=" + transmission + ", query=" + query);

        // Retrieve products with filters
        List<Product> products = productService.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission, query);

        // Sort products: featured first
        products.sort(Comparator.comparing(Product::isFeaturedActive, Comparator.reverseOrder())
                .thenComparing(Product::isFeatured, Comparator.reverseOrder())
                .thenComparing(Product::getModel, Comparator.nullsLast(String::compareTo)));

        // Add sorted products to model
        model.addAttribute("products", products);

        // Add dropdown values
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("brands", productService.findAllBrands());
        model.addAttribute("models", brand != null ? productService.findModelsByBrand(brand) : null);
        model.addAttribute("fuelTypes", productService.findAllFuelTypes());
        model.addAttribute("transmissions", productService.findAllTransmissions());

        // Add filter values to preserve them
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

        // Add authentication status
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        return "products";
    }
    @GetMapping("/search")
    public String searchProducts(@RequestParam("query") String query) {
        // Reindirizza a /products con il parametro query
        return "redirect:/products?query=" + query;
    }

    @GetMapping("/brand/{brand}")
    public String getBrand(@PathVariable String brand) {
        // Reindirizza a /products con il parametro brand
        return "redirect:/products?brand=" + brand;
    }

    @GetMapping("/category/{category}")
    public String getCategory(@PathVariable String category) {
        // Reindirizza a /products con il parametro category
        return "redirect:/products?category=" + category;
    }


    @PostMapping("/request-quote/{productId}")
    public String requestQuote(@PathVariable Long productId, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Devi essere loggato per richiedere un preventivo.");
            return "redirect:/login";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Utente non trovato"));

        Product product = productService.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Prodotto non trovato"));

        // Verifica che il prodotto abbia un venditore
        User seller = product.getSeller();
        if (seller == null) {
            redirectAttributes.addFlashAttribute("error", "Nessun venditore associato al prodotto.");
            return "redirect:/products";
        }

        // Verifica che il venditore sia un dealer
        if (!"DEALER".equalsIgnoreCase(product.getSellerType())) {
            redirectAttributes.addFlashAttribute("error", "Le richieste di preventivo sono disponibili solo per i prodotti venduti da dealer.");
            return "redirect:/products";
        }

        // Recupera il Dealer associato al seller
        Dealer dealer = dealerService.findByOwnerUsername(seller.getUsername());
        if (dealer == null) {
            redirectAttributes.addFlashAttribute("error", "Nessun dealer associato al venditore.");
            return "redirect:/products";
        }

        // Verifica se l'utente ha già una richiesta in sospeso per questo prodotto
        if (quoteRequestRepository.existsByUserIdAndProductIdAndStatus(user.getId(), productId, "PENDING")) {
            redirectAttributes.addFlashAttribute("error", "Hai già una richiesta di preventivo in sospeso per questo prodotto. Attendi una risposta prima di inviarne un'altra.");
            return "redirect:/products";
        }

        QuoteRequest quoteRequest = new QuoteRequest();
        quoteRequest.setProduct(product);
        quoteRequest.setUser(user);
        quoteRequest.setDealer(dealer);
        quoteRequest.setUserEmail(user.getEmail());
        quoteRequest.setRequestDate(LocalDateTime.now());
        quoteRequest.setStatus("PENDING");

        quoteRequestRepository.save(quoteRequest);

        redirectAttributes.addFlashAttribute("success", "Richiesta di preventivo inviata con successo!");
        return "redirect:/products";
    }
}