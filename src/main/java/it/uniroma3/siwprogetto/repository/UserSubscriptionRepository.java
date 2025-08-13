package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository JPA per gestione abbonamenti utente attivi (UserSubscription)
 * Estende JpaRepository per operazioni CRUD e query di gestione subscription
 * 
 * Responsabilità:
 * - Accesso dati abbonamenti utente specifici
 * - Gestione stato attivo/scaduto subscription
 * - Query per scadenza e pulizia automatica
 * - Relazione User-Subscription con metadati temporali
 * 
 * Pattern Repository vantaggi:
 * - Query automatiche per filtri comuni
 * - Gestione date con LocalDate type-safe
 * - Bulk operations per cleanup periodico
 * - Transazioni automatiche per consistenza
 * 
 * Business Model:
 * - UserSubscription = istanza attiva di Subscription per User specifico
 * - Stato 'active' indica subscription correntemente utilizzabile
 * - ExpiryDate gestisce scadenza automatica
 */
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    /**
     * Trova tutti gli abbonamenti di un utente specifico
     * Query generata automaticamente: SELECT * FROM user_subscription WHERE user_id = ?
     * 
     * @param userId ID dell'utente per cui cercare gli abbonamenti
     * @return Lista di UserSubscription dell'utente (può essere vuota)
     * 
     * Utilizzi:
     * - Storico completo abbonamenti utente (attivi + scaduti)
     * - Dashboard utente con cronologia subscription
     * - Analisi pattern utilizzo per statistiche
     * - Gestione rinnovi e upgrade/downgrade
     * 
     * Performance:
     * - Index su user_id per query efficienti
     * - Ordinamento per data creazione consigliato
     */
    List<UserSubscription> findByUserId(Long userId);

    /**
     * Trova abbonamenti utente filtrati per stato attivo
     * Query generata: SELECT * FROM user_subscription WHERE user_id = ? AND active = ?
     * 
     * @param user Oggetto User per cui cercare abbonamenti
     * @param active Stato di attivazione da filtrare (true/false)
     * @return Lista di UserSubscription con stato specificato
     * 
     * Utilizzi principali:
     * - findByUserAndActive(user, true): abbonamenti attivi utente
     * - findByUserAndActive(user, false): abbonamenti scaduti utente
     * - Verifica permissions e features disponibili
     * - Controllo limiti utilizzo per utente premium
     * 
     * Business Logic:
     * - Tipicamente un utente ha max 1 subscription attiva
     * - Multiple subscription possibili per piani combinati
     */
    List<UserSubscription> findByUserAndActive(User user, boolean active);

    /**
     * Trova tutti gli abbonamenti per stato attivo (global)
     * Query generata: SELECT * FROM user_subscription WHERE active = ?
     * 
     * @param active Stato di attivazione da filtrare
     * @return Lista di UserSubscription con stato specificato (tutti gli utenti)
     * 
     * Utilizzi:
     * - findByActive(true): tutti abbonamenti attivi sistema
     * - findByActive(false): tutti abbonamenti scaduti
     * - Statistiche business e revenue tracking
     * - Report amministrativi e dashboard
     * - Bulk operations su subscription specifiche
     * 
     * Performance:
     * - Index su campo 'active' essenziale
     * - Query potenzialmente costosa per grandi dataset
     */
    List<UserSubscription> findByActive(boolean active);

    /**
     * Elimina abbonamento per ID
     * Override del metodo base per documentazione specifica
     * 
     * ATTENZIONE: Operazione irreversibile
     * - Eliminazione fisica dal database
     * - Perdita storico abbonamento utente
     * - Verifica business logic prima eliminazione
     * 
     * @param id ID dell'abbonamento da eliminare
     * 
     * Casi d'uso:
     * - Cleanup abbonamenti scaduti molto vecchi
     * - Rimozione subscription errate/duplicate
     * - Compliance GDPR per cancellazione account
     * 
     * Alternative:
     * - Soft delete con flag 'deleted'
     * - Mantenimento storico per analytics
     */
    void deleteById(Long id);

    /**
     * Trova abbonamenti attivi che scadono in data specifica
     * Query generata: SELECT * FROM user_subscription WHERE active = ? AND expiry_date = ?
     * 
     * @param active Stato attivazione (tipicamente true)
     * @param expiryDate Data di scadenza specifica da cercare
     * @return Lista UserSubscription che scadono nella data indicata
     * 
     * Utilizzi:
     * - Job schedulato per notifiche scadenza
     * - Batch processing per disattivazione automatica
     * - Email reminder pre-scadenza (expiryDate - N giorni)
     * - Report scadenze giornaliere per customer service
     * 
     * Scheduling tipico:
     * - Esecuzione daily job alle 00:01
     * - Controllo scadenze giorno corrente
     * - Invio notifiche e disattivazione automatica
     * 
     * Index consigliato:
     * - Composto su (active, expiry_date) per performance
     */
    List<UserSubscription> findByActiveAndExpiryDate(boolean active, LocalDate expiryDate);
}