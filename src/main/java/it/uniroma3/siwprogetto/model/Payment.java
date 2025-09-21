package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entità JPA per la gestione dei pagamenti nel sistema FCF Motors
 * Traccia tutte le transazioni di pagamento degli utenti per abbonamenti e servizi
 * 
 * Tabella: payments
 * Relazioni: ManyToOne con User (un utente può avere più pagamenti)
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
@Table(name = "payments")
public class Payment {

    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni pagamento
     * Strategia IDENTITY per compatibilità con database MySQL/PostgreSQL
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === RELAZIONE CON UTENTE ===
    /**
     * Riferimento all'utente che ha effettuato il pagamento
     * Relazione Many-to-One: un utente può avere più pagamenti
     * Non nullable: ogni pagamento deve essere associato a un utente
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // === INFORMAZIONI FINANZIARIE ===
    /**
     * Importo del pagamento in formato BigDecimal per precisione monetaria
     * Non nullable: ogni pagamento deve avere un importo definito
     * Utilizzato per calcolare fatturato e commissioni
     */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * Identificatore univoco della transazione fornito dal gateway di pagamento
     * Non nullable: necessario per tracciabilità e riconciliazione
     * Utilizzato per rimborsi e verifiche con provider di pagamento
     */
    @Column(nullable = false)
    private String transactionId;

    // === INFORMAZIONI TEMPORALI ===
    /**
     * Data e ora di quando il pagamento è stato processato
     * Non nullable: fondamentale per audit e reportistica
     * Impostato automaticamente nel costruttore con parametri
     */
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    // === STATO TRANSAZIONE ===
    /**
     * Stato del pagamento per tracciare il ciclo di vita della transazione
     * Non nullable: necessario per gestione flusso pagamenti
     * 
     * Valori comuni:
     * - "PENDING": In attesa di elaborazione
     * - "COMPLETED": Pagamento completato con successo
     * - "FAILED": Pagamento fallito
     * - "CANCELLED": Pagamento cancellato
     * - "REFUNDED": Pagamento rimborsato
     */
    @Column(nullable = false)
    private String status;

    // === COSTRUTTORI ===
    
    /**
     * Costruttore vuoto richiesto da JPA per la creazione delle entità
     * Utilizzato dall'ORM durante il caricamento dei dati dal database
     */
    public Payment() {
    }

    /**
     * Costruttore principale per la creazione di nuovi pagamenti
     * Imposta automaticamente la data del pagamento al momento corrente
     * 
     * @param user Utente che effettua il pagamento (required)
     * @param amount Importo del pagamento (required)
     * @param transactionId ID univoco della transazione (required)
     * @param status Stato iniziale del pagamento (required)
     */
    public Payment(User user, BigDecimal amount, String transactionId, String status) {
        this.user = user;
        this.amount = amount;
        this.transactionId = transactionId;
        this.status = status;
        this.paymentDate = LocalDateTime.now(); // Timestamp automatico
    }

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco del pagamento
     * @return ID del pagamento o null se non ancora persistito
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'ID del pagamento (generalmente gestito da JPA)
     * @param id Nuovo ID del pagamento
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce l'utente associato al pagamento
     * @return Utente che ha effettuato il pagamento
     */
    public User getUser() {
        return user;
    }

    /**
     * Imposta l'utente associato al pagamento
     * @param user Utente che effettua il pagamento (required)
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Restituisce l'importo del pagamento
     * @return Importo in BigDecimal per precisione monetaria
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Imposta l'importo del pagamento
     * @param amount Importo del pagamento (required, must be positive)
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Restituisce l'ID univoco della transazione
     * @return ID transazione fornito dal gateway di pagamento
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Imposta l'ID univoco della transazione
     * @param transactionId ID fornito dal provider di pagamento (required)
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Restituisce la data e ora del pagamento
     * @return Timestamp di quando è stato processato il pagamento
     */
    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    /**
     * Imposta la data e ora del pagamento
     * @param paymentDate Timestamp del pagamento (required)
     */
    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    /**
     * Restituisce lo stato corrente del pagamento
     * @return Stato del pagamento (PENDING, COMPLETED, FAILED, etc.)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Imposta lo stato del pagamento
     * @param status Nuovo stato del pagamento (required)
     */
    public void setStatus(String status) {
        this.status = status;
    }
}