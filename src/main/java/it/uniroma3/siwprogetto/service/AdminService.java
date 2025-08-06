package it.uniroma3.siwprogetto.service;

	import it.uniroma3.siwprogetto.model.Dealer;
	import it.uniroma3.siwprogetto.model.Product;
	import it.uniroma3.siwprogetto.model.Subscription;
	import it.uniroma3.siwprogetto.model.User;
	import it.uniroma3.siwprogetto.repository.DealerRepository;
	import it.uniroma3.siwprogetto.repository.ProductRepository;
	import it.uniroma3.siwprogetto.repository.SubscriptionRepository;
	import it.uniroma3.siwprogetto.repository.UserRepository;
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.security.access.prepost.PreAuthorize;
	import org.springframework.stereotype.Service;
	import org.springframework.transaction.annotation.Transactional;

	import java.time.LocalDate;
	import java.util.List;
	import java.util.Optional;

	@Service
	public class AdminService {

	    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

	    @Autowired
	    private UserRepository userRepository;

	    @Autowired
	    private ProductRepository productRepository;

	    @Autowired
	    private DealerRepository dealerRepository;

	    @Autowired
	    private SubscriptionRepository subscriptionRepository;

		@Autowired
		private DealerService dealerService;

		@Autowired
		private UserService userService;

	    // Trova un prodotto per ID
	    @PreAuthorize("hasRole('ADMIN')")
	    public Optional<Product> findProductById(Long id) {
	        return productRepository.findById(id);
	    }

	    // Trova un concessionario per ID
	    @PreAuthorize("hasRole('ADMIN')")
	    public Optional<Dealer> findDealerById(Long id) {
	        return dealerRepository.findById(id);
	    }

	    // Modifica un prodotto (auto) di qualsiasi utente
	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public Product updateProduct(Long productId, Product updatedProduct) {
	        logger.debug("Admin updating product: id={}", productId);

	        Product product = productRepository.findById(productId)
	                .orElseThrow(() -> {
	                    logger.error("Product not found: id={}", productId);
	                    return new IllegalStateException("Prodotto non trovato");
	                });

	        product.setDescription(updatedProduct.getDescription());
	        product.setPrice(updatedProduct.getPrice());
	        product.setCategory(updatedProduct.getCategory());
	        product.setBrand(updatedProduct.getBrand());
	        product.setModel(updatedProduct.getModel());
	        product.setMileage(updatedProduct.getMileage());
	        product.setYear(updatedProduct.getYear());
	        product.setFuelType(updatedProduct.getFuelType());
	        product.setTransmission(updatedProduct.getTransmission());

	        Product savedProduct = productRepository.save(product);
	        logger.info("Product updated by admin: id={}, model={}", savedProduct.getId(), savedProduct.getModel());
	        return savedProduct;
	    }

	    // Rimuove un prodotto (auto) di qualsiasi utente - AGGIORNATO
		@Transactional
		@PreAuthorize("hasRole('ADMIN')")
		public void deleteProduct(Long productId) {
			logger.debug("Admin deleting product: id={}", productId);

			Product product = productRepository.findById(productId)
					.orElseThrow(() -> {
						logger.error("Product not found: id={}", productId);
						return new IllegalStateException("Prodotto non trovato");
					});

			// Usa il service del dealer che gestisce correttamente le dipendenze
			try {
				dealerService.deleteProduct(productId);
				logger.info("Product deleted by admin: id={}", productId);
			} catch (Exception e) {
				logger.error("Error deleting product via dealerService: id={}", productId, e);
				throw new RuntimeException("Errore durante l'eliminazione del prodotto: " + e.getMessage(), e);
			}
		}

	    // Modifica un concessionario
	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public Dealer updateDealer(Long dealerId, Dealer updatedDealer) {
	        logger.debug("Admin updating dealer: id={}", dealerId);

	        Dealer dealer = dealerRepository.findById(dealerId)
	                .orElseThrow(() -> {
	                    logger.error("Dealer not found: id={}", dealerId);
	                    return new IllegalStateException("Concessionario non trovato");
	                });

	        dealer.setName(updatedDealer.getName());
	        dealer.setDescription(updatedDealer.getDescription());
	        dealer.setAddress(updatedDealer.getAddress());
	        dealer.setPhone(updatedDealer.getPhone());
	        dealer.setEmail(updatedDealer.getEmail());
	        dealer.setImages(updatedDealer.getImages()); // Gestione immagini come lista

	        Dealer savedDealer = dealerRepository.save(dealer);
	        logger.info("Dealer updated by admin: id={}, name={}", savedDealer.getId(), savedDealer.getName());
	        return savedDealer;
	    }

	    // Rimuove un concessionario e i suoi prodotti associati
	    @Transactional
		@PreAuthorize("hasRole('ADMIN')")
		public void deleteDealer(Long dealerId) {
			logger.debug("Admin deleting dealer: id={}", dealerId);

			Dealer dealer = dealerRepository.findById(dealerId)
					.orElseThrow(() -> {
						logger.error("Dealer not found: id={}", dealerId);
						return new IllegalStateException("Concessionario non trovato");
					});

			// Usa il service del dealer che gestisce correttamente le dipendenze
			try {
				dealerService.deleteDealer(dealerId);
				logger.info("Dealer deleted by admin: id={}", dealerId);
			} catch (Exception e) {
				logger.error("Error deleting dealer via dealerService: id={}", dealerId, e);
				throw new RuntimeException("Errore durante l'eliminazione del concessionario: " + e.getMessage(), e);
			}
		}

	    // Modifica un account utente
	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public User updateUser(Long userId, User updatedUser) {
	        logger.debug("Admin updating user: id={}", userId);

	        User user = userRepository.findById(userId)
	                .orElseThrow(() -> {
	                    logger.error("User not found: id={}", userId);
	                    return new IllegalStateException("Utente non trovato");
	                });

	        user.setUsername(updatedUser.getUsername());
	        user.setEmail(updatedUser.getEmail());
	        user.setRolesString(updatedUser.getRolesString());

	        User savedUser = userRepository.save(user);
	        logger.info("User updated by admin: id={}, username={}", savedUser.getId(), savedUser.getUsername());
	        return savedUser;
	    }

	;

		// Versione semplificata che evita il problema delle subscription
		@Transactional
		@PreAuthorize("hasRole('ADMIN')")
		public void deleteUser(Long userId) {
			logger.debug("Admin deleting user: id={}", userId);

			User user = userRepository.findById(userId)
					.orElseThrow(() -> {
						logger.error("User not found: id={}", userId);
						return new IllegalStateException("Utente non trovato");
					});

			try {
				// Step 1: Elimina il concessionario associato (questo eliminerà anche i prodotti)
				Optional<Dealer> dealer = dealerRepository.findByOwner(user);
				if (dealer.isPresent()) {
					logger.debug("Deleting dealer for user: userId={}, dealerId={}", userId, dealer.get().getId());
					dealerService.deleteDealer(dealer.get().getId());
				}

				// Step 2: Elimina i prodotti rimanenti (se non associati a un dealer)
				List<Product> products = productRepository.findBySeller(user);
				for (Product product : products) {
					try {
						logger.debug("Deleting remaining product: {}", product.getId());
						dealerService.deleteProduct(product.getId());
					} catch (Exception e) {
						logger.warn("Could not delete product {}: {}", product.getId(), e.getMessage());
					}
				}

				// Step 3: Elimina l'utente (le subscription verranno gestite da cascade o constraint)
				userRepository.delete(user);
				logger.info("User deleted by admin: id={}", userId);

			} catch (Exception e) {
				logger.error("Error deleting user: id={}", userId, e);
				throw new RuntimeException("Errore durante l'eliminazione dell'utente: " + e.getMessage(), e);
			}
		}

	    // Recupera tutti i prodotti
	    @PreAuthorize("hasRole('ADMIN')")
	    public List<Product> findAllProducts() {
	        return (List<Product>) productRepository.findAll();
	    }

	    // Recupera tutti i concessionari
	    @PreAuthorize("hasRole('ADMIN')")
	    public List<Dealer> findAllDealers() {
	        return dealerRepository.findAll();
	    }

	    // Recupera tutti gli utenti
	    @PreAuthorize("hasRole('ADMIN')")
	    public List<User> findAllUsers() {
	        return userRepository.findAll();
	    }

	    // Recupera tutti gli abbonamenti
	    @PreAuthorize("hasRole('ADMIN')")
	    public List<Subscription> findAllSubscriptions() {
	        return subscriptionRepository.findAll();
	    }

	    // Trova un abbonamento per ID
	    @PreAuthorize("hasRole('ADMIN')")
	    public Optional<Subscription> findSubscriptionById(Long id) {
	        return subscriptionRepository.findById(id);
	    }

	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public Subscription addSubscription(Subscription subscription) {
	        logger.debug("Admin adding subscription: name={}", subscription.getName());
	        Subscription savedSubscription = subscriptionRepository.save(subscription);
	        logger.info("Subscription added by admin: id={}, name={}, maxFeaturedCars={}",
	                savedSubscription.getId(), savedSubscription.getName(), savedSubscription.getMaxFeaturedCars());
	        return savedSubscription;
	    }

	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public Subscription updateSubscription(Long subscriptionId, Subscription updatedSubscription) {
	        logger.debug("Admin updating subscription: id={}", subscriptionId);

	        Subscription subscription = subscriptionRepository.findById(subscriptionId)
	                .orElseThrow(() -> {
	                    logger.error("Subscription not found: id={}", subscriptionId);
	                    return new IllegalStateException("Abbonamento non trovato");
	                });

	        subscription.setName(updatedSubscription.getName());
	        subscription.setDescription(updatedSubscription.getDescription());
	        subscription.setPrice(updatedSubscription.getPrice());
	        subscription.setDiscount(updatedSubscription.getDiscount());
	        subscription.setDiscountExpiry(updatedSubscription.getDiscountExpiry());
	        subscription.setDurationDays(updatedSubscription.getDurationDays());
	        subscription.setMaxFeaturedCars(updatedSubscription.getMaxFeaturedCars());

	        Subscription savedSubscription = subscriptionRepository.save(subscription);
	        logger.info("Subscription updated by admin: id={}, name={}, maxFeaturedCars={}",
	                savedSubscription.getId(), savedSubscription.getName(), savedSubscription.getMaxFeaturedCars());
	        return savedSubscription;
	    }

	    // Applica uno sconto a tempo
	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public Subscription applyDiscount(Long subscriptionId, Double discount, LocalDate discountExpiry) {
	        logger.debug("Admin applying discount to subscription: id={}, discount={}", subscriptionId, discount);

	        Subscription subscription = subscriptionRepository.findById(subscriptionId)
	                .orElseThrow(() -> {
	                    logger.error("Subscription not found: id={}", subscriptionId);
	                    return new IllegalStateException("Abbonamento non trovato");
	                });

	        subscription.setDiscount(discount);
	        subscription.setDiscountExpiry(discountExpiry);

	        Subscription savedSubscription = subscriptionRepository.save(subscription);
	        logger.info("Discount applied to subscription: id={}, discount={}%", savedSubscription.getId(), discount);
	        return savedSubscription;
	    }

	    // Rimuove un abbonamento
	    @Transactional
	    @PreAuthorize("hasRole('ADMIN')")
	    public void deleteSubscription(Long subscriptionId) {
	        logger.debug("Admin deleting subscription: id={}", subscriptionId);

	        Subscription subscription = subscriptionRepository.findById(subscriptionId)
	                .orElseThrow(() -> {
	                    logger.error("Subscription not found: id={}", subscriptionId);
	                    return new IllegalStateException("Abbonamento non trovato");
	                });

	        subscriptionRepository.delete(subscription);
	        logger.info("Subscription deleted by admin: id={}", subscriptionId);
	    }
	}