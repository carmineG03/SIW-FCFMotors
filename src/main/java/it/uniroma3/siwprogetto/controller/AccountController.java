package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.AccountInformation;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.AccountInformationRepository;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import static it.uniroma3.siwprogetto.util.SecurityUtils.hasRole;

@Controller
public class AccountController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountInformationRepository accountInformationRepository;

    @GetMapping("/account")
    public String showAccount(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // 1) Trova l'utente loggato
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            model.addAttribute("error", "User not found");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", false);
            return "account";
        }

        // 2) Cerca l'AccountInformation in base allo user_id
        AccountInformation accountInformation = accountInformationRepository
                .findByUserId(user.getId())
                .orElse(new AccountInformation());

        model.addAttribute("accountInformation", accountInformation);
        model.addAttribute("user", user);
        model.addAttribute("editMode", false);
        return "account";
    }

    @GetMapping("/account/edit")
    public String editAccount(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            model.addAttribute("error", "User not found");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", true);
            return "account";
        }

        AccountInformation accountInformation = accountInformationRepository
                .findByUserId(user.getId())
                .orElse(new AccountInformation());

        model.addAttribute("accountInformation", accountInformation);
        model.addAttribute("user", user);
        model.addAttribute("editMode", true);
        return "account";
    }

    @PostMapping("/account")
    public String saveAccountInformation(@ModelAttribute AccountInformation formAI,
                                         Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // 1) Trova l'utente loggato
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        // 2) Recupera (o crea) l'AccountInformation associato
        AccountInformation accountInformation = accountInformationRepository
                .findByUserId(user.getId())
                .orElse(new AccountInformation());

        // 3) Aggiorna i campi dal form
        accountInformation.setFirstName(formAI.getFirstName());
        accountInformation.setLastName(formAI.getLastName());
        accountInformation.setBirthDate(formAI.getBirthDate());
        accountInformation.setAddress(formAI.getAddress());
        accountInformation.setPhoneNumber(formAI.getPhoneNumber());
        accountInformation.setAdditionalInfo(formAI.getAdditionalInfo());

        // 4) Collega l'utente esistente (il FK user_id)
        accountInformation.setUser(user);

        // 5) Salva (insert o update a seconda che abbia già ID)
        accountInformationRepository.save(accountInformation);

        return "redirect:/account";
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
        // Se l'utente è autenticato
        if (principal != null) {
            User user = userService.findByUsername(principal.getName());
            if (user != null) {
                AccountInformation accountInformation = accountInformationRepository
                        .findByUserId(user.getId())
                        .orElse(new AccountInformation());
                model.addAttribute("accountInformation", accountInformation);
                model.addAttribute("user", user);
            }
        }
        model.addAttribute("isAuthenticated", principal != null);
        return "index";
    }

    @GetMapping("/manutenzione")
    public String manutenzione(Authentication auth) {
        if (hasRole("ROLE_PRIVATE")) {
            return "redirect:/manutenzione/private";
        } else if (hasRole("ROLE_DEALER")) {
            return "redirect:/manutenzione/dealer";
        }
        return "redirect:/access-denied";
    }

    @GetMapping("/manutenzione/private")
    public String manutenzionePrivate() {
        return "add_car"; // Template per PRIVATE
    }

    @GetMapping("/manutenzione/dealer")
    public String manutenzioneDealer() {
        return "manutenzione_dealer"; // Template per DEALER
    }
}
