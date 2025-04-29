package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Trova tutti i prodotti
    public List<Product> findAll() {
        return (List<Product>) productRepository.findAll();
    }

    // Trova prodotti per categoria
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    // Trova tutte le categorie
    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    // Trova tutte le marche
    public List<String> findAllBrands() {
        return ((Collection<Product>) productRepository.findAll()).stream()
                .map(Product::getBrand)
                .distinct()
                .toList();
    }

    // Trova modelli per marca
    public List<String> findModelsByBrand(String brand) {
        return productRepository.findModelsByBrand(brand);
    }

    // Trova tutti i tipi di carburante
    public List<String> findAllFuelTypes() {
        return productRepository.findAllFuelTypes();
    }

    // Trova tutte le trasmissioni
    public List<String> findAllTransmissions() {
        return productRepository.findAllTransmissions();
    }

    // Trova prodotti con filtri
    public List<Product> findByFilters(String category, String brand, String selectedModel,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minMileage, Integer maxMileage,
                                       Integer minYear, Integer maxYear,
                                       String fuelType, String transmission, String query) {
        // Log dei filtri applicati
        System.out.println("Filtri applicati: category=" + category + ", brand=" + brand + ", model=" + selectedModel +
                ", minPrice=" + minPrice + ", maxPrice=" + maxPrice +
                ", minMileage=" + minMileage + ", maxMileage=" + maxMileage +
                ", minYear=" + minYear + ", maxYear=" + maxYear +
                ", fuelType=" + fuelType + ", transmission=" + transmission +
                ", query=" + query);

        return productRepository.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission, query);
    }

    // Trova un prodotto per ID
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    // Salva un prodotto
    public Product save(Product product) {
        return productRepository.save(product);
    }

    // Elimina un prodotto per ID
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    // Trova prodotti per ID del venditore
    public List<Product> findBySellerId(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }
}