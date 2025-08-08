/**
 * Popup Manager - FCF Motors
 * Gestisce tutti i popup/modal presenti nel sito
 */

(function() {
    'use strict';

    console.log('🎯 Popup Manager caricato!');

    // Configurazione globale
    const config = {
        overlayClass: 'modal-overlay',
        modalClass: 'modal',
        popupClass: 'popup',
        showClass: 'show',
        activeClass: 'active',
        closeSelectors: ['.close-modal', '.close-popup', '.cancel-button'],
        escapeKey: true,
        clickOutside: true
    };

    // Inizializza tutti i popup quando il DOM è caricato
    function initializePopups() {
        console.log('🔧 Inizializzazione popup manager...');

        // Trova tutti i tipi di popup/modal
        const allPopups = [
            ...document.querySelectorAll('.modal'),
            ...document.querySelectorAll('.modal-overlay'),
            ...document.querySelectorAll('.popup'),
            ...document.querySelectorAll('.filter-popup'),
            ...document.querySelectorAll('.car-details-popup')
        ];

        console.log(`📋 Trovati ${allPopups.length} popup nel DOM`);

        // Inizializza ogni popup
        allPopups.forEach(popup => initializePopup(popup));

        // Gestione globale dei tasti
        initializeGlobalKeyHandlers();

        // Inizializza pulsanti di apertura
        initializeOpenButtons();

        // Inizializza funzioni specifiche per pagina
        initializePageSpecificFunctions();

        disableAutocomplete();
    }

    function disableAutocomplete() {
    console.log('🔧 Disabilitazione autocomplete...');
    
        // Trova tutti i form nei modal/popup
        const forms = document.querySelectorAll('.modal form, .modal-overlay form, .popup form, .maintenance-form');
        
        forms.forEach(form => {
            form.setAttribute('autocomplete', 'off');
            form.setAttribute('spellcheck', 'false');
            
            // Disabilita autocomplete su tutti gli input del form
            const inputs = form.querySelectorAll('input, textarea, select');
            inputs.forEach(input => {
                input.setAttribute('autocomplete', 'off');
                input.setAttribute('spellcheck', 'false');
                
                // Rimuovi eventuali listener di autocomplete jQuery
                if (typeof $ !== 'undefined' && $.fn.autocomplete) {
                    try {
                        $(input).autocomplete('destroy');
                    } catch (e) {
                        // Ignora errori se autocomplete non è inizializzato
                    }
                }
            });
        });
        
        console.log(`✅ Autocomplete disabilitato su ${forms.length} form`);
    }

    const autocompleteObserver = new MutationObserver(() => {
    disableAutocomplete();
    });

    autocompleteObserver.observe(document.body, {
        childList: true,
        subtree: true
    });

    // Inizializza un singolo popup
    function initializePopup(popup) {
        if (!popup || popup.dataset.popupInitialized) return;

        popup.dataset.popupInitialized = 'true';
        const popupId = popup.id || `popup-${Date.now()}`;

        console.log(`🎨 Inizializzazione popup: ${popupId}`);

        // Aggiungi attributi ARIA
        popup.setAttribute('role', 'dialog');
        popup.setAttribute('aria-modal', 'true');
        popup.setAttribute('aria-hidden', 'true');

        // Trova i pulsanti di chiusura
        const closeButtons = popup.querySelectorAll(config.closeSelectors.join(','));
        
        // Aggiungi event listener per i pulsanti di chiusura
        closeButtons.forEach(button => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                closePopup(popup);
            });
        });

        // Gestione click fuori dal popup
        if (config.clickOutside) {
            popup.addEventListener('click', (event) => {
                const content = popup.querySelector('.modal-content, .popup-content, .filter-popup-content, .car-details-popup-content');
                if (content && !content.contains(event.target)) {
                    closePopup(popup);
                }
            });
        }

        // Impedisci la chiusura quando si clicca sul contenuto
        const content = popup.querySelector('.modal-content, .popup-content, .filter-popup-content, .car-details-popup-content');
        if (content) {
            content.addEventListener('click', (event) => {
                event.stopPropagation();
            });
        }
    }

    // Inizializza i gestori globali dei tasti
    function initializeGlobalKeyHandlers() {
        if (config.escapeKey) {
            document.addEventListener('keydown', (event) => {
                if (event.key === 'Escape') {
                    const openPopup = getTopMostPopup();
                    if (openPopup) {
                        closePopup(openPopup);
                    }
                }
            });
        }
    }

    // Inizializza i pulsanti di apertura
    function initializeOpenButtons() {
        // Pulsanti con data-popup-target
        document.querySelectorAll('[data-popup-target]').forEach(button => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                const targetId = button.dataset.popupTarget;
                const popup = document.getElementById(targetId);
                if (popup) {
                    openPopup(popup);
                }
            });
        });
    }

    // Inizializza funzioni specifiche per pagina
    function initializePageSpecificFunctions() {
        console.log('🔧 Inizializzazione funzioni specifiche per pagina...');

        // === ADMIN MAINTENANCE FUNCTIONS ===
        
        // Funzione per aprire popup modifica prodotto
        window.openEditProductPopup = function(button) {
            console.log('🔧 Apertura popup modifica prodotto');
            
            const popup = document.getElementById('editProductPopup');
            const form = document.getElementById('editProductForm');
            
            if (!popup || !button) {
                console.error('❌ Popup editProductPopup o button non trovati');
                return;
            }
            
            // Popola i campi del form con i dati dal button
            const data = {
                id: button.getAttribute('data-id'),
                description: button.getAttribute('data-description'),
                price: button.getAttribute('data-price'),
                category: button.getAttribute('data-category'),
                brand: button.getAttribute('data-brand'),
                model: button.getAttribute('data-model'),
                mileage: button.getAttribute('data-mileage'),
                year: button.getAttribute('data-year'),
                fuel: button.getAttribute('data-fuel'),
                transmission: button.getAttribute('data-transmission')
            };

            console.log('📋 Dati prodotto:', data);
            
            // Popola i campi
            setFieldValue('editProductId', data.id);
            setFieldValue('editProductDescription', data.description);
            setFieldValue('editProductPrice', data.price);
            setFieldValue('editProductCategory', data.category);
            setFieldValue('editProductBrand', data.brand);
            setFieldValue('editProductModel', data.model);
            setFieldValue('editProductMileage', data.mileage);
            setFieldValue('editProductYear', data.year);
            setFieldValue('editProductFuelType', data.fuel);
            setFieldValue('editProductTransmission', data.transmission);
            
            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/product/${data.id}/edit`;
            }
            
            // Apri il popup
            openPopup(popup);
        };

        // Funzione per aprire popup modifica dealer
        window.openEditDealerPopup = function(button) {
            console.log('🔧 Apertura popup modifica dealer');
            
            const popup = document.getElementById('editDealerPopup');
            const form = document.getElementById('editDealerForm');
            
            if (!popup || !button) {
                console.error('❌ Popup editDealerPopup o button non trovati');
                return;
            }
            
            // Popola i campi del form con i dati dal button
            const data = {
                id: button.getAttribute('data-id'),
                name: button.getAttribute('data-name'),
                description: button.getAttribute('data-description'),
                address: button.getAttribute('data-address'),
                email: button.getAttribute('data-email'),
                phone: button.getAttribute('data-phone')
            };

            console.log('📋 Dati dealer:', data);
            
            // Popola i campi
            setFieldValue('editDealerId', data.id);
            setFieldValue('editDealerName', data.name);
            setFieldValue('editDealerDescription', data.description);
            setFieldValue('editDealerAddress', data.address);
            setFieldValue('editDealerEmail', data.email);
            setFieldValue('editDealerPhone', data.phone);
            
            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/dealer/${data.id}/edit`;
            }
            
            // Apri il popup
            openPopup(popup);
        };

        // Funzione per aprire popup aggiungi subscription
        window.openAddSubscriptionPopup = function() {
            console.log('🔧 Apertura popup aggiungi subscription');
            
            const popup = document.getElementById('addSubscriptionPopup');
            if (!popup) {
                console.error('❌ Popup addSubscriptionPopup non trovato');
                return;
            }
            
            // Reset del form
            const form = popup.querySelector('form');
            if (form) {
                form.reset();
            }
            
            // Apri il popup
            openPopup(popup);
        };

        // Funzione per aprire popup modifica subscription
        window.openEditSubscriptionPopup = function(button) {
            console.log('🔧 Apertura popup modifica subscription');
            
            const popup = document.getElementById('editSubscriptionPopup');
            const form = document.getElementById('editSubscriptionForm');
            
            if (!popup || !button) {
                console.error('❌ Popup editSubscriptionPopup o button non trovati');
                return;
            }
            
            // Popola i campi del form con i dati dal button
            const data = {
                id: button.getAttribute('data-id'),
                name: button.getAttribute('data-name'),
                description: button.getAttribute('data-description'),
                price: button.getAttribute('data-price'),
                discount: button.getAttribute('data-discount'),
                expiry: button.getAttribute('data-expiry'),
                duration: button.getAttribute('data-duration'),
                maxCars: button.getAttribute('data-maxcars')
            };

            console.log('📋 Dati subscription:', data);
            
            // Popola i campi
            setFieldValue('editSubscriptionId', data.id);
            setFieldValue('editSubscriptionName', data.name);
            setFieldValue('editSubscriptionDescription', data.description);
            setFieldValue('editSubscriptionPrice', data.price);
            setFieldValue('editSubscriptionDiscount', data.discount);
            setFieldValue('editSubscriptionDiscountExpiry', data.expiry);
            setFieldValue('editSubscriptionDuration', data.duration);
            setFieldValue('editSubscriptionMaxCars', data.maxCars);
            
            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/subscription/${data.id}/edit`;
            }
            
            // Apri il popup
            openPopup(popup);
        };

        // Funzione per aprire popup sconto
        window.openDiscountPopup = function(button) {
            console.log('🔧 Apertura popup sconto');
            
            const popup = document.getElementById('discountPopup');
            const form = document.getElementById('discountForm');
            
            if (!popup || !button) {
                console.error('❌ Popup discountPopup o button non trovati');
                return;
            }
            
            // Popola i dati
            const id = button.getAttribute('data-id');
            const name = button.getAttribute('data-name');
            
            console.log('📋 Dati discount:', { id, name });
            
            const nameElement = document.getElementById('discountSubscriptionName');
            if (nameElement) {
                nameElement.textContent = name || 'Abbonamento';
            }
            
            // Imposta la data minima per la scadenza (oggi)
            const today = new Date().toISOString().split('T')[0];
            const expiryField = document.getElementById('discountExpiry');
            if (expiryField) {
                expiryField.min = today;
            }
            
            // Reset del form
            if (form) {
                form.reset();
                // Imposta l'azione del form
                if (id) {
                    form.action = `/admin/subscription/${id}/apply-discount`;
                }
            }
            
            // Apri il popup
            openPopup(popup);
        };

        // === PRODUCTS PAGE FUNCTIONS ===
        
        window.openFilterPopup = function() {
            console.log('🔧 Apertura popup filtri');
            const popup = document.getElementById('filterPopup');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Popup filterPopup non trovato');
            }
        };

        window.closeFilterPopup = function() {
            console.log('🔧 Chiusura popup filtri');
            const popup = document.getElementById('filterPopup');
            if (popup) {
                closePopup(popup);
            }
        };

        // === ACCOUNT PAGE FUNCTIONS ===
        
        window.openEditModal = function() {
            console.log('🔧 Apertura modal modifica account');
            const popup = document.getElementById('editAccountModal');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Modal editAccountModal non trovato');
            }
        };

        window.closeEditModal = function() {
            console.log('🔧 Chiusura modal modifica account');
            const popup = document.getElementById('editAccountModal');
            if (popup) {
                closePopup(popup);
            }
        };

        window.openDeleteModal = function() {
            console.log('🔧 Apertura modal elimina account');
            const popup = document.getElementById('deleteAccountModal');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Modal deleteAccountModal non trovato');
            }
        };

        window.closeDeleteModal = function() {
            console.log('🔧 Chiusura modal elimina account');
            const popup = document.getElementById('deleteAccountModal');
            if (popup) {
                closePopup(popup);
            }
        };

        // === MANAGE DEALER FUNCTIONS ===

        // Funzioni per aprire i modal in manage_dealer.html
        window.showModal = function(modal) {
            if (typeof modal === 'string') {
                modal = document.getElementById(modal);
            }
            if (modal) {
                console.log('🔧 Apertura modal manage dealer:', modal.id);
                openPopup(modal);
            } else {
                console.error('❌ Modal non trovato per showModal');
            }
        };

        window.hideModal = function(modal) {
            if (typeof modal === 'string') {
                modal = document.getElementById(modal);
            }
            if (modal) {
                console.log('🔧 Chiusura modal manage dealer:', modal.id);
                closePopup(modal);
            } else {
                console.error('❌ Modal non trovato per hideModal');
            }
        };

        // Funzione per aprire modal modifica dealer
        window.openEditDealerModal = function() {
            console.log('🔧 Apertura modal modifica dealer');
            
            const modal = document.getElementById('edit-dealer-modal');
            if (!modal) {
                console.error('❌ Modal edit-dealer-modal non trovato');
                return;
            }
            
            console.log('🔍 Debug: Ricerca dati dealer nel DOM...');
            
            // Strategia 1: Cerca nei span con class "value" all'interno di .info-item
            const infoItems = document.querySelectorAll('.info-item');
            console.log(`📋 Trovati ${infoItems.length} elementi .info-item`);
            
            let dealerData = {
                name: '',
                description: '',
                address: '',
                phone: '',
                email: ''
            };
            
            // Estrai i dati dalle info-item
            infoItems.forEach((item, index) => {
                const valueElement = item.querySelector('.value');
                const labelElement = item.querySelector('.label');
                const value = valueElement ? valueElement.textContent.trim() : '';
                const label = labelElement ? labelElement.textContent.trim().toLowerCase() : '';
                
                console.log(`📄 Info item ${index}: Label="${label}", Value="${value}"`);
                
                // Mappa i valori in base al label o alla posizione
                if (label.includes('nome') || index === 0) {
                    dealerData.name = value;
                } else if (label.includes('descrizione') || index === 1) {
                    dealerData.description = value;
                } else if (label.includes('indirizzo') || label.includes('address') || index === 2) {
                    dealerData.address = value;
                } else if (label.includes('telefono') || label.includes('phone') || index === 3) {
                    dealerData.phone = value;
                } else if (label.includes('email') || index === 4) {
                    dealerData.email = value;
                }
            });
            
            console.log('📋 Dati estratti:', dealerData);
            
            // Se non ha trovato dati con la strategia 1, prova con selettori alternativi
            if (!dealerData.name && !dealerData.email) {
                console.log('⚠️ Strategia 1 fallita, tentativo strategia 2...');
                
                // Strategia 2: Cerca con selettori più specifici
                const nameElement = document.querySelector('[data-dealer-name], .dealer-name, .dealership-name');
                const emailElement = document.querySelector('[data-dealer-email], .dealer-email');
                const phoneElement = document.querySelector('[data-dealer-phone], .dealer-phone');
                const addressElement = document.querySelector('[data-dealer-address], .dealer-address');
                const descElement = document.querySelector('[data-dealer-description], .dealer-description');
                
                if (nameElement) dealerData.name = nameElement.textContent.trim();
                if (emailElement) dealerData.email = emailElement.textContent.trim();
                if (phoneElement) dealerData.phone = phoneElement.textContent.trim();
                if (addressElement) dealerData.address = addressElement.textContent.trim();
                if (descElement) dealerData.description = descElement.textContent.trim();
                
                console.log('📋 Dati estratti (strategia 2):', dealerData);
            }
            
            // Popola i campi del form
            const fields = {
                'edit-dealership-name': dealerData.name,
                'edit-dealership-description': dealerData.description,
                'edit-dealership-address': dealerData.address,
                'edit-dealership-phone': dealerData.phone,
                'edit-dealership-email': dealerData.email
            };
            
            console.log('🔧 Popolamento campi form...');
            
            Object.entries(fields).forEach(([fieldId, value]) => {
                const field = document.getElementById(fieldId);
                
                if (field) {
                    // Solo popola se il valore non è un placeholder text
                    const shouldPopulate = value && 
                        !value.includes('Nome Concessionario') && 
                        !value.includes('Nessuna descrizione') && 
                        !value.includes('non disponibile') && 
                        !value.includes('N/A') &&
                        value.trim() !== '';
                    
                    if (shouldPopulate) {
                        if (field.tagName.toLowerCase() === 'textarea') {
                            field.textContent = value;
                        } else {
                            field.value = value;
                        }
                        console.log(`✅ Campo ${fieldId} popolato con: "${value}"`);
                    } else {
                        console.log(`⏭️ Campo ${fieldId} saltato (valore placeholder o vuoto): "${value}"`);
                    }
                } else {
                    console.warn(`⚠️ Campo ${fieldId} non trovato nel DOM`);
                }
            });
            
            // Se nessun dato è stato popolato, usa i valori Thymeleaf già presenti
            const populatedFields = Object.keys(fields).filter(fieldId => {
                const field = document.getElementById(fieldId);
                return field && field.value && field.value.trim() !== '';
            });
            
            if (populatedFields.length === 0) {
                console.log('ℹ️ Nessun dato popolato via JavaScript, i campi useranno i valori Thymeleaf già presenti');
            } else {
                console.log(`✅ ${populatedFields.length} campi popolati con successo`);
            }
            
            // Apri il popup
            openPopup(modal);
            
            console.log('🎯 Modal aperto, verifica che i campi siano popolati correttamente');
        };

        // Funzione per aprire modal aggiungi auto
        window.openAddCarModal = function() {
            console.log('🔧 Apertura modal aggiungi auto');
            const modal = document.getElementById('add-car-modal');
            if (modal) {
                // Reset del form
                const form = document.getElementById('dealer-add-car-form');
                if (form) {
                    form.reset();
                    
                    // Reset del preview immagini
                    const previewContainer = document.getElementById('car-image-preview');
                    if (previewContainer) {
                        previewContainer.innerHTML = `
                            <div class="image-item active">
                                <div class="modal-image-placeholder" style="display: flex; align-items: center; justify-content: center; height: 200px; background: rgba(255,255,255,0.1); border-radius: 8px;">
                                    <div style="text-align: center; color: rgba(255,255,255,0.7);">
                                        <i class="fas fa-car" style="font-size: 2rem; margin-bottom: 10px; display: block;"></i>
                                        Seleziona immagini per vedere l'anteprima
                                    </div>
                                </div>
                            </div>
                            <div class="carousel-nav" style="display: none;">
                                <button class="carousel-btn prev" type="button"><i class="fas fa-chevron-left"></i></button>
                                <button class="carousel-btn next" type="button"><i class="fas fa-chevron-right"></i></button>
                            </div>
                            <div class="carousel-dots" style="display: none;"></div>
                        `;
                    }
                    
                    // Reset checkbox evidenza
                    const featuredCheckbox = document.getElementById('dealer-car-highlighted');
                    const durationField = document.getElementById('dealer-car-feature-duration-field');
                    if (featuredCheckbox) {
                        featuredCheckbox.checked = false;
                    }
                    if (durationField) {
                        durationField.style.display = 'none';
                    }
                }
                
                openPopup(modal);
            } else {
                console.error('❌ Modal add-car-modal non trovato');
            }
        };

        // Funzione per chiudere modal specifico
        window.closeModal = function(modalId) {
            console.log('🔧 Chiusura modal tramite closeModal:', modalId);
            const modal = typeof modalId === 'string' ? document.getElementById(modalId) : modalId;
            if (modal) {
                closePopup(modal);
            } else {
                console.error(`❌ Modal ${modalId} non trovato per closeModal`);
            }
        };

        // === FUNZIONI LEGACY PER COMPATIBILITÀ ===
        
        window.closePopup = function(popupId) {
            const popup = typeof popupId === 'string' ? document.getElementById(popupId) : popupId;
            if (popup) {
                closePopup(popup);
            } else {
                console.error(`❌ Popup ${popupId} non trovato per chiusura`);
            }
        };
        
        window.openPopup = function(popupId, options = {}) {
            const popup = typeof popupId === 'string' ? document.getElementById(popupId) : popupId;
            if (popup) {
                openPopup(popup, options);
            } else {
                console.error(`❌ Popup ${popupId} non trovato per apertura`);
            }
        };
    }

    // Utility per impostare valore nei campi
    function setFieldValue(fieldId, value) {
        const field = document.getElementById(fieldId);
        if (field && value !== null && value !== undefined) {
            field.value = value;
            console.log(`✅ Campo ${fieldId} impostato a: ${value}`);
        } else if (!field) {
            console.warn(`⚠️ Campo ${fieldId} non trovato nel DOM`);
        } else {
            console.log(`ℹ️ Campo ${fieldId}: valore vuoto o null`);
        }
    }

    // Apre un popup
    function openPopup(popup, options = {}) {
        if (!popup) return;

        console.log(`🟢 Apertura popup: ${popup.id || 'unnamed'}`);

        // Chiudi altri popup se specificato
        if (options.closeOthers !== false) {
            closeAllPopups();
        }

        // Mostra il popup
        popup.classList.add(config.showClass);
        popup.classList.add(config.activeClass);
        popup.setAttribute('aria-hidden', 'false');

        // Blocca lo scroll del body
        document.body.style.overflow = 'hidden';

        // Focus management
        const focusableElements = popup.querySelectorAll(
            'input, button, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length > 0) {
            setTimeout(() => focusableElements[0].focus(), 100);
        }

        // Gestione trappola del focus
        initializeFocusTrap(popup, focusableElements);

        // Callback personalizzata
        if (options.onOpen && typeof options.onOpen === 'function') {
            options.onOpen(popup);
        }

        // Evento personalizzato
        popup.dispatchEvent(new CustomEvent('popup:open', { detail: { popup } }));
    }

    // Chiude un popup
    function closePopup(popup, options = {}) {
        if (!popup) return;

        console.log(`🔴 Chiusura popup: ${popup.id || 'unnamed'}`);

        // Nascondi il popup
        popup.classList.remove(config.showClass);
        popup.classList.remove(config.activeClass);
        popup.setAttribute('aria-hidden', 'true');

        // Ripristina lo scroll se non ci sono altri popup aperti
        setTimeout(() => {
            if (!getTopMostPopup()) {
                document.body.style.overflow = '';
            }
        }, 100);

        // Reset form se presente
        const forms = popup.querySelectorAll('form');
        forms.forEach(form => {
            if (options.resetForm !== false) {
                form.reset();
            }
        });

        // Callback personalizzata
        if (options.onClose && typeof options.onClose === 'function') {
            options.onClose(popup);
        }

        // Evento personalizzato
        popup.dispatchEvent(new CustomEvent('popup:close', { detail: { popup } }));
    }

    // Chiude tutti i popup
    function closeAllPopups() {
        const openPopups = document.querySelectorAll(`.${config.showClass}`);
        openPopups.forEach(popup => closePopup(popup));
    }

    // Ottiene il popup più in alto (z-index)
    function getTopMostPopup() {
        const openPopups = Array.from(document.querySelectorAll(`.${config.showClass}`));
        if (openPopups.length === 0) return null;
        
        return openPopups.reduce((topmost, current) => {
            const currentZ = parseInt(getComputedStyle(current).zIndex) || 0;
            const topmostZ = parseInt(getComputedStyle(topmost).zIndex) || 0;
            return currentZ > topmostZ ? current : topmost;
        });
    }

    // Inizializza la trappola del focus
    function initializeFocusTrap(popup, focusableElements) {
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        popup.addEventListener('keydown', (event) => {
            if (event.key === 'Tab') {
                if (event.shiftKey) {
                    // Shift + Tab
                    if (document.activeElement === firstElement) {
                        event.preventDefault();
                        lastElement.focus();
                    }
                } else {
                    // Tab
                    if (document.activeElement === lastElement) {
                        event.preventDefault();
                        firstElement.focus();
                    }
                }
            }
        });
    }

    // Utilità per i caroselli nei popup
    window.initializePopupCarousel = function(popup) {
        if (!popup) return;

        const carousels = popup.querySelectorAll('.carousel, .product-carousel, .image-carousel');
        
        carousels.forEach(carousel => {
            const items = carousel.querySelectorAll('.carousel-item, .product-image-item, .image-item');
            const dots = carousel.querySelectorAll('.carousel-dot, .dot');
            const prevBtn = carousel.querySelector('.carousel-btn.prev, .carousel-control-prev');
            const nextBtn = carousel.querySelector('.carousel-btn.next, .carousel-control-next');
            const thumbnails = carousel.querySelectorAll('.carousel-thumbnails img');

            if (items.length <= 1) return;

            let currentIndex = 0;

            const updateCarousel = () => {
                items.forEach((item, index) => {
                    item.classList.toggle('active', index === currentIndex);
                });
                dots.forEach((dot, index) => {
                    dot.classList.toggle('active', index === currentIndex);
                });
                thumbnails.forEach((thumb, index) => {
                    thumb.classList.toggle('active', index === currentIndex);
                });
            };

            if (prevBtn) {
                prevBtn.addEventListener('click', (event) => {
                    event.stopPropagation();
                    currentIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
                    updateCarousel();
                });
            }

            if (nextBtn) {
                nextBtn.addEventListener('click', (event) => {
                    event.stopPropagation();
                    currentIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0;
                    updateCarousel();
                });
            }

            dots.forEach((dot, index) => {
                dot.addEventListener('click', (event) => {
                    event.stopPropagation();
                    currentIndex = index;
                    updateCarousel();
                });
            });

            thumbnails.forEach((thumb, index) => {
                thumb.addEventListener('click', (event) => {
                    event.stopPropagation();
                    currentIndex = index;
                    updateCarousel();
                });
            });

            updateCarousel();
        });
    };

    // API pubblica
    window.PopupManager = {
        open: openPopup,
        close: closePopup,
        closeAll: closeAllPopups,
        getTopMost: getTopMostPopup,
        initializeCarousel: window.initializePopupCarousel
    };

    // Inizializza quando il DOM è pronto
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializePopups);
    } else {
        initializePopups();
    }

    // Re-inizializza se vengono aggiunti nuovi popup dinamicamente
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            mutation.addedNodes.forEach((node) => {
                if (node.nodeType === 1) { // Element node
                    const newPopups = [
                        ...node.classList && (
                            node.classList.contains('modal') ||
                            node.classList.contains('popup') ||
                            node.classList.contains('modal-overlay')
                        ) ? [node] : [],
                        ...node.querySelectorAll ? node.querySelectorAll('.modal, .popup, .modal-overlay') : []
                    ];
                    
                    newPopups.forEach(popup => initializePopup(popup));
                }
            });
        });
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });

})();