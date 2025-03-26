package it.uniroma3.siwprogetto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "index";
    }

    @GetMapping("/homeManutenzione")
    public String manutenzione() {
        return "manutenzione";
    }

    @GetMapping("/prodotti")
    public String prodotti() {
        return "prodotti";
    }

    @GetMapping("/user-login")
    public String login() {
        return "login";
    }

    // Change this mapping to avoid conflict
    @GetMapping("/user-register")
    public String userRegister() {
        return "register";
    }
}
