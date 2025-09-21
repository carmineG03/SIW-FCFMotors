package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Payment;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA per la gestione dei pagamenti nel sistema FCF Motors
 * Fornisce operazioni CRUD e query personalizzate per l'entità Payment
 * 
 * Funzionalità:
 * - Accesso completo a tutti i pagamenti del sistema
 * - Ricerca pagamenti per utente specifico
 * - Operazioni transazionali automatiche
 * - Integrazione con sistema di audit Spring Data
 * 
 * Utilizzi principali:
 * - Storico transazioni utente
 * - Reportistica finanziaria
 * - Riconciliazione pagamenti
 * - Gestione rimborsi e dispute
 * - Analytics revenue
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Trova tutti i pagamenti effettuati da un utente specifico
     * Utile per costruire lo storico completo delle transazioni del cliente
     * 
     * Query JPA generata automaticamente:
     * SELECT p FROM Payment p WHERE p.user = :user
     * 
     * Utilizzi:
     * - Pagina "I miei pagamenti" nell'area utente
     * - Calcolo totale speso da un cliente
     * - Verifiche customer care
     * - Generazione report personalizzati
     * - Analisi pattern di spesa
     * 
     * @param user Utente di cui cercare i pagamenti
     * @return Lista ordinata dei pagamenti dell'utente (può essere vuota)
     *         Ordinamento: tipicamente per data decrescente (più recenti primi)
     */
    List<Payment> findByUser(User user);
    
    // === METODI EREDITATI DA JpaRepository ===
    // Automaticamente disponibili senza implementazione:
    
    // save(Payment) - Salva o aggiorna un pagamento
    // findById(Long) - Trova pagamento per ID
    // findAll() - Tutti i pagamenti del sistema
    // deleteById(Long) - Elimina pagamento per ID
    // count() - Conta totale pagamenti
    // existsById(Long) - Verifica esistenza pagamento
}