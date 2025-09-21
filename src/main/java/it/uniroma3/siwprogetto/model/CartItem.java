package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;

/**
 * Entità JPA per rappresentare un singolo elemento nel carrello di un utente
 * Gestisce sia prodotti (auto) che abbonamenti nel carrello di acquisto
 * 
 * Funzionalità:
 * - Collegamento a prodotti o abbonamenti
 * - Gestione quantità per prodotti multipli
 * - Associazione univoca con utente proprietario
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
public class CartItem {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni elemento del carrello
     * Utilizzata per operazioni CRUD sugli elementi del carrello
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // === QUANTITÀ PRODOTTO ===
    /**
     * Quantità di prodotti/servizi nel carrello
     * - Per prodotti (auto): generalmente 1 (acquisto singolo)
     * - Per abbonamenti: sempre 1 (non si possono comprare più abbonamenti insieme)
     * - Valore minimo: 1 (non possono esistere elementi a quantità 0)
     */
    private int quantity;
    
    // === RELAZIONI CON ALTRE ENTITÀ ===
    
    /**
     * Riferimento al prodotto (auto) nel carrello
     * Relazione Many-to-One: più elementi carrello possono riferire lo stesso prodotto
     * nullable = true: può essere null se l'elemento è un abbonamento
     * 
     * Mutually exclusive con subscription: o è un prodotto O un abbonamento
     */
    @ManyToOne
    @JoinColumn(nullable = true)
    private Product product;
    
    /**
     * Riferimento all'utente proprietario del carrello
     * Relazione Many-to-One: un utente può avere più elementi nel carrello
     * Non nullable: ogni elemento deve appartenere a un utente specifico
     */
    @ManyToOne
    private User user;
    
    /**
     * Riferimento all'abbonamento nel carrello
     * Relazione Many-to-One: più utenti possono aggiungere lo stesso abbonamento
     * nullable = true: può essere null se l'elemento è un prodotto
     * 
     * Mutually exclusive con product: o è un prodotto O un abbonamento
     */
    @ManyToOne
    private Subscription subscription;

    // === COSTRUTTORI ===
    
    /**
     * Costruttore vuoto richiesto da JPA
     * Utilizzato dall'ORM per istanziare oggetti dal database
     */
    public CartItem() {}

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco dell'elemento carrello
     * @return ID dell'elemento o null se non ancora persistito
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID dell'elemento carrello (gestito da JPA)
     * @param id Nuovo ID dell'elemento
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce la quantità dell'elemento nel carrello
     * @return Quantità selezionata (minimo 1)
     */
    public int getQuantity() { 
        return quantity; 
    }
    
    /**
     * Imposta la quantità dell'elemento nel carrello
     * @param quantity Nuova quantità (deve essere >= 1)
     */
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }
    
    /**
     * Restituisce il prodotto associato all'elemento
     * @return Prodotto nel carrello o null se è un abbonamento
     */
    public Product getProduct() { 
        return product; 
    }
    
    /**
     * Imposta il prodotto associato all'elemento
     * @param product Prodotto da aggiungere al carrello
     */
    public void setProduct(Product product) { 
        this.product = product; 
    }
    
    /**
     * Restituisce l'utente proprietario del carrello
     * @return Utente a cui appartiene questo elemento
     */
    public User getUser() { 
        return user; 
    }
    
    /**
     * Imposta l'utente proprietario del carrello
     * @param user Utente proprietario (required)
     */
    public void setUser(User user) { 
        this.user = user; 
    }
    
    /**
     * Restituisce l'abbonamento associato all'elemento
     * @return Abbonamento nel carrello o null se è un prodotto
     */
    public Subscription getSubscription() { 
        return subscription; 
    }
    
    /**
     * Imposta l'abbonamento associato all'elemento
     * @param subscription2 Abbonamento da aggiungere al carrello
     */
    public void setSubscription(Subscription subscription2) { 
        this.subscription = subscription2; 
    }
}