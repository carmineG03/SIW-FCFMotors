package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository JPA per gestione richieste di preventivo (QuoteRequest)
 * Estende JpaRepository per operazioni CRUD standard e query personalizzate JPQL
 * 
 * Responsabilità:
 * - Accesso dati richieste preventivo
 * - Query complesse per dealer e messaggi privati
 * - Gestione relazioni User-Product-Dealer
 * - Verifica esistenza richieste specifiche
 * 
 * Pattern Repository vantaggi:
 * - Query JPQL type-safe e performanti
 * - Gestione automatica transazioni
 * - Caching JPA di primo livello
 * - Parametri named per sicurezza SQL injection
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {

    /**
     * Trova tutte le richieste preventivo associate a un dealer specifico
     * 
     * Query JPQL personalizzata che naviga la relazione QuoteRequest -> Dealer
     * Utilizza alias 'qr' per maggiore leggibilità
     * 
     * @param dealerId ID del dealer per cui cercare le richieste
     * @return Lista di QuoteRequest associate al dealer (può essere vuota)
     * 
     * Utilizzi:
     * - Dashboard dealer per visualizzare richieste ricevute
     * - Gestione workflow approvazione preventivi
     * - Statistiche performance dealer
     * 
     * Performance:
     * - Index consigliato su dealer_id per query efficienti
     * - Lazy loading delle relazioni associate
     */
    @Query("SELECT qr FROM QuoteRequest qr WHERE qr.dealer.id = :dealerId")
    List<QuoteRequest> findByDealerId(@Param("dealerId") Long dealerId);

    /**
     * Trova messaggi privati dell'utente come mittente o destinatario
     * 
     * Query JPQL complessa con condizioni multiple:
     * - Filtra solo richieste di tipo 'PRIVATE'
     * - Cerca per ID utente come mittente
     * - Cerca per email come destinatario
     * - Operatore OR per includere entrambi i casi
     * 
     * @param userId ID dell'utente mittente
     * @param userEmail Email dell'utente come potenziale destinatario
     * @return Lista di QuoteRequest private dell'utente (può essere vuota)
     * 
     * Casi d'uso:
     * - Sistema messaggistica interna piattaforma
     * - Cronologia conversazioni utente-dealer
     * - Notifiche messaggi non letti
     * 
     * Sicurezza:
     * - Parametri named prevengono SQL injection
     * - Filtro requestType garantisce privacy messaggi
     */
    @Query("SELECT qr FROM QuoteRequest qr WHERE qr.requestType = 'PRIVATE' AND (qr.user.id = :userId OR qr.recipientEmail = :userEmail)")
    List<QuoteRequest> findPrivateMessagesByUserIdOrEmail(@Param("userId") Long userId, @Param("userEmail") String userEmail);

    /**
     * Verifica esistenza richiesta preventivo con parametri specifici
     * Query generata automaticamente da Spring Data JPA
     * 
     * Pattern di naming convention Spring:
     * existsByField1AndField2AndField3 -> WHERE field1 = ? AND field2 = ? AND field3 = ?
     * 
     * @param userId ID dell'utente richiedente
     * @param productId ID del prodotto richiesto
     * @param status Stato della richiesta (es: PENDING, APPROVED, REJECTED)
     * @return true se esiste almeno una richiesta con i parametri specificati
     * 
     * Utilizzi:
     * - Prevenzione richieste duplicate
     * - Business logic validazione
     * - Controllo stato workflow preventivi
     * 
     * Performance:
     * - Query ottimizzata con COUNT(*) internamente
     * - Index composto consigliato su (user_id, product_id, status)
     */
    boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, String status);

    /**
     * Trova tutte le richieste preventivo di un utente specifico
     * Query generata automaticamente: SELECT * FROM quote_request WHERE user_id = ?
     * 
     * @param id ID dell'utente per cui cercare le richieste
     * @return Lista di QuoteRequest dell'utente (può essere vuota)
     * 
     * Utilizzi:
     * - Storico richieste utente
     * - Dashboard personale utente
     * - Gestione richieste in sospeso
     * 
     * Relazioni caricate:
     * - Lazy loading di default per Product e Dealer associati
     * - Fetch join esplicito se necessario per performance
     */
    List<QuoteRequest> findByUserId(Long id);

    /**
     * Trova tutte le richieste preventivo per un prodotto specifico
     * Query generata automaticamente: SELECT * FROM quote_request WHERE product_id = ?
     * 
     * @param productId ID del prodotto per cui cercare le richieste
     * @return Lista di QuoteRequest per il prodotto (può essere vuota)
     * 
     * Utilizzi:
     * - Analisi interesse prodotto specifico
     * - Statistiche richieste per categoria/modello
     * - Gestione inventory e pricing strategy
     * 
     * Business logic:
     * - Utile per seller per vedere interesse nei propri prodotti
     * - Dealer possono analizzare domanda di mercato
     */
    List<QuoteRequest> findByProductId(Long productId);
}