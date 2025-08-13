package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA per gestione immagini del sistema FCF Motors
 * Interfaccia semplificata che eredita tutte le operazioni CRUD standard
 * 
 * Responsabilità:
 * - Storage e retrieval immagini binarie
 * - Gestione metadati immagini (contentType, associazioni)
 * - Operazioni batch per gallery multiple
 * 
 * Caratteristiche implementazione:
 * - Storage BLOB diretto in database
 * - Supporto immagini dealer e prodotti
 * - Gestione automatica cascade dalle entità parent
 * 
 * Operazioni ereditate da JpaRepository:
 * - save(Image): Salvataggio singola immagine
 * - saveAll(List<Image>): Salvataggio batch per gallery
 * - findById(Long): Recupero per serving endpoint /images/{id}
 * - findAll(): Lista completa (uso limitato per performance)
 * - deleteById(Long): Eliminazione controllata
 * - delete(Image): Eliminazione tramite entity
 * 
 * Pattern utilizzati:
 * - Repository pattern per astrazione persistence
 * - Template method pattern da JpaRepository
 * - Proxy pattern per lazy loading (se configurato)
 * 
 * Performance considerations:
 * - Immagini BLOB possono impattare performance query
 * - Considerare paginazione per liste extensive
 * - Cache di primo livello Hibernate attivo
 * - Possibile implementazione cache Redis per serving
 **/
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    /*
     * Interfaccia intenzionalmente minimale
     * 
     * Le operazioni CRUD standard di JpaRepository sono sufficienti
     * per la maggior parte dei casi d'uso:
     * 
     * 1. Salvataggio immagini da form upload
     * 2. Serving immagini tramite endpoint REST
     * 3. Eliminazione automatica via cascade da Product/Dealer
     * 4. Conteggio immagini per validazioni limiti
     * 
     * Query personalizzate possono essere aggiunte se necessario
     * senza modificare la logica applicativa esistente
     */
}