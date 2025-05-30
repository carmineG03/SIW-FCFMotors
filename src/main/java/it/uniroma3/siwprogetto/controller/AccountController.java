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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static it.uniroma3.siwprogetto.util.SecurityUtils.hasRole;

@Controller
public class AccountController {


    @Autowired
    private UserService userService;

    @Autowired
    private AccountInformationRepository accountInformationRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;


    @GetMapping("/account")
    public String showAccount(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            model.addAttribute("error", "User not found");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", false);
            return "account";
        }

        AccountInformation accountInformation = accountInformationRepository
                .findByUserId(user.getId())
                .orElse(new AccountInformation());

        // Aggiungi abbonamenti attivi e disponibili
        model.addAttribute("activeSubscriptions", userService.getActiveSubscriptions(user.getId()));
        model.addAttribute("availableSubscriptions", userService.getAvailableSubscriptions());
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
    public String saveAccountInformation(@ModelAttribute AccountInformation formAI, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        AccountInformation accountInformation = accountInformationRepository
                .findByUserId(user.getId())
                .orElse(new AccountInformation());

        accountInformation.setFirstName(formAI.getFirstName());
        accountInformation.setLastName(formAI.getLastName());
        accountInformation.setBirthDate(formAI.getBirthDate());
        accountInformation.setAddress(formAI.getAddress());
        accountInformation.setPhoneNumber(formAI.getPhoneNumber());
        accountInformation.setAdditionalInfo(formAI.getAdditionalInfo());
        accountInformation.setUser(user);

        accountInformationRepository.save(accountInformation);
        return "redirect:/account";
    }

    @GetMapping("/subscriptions")
    public String showSubscriptionsPage(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();


        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", "Utente non trovato.");
            return "subscriptions";
        }

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserAndActive(user, true);
        model.addAttribute("user", user);
        model.addAttribute("activeSubscriptions", activeSubscriptions);
        model.addAttribute("availableSubscriptions", subscriptionRepository.findAll());
        return "subscriptions";
    }

    @PostMapping("/account/become-private")
    public String becomePrivate(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            model.addAttribute("error", "Utente non trovato");
            return "account";
        }

        // Verifica che l'utente abbia il ruolo USER e non abbia abbonamenti attivi
        if (user.getRolesString().contains("USER") && userService.getActiveSubscriptions(user.getId()).isEmpty()) {
            userService.updateUserRole(user, "PRIVATE");
            return "redirect:/account?success=Ruolo aggiornato a PRIVATO. Ora puoi vendere la tua auto!";
        } else {
            model.addAttribute("error", "Non puoi diventare PRIVATO: hai già un abbonamento attivo o un ruolo diverso.");
            return "account";
        }
    }

    @PostMapping("/account/remove-private")
    public String removePrivate(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            model.addAttribute("error", "Utente non trovato");
            return "account";
        }

        // Verifica che l'utente abbia il ruolo PRIVATE
        if (user.getRolesString().contains("PRIVATE")) {
            try {
                userService.removePrivateRoleAndCar(user);
                return "redirect:/account?success=Ruolo PRIVATO rimosso. L'auto in vendita è stata eliminata.";
            } catch (IllegalStateException e) {
                model.addAttribute("error", e.getMessage());
                return "account";
            }
        } else {
            model.addAttribute("error", "Non puoi rimuovere il ruolo PRIVATO: non hai questo ruolo.");
            return "account";
        }
    }




    @PostMapping("/subscribe")
    public String subscribe(@RequestParam("subscriptionId") Long subscriptionId, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        cartService.addSubscriptionToCart(subscriptionId, user);
        return "redirect:/cart?success=true";
    }

    @GetMapping("/cancel-subscription")
    public String cancelSubscription(@RequestParam("userSubscriptionId") Long userSubscriptionId, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        try {
            UserSubscription subscription = userSubscriptionRepository.findById(userSubscriptionId)
                    .orElseThrow(() -> new IllegalArgumentException("Abbonamento non trovato"));
            if (!subscription.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Non autorizzato");
            }
            // Toggle auto-renewal state
            boolean newAutoRenewState = !subscription.isAutoRenew();
            subscription.setAutoRenew(newAutoRenewState);
            userSubscriptionRepository.save(subscription);

            String successMessage = newAutoRenewState ? "Auto-renewal enabled" : "Auto-renewal disabled";
            return "redirect:/account?success=" + successMessage;
        } catch (IllegalArgumentException e) {
            return "redirect:/account?error=" + e.getMessage();
        }
    }

    @GetMapping("/")
    public String index(Model model, Principal principal) {
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
        return "add_car";
    }

    @GetMapping("/manutenzione/dealer")
    public String manutenzioneDealer() {
        return "manutenzione_dealer";
    }

    @PostMapping("/account/delete")
    public String deleteAccount(Principal principal, @RequestParam String password, Model model,
                                HttpServletRequest request, HttpServletResponse response) {
        if (principal == null || principal.getName() == null) {
            model.addAttribute("error", "Utente non autenticato");
            model.addAttribute("accountInformation", new AccountInformation());
            model.addAttribute("editMode", false);
            model.addAttribute("activeSubscriptions", Collections.emptyList());
            model.addAttribute("availableSubscriptions", subscriptionRepository.findAll());
            return "account";
        }

        String username = principal.getName();
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                throw new RuntimeException("Utente non trovato");
            }
            userService.deleteUser(user, password);
            new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
            return "redirect:/login?accountDeleted";
        } catch (RuntimeException e) {
            User user = userService.findByUsername(username);
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("accountInformation", accountInformationRepository
                        .findByUserId(user.getId())
                        .orElse(new AccountInformation()));
                model.addAttribute("activeSubscriptions", userService.getActiveSubscriptions(user.getId()));
                model.addAttribute("availableSubscriptions", userService.getAvailableSubscriptions());
            } else {
                model.addAttribute("accountInformation", new AccountInformation());
                model.addAttribute("activeSubscriptions", Collections.emptyList());
                model.addAttribute("availableSubscriptions", subscriptionRepository.findAll());
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editMode", false);
            return "account";
        }
    }
}