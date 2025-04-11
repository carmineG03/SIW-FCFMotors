package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
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
                                   @RequestParam(value = "transmission", required = false) String transmission) {
        // Recupera i prodotti con i filtri
        model.addAttribute("products", productService.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission));

        // Aggiungi i valori per i dropdown
        model.addAttribute("categories", productService.findAllCategories());
        model.addAttribute("brands", productService.findAllBrands());
        model.addAttribute("models", brand != null ? productService.findModelsByBrand(brand) : null);
        model.addAttribute("fuelTypes", productService.findAllFuelTypes());
        model.addAttribute("transmissions", productService.findAllTransmissions());

        // Aggiungi lo stato di autenticazione
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated);

        // Aggiungi il conteggio del carrello
        model.addAttribute("cartCount", cartService.getCartItems().size());

        return "products";
    }
}