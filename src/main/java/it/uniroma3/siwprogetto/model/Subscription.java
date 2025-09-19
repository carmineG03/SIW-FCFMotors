package it.uniroma3.siwprogetto.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entità JPA per definire i piani di abbonamento disponibili nel sistema FCF Motors
 * Rappresenta i diversi livelli di servizio che gli utenti possono sottoscrivere
 * 
 * Caratteristiche:
 * - Definizione prezzi e durata abbonamenti
 * - Sistema di sconti temporanei
 * - Limiti servizi per piano (auto in evidenza)
 * - Gestione catalogo abbonamenti
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Entity
public class Subscription {
    
    // === IDENTIFICATORE PRIMARIO ===
    /**
     * Chiave primaria auto-generata per identificare univocamente ogni piano abbonamento
     * Utilizzata per associazioni con UserSubscription e User
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // === INFORMAZIONI BASE ABBONAMENTO ===
    
    /**
     * Nome commerciale del piano abbonamento
     * Esempi: "Basic", "Premium", "Pro", "Enterprise"
     * Utilizzato per:
     * - Visualizzazione nelle interfacce utente
     * - Marketing e comunicazioni
     * - Identificazione rapida del piano
     */
    private String name;
    
    /**
     * Descrizione dettagliata del piano abbonamento
     * Include elenco benefici, caratteristiche, limitazioni
     * Utilizzato per:
     * - Pagine pricing e confronto piani
     * - Materiale marketing
     * - Documentazione contrattuale
     * - Supporto clienti
     */
    private String description;
    
    /**
     * Prezzo del piano abbonamento in formato decimale
     * Rappresenta il costo per la durata specificata in durationDays
     * 
     * Considerazioni:
     * - Prezzo base prima di eventuali sconti
     * - In valuta sistema (tipicamente EUR per FCF Motors)
     * - Per periodo completo (non pro-rata)
     */
    private double price;
    
    /**
     * Durata del piano abbonamento espressa in giorni
     * Definisce per quanto tempo l'abbonamento rimane valido
     * 
     * Valori tipici:
     * - 30 giorni (mensile)
     * - 90 giorni (trimestrale)
     * - 365 giorni (annuale)
     * 
     * Utilizzato per:
     * - Calcolo data scadenza
     * - Pricing pro-rata
     * - Confronto convenienza piani
     */
    private int durationDays;

    // === SISTEMA SCONTI E PROMOZIONI ===
    
    /**
     * Percentuale di sconto applicabile al piano
     * Valore numerico che rappresenta lo sconto percentuale
     * 
     * Esempi:
     * - 10.0 = sconto del 10%
     * - 25.5 = sconto del 25,5%
     * - null = nessuno sconto attivo
     * 
     * Calcolo prezzo scontato:
     * prezzoFinale = price * (100 - discount) / 100
     */
    private Double discount;
    
    /**
     * Data di scadenza dell'offerta sconto
     * Dopo questa data lo sconto non è più valido
     * 
     * Controlli:
     * - Se LocalDate.now() > discountExpiry → sconto scaduto
     * - null = sconto senza scadenza
     * 
     * Utilizzi:
     * - Campagne promozionali limitate nel tempo
     * - Offerte stagionali
     * - Promozioni di lancio
     * - Black Friday, saldi, etc.
     */
    private LocalDate discountExpiry;

    // === LIMITI E BENEFICI PIANO ===
    
    /**
     * Numero massimo di automobili che l'utente può mettere in evidenza
     * Definisce uno dei benefici principali dell'abbonamento
     * 
     * Valori tipici per piano:
     * - Piano Basic: 1-2 auto in evidenza
     * - Piano Premium: 5-10 auto in evidenza  
     * - Piano Pro: 20+ auto in evidenza
     * - null o 0: nessuna auto in evidenza consentita
     * 
     * Controlli:
     * - Sistema verifica questo limite prima di evidenziare prodotti
     * - Utenti non possono superare il limite del loro piano
     * - Downgrade piano può richiedere rimozione evidenze eccedenti
     */
    private Integer maxFeaturedCars;

    // === COSTRUTTORI ===
    
    /**
     * Costruttore vuoto richiesto da JPA
     * Utilizzato dall'ORM per istanziare entità dal database
     */
    public Subscription() {}

    // === GETTERS AND SETTERS ===
    
    /**
     * Restituisce l'ID univoco del piano abbonamento
     * @return ID del piano abbonamento
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Imposta l'ID del piano abbonamento (gestito da JPA)
     * @param id Nuovo ID del piano
     */
    public void setId(Long id) { 
        this.id = id; 
    }
    
    /**
     * Restituisce il nome commerciale del piano
     * @return Nome del piano abbonamento
     */
    public String getName() { 
        return name; 
    }
    
    /**
     * Imposta il nome commerciale del piano
     * @param name Nome del piano (required, should be unique)
     */
    public void setName(String name) { 
        this.name = name; 
    }
    
    /**
     * Restituisce la descrizione dettagliata del piano
     * @return Descrizione completa benefici e caratteristiche
     */
    public String getDescription() { 
        return description; 
    }
    
    /**
     * Imposta la descrizione dettagliata del piano
     * @param description Descrizione marketing del piano
     */
    public void setDescription(String description) { 
        this.description = description; 
    }
    
    /**
     * Restituisce il prezzo base del piano
     * @return Prezzo in formato decimale
     */
    public double getPrice() { 
        return price; 
    }
    
    /**
     * Imposta il prezzo base del piano
     * @param price Prezzo del piano (must be positive)
     */
    public void setPrice(double price) { 
        this.price = price; 
    }
    
    /**
     * Restituisce la durata del piano in giorni
     * @return Numero giorni di validità abbonamento
     */
    public int getDurationDays() { 
        return durationDays; 
    }
    
    /**
     * Imposta la durata del piano in giorni
     * @param durationDays Giorni di validità (must be positive)
     */
    public void setDurationDays(int durationDays) { 
        this.durationDays = durationDays; 
    }
    
    /**
     * Restituisce la percentuale di sconto attiva
     * @return Sconto percentuale o null se nessuno sconto
     */
    public Double getDiscount() { 
        return discount; 
    }
    
    /**
     * Imposta la percentuale di sconto
     * @param discount Sconto percentuale (0-100) o null per nessuno sconto
     */
    public void setDiscount(Double discount) { 
        this.discount = discount; 
    }
    
    /**
     * Restituisce la data di scadenza dello sconto
     * @return Data fine validità sconto o null se senza scadenza
     */
    public LocalDate getDiscountExpiry() { 
        return discountExpiry; 
    }
    
    /**
     * Imposta la data di scadenza dello sconto
     * @param discountExpiry Data fine validità sconto promozionale
     */
    public void setDiscountExpiry(LocalDate discountExpiry) { 
        this.discountExpiry = discountExpiry; 
    }
    
    /**
     * Restituisce il limite massimo di auto in evidenza
     * @return Numero massimo auto evidenziabili o null se illimitato
     */
    public Integer getMaxFeaturedCars() { 
        return maxFeaturedCars; 
    }
    
    /**
     * Imposta il limite massimo di auto in evidenza
     * @param maxFeaturedCars Numero massimo (null = illimitato, 0 = nessuna)
     */
    public void setMaxFeaturedCars(Integer maxFeaturedCars) { 
        this.maxFeaturedCars = maxFeaturedCars; 
    }
}