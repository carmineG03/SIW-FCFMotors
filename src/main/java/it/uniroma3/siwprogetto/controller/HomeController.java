package it.uniroma3.siwprogetto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    //Per trorna alla home page
    @GetMapping("/home")
    public String home() {
        return "index";
    }

    //Per andare alla pagina di manutenzione del sito
    @GetMapping("/homeManutenzione")
    public String manutenzione() {
        return "manutenzione";
    }

    //Per andare alla pagina di prodotti
    @GetMapping("/prodotti")
    public String prodotti() {
        return "prodotti";
    }

    //Per accedere alla schermata di login
    @GetMapping("/user-login")
    public String login() {
        return "login";
    }

    // Per accedere alla schermata di registrazione
    @GetMapping("/user-register")
    public String userRegister() {

        return "register";
    }
}
