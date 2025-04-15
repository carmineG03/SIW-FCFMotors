package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {

    List<Product> findAll();

    @Query("SELECT p FROM Product p WHERE p.category = :category")
    List<Product> findByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.brand FROM Product p")
    List<String> findAllBrands();

    @Query("SELECT DISTINCT p.model FROM Product p WHERE p.brand = :brand")
    List<String> findModelsByBrand(@Param("brand") String brand);

    @Query("SELECT DISTINCT p.fuelType FROM Product p")
    List<String> findAllFuelTypes();

    @Query("SELECT DISTINCT p.transmission FROM Product p")
    List<String> findAllTransmissions();

    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) " +
            "AND (:brand IS NULL OR p.brand = :brand) " +
            "AND (:selectedModel IS NULL OR p.model = :selectedModel) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:minMileage IS NULL OR p.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR p.mileage <= :maxMileage) " +
            "AND (:minYear IS NULL OR p.year >= :minYear) " +
            "AND (:maxYear IS NULL OR p.year <= :maxYear) " +
            "AND (:fuelType IS NULL OR p.fuelType = :fuelType) " +
            "AND (:transmission IS NULL OR p.transmission = :transmission)")
    List<Product> findByFilters(@Param("category") String category,
                                @Param("brand") String brand,
                                @Param("selectedModel") String selectedModel,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("minMileage") Integer minMileage,
                                @Param("maxMileage") Integer maxMileage,
                                @Param("minYear") Integer minYear,
                                @Param("maxYear") Integer maxYear,
                                @Param("fuelType") String fuelType,
                                @Param("transmission") String transmission);

    List<Product> findBySellerId(Long sellerId); // Modificato da findByDealerId a findBySellerId
}