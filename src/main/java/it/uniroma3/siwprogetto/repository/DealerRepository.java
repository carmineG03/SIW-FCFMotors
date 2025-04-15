package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealerRepository extends JpaRepository<Dealer, Long> {
	Optional<Dealer> findByOwnerUsername(String username);
	List<Dealer> findByAddressContainingIgnoreCase(String address);
}