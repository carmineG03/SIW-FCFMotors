package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.CartItem;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA per gestione elementi carrello acquisti
 * Gestisce persistenza e query per il sistema shopping cart di FCF Motors
 * 
 * Funzionalità principali:
 * - Gestione carrello persistente tra sessioni
 * - Query carrello per utente specifico
 * - Operazioni CRUD su elementi individuali
 * - Supporto per calcolo totali e checkout
 * 
 * Pattern architetturale:
 * - Repository pattern per separazione concerns
 * - Active Record pattern tramite JpaRepository
 * - Unit of Work pattern per transazioni
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * Recupera tutti gli elementi nel carrello di un utente specifico
     * Fondamentale per visualizzazione carrello e calcolo totali
     * 
     * Query generata automaticamente da Spring Data JPA:
     * SELECT * FROM cart_item WHERE user_id = ?
     * 
     * @param user Utente proprietario del carrello
     * @return Lista elementi carrello (vuota se carrello vuoto, mai null)
     * 
     * Utilizzi principali:
     * - Pagina carrello utente (/cart)
     * - Calcolo totale carrello
     * - Validazione disponibilità prodotti prima checkout
     * - Badge conteggio elementi nell'header
     * - Persistenza carrello tra sessioni browser
     * 
     * Performance notes:
     * - Query efficiente con indice su user_id
     * - Considerare fetch EAGER per Product associato se necessario
     * - Risultati tipicamente limitati (max 10-20 elementi per carrello)
     * 
     * Business logic correlata:
     * - Filtrare elementi con prodotti non più disponibili
     * - Controllare limiti quantità per prodotto
     * - Validare prezzi correnti vs prezzi al momento aggiunta
     */
    List<CartItem> findByUser(User user);
    
}