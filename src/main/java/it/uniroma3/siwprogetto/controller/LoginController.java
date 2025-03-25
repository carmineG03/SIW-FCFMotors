package it.uniroma3.siwprogetto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    @PostMapping("/login")
    public ModelAndView login(@RequestParam("email") String email, @RequestParam("password") String password) {
        // Add your authentication logic here
        if (authenticate(email, password)) {
            return new ModelAndView("redirect:/home");
        } else {
            return new ModelAndView("login", "error", "Invalid email or password");
        }
    }

    private boolean authenticate(String email, String password) {
        // Replace with your actual authentication logic
        return "user@example.com".equals(email) && "password".equals(password);
    }
}