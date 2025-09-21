package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA per gestione concessionari FCF Motors
 * Implementa pattern Repository per accesso dati dealer con query specializzate
 * 
 * Responsabilità principali:
 * - Gestione CRUD completa concessionari
 * - Query di ricerca geografica e per proprietario
 * - Validazione unicità proprietario (constraint business)
 * - Supporto funzionalità di localizzazione
 * 
 * Architettura:
 * - Estende JpaRepository per operazioni standard
 * - Query derivate da nome metodo (Spring Data JPA)
 * - Transazioni gestite automaticamente da Spring
 * - Caching di primo livello Hibernate
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
public interface DealerRepository extends JpaRepository<Dealer, Long> {
    
    /**
     * Trova il concessionario gestito da un utente specifico tramite username
     * Implementa constraint business: ogni utente può gestire max 1 concessionario
     * 
     * Query generata automaticamente:
     * SELECT d.* FROM dealer d 
     * JOIN users u ON d.owner_id = u.id 
     * WHERE u.username = ?
     * 
     * @param username Username dell'utente proprietario
     * @return Optional contenente Dealer se trovato, empty() se utente non ha dealer
     * 
     * Casi d'uso:
     * - Redirect automatico alla gestione dealer esistente
     * - Verifica autorizzazioni modifica dealer
     * - Dashboard personalizzata proprietario
     * - Validazione unicità proprietario
     * 
     * Business rules:
     * - Un utente può possedere massimo 1 concessionario
     * - Solo il proprietario può modificare il proprio dealer
     * - Proprietario eredita automaticamente ruolo DEALER
     */
    Optional<Dealer> findByOwnerUsername(String username);
    
    /**
     * Ricerca concessionari per località (ricerca geografica)
     * Implementa ricerca case-insensitive e con pattern matching parziale
     * 
     * Query generata automaticamente:
     * SELECT * FROM dealer WHERE LOWER(address) LIKE LOWER(CONCAT('%', ?, '%'))
     * 
     * @param address Termine di ricerca per indirizzo (parziale, case-insensitive)
     * @return Lista dealer che matchano la località (vuota se nessun match)
     * 
     * Funzionalità supportate:
     * - Ricerca "Roma" → trova "Via Roma 123", "Roma Nord", etc.
     * - Ricerca "milano" → trova "Milano Centro", "MILANO", etc.
     * - Ricerca case-insensitive per usabilità
     * - Pattern matching parziale per flessibilità
     * 
     * Utilizzi:
     * - API endpoint /dealers?location=città
     * - Filtri geografici nella ricerca dealer
     * - Raccomandazioni dealer nelle vicinanze
     * - Integrazione con servizi di geolocalizzazione
     * 
     * Performance:
     * - Considerare indice su address per performance
     * - Risultati ordinati per rilevanza (da implementare)
     * - Paginazione consigliata per città grandi
     */
    List<Dealer> findByAddressContainingIgnoreCase(String address);
    
    /**
     * Trova il concessionario posseduto da un utente specifico
     * Alternativa a findByOwnerUsername quando si ha già l'oggetto User
     * 
     * Query generata automaticamente:
     * SELECT * FROM dealer WHERE owner_id = ?
     * 
     * @param user Oggetto User proprietario del concessionario
     * @return Optional contenente Dealer se l'utente ne possiede uno
     * 
     * Vantaggi vs findByOwnerUsername:
     * - Più efficiente se User già caricato in memoria
     * - Evita join aggiuntivo sulla tabella users
     * - Type-safe con oggetto User
     * - Consistente con relazioni JPA
     * 
     * Utilizzi:
     * - Controlli autorizzazione con User in sessione
     * - Dashboard dopo login utente
     * - Validazioni business logic
     * - Operazioni transazionali con User già loaded
     * 
     * Business validation:
     * - Garantisce constraint "un utente = max un dealer"
     * - Supporta verifica rapida esistenza dealer
     * - Base per redirect condizionali post-login
     */
    Optional<Dealer> findByOwner(User user);
    
}