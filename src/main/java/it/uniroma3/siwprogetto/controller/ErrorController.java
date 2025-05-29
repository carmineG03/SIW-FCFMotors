package it.uniroma3.siwprogetto.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
	@RequestMapping("/error")
	public String handleError(HttpServletRequest request, Model model) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		String errorMessage = "Si è verificato un errore imprevisto.";
		if (status != null) {
			int statusCode = Integer.parseInt(status.toString());
			model.addAttribute("errorCode", statusCode);
			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				errorMessage = "Pagina non trovata.";
			} else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
				errorMessage = "Errore interno del server.";
			}
		}
		model.addAttribute("errorMessage", errorMessage);
		return "error";
	}
}