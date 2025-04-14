package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return (List<Product>) productRepository.findAll();
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }

    public Iterable<Product> findAllBrands() {
        return productRepository.findAll();
    }

    public List<String> findModelsByBrand(String brand) {
        return productRepository.findModelsByBrand(brand);
    }

    public List<String> findAllFuelTypes() {
        return productRepository.findAllFuelTypes();
    }

    public List<String> findAllTransmissions() {
        return productRepository.findAllTransmissions();
    }

    public List<Product> findByFilters(String category, String brand, String selectedModel,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer minMileage, Integer maxMileage,
                                       Integer minYear, Integer maxYear,
                                       String fuelType, String transmission) {
        return productRepository.findByFilters(category, brand, selectedModel,
                minPrice, maxPrice, minMileage, maxMileage,
                minYear, maxYear, fuelType, transmission);
    }

    public Product findById(Long productId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }
}