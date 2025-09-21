package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.Subscription;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.AdminService;
import it.uniroma3.siwprogetto.service.DealerService;
import it.uniroma3.siwprogetto.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Controller per la gestione dell'area amministrativa.
 * Fornisce funzionalità CRUD per prodotti, dealer, utenti e abbonamenti.
 * Accessibile solo agli utenti con ruolo ADMIN.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    /** Logger per tracciare le operazioni amministrative */
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // === DIPENDENZE INIETTATE ===

    /** Servizio per operazioni amministrative generali */
    private final AdminService adminService;

    /** Servizio per la gestione degli utenti */
    private final UserService userService;

    /** Servizio per la gestione dei dealer */
    private final DealerService dealerService;

    /**
     * Costruttore con dependency injection.
     */
    @Autowired
    public AdminController(AdminService adminService, 
                          UserService userService, 
                          DealerService dealerService) {
        this.adminService = adminService;
        this.userService = userService;
        this.dealerService = dealerService;
    }

    // === PAGINA PRINCIPALE MANUTENZIONE ===

    /**
     * Mostra la pagina principale di manutenzione con tutti i dati del sistema.
     * Include prodotti, dealer, utenti e abbonamenti per la gestione completa.
     * 
     * @param model Modello per la vista
     * @param principal Amministratore autenticato
     * @return Nome della vista admin-maintenance
     */
    @GetMapping("/maintenance")
    public String showMaintenancePage(Model model, Principal principal) {
        logger.info("Accesso alla pagina di manutenzione admin da: {}", 
                   principal != null ? principal.getName() : "utente non autenticato");

        try {
            // Carica l'utente amministratore
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Amministratore non trovato: {}", principal.getName());
                model.addAttribute("error", "Utente amministratore non trovato");
                return "error";
            }

            // Carica tutti i dati del sistema per la gestione
            model.addAttribute("user", user);
            model.addAttribute("products", adminService.findAllProducts());
            model.addAttribute("dealers", adminService.findAllDealers());
            model.addAttribute("users", adminService.findAllUsers());
            model.addAttribute("subscriptions", adminService.findAllSubscriptions());

            logger.info("Pagina manutenzione caricata con successo per admin: {}", user.getUsername());

            return "admin-maintenance";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della pagina manutenzione per admin: {}", 
                        principal.getName(), e);
            model.addAttribute("error", "Errore durante il caricamento della pagina di manutenzione");
            return "error";
        }
    }

    // === GESTIONE PRODOTTI ===

    /**
     * Mostra il form di modifica per un prodotto specifico.
     * 
     * @param id ID del prodotto da modificare
     * @param model Modello per la vista
     * @return Nome della vista admin-product-edit
     */
    @GetMapping("/product/{id}/edit")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        logger.info("Richiesta form modifica prodotto: ID={}", id);

        try {
            Product product = adminService.findProductById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Prodotto non trovato con ID: " + id));

            model.addAttribute("product", product);
            
            logger.info("Form modifica prodotto caricato con successo: ID={}, Nome={}", 
                       id, product.getBrand());

            return "admin-product-edit";

        } catch (IllegalArgumentException e) {
            logger.error("Prodotto non trovato per modifica: ID={}", id);
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/maintenance?error=Prodotto non trovato";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento form modifica prodotto: ID={}", id, e);
            model.addAttribute("error", "Errore durante il caricamento del form di modifica");
            return "redirect:/admin/maintenance?error=Errore caricamento form";
        }
    }

    /**
     * Aggiorna un prodotto esistente con i dati modificati.
     * 
     * @param id ID del prodotto da aggiornare
     * @param updatedProduct Dati aggiornati del prodotto
     * @param result Risultato della validazione
     * @param model Modello per la vista
     * @return Redirect alla pagina manutenzione o form in caso di errore
     */
    @PostMapping("/product/{id}/update")
    public String updateProduct(@PathVariable Long id, 
                               @ModelAttribute Product updatedProduct, 
                               BindingResult result, 
                               Model model) {
        logger.info("Tentativo aggiornamento prodotto: ID={}", id);

        // Validazione dati del form
        if (result.hasErrors()) {
            logger.warn("Errori di validazione nell'aggiornamento prodotto: ID={}", id);
            model.addAttribute("errorMessage", "Errore nei dati del prodotto");
            model.addAttribute("product", updatedProduct);
            return "admin-product-edit";
        }

        try {
            // Aggiorna il prodotto
            adminService.updateProduct(id, updatedProduct);
            
            logger.info("Prodotto aggiornato con successo: ID={}", id);
            
            return "redirect:/admin/maintenance?success=Prodotto aggiornato con successo";

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione aggiornamento prodotto: ID={}", id, e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("product", updatedProduct);
            return "admin-product-edit";

        } catch (Exception e) {
            logger.error("Errore interno aggiornamento prodotto: ID={}", id, e);
            model.addAttribute("errorMessage", "Errore durante l'aggiornamento del prodotto");
            model.addAttribute("product", updatedProduct);
            return "admin-product-edit";
        }
    }

    /**
     * Elimina un prodotto dal sistema.
     * 
     * @param id ID del prodotto da eliminare
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/product/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentativo eliminazione prodotto: ID={}", id);

        try {
            adminService.deleteProduct(id);
            
            logger.info("Prodotto eliminato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Prodotto eliminato con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Prodotto non trovato per eliminazione: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Prodotto non trovato");

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante eliminazione prodotto: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Impossibile eliminare il prodotto: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Errore interno eliminazione prodotto: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante l'eliminazione del prodotto");
        }

        return "redirect:/admin/maintenance";
    }

    // === GESTIONE DEALER ===

    /**
     * Mostra il form di modifica per un dealer specifico.
     * 
     * @param id ID del dealer da modificare
     * @param model Modello per la vista
     * @return Nome della vista admin-dealer-edit
     */
    @GetMapping("/dealer/{id}/edit")
    public String showEditDealerForm(@PathVariable Long id, Model model) {
        logger.info("Richiesta form modifica dealer: ID={}", id);

        try {
            Dealer dealer = adminService.findDealerById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Concessionario non trovato con ID: " + id));

            model.addAttribute("dealer", dealer);
            
            logger.info("Form modifica dealer caricato con successo: ID={}, Nome={}", 
                       id, dealer.getName());

            return "admin-dealer-edit";

        } catch (IllegalArgumentException e) {
            logger.error("Dealer non trovato per modifica: ID={}", id);
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/maintenance?error=Concessionario non trovato";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento form modifica dealer: ID={}", id, e);
            model.addAttribute("error", "Errore durante il caricamento del form di modifica");
            return "redirect:/admin/maintenance?error=Errore caricamento form";
        }
    }

    /**
     * Aggiorna un dealer esistente con i dati modificati.
     * 
     * @param id ID del dealer da aggiornare
     * @param updatedDealer Dati aggiornati del dealer
     * @param result Risultato della validazione
     * @param model Modello per la vista
     * @return Redirect alla pagina manutenzione o form in caso di errore
     */
    @PostMapping("/dealer/{id}/update")
    public String updateDealer(@PathVariable Long id, 
                              @ModelAttribute Dealer updatedDealer, 
                              BindingResult result, 
                              Model model) {
        logger.info("Tentativo aggiornamento dealer: ID={}", id);

        // Validazione dati del form
        if (result.hasErrors()) {
            logger.warn("Errori di validazione nell'aggiornamento dealer: ID={}", id);
            model.addAttribute("errorMessage", "Errore nei dati del concessionario");
            model.addAttribute("dealer", updatedDealer);
            return "admin-dealer-edit";
        }

        try {
            // Aggiorna il dealer
            adminService.updateDealer(id, updatedDealer);
            
            logger.info("Dealer aggiornato con successo: ID={}", id);
            
            return "redirect:/admin/maintenance?success=Concessionario aggiornato con successo";

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione aggiornamento dealer: ID={}", id, e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("dealer", updatedDealer);
            return "admin-dealer-edit";

        } catch (Exception e) {
            logger.error("Errore interno aggiornamento dealer: ID={}", id, e);
            model.addAttribute("errorMessage", "Errore durante l'aggiornamento del concessionario");
            model.addAttribute("dealer", updatedDealer);
            return "admin-dealer-edit";
        }
    }

    /**
     * Elimina un dealer dal sistema.
     * Utilizza il DealerService per gestire le dipendenze e i vincoli.
     * 
     * @param id ID del dealer da eliminare
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/dealer/{id}/delete")
    public String deleteDealer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentativo eliminazione dealer: ID={}", id);

        try {
            dealerService.deleteDealer(id);
            
            logger.info("Dealer eliminato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Concessionario eliminato con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Dealer non valido per eliminazione: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante eliminazione dealer: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante l'eliminazione del concessionario: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Errore imprevisto eliminazione dealer: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore imprevisto durante l'eliminazione del concessionario");
        }

        return "redirect:/admin/maintenance";
    }

    // === GESTIONE UTENTI ===

    /**
     * Aggiorna un utente esistente con i dati modificati.
     * 
     * @param id ID dell'utente da aggiornare
     * @param updatedUser Dati aggiornati dell'utente
     * @param result Risultato della validazione
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/user/{id}/update")
    public String updateUser(@PathVariable Long id, 
                            @ModelAttribute User updatedUser, 
                            BindingResult result, 
                            RedirectAttributes redirectAttributes) {
        logger.info("Tentativo aggiornamento utente: ID={}", id);

        // Validazione dati del form
        if (result.hasErrors()) {
            logger.warn("Errori di validazione nell'aggiornamento utente: ID={}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore nei dati dell'utente");
            return "redirect:/admin/maintenance";
        }

        try {
            // Aggiorna l'utente
            adminService.updateUser(id, updatedUser);
            
            logger.info("Utente aggiornato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Utente aggiornato con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione aggiornamento utente: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        } catch (Exception e) {
            logger.error("Errore interno aggiornamento utente: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'aggiornamento dell'utente");
        }

        return "redirect:/admin/maintenance";
    }

    /**
     * Elimina un utente dal sistema.
     * 
     * @param id ID dell'utente da eliminare
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentativo eliminazione utente: ID={}", id);

        try {
            adminService.deleteUser(id);
            
            logger.info("Utente eliminato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Utente eliminato con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Utente non trovato per eliminazione: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Utente non trovato");

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante eliminazione utente: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Impossibile eliminare l'utente: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Errore interno eliminazione utente: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione dell'utente");
        }

        return "redirect:/admin/maintenance";
    }

    // === GESTIONE ABBONAMENTI ===

    /**
     * Mostra il form per aggiungere un nuovo abbonamento.
     * 
     * @param model Modello per la vista
     * @return Nome della vista admin-subscription-add
     */
    @GetMapping("/subscription/add")
    public String showAddSubscriptionForm(Model model) {
        logger.info("Richiesta form aggiunta nuovo abbonamento");
        
        model.addAttribute("subscription", new Subscription());
        
        return "admin-subscription-add";
    }

    /**
     * Aggiunge un nuovo abbonamento al sistema.
     * 
     * @param subscription Dati del nuovo abbonamento
     * @param result Risultato della validazione
     * @param model Modello per la vista
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione o form in caso di errore
     */
    @PostMapping("/subscription/add")
    public String addSubscription(@ModelAttribute Subscription subscription, 
                                 BindingResult result, 
                                 Model model, 
                                 RedirectAttributes redirectAttributes) {
        logger.info("Tentativo aggiunta nuovo abbonamento: nome={}", subscription.getName());

        // Validazione dati
        if (result.hasErrors() || subscription.getDurationDays() <= 0) {
            logger.warn("Errori di validazione nell'aggiunta abbonamento: durata={}",
                       subscription.getDurationDays());
            model.addAttribute("errorMessage", 
                "Errore nei dati dell'abbonamento. La durata deve essere maggiore di 0 giorni.");
            model.addAttribute("subscription", subscription);
            return "admin-subscription-add";
        }

        try {
            adminService.addSubscription(subscription);
            
            logger.info("Abbonamento aggiunto con successo: nome={}, durata={} giorni", 
                       subscription.getName(), subscription.getDurationDays());
            redirectAttributes.addFlashAttribute("successMessage", "Abbonamento aggiunto con successo");

            return "redirect:/admin/maintenance";

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione aggiunta abbonamento: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subscription", subscription);
            return "admin-subscription-add";

        } catch (Exception e) {
            logger.error("Errore interno aggiunta abbonamento", e);
            model.addAttribute("errorMessage", "Errore durante l'aggiunta dell'abbonamento");
            model.addAttribute("subscription", subscription);
            return "admin-subscription-add";
        }
    }

    /**
     * Mostra il form di modifica per un abbonamento specifico.
     * 
     * @param id ID dell'abbonamento da modificare
     * @param model Modello per la vista
     * @return Nome della vista admin-subscription-edit
     */
    @GetMapping("/subscription/{id}/edit")
    public String showEditSubscriptionForm(@PathVariable Long id, Model model) {
        logger.info("Richiesta form modifica abbonamento: ID={}", id);

        try {
            Subscription subscription = adminService.findSubscriptionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato con ID: " + id));

            model.addAttribute("subscription", subscription);
            
            logger.info("Form modifica abbonamento caricato con successo: ID={}, Nome={}", 
                       id, subscription.getName());

            return "admin-subscription-edit";

        } catch (IllegalArgumentException e) {
            logger.error("Abbonamento non trovato per modifica: ID={}", id);
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/maintenance?error=Abbonamento non trovato";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento form modifica abbonamento: ID={}", id, e);
            model.addAttribute("error", "Errore durante il caricamento del form di modifica");
            return "redirect:/admin/maintenance?error=Errore caricamento form";
        }
    }

    /**
     * Aggiorna un abbonamento esistente con i dati modificati.
     * 
     * @param id ID dell'abbonamento da aggiornare
     * @param updatedSubscription Dati aggiornati dell'abbonamento
     * @param result Risultato della validazione
     * @param model Modello per la vista
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione o form in caso di errore
     */
    @PostMapping("/subscription/{id}/update")
    public String updateSubscription(@PathVariable Long id, 
                                    @ModelAttribute Subscription updatedSubscription, 
                                    BindingResult result, 
                                    Model model, 
                                    RedirectAttributes redirectAttributes) {
        logger.info("Tentativo aggiornamento abbonamento: ID={}", id);

        // Validazione dati
        if (result.hasErrors() || updatedSubscription.getDurationDays() <= 0) {
            logger.warn("Errori di validazione nell'aggiornamento abbonamento: ID={}, durata={}", 
                       id, updatedSubscription.getDurationDays());
            model.addAttribute("errorMessage", 
                "Errore nei dati dell'abbonamento. La durata deve essere maggiore di 0 giorni.");
            model.addAttribute("subscription", updatedSubscription);
            return "admin-subscription-edit";
        }

        try {
            adminService.updateSubscription(id, updatedSubscription);
            
            logger.info("Abbonamento aggiornato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Abbonamento aggiornato con successo");

            return "redirect:/admin/maintenance";

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione aggiornamento abbonamento: ID={}", id, e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("subscription", updatedSubscription);
            return "admin-subscription-edit";

        } catch (Exception e) {
            logger.error("Errore interno aggiornamento abbonamento: ID={}", id, e);
            model.addAttribute("errorMessage", "Errore durante l'aggiornamento dell'abbonamento");
            model.addAttribute("subscription", updatedSubscription);
            return "admin-subscription-edit";
        }
    }

    /**
     * Mostra il form per applicare uno sconto temporaneo a un abbonamento.
     * 
     * @param id ID dell'abbonamento per lo sconto
     * @param model Modello per la vista
     * @return Nome della vista admin-subscription-discount
     */
    @GetMapping("/subscription/{id}/discount")
    public String showApplyDiscountForm(@PathVariable Long id, Model model) {
        logger.info("Richiesta form applicazione sconto abbonamento: ID={}", id);

        try {
            Subscription subscription = adminService.findSubscriptionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato con ID: " + id));

            model.addAttribute("subscription", subscription);
            
            logger.info("Form sconto abbonamento caricato con successo: ID={}, Nome={}", 
                       id, subscription.getName());

            return "admin-subscription-discount";

        } catch (IllegalArgumentException e) {
            logger.error("Abbonamento non trovato per sconto: ID={}", id);
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/maintenance?error=Abbonamento non trovato";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento form sconto: ID={}", id, e);
            model.addAttribute("error", "Errore durante il caricamento del form sconto");
            return "redirect:/admin/maintenance?error=Errore caricamento form";
        }
    }

    /**
     * Applica uno sconto temporaneo a un abbonamento.
     * 
     * @param id ID dell'abbonamento
     * @param discount Percentuale di sconto da applicare
     * @param discountExpiry Data di scadenza dello sconto
     * @param model Modello per la vista
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione o form in caso di errore
     */
    @PostMapping("/subscription/{id}/discount")
    public String applyDiscount(@PathVariable Long id,
                               @RequestParam("discount") Double discount,
                               @RequestParam("discountExpiry") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate discountExpiry,
                               Model model, 
                               RedirectAttributes redirectAttributes) {
        logger.info("Tentativo applicazione sconto: ID={}, sconto={}%, scadenza={}", 
                   id, discount, discountExpiry);

        // Validazione parametri
        if (discount == null || discount <= 0 || discount > 100) {
            logger.warn("Sconto non valido: {}%", discount);
            model.addAttribute("errorMessage", "Lo sconto deve essere tra 1% e 100%");
            try {
                Subscription subscription = adminService.findSubscriptionById(id).orElse(null);
                model.addAttribute("subscription", subscription);
            } catch (Exception e) {
                // Ignora errori nel caricamento per il messaggio di errore
            }
            return "admin-subscription-discount";
        }

        if (discountExpiry == null || discountExpiry.isBefore(LocalDate.now())) {
            logger.warn("Data scadenza sconto non valida: {}", discountExpiry);
            model.addAttribute("errorMessage", "La data di scadenza deve essere futura");
            try {
                Subscription subscription = adminService.findSubscriptionById(id).orElse(null);
                model.addAttribute("subscription", subscription);
            } catch (Exception e) {
                // Ignora errori nel caricamento per il messaggio di errore
            }
            return "admin-subscription-discount";
        }

        try {
            adminService.applyDiscount(id, discount, discountExpiry);
            
            logger.info("Sconto applicato con successo: ID={}, sconto={}%, scadenza={}", 
                       id, discount, discountExpiry);
            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Sconto del %.0f%% applicato con successo (scade il %s)", 
                             discount, discountExpiry));

            return "redirect:/admin/maintenance";

        } catch (IllegalArgumentException e) {
            logger.error("Errore di validazione applicazione sconto: ID={}", id, e);
            model.addAttribute("errorMessage", e.getMessage());
            try {
                Subscription subscription = adminService.findSubscriptionById(id).orElse(null);
                model.addAttribute("subscription", subscription);
            } catch (Exception ex) {
                // Ignora errori nel caricamento per il messaggio di errore
            }
            return "admin-subscription-discount";

        } catch (Exception e) {
            logger.error("Errore interno applicazione sconto: ID={}", id, e);
            model.addAttribute("errorMessage", "Errore durante l'applicazione dello sconto");
            try {
                Subscription subscription = adminService.findSubscriptionById(id).orElse(null);
                model.addAttribute("subscription", subscription);
            } catch (Exception ex) {
                // Ignora errori nel caricamento per il messaggio di errore
            }
            return "admin-subscription-discount";
        }
    }

    /**
     * Rimuove uno sconto da un abbonamento.
     * 
     * @param id ID dell'abbonamento
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/subscription/{id}/remove-discount")
    public String removeDiscount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentativo rimozione sconto abbonamento: ID={}", id);

        try {
            Subscription subscription = adminService.findSubscriptionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato con ID: " + id));

            // Rimuove sconto e data di scadenza
            subscription.setDiscount(null);
            subscription.setDiscountExpiry(null);
            adminService.updateSubscription(id, subscription);

            logger.info("Sconto rimosso con successo da abbonamento: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Sconto rimosso con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Abbonamento non trovato per rimozione sconto: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Abbonamento non trovato");

        } catch (Exception e) {
            logger.error("Errore interno rimozione sconto: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante la rimozione dello sconto");
        }

        return "redirect:/admin/maintenance";
    }

    /**
     * Elimina un abbonamento dal sistema.
     * 
     * @param id ID dell'abbonamento da eliminare
     * @param redirectAttributes Attributi per messaggi flash
     * @return Redirect alla pagina manutenzione con messaggio
     */
    @PostMapping("/subscription/{id}/delete")
    public String deleteSubscription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("Tentativo eliminazione abbonamento: ID={}", id);

        try {
            adminService.deleteSubscription(id);
            
            logger.info("Abbonamento eliminato con successo: ID={}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Abbonamento eliminato con successo");

        } catch (IllegalArgumentException e) {
            logger.error("Abbonamento non trovato per eliminazione: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Abbonamento non trovato");

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante eliminazione abbonamento: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Impossibile eliminare l'abbonamento: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Errore interno eliminazione abbonamento: ID={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Errore durante l'eliminazione dell'abbonamento");
        }

        return "redirect:/admin/maintenance";
    }
}