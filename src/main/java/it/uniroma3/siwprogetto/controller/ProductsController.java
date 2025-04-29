package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/products")
public class ProductsController {

    private final ProductService productService;
    private final CartService cartService;

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
        // Normalizza i valori stringa
        category = category != null && !category.trim().isEmpty() ? category.trim() : null;
        brand = brand != null && !brand.trim().isEmpty() ? brand.trim() : null;
        selectedModel = selectedModel != null && !selectedModel.trim().isEmpty() ? selectedModel.trim() : null;
        fuelType = fuelType != null && !fuelType.trim().isEmpty() ? fuelType.trim() : null;
        transmission = transmission != null && !transmission.trim().isEmpty() ? transmission.trim() : null;
        query = query != null && !query.trim().isEmpty() ? query.trim() : null;

        // Validazione dei parametri numerici
        minPrice = minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0 ? minPrice : null;
        maxPrice = maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0 ? maxPrice : null;
        minMileage = minMileage != null && minMileage >= 0 ? minMileage : null;
        maxMileage = maxMileage != null && maxMileage >= 0 ? maxMileage : null;
        minYear = minYear != null && minYear >= 0 ? minYear : null;
        maxYear = maxYear != null && maxYear >= 0 ? maxYear : null;

        // Log dei parametri ricevuti
        System.out.println("Parametri ricevuti: category=" + category + ", brand=" + brand + ", model=" + selectedModel +
                ", minPrice=" + minPrice + ", maxPrice=" + maxPrice + ", minMileage=" + minMileage +
                ", maxMileage=" + maxMileage + ", minYear=" + minYear + ", maxYear=" + maxYear +
                ", fuelType=" + fuelType + ", transmission=" + transmission + ", query=" + query);

        // Recupera i prodotti con i filtri
        model.addAttribute("products", productService.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission, query));

        // Aggiungi i valori per i dropdown
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("brands", productService.findAllBrands());
        model.addAttribute("models", brand != null ? productService.findModelsByBrand(brand) : null);
        model.addAttribute("fuelTypes", productService.findAllFuelTypes());
        model.addAttribute("transmissions", productService.findAllTransmissions());

        // Aggiungi i valori dei filtri al modello per preservarli
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

        // Aggiungi lo stato di autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Aggiungi il conteggio del carrello
        model.addAttribute("cartCount", cartService.getCartItems().size());

        return "products";
    }
}