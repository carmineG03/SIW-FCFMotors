package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Dealer;
import it.uniroma3.siwprogetto.model.Product;
import it.uniroma3.siwprogetto.model.User;
import it.uniroma3.siwprogetto.repository.DealerRepository;
import it.uniroma3.siwprogetto.repository.ProductRepository;
import it.uniroma3.siwprogetto.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class DealerService {

    private static final Logger logger = LoggerFactory.getLogger(DealerService.class);

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;



    @Transactional
    public Dealer saveDealer(Dealer dealer, boolean isUpdate) {
        logger.info("Saving dealer: name={}, isUpdate={}", dealer.getName(), isUpdate);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Current authenticated user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Optional<Dealer> existingDealer = dealerRepository.findByOwnerUsername(username);
        if (isUpdate && existingDealer.isPresent()) {
            logger.debug("Updating existing dealer: id={}", existingDealer.get().getId());
            Dealer toUpdate = existingDealer.get();
            toUpdate.setName(dealer.getName());
            toUpdate.setDescription(dealer.getDescription());
            toUpdate.setAddress(dealer.getAddress());
            toUpdate.setContact(dealer.getContact());
            toUpdate.setImagePath(dealer.getImagePath());
            toUpdate.setLat(dealer.getLat());
            toUpdate.setLng(dealer.getLng());
            Dealer savedDealer = dealerRepository.save(toUpdate);
            logger.info("Dealer updated: id={}, name={}", savedDealer.getId(), savedDealer.getName());
            return savedDealer;
        } else if (!isUpdate && existingDealer.isPresent()) {
            logger.error("Dealer already exists for user '{}': id={}", username, existingDealer.get().getId());
            throw new IllegalStateException("Un concessionario esiste già per questo utente");
        } else if (!isUpdate && !existingDealer.isPresent()) {
            logger.debug("Creating new dealer for user: id={}, username={}", user.getId(), username);
            dealer.setOwner(user);
            Dealer savedDealer = dealerRepository.save(dealer);
            logger.info("Dealer created: id={}, name={}, owner_id={}",
                    savedDealer.getId(), savedDealer.getName(), savedDealer.getOwner().getId());
            return savedDealer;
        } else {
            logger.error("Invalid state: isUpdate=true but no dealer exists for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato per la modifica");
        }
    }

    public Dealer findByOwner() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Finding dealer for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return null;
        }

        Optional<Dealer> dealer = dealerRepository.findByOwnerUsername(username);
        if (dealer.isPresent()) {
            return dealer.get();
        } else {
            logger.warn("No dealer found for user '{}'", username);
            return null;
        }
    }

    @Transactional
    public Product addProduct(Product product) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Adding product for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        product.setSeller(user);
        product.setSellerType("DEALER");
        Product savedProduct = productRepository.save(product);
        logger.info("Product added: id={}, name={}, seller_id={}",
                savedProduct.getId(), savedProduct.getName(), user.getId());
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long productId, Product updatedProduct) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Updating product: id={}, user={}", productId, username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.error("No authenticated user found");
            throw new IllegalStateException("Utente non autenticato");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.error("No dealer found for user '{}'", username);
            throw new IllegalStateException("Nessun concessionario trovato");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", productId);
                    return new IllegalStateException("Prodotto non trovato");
                });

        if (!product.getSeller().getId().equals(user.getId())) {
            logger.error("Product does not belong to user: product_id={}, user_id={}", productId, user.getId());
            throw new IllegalStateException("Il prodotto non appartiene a questo utente");
        }

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        Product savedProduct = productRepository.save(product);
        logger.info("Product updated: id={}, name={}, seller_id={}",
                savedProduct.getId(), savedProduct.getName(), user.getId());
        return savedProduct;
    }

    public List<Product> getProductsByDealer() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("Retrieving products for user: {}", username);

        if (username == null || "anonymousUser".equals(username)) {
            logger.warn("No authenticated user found");
            return List.of();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("Utente non trovato: {}", username);
                    return new IllegalStateException("Utente non trovato: " + username);
                });

        Dealer dealer = findByOwner();
        if (dealer == null) {
            logger.warn("No dealer found for user '{}'", username);
            return List.of();
        }

        List<Product> products = productRepository.findBySellerId(user.getId());
        logger.info("Found {} products for user: id={}", products.size(), user.getId());
        return products;
    }

    public List<Dealer> findByLocation(String query) {
        logger.debug("Finding dealers with query: {}", query);
        return dealerRepository.findAll();
    }

    public List<Dealer> findAll() {
        logger.info("Retrieving all dealers");
        return dealerRepository.findAll();
    }

    public Product findProductById(Long id) {
        logger.debug("Finding product by ID: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", id);
                    return new IllegalStateException("Prodotto non trovato");
                });
    }

    public void deleteProduct(Long id) {
        logger.debug("Deleting product: id={}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Product not found: id={}", id);
                    return new IllegalStateException("Prodotto non trovato");
                });
        productRepository.delete(product);
        logger.info("Product deleted: id={}", id);
    }

    @Transactional
    public void deleteDealer(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID del concessionario non valido.");
        }
        if (!dealerRepository.existsById(id)) {
            throw new IllegalArgumentException("Concessionario non trovato con ID: " + id);
        }

        Dealer dealer = dealerRepository.findById(id).get();
        List<Product> products = productRepository.findBySeller(dealer.getOwner());
        if (!products.isEmpty()) {
            productRepository.deleteAll(products);
        }

        try {
            // Usa una query diretta
            Query query = entityManager.createQuery("DELETE FROM Dealer d WHERE d.id = :id");
            query.setParameter("id", id);
            int deleted = query.executeUpdate();
            dealerRepository.flush();
            if (dealerRepository.existsById(id)) {
                throw new IllegalStateException("Eliminazione non riuscita: il concessionario esiste ancora.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante l'eliminazione del concessionario.", e);
        }
    }
}