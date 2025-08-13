package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Image;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.QuoteRequest;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.ImageRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.QuoteRequestRepository;
import it.uniroma3.siwprogetto.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller per la gestione delle auto degli utenti privati
 * Gestisce l'aggiunta, modifica, eliminazione e visualizzazione delle auto
 * private
 * Gli utenti privati possono avere massimo 1 auto nel sistema
 */
@Controller
@RequestMapping("/private")
public class PrivateMaintenanceController {

    private static final Logger logger = LoggerFactory.getLogger(PrivateMaintenanceController.class);

    // === COSTANTI DI VALIDAZIONE ===
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2030;
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private QuoteRequestRepository quoteRequestRepository;

    /**
     * Pagina principale della gestione auto privata
     * Controlla se l'utente ha già un'auto:
     * - Se non ha auto → mostra form di aggiunta (add_car.html)
     * - Se ha già un'auto → mostra form di modifica (edit_car.html)
     * 
     * @param authentication Autenticazione utente corrente
     * @param model          Model per passare dati alla view
     * @return template appropriato basato sullo stato dell'utente
     */
    @GetMapping("/maintenance")
    @Transactional(readOnly = true)
    public String showMaintenancePage(Authentication authentication, Model model) {
        logger.info("Accesso pagina manutenzione privata per utente: {}", authentication.getName());

        try {
            // === VERIFICA UTENTE AUTENTICATO ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.warn("Utente non trovato: {}", authentication.getName());
                return "redirect:/login";
            }

            // === CONTROLLO AUTO ESISTENTI ===
            // Gli utenti privati possono avere massimo 1 auto
            List<Product> userProducts = productRepository.findBySellerId(user.getId());
            logger.debug("Trovate {} auto per l'utente {}", userProducts.size(), user.getUsername());

            if (userProducts.isEmpty()) {
                // === CASO: NESSUNA AUTO ESISTENTE ===
                // Mostra form per aggiungere la prima auto
                model.addAttribute("product", new Product());
                logger.debug("Nessuna auto trovata - reindirizzo a form aggiunta");
                return "add_car";

            } else {
                // === CASO: AUTO GIÀ ESISTENTE ===
                // Mostra form per modificare l'auto esistente
                Product existingProduct = userProducts.get(0);
                model.addAttribute("product", existingProduct);
                logger.debug("Auto esistente trovata - ID: {} - reindirizzo a form modifica",
                        existingProduct.getId());
                return "edit_car";
            }

        } catch (Exception e) {
            logger.error("Errore nel caricamento pagina manutenzione per {}: {}",
                    authentication.getName(), e.getMessage(), e);
            return "redirect:/login";
        }
    }

    /**
     * Aggiunge una nuova auto per l'utente privato
     * Valida tutti i campi obbligatori e processa le immagini caricate
     * Gli utenti privati possono avere solo 1 auto nel sistema
     * 
     * @param request        Richiesta HTTP contenente dati del form
     * @param authentication Autenticazione utente corrente
     * @return Redirect con messaggio di successo o errore
     */
    @PostMapping("/add")
    @Transactional
    public String addCar(HttpServletRequest request, Authentication authentication) {
        logger.info("=== INIZIO PROCESSO AGGIUNTA AUTO ===");
        logger.info("Utente: {}", authentication.getName());

        try {
            // === VERIFICA AUTENTICAZIONE ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.error("Utente non trovato durante aggiunta auto: {}", authentication.getName());
                return "redirect:/private/maintenance?error=user_not_found";
            }

            // === VERIFICA LIMITE AUTO (1 PER UTENTE PRIVATO) ===
            List<Product> existingProducts = productRepository.findBySellerId(user.getId());
            if (!existingProducts.isEmpty()) {
                logger.warn("Utente {} ha già un'auto - tentativo aggiunta rifiutato", user.getUsername());
                return "redirect:/private/maintenance?error=already_has_car";
            }

            // === ESTRAZIONE E VALIDAZIONE PARAMETRI ===
            FormParameters params = extractFormParameters(request);
            logger.info("Parametri ricevuti: brand={}, model={}, category={}, price={}, year={}, km={}",
                    params.brand, params.model, params.category, params.price, params.year, params.mileage);

            // Validazione campi obbligatori
            String validationError = validateMandatoryFields(params);
            if (validationError != null) {
                logger.warn("Validazione fallita: {}", validationError);
                return "redirect:/private/maintenance?error=" + validationError;
            }

            // Validazione valori numerici
            String numericValidationError = validateNumericFields(params);
            if (numericValidationError != null) {
                logger.warn("Validazione numerica fallita: {}", numericValidationError);
                return "redirect:/private/maintenance?error=" + numericValidationError;
            }

            // === GESTIONE IMMAGINI PRIMA DEL SALVATAGGIO ===
            List<Image> processedImages = processUploadedImages(request, null); // null perché il prodotto non esiste
                                                                                // ancora
            if (processedImages.isEmpty()) {
                logger.warn("Nessuna immagine valida caricata");
                return "redirect:/private/maintenance?error=no_valid_images";
            }

            // === CREAZIONE PRODOTTO CON IMMAGINI ===
            Product product = createProductFromParameters(params, user, processedImages);

            // Associa il prodotto alle immagini
            for (Image image : processedImages) {
                image.setProduct(product);
            }

            // === SALVATAGGIO ===
            Product savedProduct = productRepository.save(product);
            logger.info("Auto aggiunta con successo - ID: {}, Immagini: {}",
                    savedProduct.getId(), processedImages.size());

            return "redirect:/private/maintenance?success=car_added";

        } catch (Exception e) {
            logger.error("Errore durante aggiunta auto per {}: {}", authentication.getName(), e.getMessage(), e);
            return "redirect:/private/maintenance?error=add_failed";
        }
    }

    /**
     * Modifica l'auto esistente dell'utente privato
     * Aggiorna tutti i campi del prodotto e gestisce il caricamento di nuove
     * immagini
     * 
     * @param id             ID del prodotto da modificare
     * @param request        Richiesta HTTP con dati aggiornati
     * @param authentication Autenticazione utente corrente
     * @return Redirect con messaggio di successo o errore
     */
    @PostMapping("/edit/{id}")
    @Transactional
    public String editCar(@PathVariable Long id, HttpServletRequest request, Authentication authentication) {
        logger.info("Modifica auto ID: {} per utente: {}", id, authentication.getName());

        try {
            // === VERIFICA AUTENTICAZIONE E AUTORIZZAZIONE ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.error("Utente non trovato durante modifica: {}", authentication.getName());
                return "redirect:/private/maintenance?error=user_not_found";
            }

            // Verifica esistenza prodotto e proprietà
            Product existingProduct = productRepository.findById(id).orElse(null);
            if (existingProduct == null) {
                logger.warn("Prodotto non trovato per ID: {}", id);
                return "redirect:/private/maintenance?error=product_not_found";
            }

            if (!existingProduct.getSeller().getId().equals(user.getId())) {
                logger.warn("Utente {} non autorizzato a modificare prodotto {}", user.getUsername(), id);
                return "redirect:/private/maintenance?error=not_authorized";
            }

            // === AGGIORNAMENTO CAMPI PRODOTTO ===
            FormParameters params = extractFormParameters(request);
            updateProductFields(existingProduct, params);

            // === GESTIONE NUOVE IMMAGINI ===
            // Se l'utente carica nuove immagini, sostituiscono completamente quelle
            // esistenti
            if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest = (org.springframework.web.multipart.MultipartHttpServletRequest) request;

                List<MultipartFile> newImages = multipartRequest.getFiles("newImages");
                logger.debug("Trovate {} nuove immagini nella richiesta",
                        newImages != null ? newImages.size() : 0);

                if (hasValidImageFiles(newImages)) {
                    logger.info("Sostituzione immagini esistenti con {} nuove immagini", newImages.size());

                    // Elimina immagini esistenti dal database
                    deleteExistingImages(existingProduct);

                    // Processa e salva nuove immagini
                    List<Image> processedImages = processUploadedImages(request, existingProduct);
                    if (!processedImages.isEmpty()) {
                        existingProduct.setImages(processedImages);
                        logger.info("Impostate {} nuove immagini al prodotto", processedImages.size());
                    }
                }
            }

            // === SALVATAGGIO MODIFICHE ===
            Product savedProduct = productRepository.save(existingProduct);
            logger.info("Prodotto aggiornato con successo. Immagini totali: {}",
                    savedProduct.getImages() != null ? savedProduct.getImages().size() : 0);

            return "redirect:/private/maintenance?success=car_updated";

        } catch (Exception e) {
            logger.error("Errore durante modifica auto ID {}: {}", id, e.getMessage(), e);
            return "redirect:/private/maintenance?error=update_failed";
        }
    }

    /**
     * Elimina una singola immagine via AJAX
     * Endpoint REST per permettere eliminazione dinamica delle immagini
     * 
     * @param imageId        ID dell'immagine da eliminare
     * @param authentication Autenticazione utente corrente
     * @return ResponseEntity con esito operazione
     */
    @PostMapping("/images/delete/{imageId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> deleteImage(@PathVariable Long imageId, Authentication authentication) {
        logger.info("Richiesta eliminazione immagine ID: {} da utente: {}", imageId, authentication.getName());

        try {
            // === VERIFICA AUTENTICAZIONE ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.warn("Tentativo eliminazione immagine da utente non autenticato");
                return ResponseEntity.status(403).body("Non autorizzato");
            }

            // === VERIFICA PROPRIETÀ IMMAGINE ===
            Image image = imageRepository.findById(imageId).orElse(null);
            if (image == null) {
                logger.warn("Immagine non trovata per ID: {}", imageId);
                return ResponseEntity.status(404).body("Immagine non trovata");
            }

            if (!image.getProduct().getSeller().getId().equals(user.getId())) {
                logger.warn("Utente {} non autorizzato a eliminare immagine {}", user.getUsername(), imageId);
                return ResponseEntity.status(403).body("Non autorizzato");
            }

            // === ELIMINAZIONE IMMAGINE ===
            imageRepository.delete(image);
            logger.info("Immagine {} eliminata con successo", imageId);

            return ResponseEntity.ok("Immagine eliminata");

        } catch (Exception e) {
            logger.error("Errore eliminazione immagine {}: {}", imageId, e.getMessage(), e);
            return ResponseEntity.status(500).body("Errore interno");
        }
    }

    /**
     * Elimina completamente l'auto dell'utente privato
     * Rimuove anche tutte le immagini associate e i preventivi collegati
     * 
     * @param id             ID del prodotto da eliminare
     * @param authentication Autenticazione utente corrente
     * @return Redirect con messaggio di successo o errore
     */
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteCar(@PathVariable Long id, Authentication authentication) {
        logger.info("Eliminazione auto ID: {} per utente: {}", id, authentication.getName());

        try {
            // === VERIFICA AUTENTICAZIONE E AUTORIZZAZIONE ===
            User user = userService.findByUsername(authentication.getName());
            if (user == null) {
                logger.error("Utente non trovato durante eliminazione: {}", authentication.getName());
                return "redirect:/private/maintenance?error=user_not_found";
            }

            Product product = productRepository.findById(id).orElse(null);
            if (product == null) {
                logger.warn("Prodotto non trovato per eliminazione - ID: {}", id);
                return "redirect:/private/maintenance?error=product_not_found";
            }

            if (!product.getSeller().getId().equals(user.getId())) {
                logger.warn("Utente {} non autorizzato a eliminare prodotto {}", user.getUsername(), id);
                return "redirect:/private/maintenance?error=not_authorized";
            }

            // === ELIMINAZIONE DATI COLLEGATI ===
            // 1. Elimina tutti i preventivi associati
            List<QuoteRequest> quoteRequests = quoteRequestRepository.findByProductId(id);
            if (!quoteRequests.isEmpty()) {
                logger.info("Eliminazione {} preventivi associati al prodotto {}", quoteRequests.size(), id);
                quoteRequestRepository.deleteAll(quoteRequests);
            }

            // 2. Elimina tutte le immagini associate
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                int imageCount = product.getImages().size();
                imageRepository.deleteAll(product.getImages());
                logger.info("Eliminate {} immagini del prodotto {}", imageCount, id);
            }

            // 3. Elimina il prodotto principale
            productRepository.delete(product);
            logger.info("Prodotto {} eliminato con successo", id);

            return "redirect:/private/maintenance?success=car_deleted";

        } catch (Exception e) {
            logger.error("Errore eliminazione auto ID {}: {}", id, e.getMessage(), e);
            return "redirect:/private/maintenance?error=delete_failed";
        }
    }

    // === METODI HELPER PER PULIZIA CODICE ===

    /**
     * Classe helper per organizzare i parametri del form
     */
    private static class FormParameters {
        String brand, model, category, description, fuelType, transmission;
        String priceStr, yearStr, mileageStr;
        BigDecimal price;
        Integer year, mileage;
    }

    /**
     * Estrae e organizza tutti i parametri dal form
     */
    private FormParameters extractFormParameters(HttpServletRequest request) {
        FormParameters params = new FormParameters();
        params.brand = request.getParameter("brand");
        params.model = request.getParameter("model");
        params.category = request.getParameter("category");
        params.priceStr = request.getParameter("price");
        params.yearStr = request.getParameter("year");
        params.mileageStr = request.getParameter("mileage");
        params.fuelType = request.getParameter("fuelType");
        params.transmission = request.getParameter("transmission");
        params.description = request.getParameter("description");
        return params;
    }

    /**
     * Valida tutti i campi obbligatori del form
     */
    private String validateMandatoryFields(FormParameters params) {
        if (isEmpty(params.brand))
            return "missing_brand";
        if (isEmpty(params.model))
            return "missing_model";
        if (isEmpty(params.category))
            return "missing_category";
        if (isEmpty(params.fuelType))
            return "missing_fuel_type";
        if (isEmpty(params.transmission))
            return "missing_transmission";
        if (isEmpty(params.description))
            return "missing_description";
        return null;
    }

    /**
     * Valida e converte i campi numerici
     */
    private String validateNumericFields(FormParameters params) {
        // Validazione prezzo
        try {
            params.price = new BigDecimal(params.priceStr);
            if (params.price.compareTo(BigDecimal.ZERO) <= 0) {
                return "invalid_price";
            }
        } catch (Exception e) {
            return "invalid_price";
        }

        // Validazione anno
        try {
            params.year = Integer.parseInt(params.yearStr);
            if (params.year < MIN_YEAR || params.year > MAX_YEAR) {
                return "invalid_year";
            }
        } catch (Exception e) {
            return "invalid_year";
        }

        // Validazione chilometraggio
        try {
            params.mileage = Integer.parseInt(params.mileageStr);
            if (params.mileage < 0) {
                return "invalid_mileage";
            }
        } catch (Exception e) {
            return "invalid_mileage";
        }

        return null;
    }

    /**
     * Processa le immagini caricate dall'utente
     */
    private List<Image> processUploadedImages(HttpServletRequest request, Product existingProduct) {
        List<Image> images = new ArrayList<>();

        if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
            org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest = (org.springframework.web.multipart.MultipartHttpServletRequest) request;

            String fieldName = existingProduct != null ? "newImages" : "images";
            List<MultipartFile> files = multipartRequest.getFiles(fieldName);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String contentType = file.getContentType();
                        if (isValidImageType(contentType) && isValidImageSize(file)) {
                            Image img = new Image();
                            img.setData(file.getBytes());
                            img.setContentType(contentType);
                            // NON impostare il prodotto qui per le nuove creazioni
                            // sarà impostato nel controller dopo il salvataggio del prodotto
                            if (existingProduct != null) {
                                img.setProduct(existingProduct);
                            }
                            images.add(img);

                            logger.debug("Immagine processata: {} ({} bytes)",
                                    file.getOriginalFilename(), file.getSize());
                        }
                    } catch (IOException e) {
                        logger.error("Errore processing immagine {}: {}",
                                file.getOriginalFilename(), e.getMessage());
                    }
                }
            }
        }

        logger.info("Processate {} immagini valide", images.size());
        return images;
    }

    /**
     * Crea un nuovo prodotto dai parametri validati
     */
    private Product createProductFromParameters(FormParameters params, User user, List<Image> images) {
        Product product = new Product();
        product.setBrand(params.brand.trim());
        product.setModel(params.model.trim());
        product.setCategory(params.category.trim());
        product.setPrice(params.price);
        product.setYear(params.year);
        product.setMileage(params.mileage);
        product.setFuelType(params.fuelType.trim());
        product.setTransmission(params.transmission.trim());
        product.setDescription(params.description.trim());
        product.setSeller(user);
        product.setSellerType("PRIVATE");
        product.setImages(images);
        return product;
    }

    /**
     * Aggiorna i campi di un prodotto esistente
     */
    private void updateProductFields(Product product, FormParameters params) {
        if (!isEmpty(params.brand))
            product.setBrand(params.brand.trim());
        if (!isEmpty(params.model))
            product.setModel(params.model.trim());
        if (!isEmpty(params.category))
            product.setCategory(params.category.trim());
        if (!isEmpty(params.fuelType))
            product.setFuelType(params.fuelType.trim());
        if (!isEmpty(params.transmission))
            product.setTransmission(params.transmission.trim());
        if (!isEmpty(params.description))
            product.setDescription(params.description.trim());

        // Aggiorna campi numerici se validi
        if (params.priceStr != null) {
            try {
                BigDecimal price = new BigDecimal(params.priceStr);
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    product.setPrice(price);
                }
            } catch (Exception e) {
                logger.debug("Prezzo non valido, mantengo quello esistente: {}", params.priceStr);
            }
        }

        if (params.yearStr != null) {
            try {
                Integer year = Integer.parseInt(params.yearStr);
                if (year >= MIN_YEAR && year <= MAX_YEAR) {
                    product.setYear(year);
                }
            } catch (Exception e) {
                logger.debug("Anno non valido, mantengo quello esistente: {}", params.yearStr);
            }
        }

        if (params.mileageStr != null) {
            try {
                Integer mileage = Integer.parseInt(params.mileageStr);
                if (mileage >= 0) {
                    product.setMileage(mileage);
                }
            } catch (Exception e) {
                logger.debug("Chilometraggio non valido, mantengo quello esistente: {}", params.mileageStr);
            }
        }
    }

    /**
     * Verifica se ci sono file immagine validi nella richiesta
     */
    private boolean hasValidImageFiles(List<MultipartFile> files) {
        return files != null && files.stream().anyMatch(file -> !file.isEmpty());
    }

    /**
     * Elimina tutte le immagini esistenti di un prodotto
     */
    private void deleteExistingImages(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            int count = product.getImages().size();
            imageRepository.deleteAll(product.getImages());
            product.getImages().clear();
            logger.info("Eliminate {} immagini esistenti", count);
        }
    }

    /**
     * Utility per verificare se una stringa è vuota
     */
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Verifica se il tipo di contenuto è un'immagine valida
     */
    private boolean isValidImageType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Verifica se la dimensione dell'immagine è accettabile
     */
    private boolean isValidImageSize(MultipartFile file) {
        return file.getSize() <= MAX_IMAGE_SIZE;
    }
}