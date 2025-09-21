package it.uniroma3.siwprogetto.config;

/**
 * Costanti per i ruoli e le autorizzazioni di sicurezza dell'applicazione.
 * Centralizza la definizione dei ruoli per evitare duplicazioni e errori.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
public final class SecurityConstants {
    
    // === RUOLI UTENTE ===
    
    /** Ruolo amministratore - accesso completo al sistema */
    public static final String ADMIN_ROLE = "ROLE_ADMIN";
    
    /** Ruolo utente standard - accesso alle funzionalità base */
    public static final String USER_ROLE = "ROLE_USER";
    
    /** Ruolo utente privato - accesso all'area riservata */
    public static final String PRIVATE_ROLE = "ROLE_PRIVATE";
    
    /** Ruolo concessionario - gestione prodotti e vendite */
    public static final String DEALER_ROLE = "ROLE_DEALER";
    
    // === COSTRUTTORE PRIVATO ===
    
    /**
     * Costruttore privato per impedire l'istanziazione della classe.
     * Questa è una classe di sole costanti.
     */
    private SecurityConstants() {
        throw new UnsupportedOperationException("Questa è una classe di costanti e non può essere istanziata");
    }
}