package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.AccountInformation;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.model.UserSubscription;
import it.uniroma3.siwprogetto.repository.AccountInformationRepository;
import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import it.uniroma3.siwprogetto.repository.UserSubscriptionRepository;
import it.uniroma3.siwprogetto.service.CartService;
import it.uniroma3.siwprogetto.service.UserService;
import it.uniroma3.siwprogetto.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Controller per la gestione dell'account utente.
 * Gestisce visualizzazione, modifica, eliminazione account e gestione
 * abbonamenti.
 */
@Controller
@Transactional
public class AccountController {

    /** Logger per tracciare le operazioni del controller */
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    // === DIPENDENZE INIETTATE ===

    /** Servizio per la gestione degli utenti */
    private final UserService userService;

    /** Repository per le informazioni dell'account */
    private final AccountInformationRepository accountInformationRepository;

    /** Servizio per la gestione del carrello */
    private final CartService cartService;

    /** Repository per gli utenti */
    private final UserRepository userRepository;

    /** Repository per gli abbonamenti utente */
    private final UserSubscriptionRepository userSubscriptionRepository;

    /** Repository per gli abbonamenti */
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Costruttore con dependency injection.
     */
    @Autowired
    public AccountController(UserService userService,
            AccountInformationRepository accountInformationRepository,
            CartService cartService,
            UserRepository userRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionRepository subscriptionRepository) {
        this.userService = userService;
        this.accountInformationRepository = accountInformationRepository;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    // === GESTIONE VISUALIZZAZIONE ACCOUNT ===

    /**
     * Mostra la pagina principale dell'account utente.
     * Include informazioni personali, abbonamenti attivi e disponibili.
     * 
     * @param model     Modello per la vista
     * @param principal Utente autenticato corrente
     * @return Nome della vista account
     */
    @GetMapping("/account")
    public String showAccount(Model model, Principal principal) {
        logger.info("Accesso alla pagina account per utente: {}",
                principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null) {
            logger.warn("Tentativo di accesso all'account senza autenticazione");
            return "redirect:/login";
        }

        try {
            // Carica l'utente dal database
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato nel database: {}", principal.getName());
                model.addAttribute("error", "Utente non trovato");
                model.addAttribute("accountInformation", new AccountInformation());
                model.addAttribute("editMode", false);
                return "account";
            }

            // Carica le informazioni dell'account (o crea vuote se non esistono)
            AccountInformation accountInformation = accountInformationRepository
                    .findByUserId(user.getId())
                    .orElse(new AccountInformation());

            // Carica abbonamenti attivi e disponibili
            List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserAndActive(user, true);

            // Popola il modello per la vista
            model.addAttribute("user", user);
            model.addAttribute("accountInformation", accountInformation);
            model.addAttribute("activeSubscriptions", activeSubscriptions);
            model.addAttribute("availableSubscriptions", userService.getAvailableSubscriptions());
            model.addAttribute("editMode", false);

            logger.info("Pagina account caricata con successo per utente: {} (abbonamenti attivi: {})",
                    user.getUsername(), activeSubscriptions.size());

            return "account";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della pagina account per utente: {}",
                    principal.getName(), e);
            model.addAttribute("error", "Errore durante il caricamento delle informazioni account");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", false);
            return "account";
        }
    }

    /**
     * Mostra la pagina di modifica dell'account.
     * 
     * @param model     Modello per la vista
     * @param principal Utente autenticato corrente
     * @return Nome della vista account in modalità modifica
     */
    @GetMapping("/account/edit")
    public String editAccount(Model model, Principal principal) {
        logger.info("Accesso alla modalità modifica account per utente: {}",
                principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null) {
            logger.warn("Tentativo di modifica account senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per modifica: {}", principal.getName());
                model.addAttribute("error", "Utente non trovato");
                model.addAttribute("accountInformation", new AccountInformation());
                model.addAttribute("editMode", true);
                return "account";
            }

            AccountInformation accountInformation = accountInformationRepository
                    .findByUserId(user.getId())
                    .orElse(new AccountInformation());

            model.addAttribute("user", user);
            model.addAttribute("accountInformation", accountInformation);
            model.addAttribute("editMode", true);

            logger.info("Modalità modifica account attivata per utente: {}", user.getUsername());

            return "account";

        } catch (Exception e) {
            logger.error("Errore durante l'attivazione della modalità modifica per utente: {}",
                    principal.getName(), e);
            model.addAttribute("error", "Errore durante il caricamento della modalità modifica");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", true);
            return "account";
        }
    }

    // === GESTIONE SALVATAGGIO INFORMAZIONI ===

    /**
     * Salva le informazioni dell'account modificate dall'utente.
     * 
     * @param formAI    Dati del form di modifica
     * @param principal Utente autenticato corrente
     * @return Redirect alla pagina account
     */
    @PostMapping("/account")
    public String saveAccountInformation(@ModelAttribute AccountInformation formAI, Principal principal) {
        logger.info("Tentativo di salvataggio informazioni account per utente: {}",
                principal != null ? principal.getName() : "non autenticato");

        if (principal == null) {
            logger.warn("Tentativo di salvataggio senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per salvataggio: {}", principal.getName());
                return "redirect:/login";
            }

            // Carica le informazioni esistenti o crea una nuova istanza
            AccountInformation existingInfo = accountInformationRepository
                    .findByUserId(user.getId())
                    .orElse(new AccountInformation());

            // Aggiorna i campi mantenendo la version esistente
            existingInfo.setFirstName(formAI.getFirstName());
            existingInfo.setLastName(formAI.getLastName());
            existingInfo.setBirthDate(formAI.getBirthDate());
            existingInfo.setAddress(formAI.getAddress());
            existingInfo.setPhoneNumber(formAI.getPhoneNumber());
            existingInfo.setAdditionalInfo(formAI.getAdditionalInfo());
            existingInfo.setUser(user);

            // Salva nel database
            accountInformationRepository.save(existingInfo);

            logger.info("Informazioni account salvate con successo per utente: {}", user.getUsername());
            return "redirect:/account";

        } catch (Exception e) {
            logger.error("Errore durante il salvataggio delle informazioni account per utente: {}", 
                        principal.getName(), e);
            return "redirect:/account?error=Errore durante il salvataggio";
        }
    }

    // === GESTIONE ABBONAMENTI ===

    /**
     * Mostra la pagina degli abbonamenti dell'utente.
     * 
     * @param model Modello per la vista
     * @return Nome della vista subscriptions
     */
    @GetMapping("/subscriptions")
    public String showSubscriptionsPage(Model model) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Accesso alla pagina abbonamenti per utente: {}", username);

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                logger.error("Utente non trovato per visualizzazione abbonamenti: {}", username);
                model.addAttribute("errorMessage", "Utente non trovato.");
                return "subscriptions";
            }

            // Carica abbonamenti attivi dell'utente
            List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserAndActive(user, true);

            model.addAttribute("user", user);
            model.addAttribute("activeSubscriptions", activeSubscriptions);
            model.addAttribute("availableSubscriptions", subscriptionRepository.findAll());

            logger.info("Pagina abbonamenti caricata per utente: {} (abbonamenti attivi: {})",
                    username, activeSubscriptions.size());

            return "subscriptions";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della pagina abbonamenti", e);
            model.addAttribute("errorMessage", "Errore durante il caricamento degli abbonamenti.");
            return "subscriptions";
        }
    }

    /**
     * Aggiunge un abbonamento al carrello dell'utente.
     * 
     * @param subscriptionId ID dell'abbonamento da aggiungere
     * @param principal      Utente autenticato corrente
     * @return Redirect al carrello
     */
    @PostMapping("/subscribe")
    public String subscribe(@RequestParam("subscriptionId") Long subscriptionId, Principal principal) {
        logger.info("Tentativo di aggiunta abbonamento al carrello: subscriptionId={}, utente={}",
                subscriptionId, principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null) {
            logger.warn("Tentativo di sottoscrizione senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per sottoscrizione: {}", principal.getName());
                return "redirect:/login";
            }

            // Aggiunge l'abbonamento al carrello
            cartService.addSubscriptionToCart(subscriptionId, user);

            logger.info("Abbonamento aggiunto al carrello con successo: subscriptionId={}, utente={}",
                    subscriptionId, user.getUsername());

            return "redirect:/cart?success=true";

        } catch (Exception e) {
            logger.error("Errore durante l'aggiunta abbonamento al carrello: subscriptionId={}, utente={}",
                    subscriptionId, principal.getName(), e);
            return "redirect:/cart?error=Errore durante l'aggiunta dell'abbonamento";
        }
    }

    // === GESTIONE RUOLI UTENTE ===

    /**
     * Promuove un utente al ruolo PRIVATE per vendere auto.
     * 
     * @param principal Utente autenticato corrente
     * @param model     Modello per eventuali messaggi di errore
     * @return Redirect alla pagina account con messaggio
     */
    @PostMapping("/account/become-private")
    public String becomePrivate(Principal principal, Model model) {
        logger.info("Richiesta promozione a utente PRIVATE da: {}",
                principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null) {
            logger.warn("Tentativo di promozione senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per promozione PRIVATE: {}", principal.getName());
                model.addAttribute("error", "Utente non trovato");
                return "account";
            }

            // Verifica che l'utente abbia il ruolo USER e non abbia abbonamenti attivi
            if (user.getRolesString().contains("USER") && userService.getActiveSubscriptions(user.getId()).isEmpty()) {
                userService.updateUserRole(user, "PRIVATE");

                logger.info("Utente promosso a PRIVATE con successo: {}", user.getUsername());

                return "redirect:/account?success=Ruolo aggiornato a PRIVATO. Ora puoi vendere la tua auto!";
            } else {
                logger.warn("Promozione PRIVATE negata per utente: {} (ha abbonamenti attivi o ruolo diverso)",
                        user.getUsername());
                model.addAttribute("error",
                        "Non puoi diventare PRIVATO: hai già un abbonamento attivo o un ruolo diverso.");
                return "account";
            }

        } catch (Exception e) {
            logger.error("Errore durante la promozione a PRIVATE per utente: {}", principal.getName(), e);
            model.addAttribute("error", "Errore durante l'aggiornamento del ruolo");
            return "account";
        }
    }

    /**
     * Rimuove il ruolo PRIVATE dall'utente e elimina l'auto in vendita.
     * 
     * @param principal Utente autenticato corrente
     * @param model     Modello per eventuali messaggi di errore
     * @return Redirect alla pagina account con messaggio
     */
    @PostMapping("/account/remove-private")
    public String removePrivate(Principal principal, Model model) {
        logger.info("Richiesta rimozione ruolo PRIVATE da: {}",
                principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null) {
            logger.warn("Tentativo di rimozione ruolo senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per rimozione PRIVATE: {}", principal.getName());
                model.addAttribute("error", "Utente non trovato");
                return "account";
            }

            // Verifica che l'utente abbia il ruolo PRIVATE
            if (user.getRolesString().contains("PRIVATE")) {
                userService.removePrivateRoleAndCar(user);

                logger.info("Ruolo PRIVATE rimosso con successo per utente: {}", user.getUsername());

                return "redirect:/account?success=Ruolo PRIVATO rimosso. L'auto in vendita è stata eliminata.";
            } else {
                logger.warn("Tentativo di rimozione ruolo PRIVATE su utente che non lo possiede: {}",
                        user.getUsername());
                model.addAttribute("error", "Non puoi rimuovere il ruolo PRIVATO: non hai questo ruolo.");
                return "account";
            }

        } catch (IllegalStateException e) {
            logger.error("Stato illegale durante rimozione PRIVATE per utente: {}", principal.getName(), e);
            model.addAttribute("error", e.getMessage());
            return "account";
        } catch (Exception e) {
            logger.error("Errore durante la rimozione PRIVATE per utente: {}", principal.getName(), e);
            model.addAttribute("error", "Errore durante la rimozione del ruolo");
            return "account";
        }
    }

    // === GESTIONE ABBONAMENTI UTENTE ===

    /**
     * Gestisce la cancellazione/riattivazione dell'auto-renewal degli abbonamenti.
     * 
     * @param userSubscriptionId ID dell'abbonamento utente
     * @param principal          Utente autenticato corrente
     * @param redirectAttributes Attributi per il redirect
     * @return Redirect alla pagina account con messaggio
     */
    @GetMapping("/toggle-auto-renewal") 
    public String toggleAutoRenewal(@RequestParam("userSubscriptionId") Long userSubscriptionId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        logger.info("🔄 TOGGLE auto-renewal abbonamento: userSubscriptionId={}, utente={}",
                userSubscriptionId, principal != null ? principal.getName() : "non autenticato");

        if (principal == null) {
            logger.warn("Tentativo di modifica abbonamento senza autenticazione");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName());
            if (user == null) {
                logger.error("Utente non trovato per modifica abbonamento: {}", principal.getName());
                return "redirect:/login";
            }

            // Trova e verifica l'abbonamento
            UserSubscription subscription = userSubscriptionRepository.findById(userSubscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));

            // 🔍 DEBUG STATO PRIMA
            logger.info("🔍 STATO PRIMA: id={}, active={}, autoRenew={}",
                    subscription.getId(), subscription.isActive(), subscription.isAutoRenew());

            // Verifica autorizzazione
            if (!subscription.getUser().getId().equals(user.getId())) {
                logger.warn("Tentativo non autorizzato di modifica abbonamento: utente={}, subscriptionId={}",
                        user.getUsername(), userSubscriptionId);
                throw new IllegalArgumentException("Non autorizzato");
            }

            // SOLO TOGGLE AUTO-RENEWAL - NIENT'ALTRO!
            boolean newAutoRenewState = !subscription.isAutoRenew();
            subscription.setAutoRenew(newAutoRenewState);

            // SAVE DIRETTO - NON CHIAMARE ALTRI METODI
            userSubscriptionRepository.save(subscription);

            // 🔍 DEBUG STATO DOPO
            UserSubscription saved = userSubscriptionRepository.findById(userSubscriptionId).orElse(null);
            logger.info("🔍 STATO DOPO: id={}, active={}, autoRenew={}",
                    saved.getId(), saved.isActive(), saved.isAutoRenew());

            // Messaggio personalizzato
            if (newAutoRenewState) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "✅ Rinnovo automatico riattivato! Il tuo abbonamento si rinnoverà automaticamente.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "⚠️ Rinnovo automatico disattivato. L'abbonamento rimarrà attivo fino al " +
                                subscription.getExpiryDate() + " e poi scadrà.");
            }

            logger.info("✅ Auto-renewal modificato: subscriptionId={}, utente={}, nuovoStato={}",
                    userSubscriptionId, user.getUsername(), newAutoRenewState);

            return "redirect:/account";

        } catch (IllegalArgumentException e) {
            logger.error("❌ Errore validazione: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/account";
        } catch (Exception e) {
            logger.error("❌ Errore toggle auto-renewal: userSubscriptionId={}, utente={}",
                    userSubscriptionId, principal.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Errore durante la modifica dell'abbonamento. Riprova più tardi.");
            return "redirect:/account";
        }
    }

    // === GESTIONE HOME PAGE ===

    /**
     * Gestisce la visualizzazione della home page.
     * Carica le informazioni utente se autenticato.
     * 
     * @param model     Modello per la vista
     * @param principal Utente autenticato (opzionale)
     * @return Nome della vista index
     */
    @GetMapping("/")
    public String index(Model model, Principal principal) {
        logger.debug("Accesso alla home page da utente: {}",
                principal != null ? principal.getName() : "non autenticato");

        try {
            // Se l'utente è autenticato, carica le sue informazioni
            if (principal != null) {
                User user = userService.findByUsername(principal.getName());
                if (user != null) {
                    AccountInformation accountInformation = accountInformationRepository
                            .findByUserId(user.getId())
                            .orElse(new AccountInformation());

                    model.addAttribute("user", user);
                    model.addAttribute("accountInformation", accountInformation);

                    logger.debug("Informazioni utente caricate per home page: {}", user.getUsername());
                }
            }

            model.addAttribute("isAuthenticated", principal != null);

            return "index";

        } catch (Exception e) {
            logger.error("Errore durante il caricamento della home page per utente: {}",
                    principal != null ? principal.getName() : "non autenticato", e);
            model.addAttribute("isAuthenticated", principal != null);
            return "index";
        }
    }

    // === ROUTING MANUTENZIONE ===

    /**
     * Reindirizza gli utenti alla pagina di manutenzione appropriata in base al
     * ruolo.
     * 
     * @param auth Oggetto Authentication corrente
     * @return Redirect alla pagina appropriata
     */
    @GetMapping("/manutenzione")
    public String manutenzione(Authentication auth) {
        logger.info("Accesso a manutenzione da utente con ruoli: {}",
                auth != null ? auth.getAuthorities() : "non autenticato");

        try {
            if (SecurityUtils.hasRole("ROLE_PRIVATE")) {
                logger.info("Reindirizzamento a manutenzione privata");
                return "redirect:/manutenzione/private";
            } else if (SecurityUtils.hasRole("ROLE_DEALER")) {
                logger.info("Reindirizzamento a manutenzione dealer");
                return "redirect:/manutenzione/dealer";
            }

            logger.warn("Accesso negato a manutenzione: utente non ha ruoli appropriati");
            return "redirect:/access-denied";

        } catch (Exception e) {
            logger.error("Errore durante il routing manutenzione", e);
            return "redirect:/access-denied";
        }
    }

    /**
     * Mostra la pagina di manutenzione per utenti privati.
     * 
     * @return Nome della vista add_car
     */
    @GetMapping("/manutenzione/private")
    public String manutenzionePrivate() {
        logger.info("Accesso a manutenzione privata");
        return "add_car";
    }

    /**
     * Mostra la pagina di manutenzione per dealer.
     * 
     * @return Nome della vista manutenzione_dealer
     */
    @GetMapping("/manutenzione/dealer")
    public String manutenzioneDealer() {
        logger.info("Accesso a manutenzione dealer");
        return "manutenzione_dealer";
    }

    // === ELIMINAZIONE ACCOUNT ===

    /**
     * Elimina definitivamente l'account utente dopo verifica password.
     * 
     * @param principal Utente autenticato corrente
     * @param password  Password di conferma
     * @param model     Modello per eventuali messaggi di errore
     * @param request   Richiesta HTTP
     * @param response  Risposta HTTP
     * @return Redirect al login con messaggio di conferma
     */
    @PostMapping("/account/delete")
    public String deleteAccount(Principal principal,
            @RequestParam String password,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.info("Richiesta eliminazione account da utente: {}",
                principal != null ? principal.getName() : "non autenticato");

        // Verifica autenticazione
        if (principal == null || principal.getName() == null) {
            logger.warn("Tentativo di eliminazione account senza autenticazione");
            model.addAttribute("error", "Utente non autenticato");
            populateModelForAccountPage(model, null);
            return "account";
        }

        String username = principal.getName();

        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.error("Utente non trovato per eliminazione: {}", username);
                throw new RuntimeException("Utente non trovato");
            }

            // Elimina l'utente (include verifica password)
            userService.deleteUser(user, password);

            // Effettua il logout
            new SecurityContextLogoutHandler().logout(request, response,
                    SecurityContextHolder.getContext().getAuthentication());

            logger.info("Account eliminato con successo: {}", username);

            return "redirect:/login?accountDeleted";

        } catch (RuntimeException e) {
            logger.error("Errore durante eliminazione account per utente: {}", username, e);

            // Ricarica il modello in caso di errore
            User user = userService.findByUsername(username);
            populateModelForAccountPage(model, user);
            model.addAttribute("error", e.getMessage());

            return "account";
        }
    }

    // === METODI UTILITY ===

    /**
     * Popola il modello con i dati necessari per la pagina account.
     * Utilizzato per evitare duplicazione di codice.
     * 
     * @param model Modello da popolare
     * @param user  Utente corrente (può essere null)
     */
    private void populateModelForAccountPage(Model model, User user) {
        if (user != null) {
            AccountInformation accountInfo = accountInformationRepository
                    .findByUserId(user.getId())
                    .orElse(new AccountInformation());

            model.addAttribute("user", user);
            model.addAttribute("accountInformation", accountInfo);
            model.addAttribute("activeSubscriptions", userService.getActiveSubscriptions(user.getId()));
            model.addAttribute("availableSubscriptions", userService.getAvailableSubscriptions());
        } else {
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("activeSubscriptions", Collections.emptyList());
            model.addAttribute("availableSubscriptions", subscriptionRepository.findAll());
        }

        model.addAttribute("editMode", false);
    }
}