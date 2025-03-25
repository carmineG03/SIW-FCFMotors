package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}