package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA per gestione piani di abbonamento (Subscription)
 * Estende JpaRepository per operazioni CRUD standard
 * 
 * Responsabilità:
 * - Accesso dati piani abbonamento
 * - Gestione catalogo subscription disponibili
 * - Supporto per future query personalizzate
 * 
 * Pattern Repository vantaggi:
 * - Operazioni CRUD automatiche (save, findById, findAll, delete)
 * - Transazioni gestite automaticamente da Spring
 * - Type-safety su operazioni database
 * - Paginazione e sorting built-in
 * 
 * Architettura:
 * - Subscription rappresenta template/piano abbonamento
 * - UserSubscription collega User a Subscription specifica
 * - Separazione tra definizione piano e istanza attiva
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    /**
     * Metodi ereditati da JpaRepository<Subscription, Long>:
     * 
     * - save(Subscription) : salvataggio/aggiornamento piano
     * - findById(Long) : ricerca piano per ID
     * - findAll() : tutti i piani disponibili
     * - deleteById(Long) : eliminazione piano
     * - count() : conteggio totale piani
     * - existsById(Long) : verifica esistenza piano
     * 
     * Utilizzi principali:
     * - Caricamento catalogo piani per UI selezione
     * - Gestione amministrativa piani abbonamento
     * - Configurazione pricing e features
     */
    
}