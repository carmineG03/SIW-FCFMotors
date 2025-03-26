// src/main/java/it/uniroma3/siwprogetto/controller/UserController.java
package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    //Vedi pagina registrazione
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    //Registra l'utente
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        userService.save(user);
        return "redirect:/login";
    }

    //Vedi pagina di login
    @GetMapping("/user/login")
    public String showUserLoginForm() {
        return "login";
    }
}


