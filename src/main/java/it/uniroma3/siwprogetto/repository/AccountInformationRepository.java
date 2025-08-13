package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.AccountInformation;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository JPA per gestione dati AccountInformation
 * Estende JpaRepository per operazioni CRUD standard e query personalizzate
 * 
 * Responsabilità:
 * - Accesso dati informazioni account utente
 * - Query di ricerca per utente specifico
 * - Operazioni di eliminazione controllata
 * 
 * Pattern Repository vantaggi:
 * - Astrazione accesso dati
 * - Query type-safe generate automaticamente
 * - Transazioni gestite da Spring
 * - Caching automatico di primo livello
 */
public interface AccountInformationRepository extends JpaRepository<AccountInformation, Long> {
    
    /**
     * Trova le informazioni account associate a un utente specifico
     * Relazione One-to-One: ogni utente ha al massimo un AccountInformation
     * 
     * Query generata automaticamente da Spring Data JPA:
     * SELECT * FROM account_information WHERE user_id = ?
     * 
     * @param user Utente per cui cercare le informazioni account
     * @return Optional contenente AccountInformation se trovato, empty() se non esiste
     * 
     * Utilizzi:
     * - Caricamento profilo utente completo
     * - Verifica esistenza informazioni aggiuntive
     * - Aggiornamento dati profilo
     */
    Optional<AccountInformation> findByUser(User user);

    /**
     * Trova le informazioni account tramite ID utente
     * Alternativa più efficiente a findByUser quando si ha solo l'ID
     * 
     * Query generata automaticamente:
     * SELECT * FROM account_information WHERE user_id = ?
     * 
     * @param id ID dell'utente proprietario delle informazioni
     * @return Optional contenente AccountInformation se trovato, empty() se non esiste
     * 
     * Vantaggi:
     * - Non richiede caricamento completo oggetto User
     * - Query più efficiente per join semplici
     * - Riduce overhead memoria per operazioni specifiche
     */
    Optional<AccountInformation> findByUserId(Long id);

    /**
     * Elimina informazioni account tramite ID
     * Override del metodo base per documentazione specifica
     * 
     * ATTENZIONE: Operazione irreversibile
     * - Eliminazione fisica dal database
     * - Nessun soft delete implementato
     * - Controllare dipendenze prima dell'eliminazione
     * 
     * @param id ID delle informazioni account da eliminare
     * 
     * Casi d'uso:
     * - Cancellazione account utente (GDPR compliance)
     * - Cleanup dati obsoleti
     * - Reset profilo utente
     * 
     * Transazione:
     * - Automaticamente wrapped in transazione da Spring
     * - Rollback automatico in caso di errore
     */
    void deleteById(Long id);
}