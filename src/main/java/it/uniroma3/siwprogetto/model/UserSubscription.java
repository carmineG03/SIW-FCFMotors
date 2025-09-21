package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entità JPA per gestire l'associazione tra utenti e abbonamenti attivi
 * Rappresenta l'istanza di un abbonamento sottoscritto da un utente specifico
 * 
 * Funzionalità principali:
 * - Traccia periodo di validità abbonamento
 * - Gestisce rinnovo automatico
 * - Controlla stato attivazione
 * - Verifica scadenze
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
public class UserSubscription {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni sottoscrizione
     * Utilizzata per gestire ciclo di vita degli abbonamenti utente
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === RELAZIONI CON ALTRE ENTITÀ ===
    
    /**
     * Riferimento all'utente che ha sottoscritto l'abbonamento
     * Relazione Many-to-One: un utente può avere più abbonamenti nel tempo
     * (anche se tipicamente uno solo attivo alla volta)
     * 
     * Utilizzi:
     * - Identificazione proprietario abbonamento
     * - Applicazione benefici e limitazioni
     * - Storico sottoscrizioni utente
     */
    @ManyToOne
    private User user;

    /**
     * Riferimento al tipo di abbonamento sottoscritto
     * Relazione Many-to-One: lo stesso piano può essere sottoscritto da più utenti
     * 
     * Utilizzi:
     * - Definizione caratteristiche abbonamento
     * - Prezzo e durata
     * - Benefici inclusi (max auto in evidenza, etc.)
     */
    @ManyToOne
    private Subscription subscription;

    // === INFORMAZIONI TEMPORALI ===
    
    /**
     * Data di inizio validità dell'abbonamento
     * Utilizzata per:
     * - Calcolo periodo di validità
     * - Generazione fatture
     * - Reportistica sottoscrizioni
     * - Audit trail modifiche abbonamento
     */
    private LocalDate startDate;
    
    /**
     * Data di scadenza dell'abbonamento
     * Utilizzata per:
     * - Controllo validità abbonamento
     * - Notifiche scadenza
     * - Gestione rinnovi automatici
     * - Disattivazione servizi scaduti
     */
    private LocalDate expiryDate;

    // === STATO ABBONAMENTO ===
    
    /**
     * Flag per indicare se l'abbonamento è attualmente attivo
     * 
     * Controlli:
     * - true: abbonamento valido e servizi disponibili
     * - false: abbonamento sospeso o disattivato
     * 
     * Differenza da scadenza:
     * - Un abbonamento può essere attivo ma scaduto (in attesa rinnovo)
     * - Un abbonamento può essere non-attivo ma non scaduto (sospensione)
     */
    private boolean active;

    /**
     * Flag per gestione del rinnovo automatico
     * 
     * Comportamento:
     * - true: abbonamento si rinnova automaticamente alla scadenza
     * - false: abbonamento scade senza rinnovo automatico
     * 
     * Utilizzi:
     * - Processamento pagamenti ricorrenti
     * - Continuità servizi senza interruzioni
     * - Gestione billing automatizzato
     */
    private boolean autoRenew;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco della sottoscrizione
     * @return ID della sottoscrizione utente
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID della sottoscrizione (gestito da JPA)
     * @param id Nuovo ID della sottoscrizione
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce l'utente proprietario dell'abbonamento
     * @return Utente che ha sottoscritto l'abbonamento
     */
    public User getUser() { 
        return user; 
    }
    
    /**
     * Imposta l'utente proprietario dell'abbonamento
     * @param user Utente sottoscrittore (required)
     */
    public void setUser(User user) { 
        this.user = user; 
    }
    
    /**
     * Restituisce il piano di abbonamento sottoscritto
     * @return Dettagli del piano abbonamento
     */
    public Subscription getSubscription() { 
        return subscription; 
    }
    
    /**
     * Imposta il piano di abbonamento sottoscritto
     * @param subscription Piano abbonamento da associare (required)
     */
    public void setSubscription(Subscription subscription) { 
        this.subscription = subscription; 
    }
    
    /**
     * Restituisce la data di inizio abbonamento
     * @return Data di attivazione dell'abbonamento
     */
    public LocalDate getStartDate() { 
        return startDate; 
    }
    
    /**
     * Imposta la data di inizio abbonamento
     * @param startDate Data di attivazione (required)
     */
    public void setStartDate(LocalDate startDate) { 
        this.startDate = startDate; 
    }
    
    /**
     * Restituisce la data di scadenza abbonamento
     * @return Data di fine validità abbonamento
     */
    public LocalDate getExpiryDate() { 
        return expiryDate; 
    }
    
    /**
     * Imposta la data di scadenza abbonamento
     * @param expiryDate Data di scadenza (must be after startDate)
     */
    public void setExpiryDate(LocalDate expiryDate) { 
        this.expiryDate = expiryDate; 
    }
    
    /**
     * Verifica se l'abbonamento è attualmente attivo
     * @return true se l'abbonamento è attivo e utilizzabile
     */
    public boolean isActive() { 
        return active; 
    }
    
    /**
     * Imposta lo stato di attivazione dell'abbonamento
     * @param active true per attivare, false per disattivare
     */
    public void setActive(boolean active) { 
        this.active = active; 
    }
    
    /**
     * Verifica se il rinnovo automatico è abilitato
     * @return true se l'abbonamento si rinnova automaticamente
     */
    public boolean isAutoRenew() { 
        return autoRenew; 
    }
    
    /**
     * Imposta la modalità di rinnovo automatico
     * @param autoRenew true per abilitare rinnovo automatico
     */
    public void setAutoRenew(boolean autoRenew) { 
        this.autoRenew = autoRenew; 
    }
    
    // === METODI DI BUSINESS LOGIC ===
    
    /**
     * Verifica se l'abbonamento è scaduto e non rinnovabile
     * 
     * Logica di controllo:
     * - Se expiryDate è null → non scaduto
     * - Se data corrente è dopo expiryDate E autoRenew è false → scaduto
     * - Se autoRenew è true → non considerato scaduto (rinnovo automatico)
     * 
     * @return true se l'abbonamento è definitivamente scaduto
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate) && !autoRenew;
    }

    /**
     * Effettua il rinnovo dell'abbonamento se il rinnovo automatico è abilitato
     * 
     * Operazioni eseguite:
     * - Estende expiryDate di 1 mese dalla data corrente
     * - Riattiva l'abbonamento (active = true)
     * - Operazione eseguita solo se autoRenew = true
     * 
     * Nota: il metodo andrebbe integrato con sistema di pagamento
     * per gestire anche l'addebito della quota di rinnovo
     */
    public void renew() {
        if (autoRenew) {
            this.expiryDate = this.expiryDate.plusMonths(1);
            this.active = true;
        }
    }
}