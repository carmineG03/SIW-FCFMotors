package it.uniroma3.siwprogetto.controller;



import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manutenzione")
public class MaintenanceController {

    private final ProductRepository productRepository;

    public MaintenanceController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Mostra la pagina di manutenzione
    @GetMapping
    public String showMaintenancePage(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("productForm", new Product());
        return "maintenance";
    }

    // Aggiungi un nuovo prodotto
    @PostMapping("/aggiungi")
    public String addProduct(@ModelAttribute("productForm") Product product) {
        productRepository.save(product);
        return "redirect:/manutenzione";
    }

    // Modifica un prodotto esistente
    @GetMapping("/modifica/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/manutenzione"; // Redirect in caso di prodotto non trovato
        }
        model.addAttribute("productForm", product); // Popola il form con il prodotto esistente
        return "maintenance";
    }

    // Salva le modifiche al prodotto
    @PostMapping("/modifica")
    public String updateProduct(@ModelAttribute("productForm") Product product) {
        // Non includere l'ID nel form di modifica, usiamo quello già passato nel Path
        productRepository.save(product);
        return "redirect:/manutenzione";
    }
}