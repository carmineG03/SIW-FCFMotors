package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
	// Existing query for dealer
	@Query("SELECT qr FROM QuoteRequest qr WHERE qr.dealer.id = :dealerId")
	List<QuoteRequest> findByDealerId(@Param("dealerId") Long dealerId);

	// New query for private messages by user ID (sender or recipient)
	@Query("SELECT qr FROM QuoteRequest qr WHERE qr.requestType = 'PRIVATE' AND (qr.user.id = :userId OR qr.recipientEmail = :userEmail)")
	List<QuoteRequest> findPrivateMessagesByUserIdOrEmail(@Param("userId") Long userId, @Param("userEmail") String userEmail);

	// Existing methods
	boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, String status);

	List<QuoteRequest> findByUserId(Long id);

	List<QuoteRequest> findByProductId(Long productId);
}