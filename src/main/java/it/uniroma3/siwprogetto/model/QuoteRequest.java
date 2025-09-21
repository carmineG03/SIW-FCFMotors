package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entità JPA per gestire le richieste di preventivo nel sistema FCF Motors
 * Permette ai clienti di richiedere informazioni e preventivi sui prodotti
 * 
 * Flusso funzionale:
 * 1. Cliente vede prodotto interessante
 * 2. Richiede preventivo compilando form
 * 3. Sistema crea QuoteRequest e notifica dealer
 * 4. Dealer risponde con dettagli e offerta
 * 5. Sistema invia risposta via email al cliente
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
@Table(name = "quote_requests")
public class QuoteRequest {

    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni richiesta preventivo
     * Utilizzata per tracking e gestione della richiesta nel ciclo di vita completo
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === RELAZIONI CON ENTITÀ PRINCIPALI ===
    
    /**
     * Prodotto per cui viene richiesto il preventivo
     * Relazione Many-to-One: un prodotto può ricevere più richieste preventivo
     * Non nullable: ogni richiesta deve essere associata a un prodotto specifico
     * 
     * Utilizzi:
     * - Identificazione oggetto della richiesta
     * - Recupero dettagli prodotto per risposta
     * - Statistiche prodotti più richiesti
     * - Collegamento con dealer venditore
     */
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Utente registrato che ha fatto la richiesta (opzionale)
     * Relazione Many-to-One: un utente può fare più richieste preventivo
     * Nullable: anche utenti non registrati possono richiedere preventivi
     * 
     * Utilizzi:
     * - Storico richieste utente registrato
     * - Personalizzazione risposta
     * - Marketing targettizzato
     * - Gestione preferenze comunicazione
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Concessionario che riceve e gestisce la richiesta
     * Relazione Many-to-One: un dealer può ricevere più richieste
     * Nullable: determinate automaticamente dal prodotto
     * 
     * Logica assegnazione:
     * - Se product.seller ha dealer → richiesta assegnata al dealer
     * - Se product.seller non ha dealer → gestione centralizzata
     * 
     * Utilizzi:
     * - Routing richieste al dealer corretto
     * - Dashboard richieste dealer
     * - Statistiche performance dealer
     */
    @ManyToOne
    @JoinColumn(name = "dealer_id", nullable = true)
    private Dealer dealer;

    // === INFORMAZIONI TEMPORALI ===
    
    /**
     * Data e ora di creazione della richiesta preventivo
     * Timestamp automatico impostato alla creazione
     * 
     * Utilizzi:
     * - Tracking tempi di risposta
     * - Ordinamento cronologico richieste
     * - SLA gestione preventivi
     * - Reportistica temporale
     * - Scadenza automatica richieste vecchie
     */
    private LocalDateTime requestDate;

    // === GESTIONE STATO RICHIESTA ===
    
    /**
     * Stato corrente della richiesta preventivo
     * Traccia l'avanzamento nel processo di gestione
     * 
     * Stati possibili:
     * - "PENDING": Richiesta creata, in attesa di gestione
     * - "RESPONDED": Dealer ha risposto alla richiesta
     * - "CLOSED": Richiesta chiusa (vendita completata o abbandonata)
     * - "EXPIRED": Richiesta scaduta per mancanza di attività
     * 
     * Utilizzi:
     * - Filtri dashboard dealer
     * - Automazioni workflow
     * - Reportistica conversion rate
     * - Notifiche promemoria
     */
    private String status;

    // === INFORMAZIONI CONTATTO ===
    
    /**
     * Email del richiedente per l'invio della risposta
     * Obbligatoria anche per utenti registrati (potrebbero voler usare email diversa)
     * 
     * Utilizzi:
     * - Invio risposta preventivo
     * - Comunicazioni follow-up
     * - Newsletter marketing (con consenso)
     * - Notifiche stato richiesta
     * 
     * Validazione:
     * - Deve essere formato email valido
     * - Viene verificata raggiungibilità se possibile
     */
    private String userEmail;

    // === CLASSIFICAZIONE RICHIESTA ===
    
    /**
     * Tipologia della richiesta per classificazione e routing
     * Aiuta dealer a prioritizzare e gestire appropriatamente
     * 
     * Tipi comuni:
     * - "PRICE_QUOTE": Richiesta preventivo prezzo
     * - "INFO_REQUEST": Richiesta informazioni dettagliate
     * - "TEST_DRIVE": Richiesta prova su strada
     * - "FINANCING": Richiesta informazioni finanziamento
     * - "TRADE_IN": Richiesta valutazione permuta
     * 
     * Utilizzi:
     * - Personalizzazione template risposta
     * - Routing a specialisti
     * - Statistiche tipologie richieste
     */
    private String requestType;

    /**
     * Email destinatario per l'invio (tipicamente dealer)
     * Campo tecnico per gestione routing email
     * Può differire dall'email del dealer per casi speciali
     * 
     * Utilizzi:
     * - Sistema notifiche interne
     * - Routing personalizzato
     * - Backup contatti
     * - Gestione team dealer
     */
    private String recipientEmail;

    // === GESTIONE RISPOSTA ===
    
    /**
     * Messaggio di risposta del dealer alla richiesta
     * Campo TEXT per supportare risposte lunghe e dettagliate
     * 
     * Contenuto tipico:
     * - Dettagli prezzo e condizioni
     * - Informazioni aggiuntive prodotto
     * - Proposte finanziamento
     * - Inviti a contatto diretto
     * - Offerte speciali
     * 
     * Utilizzi:
     * - Invio email risposta cliente
     * - Storico comunicazioni
     * - Template future risposte
     * - Analisi qualità risposte
     */
    @Column(columnDefinition = "TEXT")
    private String responseMessage;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco della richiesta preventivo
     * @return ID della richiesta
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID della richiesta preventivo (gestito da JPA)
     * @param id Nuovo ID della richiesta
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce il prodotto oggetto della richiesta
     * @return Prodotto per cui si richiede preventivo
     */
    public Product getProduct() { 
        return product; 
    }
    
    /**
     * Imposta il prodotto oggetto della richiesta
     * @param product Prodotto per preventivo (required)
     */
    public void setProduct(Product product) { 
        this.product = product; 
    }
    
    /**
     * Restituisce l'utente che ha fatto la richiesta
     * @return Utente richiedente o null se anonimo
     */
    public User getUser() { 
        return user; 
    }
    
    /**
     * Imposta l'utente che ha fatto la richiesta
     * @param user Utente richiedente (optional)
     */
    public void setUser(User user) { 
        this.user = user; 
    }
    
    /**
     * Restituisce il dealer che gestisce la richiesta
     * @return Concessionario responsabile della risposta
     */
    public Dealer getDealer() { 
        return dealer; 
    }
    
    /**
     * Imposta il dealer che gestisce la richiesta
     * @param dealer Concessionario assegnato (determined from product)
     */
    public void setDealer(Dealer dealer) { 
        this.dealer = dealer; 
    }
    
    /**
     * Restituisce la data di creazione della richiesta
     * @return Timestamp creazione richiesta
     */
    public LocalDateTime getRequestDate() { 
        return requestDate; 
    }
    
    /**
     * Imposta la data di creazione della richiesta
     * @param requestDate Timestamp creazione (typically set automatically)
     */
    public void setRequestDate(LocalDateTime requestDate) { 
        this.requestDate = requestDate; 
    }
    
    /**
     * Restituisce lo stato corrente della richiesta
     * @return Stato processo (PENDING, RESPONDED, etc.)
     */
    public String getStatus() { 
        return status; 
    }
    
    /**
     * Imposta lo stato della richiesta
     * @param status Nuovo stato processo (required)
     */
    public void setStatus(String status) { 
        this.status = status; 
    }
    
    /**
     * Restituisce l'email del richiedente
     * @return Email per invio risposta
     */
    public String getUserEmail() { 
        return userEmail; 
    }
    
    /**
     * Imposta l'email del richiedente
     * @param userEmail Email valida per contatto (required)
     */
    public void setUserEmail(String userEmail) { 
        this.userEmail = userEmail; 
    }
    
    /**
     * Restituisce il messaggio di risposta del dealer
     * @return Risposta dettagliata del concessionario
     */
    public String getResponseMessage() { 
        return responseMessage; 
    }
    
    /**
     * Imposta il messaggio di risposta del dealer
     * @param responseMessage Risposta da inviare al cliente
     */
    public void setResponseMessage(String responseMessage) { 
        this.responseMessage = responseMessage; 
    }
    
    /**
     * Restituisce il tipo della richiesta
     * @return Classificazione richiesta (PRICE_QUOTE, INFO_REQUEST, etc.)
     */
    public String getRequestType() { 
        return requestType; 
    }
    
    /**
     * Imposta il tipo della richiesta
     * @param requestType Classificazione per routing e gestione
     */
    public void setRequestType(String requestType) { 
        this.requestType = requestType; 
    }
    
    /**
     * Restituisce l'email destinatario per notifiche
     * @return Email per routing interno notifiche
     */
    public String getRecipientEmail() { 
        return recipientEmail; 
    }
    
    /**
     * Imposta l'email destinatario per notifiche
     * @param recipientEmail Email routing notifiche sistema
     */
    public void setRecipientEmail(String recipientEmail) { 
        this.recipientEmail = recipientEmail; 
    }
}