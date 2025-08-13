package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entità JPA per memorizzare informazioni dettagliate dell'account utente
 * Estende i dati base dell'utente con informazioni personali e di contatto
 * 
 * Tabella: account_information
 * Relazione: OneToOne con User (ogni utente ha un'unica informazione account)
 * 
 * Utilizzo:
 * - Profilo utente completo
 * - Dati per fatturazione e spedizione
 * - Informazioni anagrafiche per verifiche
 */
@Entity
@Table(name = "account_information")
public class AccountInformation {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente le informazioni account
     * Strategia IDENTITY per compatibilità con database relazionali standard
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === INFORMAZIONI ANAGRAFICHE ===
    
    /**
     * Nome proprio dell'utente
     * Utilizzato per:
     * - Personalizzazione interfaccia
     * - Generazione fatture e documenti
     * - Comunicazioni formali
     */
    private String firstName;
    
    /**
     * Cognome dell'utente
     * Utilizzato per:
     * - Identificazione completa utente
     * - Documenti legali e contratti
     * - Ordinamento e ricerca utenti
     */
    private String lastName;
    
    /**
     * Data di nascita dell'utente
     * Formato: LocalDate per gestione date senza timezone
     * Utilizzi:
     * - Verifica età per servizi riservati
     * - Validazioni anagrafiche
     * - Statistiche demografiche
     */
    private LocalDate birthDate;

    // === INFORMAZIONI DI CONTATTO ===
    
    /**
     * Indirizzo di residenza o domicilio dell'utente
     * Formato libero per supportare diversi formati internazionali
     * Utilizzi:
     * - Spedizione documenti
     * - Localizzazione geografica
     * - Verifiche di residenza
     */
    private String address;
    
    /**
     * Numero di telefono dell'utente
     * Formato libero per supportare prefissi internazionali
     * Utilizzi:
     * - Contatti di emergenza
     * - Autenticazione a due fattori
     * - Comunicazioni urgenti
     */
    private String phoneNumber;

    // === INFORMAZIONI AGGIUNTIVE ===
    
    /**
     * Campo libero per informazioni aggiuntive dell'utente
     * Utilizzi:
     * - Note speciali sul cliente
     * - Preferenze particolari
     * - Istruzioni per consegne
     * - Codici cliente di sistemi esterni
     */
    private String additionalInfo;

    // === CONTROLLO CONCORRENZA ===
    
    /**
     * Versione dell'entità per controllo della concorrenza ottimistica
     * JPA incrementa automaticamente ad ogni update
     * Previene perdite di dati in caso di modifiche concorrenti
     * 
     * Funzionamento:
     * 1. Caricamento entità → version = N
     * 2. Modifica concorrente → version = N+1
     * 3. Tentativo salvataggio con version = N → OptimisticLockException
     */
    @Version
    @Column(name = "version")
    private Long version;

    // === RELAZIONE CON UTENTE ===
    
    /**
     * Relazione bidirezionale One-to-One con l'entità User
     * Ogni utente ha esattamente un'informazione account associata
     * 
     * Join Column: user_id referenzia User.id
     * Garantisce integrità referenziale nel database
     */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco delle informazioni account
     * @return ID dell'account information o null se non ancora persistito
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'ID delle informazioni account (gestito da JPA)
     * @param id Nuovo ID dell'account information
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce il nome dell'utente
     * @return Nome proprio dell'utente
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Imposta il nome dell'utente
     * @param firstName Nome proprio dell'utente
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Restituisce il cognome dell'utente
     * @return Cognome dell'utente
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Imposta il cognome dell'utente
     * @param lastName Cognome dell'utente
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Restituisce la data di nascita dell'utente
     * @return Data di nascita in formato LocalDate
     */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Imposta la data di nascita dell'utente
     * @param birthDate Data di nascita (deve essere nel passato)
     */
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Restituisce l'indirizzo dell'utente
     * @return Indirizzo completo di residenza/domicilio
     */
    public String getAddress() {
        return address;
    }

    /**
     * Imposta l'indirizzo dell'utente
     * @param address Indirizzo di residenza/domicilio
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Restituisce il numero di telefono dell'utente
     * @return Numero di telefono con eventuale prefisso
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Imposta il numero di telefono dell'utente
     * @param phoneNumber Numero di telefono valido
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Restituisce le informazioni aggiuntive dell'utente
     * @return Campo libero con note aggiuntive
     */
    public String getAdditionalInfo() {
        return additionalInfo;
    }

    /**
     * Imposta le informazioni aggiuntive dell'utente
     * @param additionalInfo Note aggiuntive o istruzioni speciali
     */
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    /**
     * Restituisce la versione corrente dell'entità
     * @return Numero di versione per controllo concorrenza
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Imposta la versione dell'entità (gestito da JPA)
     * @param version Nuovo numero di versione
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Restituisce l'utente associato a queste informazioni
     * @return Utente proprietario dell'account
     */
    public User getUser() {
        return user;
    }

    /**
     * Imposta l'utente associato a queste informazioni
     * @param user Utente proprietario (required)
     */
    public void setUser(User user) {
        this.user = user;
    }
}