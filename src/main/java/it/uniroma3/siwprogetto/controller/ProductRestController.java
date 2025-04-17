package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/rest/api/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    // Crea un nuovo prodotto
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        try {
            // Imposta il venditore basato sull'utente autenticato
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(null); // Non autenticato
            }
            // Nota: Assumi che il frontend invii l'ID del venditore o che venga gestito dal backend
            Product savedProduct = productService.save(product);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Recupera un prodotto per ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        return product.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Aggiorna un prodotto
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        Optional<Product> existingProduct = productService.findById(id);
        if (existingProduct.isPresent()) {
            product.setId(id);
            Product updatedProduct = productService.save(product);
            return ResponseEntity.ok(updatedProduct);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Elimina un prodotto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (product.isPresent()) {
            productService.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}