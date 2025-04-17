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

@Controller
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private DealerService dealerService;

    @GetMapping("/dealers")
    public String showDealersPage(Model model) {
        logger.info("Accessing /dealers page");
        try {
            List<Dealer> dealers = dealerService.findAll();
            model.addAttribute("dealers", dealers);
            return "dealers";
        } catch (Exception e) {
            logger.error("Error loading dealers page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Errore nel caricamento della pagina dei concessionari.");
            return "dealers";
        }
    }
}