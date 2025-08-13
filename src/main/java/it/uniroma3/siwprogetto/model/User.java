package it.uniroma3.siwprogetto.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Entità JPA principale per rappresentare gli utenti del sistema FCF Motors
 * Implementa UserDetails di Spring Security per integrazione con autenticazione
 * 
 * Funzionalità:
 * - Gestione credenziali e autenticazione
 * - Profilo utente con informazioni dettagliate
 * - Associazione con prodotti venduti e concessionari
 * - Sistema di reset password
 * - Carrello acquisti personale
 * - Gestione abbonamenti attivi
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni utente
     * Utilizzata come riferimento in tutte le relazioni con altre entità
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // === CREDENZIALI AUTENTICAZIONE ===
    
    /**
     * Nome utente univoco per il login
     * Deve essere unico nell'intero sistema
     * Utilizzato per:
     * - Login e autenticazione
     * - Identificazione pubblica utente
     * - URL profili pubblici
     * - Ricerca utenti admin
     */
    private String username;
    
    /**
     * Password hashata dell'utente
     * Memorizzata con algoritmo di hashing sicuro (BCrypt)
     * Mai esposta in plain text nelle API o log
     * 
     * Sicurezza:
     * - Hash irreversibile
     * - Salt automatico
     * - Resistente ad attacchi rainbow table
     */
    private String password;
    
    /**
     * Indirizzo email dell'utente
     * Deve essere univoco e valido
     * Utilizzato per:
     * - Comunicazioni sistema
     * - Reset password
     * - Notifiche importanti
     * - Marketing (con consenso)
     */
    private String email;

    // === CAMPI TRANSIENT (NON PERSISTITI) ===
    
    /**
     * Conferma password per validazione form registrazione
     * Campo transient: non memorizzato nel database
     * Utilizzato solo durante il processo di registrazione
     * per verificare che l'utente abbia inserito correttamente la password
     */
    @Transient
    private String confirmPassword;

    // === GESTIONE AUTORIZZAZIONI ===
    
    /**
     * Ruoli utente serializzati come stringa
     * Formato: ruoli separati da virgola (es. "USER,ADMIN")
     * 
     * Ruoli comuni:
     * - "USER": utente standard
     * - "DEALER": concessionario
     * - "ADMIN": amministratore sistema
     * 
     * Utilizzato da Spring Security per controllo accessi
     */
    private String rolesString;

    // === SISTEMA RESET PASSWORD ===
    
    /**
     * Token univoco per il reset della password
     * Generato quando l'utente richiede il reset password
     * Token sicuro e casuale per prevenire attacchi
     * 
     * Flusso reset:
     * 1. Utente richiede reset
     * 2. Sistema genera token e lo salva
     * 3. Token inviato via email
     * 4. Utente usa token per impostare nuova password
     * 5. Token viene invalidato
     */
    private String resetToken;
    
    /**
     * Data e ora di scadenza del token reset password
     * Limita nel tempo la validità del token per sicurezza
     * Tipicamente valido per 1-24 ore dalla generazione
     * 
     * Controllo scadenza:
     * - Se LocalDateTime.now() > resetTokenExpiry → token scaduto
     * - Token scaduti non sono accettati per reset password
     */
    private LocalDateTime resetTokenExpiry;

    // === RELAZIONI CON ALTRE ENTITÀ ===
    
    /**
     * Informazioni dettagliate dell'account utente
     * Relazione One-to-One bidirezionale con AccountInformation
     * 
     * mappedBy "user": AccountInformation ha campo user per relazione inversa
     * CASCADE ALL: operazioni su User si propagano ad AccountInformation
     * 
     * Contiene:
     * - Dati anagrafici (nome, cognome, data nascita)
     * - Informazioni contatto (indirizzo, telefono)
     * - Note aggiuntive
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private AccountInformation accountInformation;

    /**
     * Prodotti venduti dall'utente
     * Relazione One-to-Many: un utente può vendere più prodotti
     * 
     * mappedBy "seller": Product ha campo seller per relazione
     * @JsonIgnore: previene loop infiniti nella serializzazione JSON
     * 
     * Utilizzi:
     * - Catalogo personale venditore
     * - Gestione inventario utente
     * - Statistiche vendite
     * - Controllo proprietario prodotto
     */
    @OneToMany(mappedBy = "seller")
    @JsonIgnore
    private Set<Product> products;

    /**
     * Concessionario di proprietà dell'utente
     * Relazione One-to-One: ogni utente può possedere al massimo un concessionario
     * 
     * mappedBy "owner": Dealer ha campo owner per relazione inversa
     * CASCADE ALL: eliminazione user → eliminazione dealer
     * @JsonBackReference: evita loop in serializzazione JSON bidirezionale
     * 
     * Controlli business:
     * - Solo utenti con ruolo DEALER possono avere un concessionario
     * - Un concessionario ha sempre un solo proprietario
     */
    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL)
    @JsonBackReference
    private Dealer dealer;

    /**
     * Elementi nel carrello dell'utente
     * Relazione One-to-Many: un utente ha un carrello con più elementi
     * 
     * mappedBy "user": CartItem ha campo user per relazione
     * Set: garantisce unicità elementi (stesso prodotto non duplicato)
     * 
     * Funzionalità:
     * - Carrello persistente tra sessioni
     * - Gestione quantità prodotti
     * - Calcolo totali pre-checkout
     */
    @OneToMany(mappedBy = "user")
    private Set<CartItem> cartItems;

    /**
     * Abbonamento attivo dell'utente
     * Relazione Many-to-One: più utenti possono avere lo stesso piano abbonamento
     * 
     * Nullable: utenti possono non avere abbonamento attivo
     * 
     * Utilizzi:
     * - Controllo limiti servizi (max auto in evidenza)
     * - Applicazione sconti e benefici
     * - Gestione fatturazione ricorrente
     * - Controllo accesso funzionalità premium
     */
    @ManyToOne
    private Subscription subscription;

    // === COSTRUTTORI ===
    
    /**
     * Costruttore vuoto richiesto da JPA
     * Utilizzato dall'ORM per istanziare entità dal database
     */
    public User() {
    }

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco dell'utente
     * @return ID utente o null se non ancora persistito
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'ID dell'utente (gestito da JPA)
     * @param id Nuovo ID utente
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce il nome utente per il login
     * @return Username univoco nel sistema
     */
    public String getUsername() {
        return username;
    }

    /**
     * Imposta il nome utente per il login
     * @param username Nome utente univoco (required)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Restituisce la password hashata dell'utente
     * @return Password in formato hash (mai plain text)
     */
    public String getPassword() {
        return password;
    }

    /**
     * Imposta la password dell'utente
     * @param password Password da hashare (should be hashed before storing)
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Restituisce l'email dell'utente
     * @return Indirizzo email per comunicazioni
     */
    public String getEmail() {
        return email;
    }

    /**
     * Imposta l'email dell'utente
     * @param email Indirizzo email valido e univoco (required)
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Restituisce la conferma password (campo transient)
     * @return Password di conferma inserita dall'utente
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * Imposta la conferma password per validazione
     * @param confirmPassword Conferma password (used only during registration)
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    /**
     * Restituisce i ruoli dell'utente come stringa
     * @return Ruoli separati da virgola
     */
    public String getRolesString() {
        return rolesString;
    }

    /**
     * Imposta i ruoli dell'utente
     * @param rolesString Ruoli separati da virgola (es. "USER,DEALER")
     */
    public void setRolesString(String rolesString) {
        this.rolesString = rolesString;
    }

    /**
     * Restituisce il token per reset password
     * @return Token sicuro per reset password o null
     */
    public String getResetToken() {
        return resetToken;
    }

    /**
     * Imposta il token per reset password
     * @param resetToken Token generato per reset password
     */
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    /**
     * Restituisce la scadenza del token reset password
     * @return Data/ora scadenza token
     */
    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    /**
     * Imposta la scadenza del token reset password
     * @param resetTokenExpiry Data/ora scadenza (typically 1-24 hours from generation)
     */
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    /**
     * Restituisce le informazioni dettagliate dell'account
     * @return Dati anagrafici e di contatto estesi
     */
    public AccountInformation getAccountInformation() {
        return accountInformation;
    }

    /**
     * Imposta le informazioni dettagliate dell'account
     * @param accountInformation Dati estesi del profilo utente
     */
    public void setAccountInformation(AccountInformation accountInformation) {
        this.accountInformation = accountInformation;
    }

    /**
     * Restituisce i prodotti venduti dall'utente
     * @return Set di prodotti in vendita
     */
    public Set<Product> getProducts() {
        return products;
    }

    /**
     * Imposta i prodotti venduti dall'utente
     * @param products Set di prodotti da associare all'utente
     */
    public void setProducts(Set<Product> products) {
        this.products = products;
    }

    /**
     * Restituisce gli elementi nel carrello dell'utente
     * @return Set di elementi nel carrello
     */
    public Set<CartItem> getCartItems() {
        return cartItems;
    }

    /**
     * Imposta gli elementi nel carrello dell'utente
     * @param cartItems Set di elementi carrello
     */
    public void setCartItems(Set<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    /**
     * Restituisce il concessionario di proprietà
     * @return Concessionario gestito dall'utente o null
     */
    public Dealer getDealer() {
        return dealer;
    }

    /**
     * Imposta il concessionario di proprietà
     * @param dealer Concessionario da associare (max 1 per utente)
     */
    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
    }

    /**
     * Restituisce l'abbonamento attivo dell'utente
     * @return Piano abbonamento corrente o null
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * Imposta l'abbonamento dell'utente
     * @param subscription Piano abbonamento da attivare
     */
    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    // === IMPLEMENTAZIONE USERDETAILS (SPRING SECURITY) ===
    
    /**
     * Restituisce le autorizzazioni dell'utente per Spring Security
     * 
     * Implementazione semplificata: restituisce lista vuota
     * In implementazione completa, parsa rolesString e crea GrantedAuthority
     * 
     * @return Lista autorizzazioni (attualmente vuota)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    /**
     * Verifica se l'account utente non è scaduto
     * @return true (account non hanno scadenza nel sistema corrente)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Verifica se l'account utente non è bloccato
     * @return true (nessun sistema di blocco account implementato)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Verifica se le credenziali non sono scadute
     * @return true (password non hanno scadenza nel sistema corrente)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Verifica se l'account è abilitato
     * @return true (tutti gli account sono abilitati di default)
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}