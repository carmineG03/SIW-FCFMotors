package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
