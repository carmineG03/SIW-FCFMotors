package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Entità JPA per rappresentare un ordine di acquisto nel sistema FCF Motors
 * Un ordine raggruppa più prodotti acquistati da un utente in una singola transazione
 * 
 * Tabella: orders (nome riservato SQL, quindi specificato esplicitamente)
 * 
 * Funzionalità:
 * - Aggregazione prodotti in un singolo ordine
 * - Calcolo totale automatico
 * - Tracciabilità acquisti utente
 * - Storico transazioni commerciali
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
@Table(name = "orders") // "order" è parola riservata SQL, usiamo "orders"
public class Order {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni ordine
     * Utilizzata per:
     * - Numero ordine nelle comunicazioni cliente
     * - Riferimento per supporto e resi
     * - Tracking spedizioni
     * - Reportistica vendite
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // === RELAZIONE CON ACQUIRENTE ===
    /**
     * Utente che ha effettuato l'ordine
     * Relazione Many-to-One: un utente può avere più ordini nel tempo
     * Non nullable: ogni ordine deve essere associato a un acquirente
     * 
     * Utilizzi:
     * - Storico acquisti personale
     * - Gestione profilo cliente
     * - Analisi comportamento acquisto
     * - Supporto customer care
     */
    @ManyToOne
    private User user;
    
    // === PRODOTTI NELL'ORDINE ===
    /**
     * Lista dei prodotti inclusi nell'ordine
     * Relazione One-to-Many: un ordine può contenere più prodotti
     * 
     * NOTA IMPORTANTE: 
     * Questa configurazione crea una tabella di join separata per gestire la relazione
     * Alternativa più comune sarebbe ManyToMany per prodotti che possono essere in più ordini
     * O aggiungere un campo "orderId" in Product per relazione diretta
     * 
     * Implicazioni:
     * - Ogni prodotto può appartenere a un solo ordine
     * - Necessario clonare prodotto se venduto multiple volte
     * - Storico modifiche prodotto non influenza ordini passati
     */
    @OneToMany
    private List<Product> products;
    
    // === INFORMAZIONI FINANZIARIE ===
    /**
     * Totale complessivo dell'ordine
     * BigDecimal per precisione monetaria (evita errori floating-point)
     * 
     * Calcolo:
     * - Somma prezzi di tutti i prodotti nell'ordine
     * - Include eventuali sconti o promozioni
     * - Esclude tipicamente tasse (gestite separatamente)
     * - Base per calcolo commissioni dealer
     * 
     * Utilizzi:
     * - Fatturazione cliente
     * - Reportistica vendite
     * - Calcolo provvigioni
     * - Analisi revenue
     */
    private BigDecimal total;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco dell'ordine
     * @return ID dell'ordine utilizzato come numero ordine
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'ID dell'ordine (gestito da JPA)
     * @param id Nuovo ID dell'ordine
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce l'utente che ha effettuato l'ordine
     * @return Acquirente dell'ordine
     */
    public User getUser() {
        return user;
    }

    /**
     * Imposta l'utente che ha effettuato l'ordine
     * @param user Acquirente (required)
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Restituisce la lista dei prodotti nell'ordine
     * @return Lista prodotti acquistati (può essere vuota ma non null)
     */
    public List<Product> getProducts() {
        return products;
    }

    /**
     * Imposta la lista dei prodotti nell'ordine
     * @param products Lista prodotti da includere nell'ordine
     */
    public void setProducts(List<Product> products) {
        this.products = products;
    }

    /**
     * Restituisce il totale dell'ordine
     * @return Importo totale in BigDecimal per precisione monetaria
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Imposta il totale dell'ordine
     * @param total Importo totale (deve essere positivo e coerente con i prodotti)
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}