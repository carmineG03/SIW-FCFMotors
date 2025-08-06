package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // Puoi aggiungere metodi personalizzati se necessario, es.:
    // List<Subscription> findByDurationDays(int durationDays);
}
