package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA per gestione utenti del sistema
 * Estende JpaRepository per operazioni CRUD standard e query di autenticazione
 * 
 * Responsabilità:
 * - Accesso dati utenti registrati
 * - Query per autenticazione e login
 * - Gestione reset password tramite token
 * - Ricerche per credenziali univoche
 * 
 * Pattern Repository vantaggi:
 * - Query method generation automatica
 * - Type-safety su operazioni database
 * - Gestione Optional per null-safety
 * - Transazioni automatiche Spring
 * 
 * Sicurezza:
 * - Password hash memorizzate (mai plaintext)
 * - Token reset temporanei e univoci
 * - Validazione unicità username/email
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trova utente per username univoco
     * Query generata automaticamente: SELECT * FROM user WHERE username = ?
     * 
     * @param username Username da cercare (case-sensitive)
     * @return Optional contenente User se trovato, empty() se non esiste
     * 
     * Utilizzi:
     * - Login con username
     * - Validazione univocità username in registrazione
     * - Ricerca utente per operazioni amministrative
     * 
     * Vincoli database:
     * - Username deve essere UNIQUE per integrità referenziale
     * - Index automatico su colonna username per performance
     */
    Optional<User> findByUsername(String username);

    /**
     * Trova utente per email univoca
     * Query generata automaticamente: SELECT * FROM user WHERE email = ?
     * 
     * @param email Indirizzo email da cercare (case-sensitive)
     * @return Optional contenente User se trovato, empty() se non esiste
     * 
     * Utilizzi:
     * - Login alternativo con email
     * - Reset password tramite email
     * - Validazione univocità email in registrazione
     * - Invio notifiche e comunicazioni
     * 
     * Vincoli database:
     * - Email deve essere UNIQUE e NOT NULL
     * - Index su colonna email per query efficienti
     */
    Optional<User> findByEmail(String email);

    /**
     * Trova utente tramite token di reset password
     * Query generata automaticamente: SELECT * FROM user WHERE reset_token = ?
     * 
     * @param resetToken Token temporaneo per reset password
     * @return User se token valido e trovato, null se non esiste
     * 
     * ATTENZIONE: Ritorna direttamente User invece di Optional
     * - Comportamento legacy da mantenere per compatibilità
     * - Gestire null check esplicito nel service layer
     * 
     * Sicurezza:
     * - Token deve essere UUID random e temporaneo
     * - Scadenza token gestita a livello business logic
     * - Token eliminato dopo utilizzo per sicurezza
     * 
     * Processo reset password:
     * 1. Utente richiede reset via email
     * 2. Sistema genera token univoco e scadenza
     * 3. Invio email con link contenente token
     * 4. Validazione token e permettere cambio password
     * 5. Invalidazione token dopo successo
     */
    User findByResetToken(String resetToken);

    /**
     * Trova utente per username O email
     * Query generata automaticamente: SELECT * FROM user WHERE username = ? OR email = ?
     * 
     * @param username Username da cercare
     * @param email Email da cercare  
     * @return Optional contenente User se trovato con uno dei due criteri
     * 
     * Utilizzi:
     * - Login unificato (utente può inserire username o email)
     * - Validazione duplicati in registrazione
     * - Recupero account con credenziali parziali
     * 
     * Logica OR:
     * - Trova match se username corrisponde OPPURE se email corrisponde
     * - Utile per UX login semplificata
     * - Riduce query multiple nel service layer
     * 
     * Performance:
     * - Index separati su username e email
     * - Query optimizer sceglie strategia più efficiente
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
}