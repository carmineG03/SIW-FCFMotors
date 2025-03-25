package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.ProductForm;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manutenzione")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/prodotti")
    public String showMaintenancePage(Model model) {
        model.addAttribute("productForm", new ProductForm());
        model.addAttribute("products", productService.findAll());
        return "maintenance";
    }

    @PostMapping("/aggiungiProdotto")
    public String addProduct(@ModelAttribute ProductForm productForm) {
        productService.save(productForm);
        return "redirect:/manutenzione/prodotti";
    }

    @PostMapping("/elimina/{id}")
    public String deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProductById(id);
        return "redirect:/manutenzione/prodotti";
    }

    @GetMapping("/modificaProdotto/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.findById(id);
        if (product == null) {
            return "redirect:/manutenzione/prodotti";
        }
        model.addAttribute("productForm", product);
        return "maintenance";
    }

  @PostMapping("/modificaProdotto")
  public String updateProduct(@ModelAttribute("productForm") ProductForm productForm) {
      Long id = productForm.getId();
      if (id == null) {
          // Handle error
          return "redirect:/error";
      }
      productService.updateProduct(id, productForm);
      return "redirect:/manutenzione/prodotti";
  }
}