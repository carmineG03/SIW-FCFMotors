package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entità JPA per rappresentare un prodotto (automobile) nel sistema FCF Motors
 * Gestisce tutte le informazioni tecniche, commerciali e di marketing dei veicoli
 * 
 * Funzionalità principali:
 * - Catalogazione dettagliata veicoli
 * - Sistema di evidenziazione temporizzata
 * - Gestione gallery fotografica
 * - Associazione con venditore
 * - Pricing e informazioni commerciali
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
@Table(name = "product")
public class Product {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni prodotto
     * Utilizzata come riferimento in tutte le operazioni CRUD e relazioni
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === INFORMAZIONI DESCRITTIVE ===
    /**
     * Descrizione dettagliata del prodotto
     * Campo TEXT per supportare descrizioni estese
     * Nullable: può essere vuoto per prodotti con informazioni minime
     * 
     * Utilizzi:
     * - Pagina dettaglio prodotto
     * - SEO content per motori ricerca
     * - Informazioni tecniche aggiuntive
     * - Note specifiche del venditore
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    // === INFORMAZIONI COMMERCIALI ===
    /**
     * Prezzo del prodotto in formato BigDecimal
     * Campo obbligatorio per tutti i prodotti in vendita
     * Precisione monetaria garantita per transazioni commerciali
     * 
     * Utilizzi:
     * - Visualizzazione prezzo nei cataloghi
     * - Calcoli carrello e ordini
     * - Filtri ricerca per fascia prezzo
     * - Reportistica vendite
     */
    @Column(nullable = false)
    private BigDecimal price;

    /**
     * Categoria del prodotto per classificazione
     * Lunghezza massima: 100 caratteri
     * 
     * Esempi:
     * - "Auto"
     * - "Moto"
     * - "Veicoli Commerciali"
     * - "Auto d'Epoca"
     * 
     * Utilizzi:
     * - Filtri ricerca avanzata
     * - Navigazione catalogo
     * - Statistiche per categoria
     * - Organizzazione inventario
     */
    @Column(length = 100)
    private String category;

    // === INFORMAZIONI TECNICHE VEICOLO ===
    /**
     * Marca/Brand del veicolo
     * Lunghezza massima: 100 caratteri
     * 
     * Esempi: "BMW", "Mercedes", "Audi", "Toyota", "Ford"
     * 
     * Utilizzi:
     * - Ricerca per marca
     * - Filtri brand-specific
     * - Statistiche vendite per marca
     * - Presentazione organizzata catalogo
     */
    @Column(length = 100)
    private String brand;

    /**
     * Modello specifico del veicolo
     * Lunghezza massima: 100 caratteri
     * 
     * Esempi: "Serie 3", "Classe A", "Golf", "Corolla"
     * 
     * Utilizzi:
     * - Identificazione specifica veicolo
     * - Ricerca modello esatto
     * - Confronto tra varianti
     * - Valutazione di mercato
     */
    @Column(length = 100)
    private String model;

    /**
     * Chilometraggio del veicolo
     * Campo opzionale per veicoli usati
     * Nullo per veicoli km 0 o nuovi
     * 
     * Utilizzi:
     * - Valutazione stato veicolo
     * - Filtri ricerca per chilometraggio
     * - Calcolo valore residuo
     * - Informazione acquisto critica
     */
    private Integer mileage;

    /**
     * Anno di immatricolazione del veicolo
     * Campo opzionale ma molto importante per valutazione
     * 
     * Utilizzi:
     * - Determinazione età veicolo
     * - Filtri ricerca per anno
     * - Calcolo deprezzamento
     * - Controllo compatibilità normative
     */
    private Integer year;

    /**
     * Tipo di alimentazione del veicolo
     * Lunghezza massima: 50 caratteri
     * 
     * Valori comuni:
     * - "Benzina"
     * - "Diesel"
     * - "GPL"
     * - "Metano"
     * - "Elettrica"
     * - "Ibrida"
     * 
     * Utilizzi:
     * - Filtri ecologici
     * - Calcolo consumi stimati
     * - Compatibilità incentivi
     * - Restrizioni traffico urbano
     */
    @Column(length = 50)
    private String fuelType;

    /**
     * Tipo di trasmissione del veicolo
     * Lunghezza massima: 50 caratteri
     * 
     * Valori comuni:
     * - "Manuale"
     * - "Automatica"
     * - "Semiautomatica"
     * - "CVT"
     * 
     * Utilizzi:
     * - Preferenze guidatore
     * - Filtri comfort
     * - Valutazione facilità guida
     * - Segmentazione target clientela
     */
    @Column(length = 50)
    private String transmission;

    /**
     * Tipologia del venditore
     * Lunghezza massima: 50 caratteri
     * 
     * Valori possibili:
     * - "Concessionario"
     * - "Privato" 
     * - "Dealer"
     * 
     * Utilizzi:
     * - Distinzione fonte vendita
     * - Filtri per tipo venditore
     * - Gestione garanzie diverse
     * - Politiche prezzo differenziate
     */
    @Column(length = 50)
    private String sellerType;

    // === RELAZIONE CON VENDITORE ===
    /**
     * Utente che vende il prodotto
     * Relazione Many-to-One: un utente può vendere più prodotti
     * 
     * Utilizzi:
     * - Identificazione proprietario annuncio
     * - Gestione autorizzazioni modifica
     * - Contatti per informazioni
     * - Storico vendite utente
     */
    @ManyToOne
    private User seller;

    // === SISTEMA EVIDENZIAZIONE ===
    /**
     * Flag per indicare se il prodotto è in evidenza
     * True = prodotto promosso con visibilità premium
     * False = prodotto normale nel catalogo
     * 
     * Comportamento:
     * - Prodotti featured mostrati per primi
     * - Stile grafico distintivo
     * - Posizioni privilegiate in homepage
     * - Maggiore visibilità ricerche
     */
    private boolean isFeatured;

    /**
     * Data/ora di scadenza dell'evidenziazione
     * Null = evidenza permanente (generalmente non usato)
     * 
     * Funzionalità:
     * - Evidenziazione temporizzata
     * - Automatic downgrade dopo scadenza
     * - Controllo durata promozioni
     * - Sistema abbonamenti premium
     */
    private LocalDateTime featuredUntil;

    // === GALLERY IMMAGINI ===
    /**
     * Lista delle immagini associate al prodotto
     * Relazione One-to-Many con caricamento EAGER
     * 
     * Configurazione:
     * - EAGER fetch: immagini sempre caricate con il prodotto
     * - CASCADE ALL: eliminazione prodotto elimina anche immagini
     * - mappedBy "product": relazione bidirezionale con Image
     * 
     * Utilizzi:
     * - Gallery prodotto nella pagina dettaglio
     * - Anteprima nei risultati ricerca
     * - Slider immagini
     * - Marketing visivo prodotto
     */
    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Image> images;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco del prodotto
     * @return ID del prodotto o null se non ancora persistito
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID del prodotto (gestito da JPA)
     * @param id Nuovo ID del prodotto
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce la descrizione del prodotto
     * @return Descrizione dettagliata o null se non fornita
     */
    public String getDescription() { 
        return description; 
    }
    
    /**
     * Imposta la descrizione del prodotto
     * @param description Testo descrittivo del veicolo
     */
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    /**
     * Restituisce il prezzo del prodotto
     * @return Prezzo in BigDecimal per precisione monetaria
     */
    public BigDecimal getPrice() { 
        return price; 
    }
    
    /**
     * Imposta il prezzo del prodotto
     * @param price Prezzo del veicolo (required, deve essere positivo)
     */
    public void setPrice(BigDecimal price) { 
        this.price = price; 
    }
    
    /**
     * Restituisce la categoria del prodotto
     * @return Categoria di classificazione
     */
    public String getCategory() { 
        return category; 
    }
    
    /**
     * Imposta la categoria del prodotto
     * @param category Categoria per classificazione (max 100 caratteri)
     */
    public void setCategory(String category) { 
        this.category = category; 
    }
    
    /**
     * Restituisce la marca del veicolo
     * @return Brand del prodotto
     */
    public String getBrand() { 
        return brand; 
    }
    
    /**
     * Imposta la marca del veicolo
     * @param brand Marca del veicolo (max 100 caratteri)
     */
    public void setBrand(String brand) { 
        this.brand = brand; 
    }
    
    /**
     * Restituisce il modello del veicolo
     * @return Modello specifico del prodotto
     */
    public String getModel() { 
        return model; 
    }
    
    /**
     * Imposta il modello del veicolo
     * @param model Modello del veicolo (max 100 caratteri)
     */
    public void setModel(String model) { 
        this.model = model; 
    }
    
    /**
     * Restituisce il chilometraggio del veicolo
     * @return Km percorsi o null per veicoli nuovi
     */
    public Integer getMileage() { 
        return mileage; 
    }
    
    /**
     * Imposta il chilometraggio del veicolo
     * @param mileage Chilometri percorsi (può essere null per auto nuove)
     */
    public void setMileage(Integer mileage) { 
        this.mileage = mileage; 
    }
    
    /**
     * Restituisce l'anno del veicolo
     * @return Anno di immatricolazione
     */
    public Integer getYear() { 
        return year; 
    }
    
    /**
     * Imposta l'anno del veicolo
     * @param year Anno di immatricolazione
     */
    public void setYear(Integer year) { 
        this.year = year; 
    }
    
    /**
     * Restituisce il tipo di alimentazione
     * @return Carburante utilizzato dal veicolo
     */
    public String getFuelType() { 
        return fuelType; 
    }
    
    /**
     * Imposta il tipo di alimentazione
     * @param fuelType Tipo di carburante (max 50 caratteri)
     */
    public void setFuelType(String fuelType) { 
        this.fuelType = fuelType; 
    }
    
    /**
     * Restituisce il tipo di trasmissione
     * @return Tipo di cambio del veicolo
     */
    public String getTransmission() { 
        return transmission; 
    }
    
    /**
     * Imposta il tipo di trasmissione
     * @param transmission Tipo di cambio (max 50 caratteri)
     */
    public void setTransmission(String transmission) { 
        this.transmission = transmission; 
    }
    
    /**
     * Restituisce il tipo di venditore
     * @return Categoria del venditore (Concessionario, Privato, etc.)
     */
    public String getSellerType() { 
        return sellerType; 
    }
    
    /**
     * Imposta il tipo di venditore
     * @param sellerType Categoria venditore (max 50 caratteri)
     */
    public void setSellerType(String sellerType) { 
        this.sellerType = sellerType; 
    }
    
    /**
     * Restituisce il venditore del prodotto
     * @return Utente che vende il prodotto
     */
    public User getSeller() { 
        return seller; 
    }
    
    /**
     * Imposta il venditore del prodotto
     * @param seller Utente venditore (required)
     */
    public void setSeller(User seller) { 
        this.seller = seller; 
    }
    
    /**
     * Verifica se il prodotto è in evidenza
     * @return True se il prodotto è featured, false altrimenti
     */
    public boolean isFeatured() { 
        return isFeatured; 
    }
    
    /**
     * Imposta lo stato di evidenziazione del prodotto
     * @param isFeatured True per mettere in evidenza il prodotto
     */
    public void setIsFeatured(boolean isFeatured) { 
        this.isFeatured = isFeatured; 
    }
    
    /**
     * Restituisce la data di scadenza dell'evidenziazione
     * @return Timestamp di scadenza o null se permanente
     */
    public LocalDateTime getFeaturedUntil() { 
        return featuredUntil; 
    }
    
    /**
     * Imposta la data di scadenza dell'evidenziazione
     * @param featuredUntil Timestamp di scadenza evidenza
     */
    public void setFeaturedUntil(LocalDateTime featuredUntil) { 
        this.featuredUntil = featuredUntil; 
    }
    
    /**
     * Verifica se l'evidenziazione è attualmente attiva
     * Controlla sia il flag featured che la data di scadenza
     * 
     * @return True se il prodotto è effettivamente in evidenza al momento corrente
     * 
     * Logica:
     * - Deve essere isFeatured = true
     * - E featuredUntil deve essere null (permanente) O nel futuro
     */
    public boolean isFeaturedActive() {
        return isFeatured && (featuredUntil == null || LocalDateTime.now().isBefore(featuredUntil));
    }
    
    /**
     * Restituisce la lista delle immagini del prodotto
     * @return Lista immagini associate (può essere vuota ma non null)
     */
    public List<Image> getImages() { 
        return images; 
    }
    
    /**
     * Imposta la lista delle immagini del prodotto
     * @param images Nuova lista di immagini per il prodotto
     */
    public void setImages(List<Image> images) { 
        this.images = images; 
    }
}