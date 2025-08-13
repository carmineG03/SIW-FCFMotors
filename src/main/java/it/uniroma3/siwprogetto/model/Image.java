package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;

/**
 * Entità JPA per memorizzare immagini binarie nel database
 * Gestisce immagini associate a concessionari e prodotti del sistema FCF Motors
 * 
 * Caratteristiche:
 * - Storage binario diretto nel database (strategia BLOB)
 * - Supporto per diversi tipi di contenuto (JPEG, PNG, GIF)
 * - Associazione flessibile con dealer o prodotti
 * - Metadati per tipo MIME e gestione cache
 */
@Entity
public class Image {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni immagine
     * Utilizzata come ID per endpoint di serving delle immagini (/images/{id})
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === DATI BINARI IMMAGINE ===
    /**
     * Dati binari dell'immagine memorizzati come BLOB nel database
     * Annotazione @Lob indica Large Object per supportare file di grandi dimensioni
     * 
     * Vantaggi storage in DB:
     * - Transazionalità garantita
     * - Backup automatico con il database
     * - Controllo accessi integrato
     * - Nessuna gestione filesystem separata
     * 
     * Svantaggi:
     * - Aumento dimensioni database
     * - Possibile impatto performance per immagini molto grandi
     * - Difficoltà caching lato server web
     */
    @Lob
    private byte[] data;

    // === METADATI IMMAGINE ===
    /**
     * Tipo MIME dell'immagine per corretta gestione HTTP
     * Esempi:
     * - "image/jpeg" per file JPG/JPEG
     * - "image/png" per file PNG
     * - "image/gif" per file GIF animate
     * - "image/webp" per formato WebP moderno
     * 
     * Utilizzi:
     * - Header Content-Type nelle response HTTP
     * - Validazione formati supportati
     * - Ottimizzazione rendering browser
     * - Decisioni cache client-side
     */
    private String contentType;

    // === RELAZIONI OPZIONALI ===
    /**
     * Associazione opzionale con un concessionario
     * Relazione Many-to-One: un dealer può avere più immagini
     * Nullable: l'immagine può non essere associata a nessun dealer
     * 
     * Mutually exclusive con product:
     * - Se dealer != null → product deve essere null (immagine dealer)
     * - Se dealer == null → product può essere != null (immagine prodotto)
     * 
     * Utilizzi:
     * - Gallery concessionario
     * - Logo dealer
     * - Foto sede/showroom
     * - Immagini marketing dealer
     */
    @ManyToOne
    private Dealer dealer;

    /**
     * Associazione opzionale con un prodotto (automobile)
     * Relazione Many-to-One: un prodotto può avere più immagini
     * Nullable: l'immagine può non essere associata a nessun prodotto
     * 
     * Mutually exclusive con dealer:
     * - Se product != null → dealer deve essere null (immagine prodotto)
     * - Se product == null → dealer può essere != null (immagine dealer)
     * 
     * Utilizzi:
     * - Gallery prodotto
     * - Foto dettaglio automobile
     * - Immagini interni/esterni
     * - Documenti tecnici visuali
     */
    @ManyToOne
    private Product product;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco dell'immagine
     * @return ID dell'immagine o null se non ancora persistita
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID dell'immagine (gestito da JPA)
     * @param id Nuovo ID dell'immagine
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce i dati binari dell'immagine
     * @return Array di byte contenente l'immagine
     */
    public byte[] getData() { 
        return data; 
    }
    
    /**
     * Imposta i dati binari dell'immagine
     * @param data Array di byte dell'immagine (required)
     */
    public void setData(byte[] data) { 
        this.data = data; 
    }
    
    /**
     * Restituisce il tipo MIME dell'immagine
     * @return Content-Type per header HTTP (es. "image/jpeg")
     */
    public String getContentType() { 
        return contentType; 
    }
    
    /**
     * Imposta il tipo MIME dell'immagine
     * @param contentType Tipo MIME valido per immagini
     */
    public void setContentType(String contentType) { 
        this.contentType = contentType; 
    }
    
    /**
     * Restituisce il concessionario associato all'immagine
     * @return Dealer proprietario dell'immagine o null se è un'immagine prodotto
     */
    public Dealer getDealer() { 
        return dealer; 
    }
    
    /**
     * Imposta il concessionario associato all'immagine
     * @param dealer Concessionario proprietario (mutually exclusive con product)
     */
    public void setDealer(Dealer dealer) { 
        this.dealer = dealer; 
    }
    
    /**
     * Restituisce il prodotto associato all'immagine
     * @return Prodotto proprietario dell'immagine o null se è un'immagine dealer
     */
    public Product getProduct() { 
        return product; 
    }
    
    /**
     * Imposta il prodotto associato all'immagine
     * @param product Prodotto proprietario (mutually exclusive con dealer)
     */
    public void setProduct(Product product) { 
        this.product = product; 
    }
}