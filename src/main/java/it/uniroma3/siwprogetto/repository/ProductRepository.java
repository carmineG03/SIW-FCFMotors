package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.model FROM Product p WHERE p.brand = :brand")
    List<String> findModelsByBrand(@Param("brand") String brand);

    @Query("SELECT DISTINCT p.fuelType FROM Product p")
    List<String> findAllFuelTypes();

    @Query("SELECT DISTINCT p.transmission FROM Product p")
    List<String> findAllTransmissions();

    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:selectedModel IS NULL OR p.model = :selectedModel) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minMileage IS NULL OR p.mileage >= :minMileage) AND " +
           "(:maxMileage IS NULL OR p.mileage <= :maxMileage) AND " +
           "(:minYear IS NULL OR p.year >= :minYear) AND " +
           "(:maxYear IS NULL OR p.year <= :maxYear) AND " +
           "(:fuelType IS NULL OR p.fuelType = :fuelType) AND " +
           "(:transmission IS NULL OR p.transmission = :transmission) AND " +
           "(:query IS NULL OR p.model LIKE CONCAT('%', CAST(:query AS STRING), '%') OR " +
           "p.description LIKE CONCAT('%', CAST(:query AS STRING), '%'))")
    List<Product> findByFilters(
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("selectedModel") String selectedModel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("fuelType") String fuelType,
            @Param("transmission") String transmission,
            @Param("query") String query);

    List<Product> findBySellerId(Long id);

    List<Product> findBySeller(User owner);
}