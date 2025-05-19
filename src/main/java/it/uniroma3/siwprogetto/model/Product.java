package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String model;

    private Integer mileage;
    private Integer year;

    @Column(length = 50)
    private String fuelType;

    @Column(length = 50)
    private String transmission;

    @Column(length = 50)
    private String sellerType;

    @ManyToOne
    private User seller;

    private boolean isFeatured;

    private LocalDateTime featuredUntil;

    public Product() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public String getSellerType() { return sellerType; }
    public void setSellerType(String sellerType) { this.sellerType = sellerType; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public boolean isFeatured() { return isFeatured; }
    public void setIsFeatured(boolean isFeatured) { this.isFeatured = isFeatured; }
    public LocalDateTime getFeaturedUntil() { return featuredUntil; }
    public void setFeaturedUntil(LocalDateTime featuredUntil) { this.featuredUntil = featuredUntil; }
    public boolean isFeaturedActive() {
        return isFeatured && (featuredUntil == null || LocalDateTime.now().isBefore(featuredUntil));
    }
}