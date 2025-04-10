package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.service.DealerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class DealerController {

    @Autowired
    private DealerService dealerService;

    @GetMapping("/dealers")
    public String showDealersPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("isAuthenticated", isAuthenticated); // Passa isAuthenticated al modello
        return "dealers"; // Restituisce il template dealers.html
    }

    @GetMapping("/api/dealers")
    @ResponseBody
    public List<Dealer> findDealers(@RequestParam String query) {
        return dealerService.findByLocation(query);
    }
}