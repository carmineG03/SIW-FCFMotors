package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST Controller per la gestione e servizio delle immagini.
 * Fornisce endpoint per recuperare immagini dal database e servirle
 * con il content-type appropriato per la visualizzazione web.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@RestController
public class ImageController {

    /** Logger per tracciare le operazioni sulle immagini */
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    /** Content-Type di fallback per immagini sconosciute */
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";

    /** Dimensione massima immagine in byte (10MB) */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    // === DIPENDENZE INIETTATE ===

    /** Repository per l'accesso alle immagini */
    private final ImageRepository imageRepository;

    /**
     * Costruttore con dependency injection.
     * 
     * @param imageRepository Repository per operazioni sulle immagini
     */
    @Autowired
    public ImageController(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    // === ENDPOINT RECUPERO IMMAGINI ===

    /**
     * Recupera e serve un'immagine specifica tramite il suo ID.
     * L'immagine viene servita con il content-type originale e header appropriati
     * per la cache e visualizzazione ottimale nel browser.
     * 
     * @param id ID univoco dell'immagine da recuperare
     * @return ResponseEntity contenente i dati dell'immagine o 404 se non trovata
     */
    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Long id) {
        logger.debug("Richiesta immagine con ID: {}", id);

        try {
            // Validazione ID
            if (id == null || id <= 0) {
                logger.warn("ID immagine non valido: {}", id);
                return ResponseEntity.badRequest().build();
            }

            // Recupera l'immagine dal database
            Optional<Image> imageOptional = imageRepository.findById(id);
            
            if (imageOptional.isEmpty()) {
                logger.info("Immagine non trovata con ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            Image image = imageOptional.get();

            // Validazione dati immagine
            if (image.getData() == null || image.getData().length == 0) {
                logger.warn("Immagine con ID {} ha dati nulli o vuoti", id);
                return ResponseEntity.notFound().build();
            }

            // Verifica dimensione immagine per sicurezza
            if (image.getData().length > MAX_IMAGE_SIZE) {
                logger.error("Immagine con ID {} troppo grande: {} bytes", id, image.getData().length);
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            // Determina il content-type appropriato
            MediaType mediaType = determineMediaType(image.getContentType());

            // Costruisce la risposta con header appropriati
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(image.getData().length);
            
            // Header per cache del browser (24 ore)
            headers.setCacheControl("public, max-age=86400");
            
           	// Header per gestione download - SOLUZIONE SENZA MODIFICARE IMAGE
            String filename = generateFilename(image, id);
            if (filename != null && !filename.isEmpty()) {
                headers.setContentDispositionFormData("inline", filename);
            }

            logger.debug("Immagine servita con successo: ID={}, size={} bytes, type={}", 
                       id, image.getData().length, mediaType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(image.getData());

        } catch (Exception e) {
            logger.error("Errore durante il recupero dell'immagine con ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint alternativo per recuperare immagini con path più user-friendly.
     * Supporta lo stesso funzionamento di /images/{id}.
     * 
     * @param id ID univoco dell'immagine
     * @return ResponseEntity con l'immagine o errore appropriato
     */
    @GetMapping("/rest/api/images/{id}")
    public ResponseEntity<byte[]> getImageByIdApi(@PathVariable Long id) {
        logger.debug("Richiesta immagine via API REST con ID: {}", id);
        
        // Riutilizza la logica principale
        return getImageById(id);
    }

    // === METODI UTILITY PRIVATI ===

    /**
     * Determina il MediaType appropriato basandosi sul content-type dell'immagine.
     * Fornisce fallback sicuro per content-type non riconosciuti.
     * 
     * @param contentType Content-type originale dell'immagine
     * @return MediaType appropriato per la risposta HTTP
     */
    private MediaType determineMediaType(String contentType) {
        try {
            // Se content-type è nullo o vuoto, usa il default
            if (contentType == null || contentType.trim().isEmpty()) {
                logger.debug("Content-type nullo, usando default: {}", DEFAULT_IMAGE_CONTENT_TYPE);
                return MediaType.parseMediaType(DEFAULT_IMAGE_CONTENT_TYPE);
            }

            // Normalizza il content-type
            String normalizedType = contentType.toLowerCase().trim();

            // Verifica che sia effettivamente un'immagine
            if (!normalizedType.startsWith("image/")) {
                logger.warn("Content-type non valido per immagine: {}, usando default", contentType);
                return MediaType.parseMediaType(DEFAULT_IMAGE_CONTENT_TYPE);
            }

            // Mappa content-type comuni a MediaType sicuri
            switch (normalizedType) {
                case "image/jpeg":
                case "image/jpg":
                    return MediaType.IMAGE_JPEG;
                    
                case "image/png":
                    return MediaType.IMAGE_PNG;
                    
                case "image/gif":
                    return MediaType.IMAGE_GIF;
                    
                case "image/webp":
                case "image/bmp":
                case "image/tiff":
                case "image/svg+xml":
                    // Per questi tipi, usa il parsing diretto se supportato
                    try {
                        return MediaType.parseMediaType(normalizedType);
                    } catch (Exception e) {
                        logger.warn("Content-type {} non supportato, usando default", normalizedType);
                        return MediaType.parseMediaType(DEFAULT_IMAGE_CONTENT_TYPE);
                    }
                    
                default:
                    logger.debug("Content-type sconosciuto: {}, usando default", normalizedType);
                    return MediaType.parseMediaType(DEFAULT_IMAGE_CONTENT_TYPE);
            }

        } catch (Exception e) {
            logger.warn("Errore nel parsing del content-type '{}', usando default: {}", 
                       contentType, DEFAULT_IMAGE_CONTENT_TYPE, e);
            return MediaType.parseMediaType(DEFAULT_IMAGE_CONTENT_TYPE);
        }
    }

   /**
     * Genera un nome file appropriato per l'immagine.
     * Utilizza i dati disponibili nel model Image esistente senza modifiche.
     * 
     * @param image Oggetto Image dal database
     * @param id ID dell'immagine per fallback
     * @return Nome file generato o null se non determinabile
     */
    private String generateFilename(Image image, Long id) {
        try {
            // Determina l'estensione basata sul content-type
            String extension = getExtensionFromContentType(image.getContentType());
            
            // Strategia di naming gerarchica:
            
            // 1. Se l'immagine ha un prodotto associato
            if (image.getProduct() != null) {
                String productName = sanitizeFilename(image.getProduct().getBrand());
                if (productName != null && !productName.isEmpty()) {
                    return "product_" + productName + "_" + id + "." + extension;
                }
            }
            
            // 2. Se l'immagine ha un dealer associato
            if (image.getDealer() != null) {
                String dealerName = sanitizeFilename(image.getDealer().getName());
                if (dealerName != null && !dealerName.isEmpty()) {
                    return "dealer_" + dealerName + "_" + id + "." + extension;
                }
            }
            
            // 3. Nome generico con ID e timestamp
            return "image_" + id + "." + extension;
            
        } catch (Exception e) {
            logger.warn("Errore nella generazione filename per immagine ID {}: {}", id, e.getMessage());
            return "image_" + id + ".jpg"; // Fallback sicuro
        }
    }
    
    /**
     * Ottiene l'estensione file dal content-type.
     * 
     * @param contentType Content-type dell'immagine
     * @return Estensione appropriata
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return "jpg"; // Default
        }
        
        String normalizedType = contentType.toLowerCase().trim();
        
        switch (normalizedType) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/bmp":
                return "bmp";
            case "image/tiff":
            case "image/tif":
                return "tiff";
            case "image/svg+xml":
                return "svg";
            default:
                return "img";
        }
    }
    
    /**
     * Sanitizza una stringa per renderla safe come nome file.
     * Rimuove caratteri speciali e limita la lunghezza.
     * 
     * @param input Stringa da sanitizzare
     * @return Stringa sanitizzata o null se input non valido
     */
    private String sanitizeFilename(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        // Rimuove caratteri non sicuri per filename
        String sanitized = input.trim()
                               .replaceAll("[^a-zA-Z0-9\\s\\-_]", "") // Solo lettere, numeri, spazi, trattini
                               .replaceAll("\\s+", "_") // Spazi diventano underscore
                               .toLowerCase();
        
        // Limita lunghezza per evitare problemi filesystem
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        // Rimuove underscore multipli
        sanitized = sanitized.replaceAll("_+", "_");
        
        // Rimuove underscore iniziali/finali
        sanitized = sanitized.replaceAll("^_|_$", "");
        
        return sanitized.isEmpty() ? null : sanitized;
    }
}