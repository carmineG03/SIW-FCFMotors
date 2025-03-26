package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.ProductForm;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    // Questo metodo serve per salvare un prodotto
    public void save(ProductForm productForm) {
        Product product = new Product();
        product.setName(productForm.getName());
        product.setPrice(productForm.getPrice());
        product.setDescription(productForm.getDescription());
        product.setImageUrl(productForm.getImageUrl());
        productRepository.save(product);
    }


    // Questo metodo serve per eliminare un prodotto dato il suo id
    public void deleteProductById(Long id) {
        productRepository.deleteById(id);
    }

    // Questo metodo serve per trovare un prodotto dato il suo id
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
    }

    // Questo metodo serve per aggiornare un prodotto dato il suo id
    public void updateProduct(Long id, ProductForm productForm) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        product.setName(productForm.getName());
        product.setPrice(productForm.getPrice());
        product.setDescription(productForm.getDescription());
        product.setImageUrl(productForm.getImageUrl());
        productRepository.save(product);
    }

    // Questo metodo serve per salvare un prodotto
    public void saveProduct(Product product) {
        productRepository.save(product);
    }
}
