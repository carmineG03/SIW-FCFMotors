package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.service.EmailService;
import it.uniroma3.siwprogetto.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @Autowired
    UserService userService;

    private final EmailService emailService;

    public LoginController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
                User user = userService.findByEmail(email);
            if (user != null) {
                String resetToken = userService.generateResetToken(email);
                emailService.sendResetPasswordEmail(email, resetToken);
                redirectAttributes.addFlashAttribute("message", "A reset link has been sent to your email.");
            } else {
                redirectAttributes.addFlashAttribute("error", "No account found with this email.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error sending reset link. Please try again.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        if (userService.isResetTokenValid(token)) {
            return "reset-password";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset token.");
            return "redirect:/login";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (userService.isResetTokenValid(token)) {
                userService.resetPassword(token, newPassword);
                redirectAttributes.addFlashAttribute("message", "Password reset successfully. Please log in.");
                return "redirect:/login";
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid or expired reset token.");
                return "redirect:/forgot-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resetting password. Please try again.");
            return "redirect:/forgot-password";
        }
    }
}