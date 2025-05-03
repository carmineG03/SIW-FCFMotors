package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
	List<UserSubscription> findByUserId(Long userId);

	List<UserSubscription> findByUserAndActive(User user, boolean b);
}