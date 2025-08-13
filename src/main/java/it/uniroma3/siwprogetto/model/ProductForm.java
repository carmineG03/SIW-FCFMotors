package it.uniroma3.siwprogetto.model;

import java.math.BigDecimal;

/**
 * Classe DTO (Data Transfer Object) per gestire form di inserimento/modifica prodotti
 * Non è un'entità JPA - serve solo per trasferimento dati tra frontend e backend
 * 
 * Utilizzi:
 * - Binding dati form HTML
 * - Validazione input utente
 * - Trasformazione dati per entità Product
 * - Interfaccia semplificata per form
 * 
 * Differenze da Product:
 * - Campi ridotti ai soli essenziali per form
 * - Nessuna relazione JPA
 * - Focus su usabilità form frontend
 */
public class ProductForm {
    
    // === IDENTIFICATORE ===
    /**
     * ID del prodotto per operazioni di modifica
     * Null per nuovi prodotti, valorizzato per modifiche esistenti
     * 
     * Utilizzi:
     * - Distinzione create vs update operation
     * - Binding con prodotto esistente nel database
     * - Validation che prodotto esista per modifiche
     */
    private Long id;
    
    // === INFORMAZIONI COMMERCIALI ===
    
    /**
     * Prezzo del prodotto in formato BigDecimal
     * Campo principale per definire valore commerciale
     * 
     * Validazioni tipiche:
     * - Deve essere positivo
     * - Massimo 2 decimali per valute
     * - Range min/max sensato per auto
     * 
     * Utilizzi:
     * - Calcolo totali carrello
     * - Filtri ricerca per prezzo
     * - Ordinamento prodotti
     * - Reportistica vendite
     */
    private BigDecimal price;
    
    /**
     * Descrizione dettagliata del prodotto
     * Campo di testo libero per informazioni complete
     * 
     * Contenuto tipico:
     * - Caratteristiche tecniche
     * - Condizioni del veicolo
     * - Storia e manutenzioni
     * - Accessori inclusi
     * - Note venditore
     * 
     * Utilizzi:
     * - Pagina dettaglio prodotto
     * - SEO content
     * - Supporto decisione acquisto
     */
    private String description;
    
    /**
     * URL immagine principale del prodotto
     * Utilizzato per anteprima e preview nel form
     * 
     * Note implementative:
     * - In FCF Motors le immagini sono gestite come entità Image separate
     * - Questo campo serve per compatibilità form legacy
     * - Potrebbe essere deprecato in favore di upload multipli
     * 
     * Utilizzi:
     * - Preview nel form editing
     * - Fallback se immagini non caricate
     * - Integrazione con servizi immagini esterni
     */
    private String imageUrl;

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID del prodotto
     * @return ID prodotto per modifica o null per nuovo prodotto
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'ID del prodotto
     * @param id ID prodotto esistente per operazioni update
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce il prezzo del prodotto
     * @return Prezzo in formato BigDecimal per precisione monetaria
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Imposta il prezzo del prodotto
     * @param price Prezzo prodotto (must be positive)
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * Restituisce la descrizione del prodotto
     * @return Descrizione dettagliata inserita nel form
     */
    public String getDescription() {
        return description;
    }

    /**
     * Imposta la descrizione del prodotto
     * @param description Testo descrittivo del prodotto
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Restituisce l'URL dell'immagine principale
     * @return URL immagine per preview form
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Imposta l'URL dell'immagine principale
     * @param imageUrl URL valido per immagine prodotto
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}