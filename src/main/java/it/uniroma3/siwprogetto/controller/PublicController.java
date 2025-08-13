package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.service.DealerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller pubblico per pagine accessibili senza autenticazione
 * Gestisce le pagine informative e di visualizzazione pubblica
 */
@Controller
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private DealerService dealerService;

    /**
     * Mostra la pagina pubblica con l'elenco di tutti i concessionari
     * Accessibile a tutti gli utenti senza autenticazione
     * 
     * @param model Model per passare dati alla view
     * @return template della pagina concessionari
     */
    @GetMapping("/dealers")
    public String showDealersPage(Model model) {
        logger.info("Richiesta accesso alla pagina /dealers");
        
        try {
            // Recupera tutti i concessionari attivi dal database
            List<Dealer> dealers = dealerService.findAll();
            logger.debug("Trovati {} concessionari", dealers.size());
            
            // Passa l'elenco dei concessionari alla view
            model.addAttribute("dealers", dealers);
            return "dealers";
            
        } catch (Exception e) {
            // Log dell'errore per debugging
            logger.error("Errore nel caricamento della pagina concessionari: {}", e.getMessage(), e);
            
            // Mostra messaggio d'errore all'utente ma carica comunque la pagina
            model.addAttribute("errorMessage", "Errore nel caricamento della pagina dei concessionari.");
            model.addAttribute("dealers", List.of()); // Lista vuota come fallback
            return "dealers";
        }
    }
}