/**
 * ===============================================
 * HOMEPAGE MANAGEMENT - FCF MOTORS
 * ===============================================
 * 
 * Sistema di gestione per la homepage dell'applicazione FCF Motors.
 * Gestisce il carosello delle categorie, la ricerca e gli effetti UI.
 * 
 * Funzionalità principali:
 * - Carosello responsive delle categorie auto
 * - Sistema di navigazione con pulsanti e dots
 * - Validazione e gestione della ricerca
 * - Toast notifications per feedback utente
 * - Effetti UI per migliorare l'esperienza utente
 * 
 * @author FCF Motors Team
 * @version 2.1.0
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log('🏠 homepage.js: Inizializzazione homepage');

    /**
     * ============================================
     * CONFIGURAZIONE E VARIABILI GLOBALI
     * ============================================
     */

    // Elementi DOM del carosello
    const carouselElements = {
        carousel: document.querySelector('.carousel'),
        items: document.querySelectorAll('.category-item'),
        prevBtn: document.querySelector('.carousel-btn.prev'),
        nextBtn: document.querySelector('.carousel-btn.next'),
        dotsContainer: document.querySelector('.dots')
    };

    // Stato del carosello
    let carouselState = {
        currentIndex: 0,
        itemsPerSlide: 3,
        totalItems: carouselElements.items.length
    };

    // Breakpoints per responsive design
    const BREAKPOINTS = {
        mobile: 480,
        tablet: 768
    };

    /**
     * ============================================
     * GESTIONE CAROSELLO - RESPONSIVENESS
     * ============================================
     */

    /**
     * Calcola e aggiorna il numero di elementi per slide
     * in base alla larghezza della finestra
     */
    function updateResponsiveLayout() {
        const windowWidth = window.innerWidth;

        // Determina items per slide in base ai breakpoints
        if (windowWidth <= BREAKPOINTS.mobile) {
            carouselState.itemsPerSlide = 1;
        } else if (windowWidth <= BREAKPOINTS.tablet) {
            carouselState.itemsPerSlide = 2;
        } else {
            carouselState.itemsPerSlide = 3;
        }

        // Imposta le variabili CSS per il numero totale di elementi e items per slide
        if (carouselElements.carousel) {
            carouselElements.carousel.style.setProperty('--total-items', carouselState.totalItems);
            carouselElements.carousel.style.setProperty('--items-per-slide', carouselState.itemsPerSlide);
        }

        // Calcola nuovo indice valido per evitare overflow
        const maxIndex = Math.ceil(carouselState.totalItems / carouselState.itemsPerSlide) - 1;
        carouselState.currentIndex = Math.min(carouselState.currentIndex, maxIndex);

        // Aggiorna UI
        updateNavigationDots();
        updateCarouselPosition();

        console.log(`📱 Responsive update: ${carouselState.itemsPerSlide} items per slide`);
    }

    /**
     * ============================================
     * GESTIONE CAROSELLO - NAVIGAZIONE
     * ============================================
     */

    /**
     * Crea o aggiorna i dots di navigazione del carosello
     */
    function updateNavigationDots() {
        if (!carouselElements.dotsContainer) return;

        // Calcola numero totale di slide
        const totalSlides = Math.ceil(carouselState.totalItems / carouselState.itemsPerSlide);

        // Pulisce container esistente
        carouselElements.dotsContainer.innerHTML = '';

        // Crea nuovo dot per ogni slide
        for (let i = 0; i < totalSlides; i++) {
            const dot = createNavigationDot(i);
            carouselElements.dotsContainer.appendChild(dot);
        }

        console.log(`🔘 Creati ${totalSlides} navigation dots`);
    }

    /**
     * Crea un singolo dot di navigazione
     * @param {number} index - Indice del dot
     * @returns {HTMLElement} Elemento dot creato
     */
    function createNavigationDot(index) {
        const dot = document.createElement('span');
        dot.classList.add('dot');

        // Marca come attivo se è la slide corrente
        if (index === carouselState.currentIndex) {
            dot.classList.add('active');
        }

        // Aggiunge event listener per navigazione
        dot.addEventListener('click', () => navigateToSlide(index));

        // Attributi per accessibilità
        dot.setAttribute('role', 'button');
        dot.setAttribute('aria-label', `Vai alla slide ${index + 1}`);
        dot.setAttribute('tabindex', '0');

        return dot;
    }

    /**
     * ============================================
     * GESTIONE CAROSELLO - MOVIMENTO
     * ============================================
     */

    /**
    * Aggiorna la posizione visuale del carosello
    */
    function updateCarouselPosition() {
        if (!carouselElements.carousel) return;

        // Calcola i limiti validi per l'indice
        const totalSlides = Math.ceil(carouselState.totalItems / carouselState.itemsPerSlide);
        carouselState.currentIndex = Math.max(0, Math.min(carouselState.currentIndex, totalSlides - 1));

        // FORMULA CORRETTA per la traslazione (muove di 1/3 della larghezza per 3 item)
        const translateX = -(carouselState.currentIndex * (100 / carouselState.itemsPerSlide));

        // Applica trasformazione CSS
        carouselElements.carousel.style.transform = `translateX(${translateX}%)`;

        // Aggiorna stato visuale dei dots
        updateDotsActiveState();

        console.log(`🔄 Carosello spostato a slide ${carouselState.currentIndex + 1}`);
    }

    /**
     * Aggiorna lo stato attivo/inattivo dei navigation dots
     */
    function updateDotsActiveState() {
        const dots = carouselElements.dotsContainer?.querySelectorAll('.dot');
        if (!dots) return;

        dots.forEach((dot, index) => {
            const isActive = index === carouselState.currentIndex;
            dot.classList.toggle('active', isActive);
            dot.setAttribute('aria-pressed', isActive.toString());
        });
    }

    /**
     * Naviga a una slide specifica con validazione
     * @param {number} targetIndex - Indice della slide target
     */
    function navigateToSlide(targetIndex) {
        const totalSlides = Math.ceil(carouselState.totalItems / carouselState.itemsPerSlide);

        // Gestione indici con wrap-around
        if (targetIndex >= totalSlides) {
            targetIndex = 0;  // Vai alla prima slide
        } else if (targetIndex < 0) {
            targetIndex = totalSlides - 1;  // Vai all'ultima slide
        }

        carouselState.currentIndex = targetIndex;
        updateCarouselPosition();
    }

    /**
     * ============================================
     * EVENT LISTENERS - NAVIGAZIONE
     * ============================================
     */

    /**
     * Configura gli event listeners per i controlli del carosello
     */
    function setupCarouselControls() {
        // Pulsante precedente
        carouselElements.prevBtn?.addEventListener('click', () => {
            console.log('👈 Click pulsante precedente');
            navigateToSlide(carouselState.currentIndex - 1);
        });

        // Pulsante successivo
        carouselElements.nextBtn?.addEventListener('click', () => {
            console.log('👉 Click pulsante successivo');
            navigateToSlide(carouselState.currentIndex + 1);
        });

        // Gestione ridimensionamento finestra
        window.addEventListener('resize', debounce(updateResponsiveLayout, 250));

        console.log('🎛️ Controlli carosello configurati');
    }

    /**
     * ============================================
     * VALIDAZIONE E GESTIONE RICERCA
     * ============================================
     */

    /**
     * Configura la funzionalità di ricerca e i suoi effetti
     */
    function setupSearchFunctionality() {
        const searchInput = document.getElementById('search-input');
        const searchForm = document.querySelector('.new-search-form');
        const autocompleteResults = document.getElementById('autocomplete-results');
        let debounceTimer;

        if (searchInput && searchForm) {
            // Gestione submit del form
            searchForm.addEventListener('submit', function(e) {
                e.preventDefault();
                const query = searchInput.value.trim();
                if (query) {
                    window.location.href = `/products?query=${encodeURIComponent(query)}`;
                }
            });

            // Gestione autocompletamento
            searchInput.addEventListener('input', function() {
                clearTimeout(debounceTimer);
                const query = this.value.trim();
                
                if (query.length < 2) {
                    autocompleteResults.innerHTML = '';
                    autocompleteResults.style.display = 'none';
                    return;
                }

                debounceTimer = setTimeout(() => {
                    fetch(`/products/api/search?query=${encodeURIComponent(query)}`)
                        .then(response => response.json())
                        .then(data => {
                            autocompleteResults.innerHTML = '';
                            
                            if (data.length > 0) {
                                data.forEach(product => {
                                    const div = document.createElement('div');
                                    div.className = 'autocomplete-item';
                                    div.innerHTML = `
                                        <div class="product-info">
                                            <div class="product-main">
                                                <span class="product-brand">${product.brand || ''}</span>
                                                <span class="product-model">${product.model || ''}</span>
                                            </div>
                                            <span class="product-price">€${product.price ? product.price.toLocaleString() : 'N/D'}</span>
                                        </div>
                                    `;
                                    div.addEventListener('click', () => {
                                        window.location.href = `/products/${product.id}`;
                                    });
                                    autocompleteResults.appendChild(div);
                                });
                                autocompleteResults.style.display = 'block';
                            } else {
                                autocompleteResults.style.display = 'none';
                            }
                        })
                        .catch(error => {
                            console.error('Errore nella ricerca:', error);
                        });
                }, 300);
            });

            // Chiudi i risultati quando si clicca fuori
            document.addEventListener('click', function(e) {
                if (!searchInput.contains(e.target) && !autocompleteResults.contains(e.target)) {
                    autocompleteResults.style.display = 'none';
                }
            });
        }

        console.log('🔍 Funzionalità di ricerca configurata');
    }

    /**
     * Gestisce il focus sull'input di ricerca
     * @param {FocusEvent} event - Evento focus
     */
    function handleSearchFocus(event) {
        const inputContainer = event.target.parentElement.parentElement;
        if (inputContainer) {
            inputContainer.style.transform = 'scale(1.05)';
            inputContainer.style.transition = 'transform 0.3s ease';
        }
    }

    /**
     * Gestisce la perdita di focus sull'input di ricerca
     * @param {FocusEvent} event - Evento blur
     */
    function handleSearchBlur(event) {
        const inputContainer = event.target.parentElement.parentElement;
        if (inputContainer) {
            inputContainer.style.transform = 'scale(1)';
        }
    }

    /**
     * Valida l'input di ricerca prima dell'invio
     * @returns {boolean} True se la validazione passa
     */
    window.validateSearch = function () {
        const searchInput = document.getElementById('search-input');
        if (!searchInput) return false;

        const searchTerm = searchInput.value.trim();
        const minLength = 3;

        if (searchTerm.length < minLength) {
            showToast(`Inserisci almeno ${minLength} caratteri per la ricerca.`, true);
            return false;
        }

        console.log(`🔍 Ricerca validata: "${searchTerm}"`);
        return true;
    };

    /**
     * ============================================
     * SISTEMA TOAST NOTIFICATIONS
     * ============================================
     */

    /**
     * Mostra un messaggio toast all'utente
     * @param {string} message - Messaggio da mostrare
     * @param {boolean} isError - Se true, mostra come errore
     */
    function showToast(message, isError = false) {
        let toast = document.getElementById('toast');

        // Crea elemento toast se non esiste
        if (!toast) {
            toast = createToastElement();
            document.body.appendChild(toast);
        }

        // Configura contenuto e stile
        toast.textContent = message;
        toast.className = 'toast show';
        toast.style.backgroundColor = isError ? '#DC3545' : '#28A745';

        // Auto-nascondimento dopo 3 secondi
        setTimeout(() => {
            toast.className = 'toast';
        }, 3000);

        console.log(`📢 Toast mostrato: ${message} (error: ${isError})`);
    }

    /**
     * Crea l'elemento DOM per le notifiche toast
     * @returns {HTMLElement} Elemento toast creato
     */
    function createToastElement() {
        const toast = document.createElement('div');
        toast.id = 'toast';
        toast.className = 'toast';
        return toast;
    }

    /**
     * ============================================
     * VALIDAZIONE CONTENUTO E DEBUG
     * ============================================
     */

    /**
     * Verifica il caricamento corretto delle immagini di categoria
     */
    function validateCategoryImages() {
        let missingImages = 0;

        carouselElements.items.forEach((item, index) => {
            const bgImage = item.style.backgroundImage;
            const categoryName = item.querySelector('p')?.textContent || `Categoria ${index + 1}`;

            if (!bgImage || bgImage === 'url("")') {
                console.warn(`⚠️ Immagine mancante per: ${categoryName}`);

                // Imposta colore di fallback
                item.style.backgroundColor = '#cccccc';
                item.style.border = '2px dashed #999';
                missingImages++;
            }
        });

        if (missingImages === 0) {
            console.log('✅ Tutte le immagini delle categorie caricate correttamente');
        } else {
            console.warn(`⚠️ ${missingImages} immagini mancanti rilevate`);
        }
    }

    /**
     * ============================================
     * UTILITÀ E FUNZIONI HELPER
     * ============================================
     */

    /**
     * Implementazione debounce per ottimizzare eventi di resize
     * @param {Function} func - Funzione da eseguire con debounce
     * @param {number} wait - Millisecondi di attesa
     * @returns {Function} Funzione con debounce applicato
     */
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func.apply(this, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * ============================================
     * INIZIALIZZAZIONE PRINCIPALE
     * ============================================
     */

    /**
     * Inizializza tutti i componenti della homepage
     */
    function initializeHomepage() {
        console.log('🚀 Inizializzazione componenti homepage...');

        // Verifica disponibilità elementi essenziali
        if (!carouselElements.carousel || carouselElements.items.length === 0) {
            console.warn('⚠️ Elementi carosello non trovati - skip inizializzazione');
            return;
        }

        // Inizializza componenti
        updateResponsiveLayout();
        setupCarouselControls();
        setupSearchFunctionality();
        validateCategoryImages();

        console.log('✅ Homepage inizializzata correttamente');
        console.log(`📊 Stats: ${carouselState.totalItems} categorie, ${carouselState.itemsPerSlide} per slide`);
    }

    // Avvia inizializzazione
    initializeHomepage();

    // Espone funzione toast globalmente per utilizzo da altre parti
    window.showToast = showToast;

});