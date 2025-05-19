package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
	@Query("SELECT qr FROM QuoteRequest qr WHERE qr.dealer.id = :dealerId")
	List<QuoteRequest> findByDealerId(@Param("dealerId") Long dealerId);


	boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, String status);

	List<QuoteRequest> findByUserId(Long id);
}