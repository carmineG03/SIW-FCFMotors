package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entità JPA per rappresentare un concessionario di automobili nel sistema FCF Motors
 * Un dealer è una struttura commerciale che vende veicoli ed è gestita da un utente registrato
 * 
 * Funzionalità principali:
 * - Gestione informazioni concessionario (nome, descrizione, contatti)
 * - Associazione con proprietario (User)
 * - Gallery immagini per presentazione
 * - Localizzazione geografica per ricerche
 */
@Entity
public class Dealer {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni concessionario
     * Utilizzata per tutte le operazioni CRUD e per le relazioni con altre entità
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === INFORMAZIONI IDENTIFICATIVE ===
    
    /**
     * Nome commerciale del concessionario
     * Campo principale per identificazione e ricerca
     * Utilizzato in:
     * - Elenchi e cataloghi
     * - Ricerche utenti
     * - Intestazioni documenti
     * - SEO e marketing
     */
    private String name;
    
    /**
     * Descrizione dettagliata del concessionario
     * Testo libero per presentare l'attività, servizi offerti, storia aziendale
     * Utilizzi:
     * - Pagina dettaglio concessionario
     * - Marketing e presentazione
     * - SEO content
     * - Differenziazione dalla concorrenza
     */
    private String description;

    // === INFORMAZIONI DI LOCALIZZAZIONE ===
    
    /**
     * Indirizzo fisico del concessionario
     * Include via, civico, città, CAP per localizzazione completa
     * Utilizzi:
     * - Ricerca geografica concessionari
     * - Navigazione GPS
     * - Spedizioni e logistica
     * - Servizi di localizzazione
     */
    private String address;

    // === INFORMAZIONI DI CONTATTO ===
    
    /**
     * Numero di telefono del concessionario
     * Contatto principale per chiamate dirette
     * Formato libero per supportare formati internazionali
     * Utilizzi:
     * - Click-to-call su mobile
     * - Contatti di emergenza
     * - Prenotazione appuntamenti
     */
    private String phone;
    
    /**
     * Indirizzo email del concessionario
     * Utilizzato per comunicazioni ufficiali e automatizzate
     * Deve essere un indirizzo valido e monitorato
     * Utilizzi:
     * - Notifiche preventivi
     * - Comunicazioni clienti
     * - Newsletter e marketing
     * - Supporto tecnico
     */
    private String email;

    // === RELAZIONI CON ALTRE ENTITÀ ===
    
    /**
     * Proprietario del concessionario - Relazione One-to-One con User
     * Ogni concessionario ha un unico proprietario responsabile
     * Ogni utente può possedere al massimo un concessionario
     * 
     * Implicazioni:
     * - Solo il proprietario può modificare il concessionario
     * - Solo il proprietario può gestire i prodotti
     * - Solo il proprietario può rispondere ai preventivi
     * - Il proprietario riceve notifiche per il concessionario
     */
    @OneToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Gallery immagini del concessionario - Relazione One-to-Many con Image
     * Ogni concessionario può avere multiple immagini per la presentazione
     * 
     * Configurazione:
     * - EAGER fetch: immagini caricate automaticamente con il dealer
     * - CASCADE ALL: operazioni su dealer si propagano alle immagini
     * - mappedBy "dealer": Image ha campo dealer per la relazione bidirezionale
     * 
     * Utilizzi:
     * - Galleria fotografica nella pagina dealer
     * - Anteprime nelle liste
     * - Marketing visivo
     * - Presentazione sede/showroom
     */
    @OneToMany(mappedBy = "dealer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Image> images;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco del concessionario
     * @return ID del dealer o null se non ancora persistito
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID del concessionario (gestito da JPA)
     * @param id Nuovo ID del dealer
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce il nome commerciale del concessionario
     * @return Nome del concessionario
     */
    public String getName() { 
        return name; 
    }
    
    /**
     * Imposta il nome commerciale del concessionario
     * @param name Nome del concessionario (required, deve essere univoco)
     */
    public void setName(String name) { 
        this.name = name; 
    }
    
    /**
     * Restituisce la descrizione del concessionario
     * @return Descrizione dettagliata dell'attività
     */
    public String getDescription() { 
        return description; 
    }
    
    /**
     * Imposta la descrizione del concessionario
     * @param description Descrizione dell'attività e servizi offerti
     */
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    /**
     * Restituisce l'indirizzo fisico del concessionario
     * @return Indirizzo completo della sede
     */
    public String getAddress() { 
        return address; 
    }
    
    /**
     * Imposta l'indirizzo fisico del concessionario
     * @param address Indirizzo completo con città e CAP
     */
    public void setAddress(String address) { 
        this.address = address; 
    }
    
    /**
     * Restituisce il numero di telefono del concessionario
     * @return Numero di telefono per contatti diretti
     */
    public String getPhone() { 
        return phone; 
    }
    
    /**
     * Imposta il numero di telefono del concessionario
     * @param phone Numero di telefono valido
     */
    public void setPhone(String phone) { 
        this.phone = phone; 
    }
    
    /**
     * Restituisce l'email del concessionario
     * @return Indirizzo email per comunicazioni ufficiali
     */
    public String getEmail() { 
        return email; 
    }
    
    /**
     * Imposta l'email del concessionario
     * @param email Indirizzo email valido e monitorato
     */
    public void setEmail(String email) { 
        this.email = email; 
    }
    
    /**
     * Restituisce il proprietario del concessionario
     * @return Utente proprietario e responsabile del dealer
     */
    public User getOwner() { 
        return owner; 
    }
    
    /**
     * Imposta il proprietario del concessionario
     * @param owner Utente che diventa proprietario (required)
     */
    public void setOwner(User owner) { 
        this.owner = owner; 
    }
    
    /**
     * Restituisce la lista delle immagini del concessionario
     * @return Lista di immagini per la gallery (può essere vuota ma non null)
     */
    public List<Image> getImages() { 
        return images; 
    }
    
    /**
     * Imposta la lista delle immagini del concessionario
     * @param images Nuova lista di immagini per la gallery
     */
    public void setImages(List<Image> images) { 
        this.images = images; 
    }
}