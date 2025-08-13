/**
 * ===============================================
 * POPUP MANAGER - FCF MOTORS
 * ===============================================
 * 
 * Sistema di gestione completo per tutti i popup/modal presenti
 * nell'applicazione FCF Motors.
 * 
 * Funzionalità principali:
 * - Gestione apertura/chiusura popup e modal
 * - Supporto per multiple tipologie (modal, popup, overlay)
 * - Sistema di focus trap per accessibilità
 * - Gestione tastiera (Tab, Escape, Enter)
 * - Chiusura automatica al click esterno
 * - Disabilitazione autocomplete automatica
 * - Sistema di caroselli integrato per immagini
 * - API pubblica per integrazione con altre parti del codice
 * 
 * @author FCF Motors Team
 * @version 2.3.0
 */

(function () {
    'use strict';
    console.log('🎯 Popup Manager caricato!');

    /**
     * ============================================
     * CONFIGURAZIONE GLOBALE
     * ============================================
     */
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

    /**
     * ============================================
     * INIZIALIZZAZIONE PRINCIPALE
     * ============================================
     */

    /**
     * Inizializza tutti i popup presenti nel DOM
     * Eseguito automaticamente al caricamento della pagina
     */
    function initializePopups() {
        console.log('🔧 Inizializzazione popup manager...');

        // Trova tutti i tipi di popup/modal supportati
        const allPopups = [
            ...document.querySelectorAll('.modal'),
            ...document.querySelectorAll('.modal-overlay'),
            ...document.querySelectorAll('.popup'),
            ...document.querySelectorAll('.filter-popup'),
            ...document.querySelectorAll('.car-details-popup')
        ];

        console.log(`📋 Trovati ${allPopups.length} popup nel DOM`);

        // Inizializza ogni popup individualmente
        allPopups.forEach(popup => initializePopup(popup));

        // Configura gestori globali
        initializeGlobalKeyHandlers();
        initializeOpenButtons();
        initializePageSpecificFunctions();

        // Sistema anti-autocomplete
        disableAutocomplete();
    }

    /**
     * ============================================
     * SISTEMA ANTI-AUTOCOMPLETE
     * ============================================
     */

    /**
     * Disabilita completamente l'autocomplete su tutti i form
     * nei popup per evitare interferenze con jQuery UI
     */
    function disableAutocomplete() {
        console.log('🔧 Disabilitazione autocomplete...');

        // Trova tutti i form nei modal/popup
        const forms = document.querySelectorAll('.modal form, .modal-overlay form, .popup form, .maintenance-form');

        forms.forEach(form => {
            // Disabilita autocomplete a livello di form
            form.setAttribute('autocomplete', 'off');
            form.setAttribute('spellcheck', 'false');

            // Disabilita autocomplete su tutti gli input del form
            const inputs = form.querySelectorAll('input, textarea, select');
            inputs.forEach(input => {
                input.setAttribute('autocomplete', 'off');
                input.setAttribute('spellcheck', 'false');

                // Rimuovi eventuali listener di autocomplete jQuery se presente
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

    /**
     * Observer per disabilitare autocomplete su elementi aggiunti dinamicamente
     */
    const autocompleteObserver = new MutationObserver(() => {
        disableAutocomplete();
    });

    autocompleteObserver.observe(document.body, {
        childList: true,
        subtree: true
    });

    /**
     * ============================================
     * INIZIALIZZAZIONE POPUP INDIVIDUALI
     * ============================================
     */

    /**
     * Inizializza un singolo popup con tutti i suoi event listeners
     * @param {HTMLElement} popup - Elemento popup da inizializzare
     */
    function initializePopup(popup) {
        if (!popup || popup.dataset.popupInitialized) return;

        // Marca come inizializzato per evitare doppia inizializzazione
        popup.dataset.popupInitialized = 'true';
        const popupId = popup.id || `popup-${Date.now()}`;

        console.log(`🎨 Inizializzazione popup: ${popupId}`);

        // Configura attributi ARIA per accessibilità
        setupPopupARIA(popup);

        // Configura pulsanti di chiusura
        setupCloseButtons(popup);

        // Configura click esterno per chiusura
        setupClickOutsideHandler(popup);

        // Impedisci chiusura accidentale dal contenuto
        setupContentClickProtection(popup);
    }

    /**
     * Configura gli attributi ARIA per l'accessibilità
     * @param {HTMLElement} popup - Elemento popup
     */
    function setupPopupARIA(popup) {
        popup.setAttribute('role', 'dialog');
        popup.setAttribute('aria-modal', 'true');
        popup.setAttribute('aria-hidden', 'true');
    }

    /**
     * Configura i pulsanti di chiusura del popup
     * @param {HTMLElement} popup - Elemento popup
     */
    function setupCloseButtons(popup) {
        const closeButtons = popup.querySelectorAll(config.closeSelectors.join(','));

        closeButtons.forEach(button => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                closePopup(popup);
            });
        });
    }

    /**
     * Configura la chiusura al click esterno al popup
     * @param {HTMLElement} popup - Elemento popup
     */
    function setupClickOutsideHandler(popup) {
        if (!config.clickOutside) return;

        popup.addEventListener('click', (event) => {
            const content = popup.querySelector('.modal-content, .popup-content, .filter-popup-content, .car-details-popup-content');
            if (content && !content.contains(event.target)) {
                closePopup(popup);
            }
        });
    }

    /**
     * Impedisce la chiusura quando si clicca sul contenuto
     * @param {HTMLElement} popup - Elemento popup
     */
    function setupContentClickProtection(popup) {
        const content = popup.querySelector('.modal-content, .popup-content, .filter-popup-content, .car-details-popup-content');
        if (content) {
            content.addEventListener('click', (event) => {
                event.stopPropagation();
            });
        }
    }

    /**
     * ============================================
     * GESTORI GLOBALI EVENTI
     * ============================================
     */

    /**
     * Inizializza i gestori globali per la tastiera
     */
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

    /**
     * Inizializza i pulsanti di apertura con attributo data-popup-target
     */
    function initializeOpenButtons() {
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

    /**
     * ============================================
     * FUNZIONI SPECIFICHE PER PAGINA
     * ============================================
     */

    /**
     * Inizializza tutte le funzioni specifiche per le diverse pagine
     * dell'applicazione
     */
    function initializePageSpecificFunctions() {
        console.log('🔧 Inizializzazione funzioni specifiche per pagina...');

        // Inizializza funzioni per ogni area dell'applicazione
        setupAdminMaintenanceFunctions();
        setupProductsPageFunctions();
        setupAccountPageFunctions();
        setupManageDealerFunctions();
        setupLegacyCompatibilityFunctions();
    }

    /**
     * ============================================
     * ADMIN MAINTENANCE FUNCTIONS
     * ============================================
     */

    /**
     * Configura le funzioni per la gestione admin/manutenzione
     */
    function setupAdminMaintenanceFunctions() {
        /**
         * Apre il popup di modifica prodotto popolando i campi con i dati
         * @param {HTMLElement} button - Pulsante con attributi data contenenti i dati del prodotto
         */
        window.openEditProductPopup = function (button) {
            console.log('🔧 Apertura popup modifica prodotto');

            const popup = document.getElementById('editProductPopup');
            const form = document.getElementById('editProductForm');

            if (!popup || !button) {
                console.error('❌ Popup editProductPopup o button non trovati');
                return;
            }

            // Estrai dati dal button
            const data = extractProductDataFromButton(button);
            console.log('📋 Dati prodotto:', data);

            // Popola i campi del form
            populateProductForm(data);

            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/product/${data.id}/edit`;
            }

            openPopup(popup);
        };

        /**
         * Apre il popup di modifica dealer popolando i campi con i dati
         * @param {HTMLElement} button - Pulsante con attributi data contenenti i dati del dealer
         */
        window.openEditDealerPopup = function (button) {
            console.log('🔧 Apertura popup modifica dealer');

            const popup = document.getElementById('editDealerPopup');
            const form = document.getElementById('editDealerForm');

            if (!popup || !button) {
                console.error('❌ Popup editDealerPopup o button non trovati');
                return;
            }

            // Estrai dati dal button
            const data = extractDealerDataFromButton(button);
            console.log('📋 Dati dealer:', data);

            // Popola i campi del form
            populateDealerForm(data);

            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/dealer/${data.id}/edit`;
            }

            openPopup(popup);
        };

        /**
         * Apre il popup per aggiungere una nuova subscription
         */
        window.openAddSubscriptionPopup = function () {
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

            openPopup(popup);
        };

        /**
         * Apre il popup di modifica subscription popolando i campi
         * @param {HTMLElement} button - Pulsante con attributi data della subscription
         */
        window.openEditSubscriptionPopup = function (button) {
            console.log('🔧 Apertura popup modifica subscription');

            const popup = document.getElementById('editSubscriptionPopup');
            const form = document.getElementById('editSubscriptionForm');

            if (!popup || !button) {
                console.error('❌ Popup editSubscriptionPopup o button non trovati');
                return;
            }

            // Estrai dati dal button
            const data = extractSubscriptionDataFromButton(button);
            console.log('📋 Dati subscription:', data);

            // Popola i campi del form
            populateSubscriptionForm(data);

            // Imposta l'azione del form
            if (form && data.id) {
                form.action = `/admin/subscription/${data.id}/edit`;
            }

            openPopup(popup);
        };

        /**
         * Apre il popup per applicare sconti alla subscription
         * @param {HTMLElement} button - Pulsante con dati della subscription
         */
        window.openDiscountPopup = function (button) {
            console.log('🔧 Apertura popup sconto');

            const popup = document.getElementById('discountPopup');
            const form = document.getElementById('discountForm');

            if (!popup || !button) {
                console.error('❌ Popup discountPopup o button non trovati');
                return;
            }

            // Estrai dati essenziali
            const id = button.getAttribute('data-id');
            const name = button.getAttribute('data-name');

            console.log('📋 Dati discount:', { id, name });

            // Popola nome subscription
            const nameElement = document.getElementById('discountSubscriptionName');
            if (nameElement) {
                nameElement.textContent = name || 'Abbonamento';
            }

            // Imposta la data minima per la scadenza (oggi)
            setupDiscountDateConstraints();

            // Reset e configura form
            if (form) {
                form.reset();
                if (id) {
                    form.action = `/admin/subscription/${id}/apply-discount`;
                }
            }

            openPopup(popup);
        };
    }

    /**
     * ============================================
     * PRODUCTS PAGE FUNCTIONS
     * ============================================
     */

    /**
     * Configura le funzioni per la pagina prodotti
     */
    function setupProductsPageFunctions() {
        /**
         * Apre il popup dei filtri di ricerca
         */
        window.openFilterPopup = function () {
            console.log('🔧 Apertura popup filtri');
            const popup = document.getElementById('filterPopup');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Popup filterPopup non trovato');
            }
        };

        /**
         * Chiude il popup dei filtri di ricerca
         */
        window.closeFilterPopup = function () {
            console.log('🔧 Chiusura popup filtri');
            const popup = document.getElementById('filterPopup');
            if (popup) {
                closePopup(popup);
            }
        };
    }

    /**
     * ============================================
     * ACCOUNT PAGE FUNCTIONS
     * ============================================
     */

    /**
     * Configura le funzioni per la pagina account
     */
    function setupAccountPageFunctions() {
        /**
         * Apre il modal di modifica account
         */
        window.openEditModal = function () {
            console.log('🔧 Apertura modal modifica account');
            const popup = document.getElementById('editAccountModal');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Modal editAccountModal non trovato');
            }
        };

        /**
         * Chiude il modal di modifica account
         */
        window.closeEditModal = function () {
            console.log('🔧 Chiusura modal modifica account');
            const popup = document.getElementById('editAccountModal');
            if (popup) {
                closePopup(popup);
            }
        };

        /**
         * Apre il modal di eliminazione account
         */
        window.openDeleteModal = function () {
            console.log('🔧 Apertura modal elimina account');
            const popup = document.getElementById('deleteAccountModal');
            if (popup) {
                openPopup(popup);
            } else {
                console.error('❌ Modal deleteAccountModal non trovato');
            }
        };

        /**
         * Chiude il modal di eliminazione account
         */
        window.closeDeleteModal = function () {
            console.log('🔧 Chiusura modal elimina account');
            const popup = document.getElementById('deleteAccountModal');
            if (popup) {
                closePopup(popup);
            }
        };
    }

    /**
     * ============================================
     * MANAGE DEALER FUNCTIONS
     * ============================================
     */

    /**
     * Configura le funzioni per la gestione del dealer
     */
    function setupManageDealerFunctions() {
        /**
         * Funzione generica per mostrare modal (compatibilità)
         * @param {string|HTMLElement} modal - ID del modal o elemento DOM
         */
        window.showModal = function (modal) {
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

        /**
         * Funzione generica per nascondere modal (compatibilità)
         * @param {string|HTMLElement} modal - ID del modal o elemento DOM
         */
        window.hideModal = function (modal) {
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

        /**
         * Apre il modal di modifica dealer con estrazione avanzata dei dati
         */
        window.openEditDealerModal = function () {
            console.log('🔧 Apertura modal modifica dealer');

            const modal = document.getElementById('edit-dealer-modal');
            if (!modal) {
                console.error('❌ Modal edit-dealer-modal non trovato');
                return;
            }

            console.log('🔍 Debug: Ricerca dati dealer nel DOM...');

            // Estrai dati dealer dal DOM usando multiple strategie
            const dealerData = extractDealerDataFromDOM();
            console.log('📋 Dati estratti:', dealerData);

            // Popola i campi del form
            populateDealerModalForm(dealerData);

            openPopup(modal);
            console.log('🎯 Modal aperto, verifica che i campi siano popolati correttamente');
        };

        /**
         * Apre il modal per aggiungere auto con reset completo
         */
        window.openAddCarModal = function () {
            console.log('🔧 Apertura modal aggiungi auto');
            const modal = document.getElementById('add-car-modal');
            if (modal) {
                // Reset completo del form e preview
                resetAddCarModal(modal);
                openPopup(modal);
            } else {
                console.error('❌ Modal add-car-modal non trovato');
            }
        };

        /**
         * Funzione generica per chiudere modal specifico
         * @param {string|HTMLElement} modalId - ID o elemento del modal
         */
        window.closeModal = function (modalId) {
            console.log('🔧 Chiusura modal tramite closeModal:', modalId);
            const modal = typeof modalId === 'string' ? document.getElementById(modalId) : modalId;
            if (modal) {
                closePopup(modal);
            } else {
                console.error(`❌ Modal ${modalId} non trovato per closeModal`);
            }
        };
    }

    /**
     * ============================================
     * FUNZIONI LEGACY PER COMPATIBILITÀ
     * ============================================
     */

    /**
     * Configura funzioni legacy per retrocompatibilità
     */
    function setupLegacyCompatibilityFunctions() {
        /**
         * Chiusura popup legacy
         * @param {string|HTMLElement} popupId - ID o elemento del popup
         * @deprecated Usare PopupManager.close() invece
         */
        window.closePopup = function (popupId) {
            const popup = typeof popupId === 'string' ? document.getElementById(popupId) : popupId;
            if (popup) {
                closePopup(popup);
            } else {
                console.error(`❌ Popup ${popupId} non trovato per chiusura`);
            }
        };

        /**
         * Apertura popup legacy
         * @param {string|HTMLElement} popupId - ID o elemento del popup
         * @param {Object} options - Opzioni di apertura
         * @deprecated Usare PopupManager.open() invece
         */
        window.openPopup = function (popupId, options = {}) {
            const popup = typeof popupId === 'string' ? document.getElementById(popupId) : popupId;
            if (popup) {
                openPopup(popup, options);
            } else {
                console.error(`❌ Popup ${popupId} non trovato per apertura`);
            }
        };
    }

    /**
     * ============================================
     * UTILITÀ ESTRAZIONE DATI
     * ============================================
     */

    /**
     * Estrae i dati del prodotto dagli attributi data del pulsante
     * @param {HTMLElement} button - Pulsante con attributi data
     * @returns {Object} Oggetto con i dati del prodotto
     */
    function extractProductDataFromButton(button) {
        return {
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
    }

    /**
     * Estrae i dati del dealer dagli attributi data del pulsante
     * @param {HTMLElement} button - Pulsante con attributi data
     * @returns {Object} Oggetto con i dati del dealer
     */
    function extractDealerDataFromButton(button) {
        return {
            id: button.getAttribute('data-id'),
            name: button.getAttribute('data-name'),
            description: button.getAttribute('data-description'),
            address: button.getAttribute('data-address'),
            email: button.getAttribute('data-email'),
            phone: button.getAttribute('data-phone')
        };
    }

    /**
     * Estrae i dati della subscription dagli attributi data del pulsante
     * @param {HTMLElement} button - Pulsante con attributi data
     * @returns {Object} Oggetto con i dati della subscription
     */
    function extractSubscriptionDataFromButton(button) {
        return {
            id: button.getAttribute('data-id'),
            name: button.getAttribute('data-name'),
            description: button.getAttribute('data-description'),
            price: button.getAttribute('data-price'),
            discount: button.getAttribute('data-discount'),
            expiry: button.getAttribute('data-expiry'),
            duration: button.getAttribute('data-duration'),
            maxCars: button.getAttribute('data-maxcars')
        };
    }

    /**
     * Estrae i dati del dealer dal DOM usando strategie multiple
     * @returns {Object} Dati del dealer estratti
     */
    function extractDealerDataFromDOM() {
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

        // Strategia 2: Selettori alternativi se la strategia 1 fallisce
        if (!dealerData.name && !dealerData.email) {
            console.log('⚠️ Strategia 1 fallita, tentativo strategia 2...');
            dealerData = extractDealerDataAlternative();
        }

        return dealerData;
    }

    /**
     * Strategia alternativa per estrazione dati dealer
     * @returns {Object} Dati dealer estratti con selettori alternativi
     */
    function extractDealerDataAlternative() {
        const selectors = {
            name: '[data-dealer-name], .dealer-name, .dealership-name',
            email: '[data-dealer-email], .dealer-email',
            phone: '[data-dealer-phone], .dealer-phone',
            address: '[data-dealer-address], .dealer-address',
            description: '[data-dealer-description], .dealer-description'
        };

        const dealerData = {};
        Object.entries(selectors).forEach(([key, selector]) => {
            const element = document.querySelector(selector);
            dealerData[key] = element ? element.textContent.trim() : '';
        });

        console.log('📋 Dati estratti (strategia 2):', dealerData);
        return dealerData;
    }

    /**
     * ============================================
     * UTILITÀ POPOLAMENTO FORM
     * ============================================
     */

    /**
     * Popola i campi del form prodotto
     * @param {Object} data - Dati del prodotto
     */
    function populateProductForm(data) {
        const fieldMappings = {
            'editProductId': data.id,
            'editProductDescription': data.description,
            'editProductPrice': data.price,
            'editProductCategory': data.category,
            'editProductBrand': data.brand,
            'editProductModel': data.model,
            'editProductMileage': data.mileage,
            'editProductYear': data.year,
            'editProductFuelType': data.fuel,
            'editProductTransmission': data.transmission
        };

        Object.entries(fieldMappings).forEach(([fieldId, value]) => {
            setFieldValue(fieldId, value);
        });
    }

    /**
     * Popola i campi del form dealer
     * @param {Object} data - Dati del dealer
     */
    function populateDealerForm(data) {
        const fieldMappings = {
            'editDealerId': data.id,
            'editDealerName': data.name,
            'editDealerDescription': data.description,
            'editDealerAddress': data.address,
            'editDealerEmail': data.email,
            'editDealerPhone': data.phone
        };

        Object.entries(fieldMappings).forEach(([fieldId, value]) => {
            setFieldValue(fieldId, value);
        });
    }

    /**
     * Popola i campi del form subscription
     * @param {Object} data - Dati della subscription
     */
    function populateSubscriptionForm(data) {
        const fieldMappings = {
            'editSubscriptionId': data.id,
            'editSubscriptionName': data.name,
            'editSubscriptionDescription': data.description,
            'editSubscriptionPrice': data.price,
            'editSubscriptionDiscount': data.discount,
            'editSubscriptionDiscountExpiry': data.expiry,
            'editSubscriptionDuration': data.duration,
            'editSubscriptionMaxCars': data.maxCars
        };

        Object.entries(fieldMappings).forEach(([fieldId, value]) => {
            setFieldValue(fieldId, value);
        });
    }

    /**
     * Popola i campi del modal dealer con validazione avanzata
     * @param {Object} dealerData - Dati del dealer
     */
    function populateDealerModalForm(dealerData) {
        const fields = {
            'edit-dealership-name': dealerData.name,
            'edit-dealership-description': dealerData.description,
            'edit-dealership-address': dealerData.address,
            'edit-dealership-phone': dealerData.phone,
            'edit-dealership-email': dealerData.email
        };

        console.log('🔧 Popolamento campi form...');

        let populatedCount = 0;
        Object.entries(fields).forEach(([fieldId, value]) => {
            const field = document.getElementById(fieldId);

            if (field && shouldPopulateField(value)) {
                if (field.tagName.toLowerCase() === 'textarea') {
                    field.textContent = value;
                } else {
                    field.value = value;
                }
                console.log(`✅ Campo ${fieldId} popolato con: "${value}"`);
                populatedCount++;
            } else if (field) {
                console.log(`⏭️ Campo ${fieldId} saltato (valore placeholder o vuoto): "${value}"`);
            } else {
                console.warn(`⚠️ Campo ${fieldId} non trovato nel DOM`);
            }
        });

        if (populatedCount === 0) {
            console.log('ℹ️ Nessun dato popolato via JavaScript, i campi useranno i valori Thymeleaf già presenti');
        } else {
            console.log(`✅ ${populatedCount} campi popolati con successo`);
        }
    }

    /**
     * ============================================
     * UTILITÀ RESET E CONFIGURAZIONE
     * ============================================
     */

    /**
     * Reset completo del modal aggiungi auto
     * @param {HTMLElement} modal - Elemento modal
     */
    function resetAddCarModal(modal) {
        const form = document.getElementById('dealer-add-car-form');
        if (form) {
            form.reset();
            resetImagePreview();
            resetFeaturedCheckbox();
        }
    }

    /**
     * Reset del preview immagini
     */
    function resetImagePreview() {
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
    }

    /**
     * Reset del checkbox evidenza auto
     */
    function resetFeaturedCheckbox() {
        const featuredCheckbox = document.getElementById('dealer-car-highlighted');
        const durationField = document.getElementById('dealer-car-feature-duration-field');
        
        if (featuredCheckbox) {
            featuredCheckbox.checked = false;
        }
        if (durationField) {
            durationField.style.display = 'none';
        }
    }

    /**
     * Configura vincoli di data per il popup sconto
     */
    function setupDiscountDateConstraints() {
        const today = new Date().toISOString().split('T')[0];
        const expiryField = document.getElementById('discountExpiry');
        if (expiryField) {
            expiryField.min = today;
        }
    }

    /**
     * ============================================
     * UTILITÀ GENERICHE
     * ============================================
     */

    /**
     * Imposta il valore in un campo del form con validazione
     * @param {string} fieldId - ID del campo
     * @param {*} value - Valore da impostare
     */
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

    /**
     * Verifica se un valore dovrebbe essere popolato nel form
     * @param {string} value - Valore da verificare
     * @returns {boolean} True se il valore è valido per il popolamento
     */
    function shouldPopulateField(value) {
        if (!value) return false;
        
        const placeholderTexts = [
            'Nome Concessionario',
            'Nessuna descrizione',
            'non disponibile',
            'N/A'
        ];
        
        return !placeholderTexts.some(placeholder => 
            value.includes(placeholder)
        ) && value.trim() !== '';
    }

    /**
     * ============================================
     * CORE POPUP MANAGEMENT
     * ============================================
     */

    /**
     * Apre un popup con configurazioni avanzate
     * @param {HTMLElement} popup - Elemento popup da aprire
     * @param {Object} options - Opzioni di configurazione
     */
    function openPopup(popup, options = {}) {
        if (!popup) return;

        console.log(`🟢 Apertura popup: ${popup.id || 'unnamed'}`);

        // Chiudi altri popup se necessario
        if (options.closeOthers !== false) {
            closeAllPopups();
        }

        // Applica classi e attributi per mostrare il popup
        popup.classList.add(config.showClass);
        popup.classList.add(config.activeClass);
        popup.setAttribute('aria-hidden', 'false');

        // Gestione scroll del body
        document.body.style.overflow = 'hidden';

        // Gestione focus per accessibilità
        setupPopupFocus(popup);

        // Callbacks ed eventi personalizzati
        executePopupCallbacks(popup, options, 'onOpen', 'popup:open');
    }

    /**
     * Chiude un popup con cleanup completo
     * @param {HTMLElement} popup - Elemento popup da chiudere
     * @param {Object} options - Opzioni di configurazione
     */
    function closePopup(popup, options = {}) {
        if (!popup) return;

        console.log(`🔴 Chiusura popup: ${popup.id || 'unnamed'}`);

        // Rimuovi classi e attributi per nascondere il popup
        popup.classList.remove(config.showClass);
        popup.classList.remove(config.activeClass);
        popup.setAttribute('aria-hidden', 'true');

        // Ripristina scroll se necessario
        setTimeout(() => {
            if (!getTopMostPopup()) {
                document.body.style.overflow = '';
            }
        }, 100);

        // Reset form se richiesto
        if (options.resetForm !== false) {
            const forms = popup.querySelectorAll('form');
            forms.forEach(form => form.reset());
        }

        // Callbacks ed eventi personalizzati
        executePopupCallbacks(popup, options, 'onClose', 'popup:close');
    }

    /**
     * Chiude tutti i popup aperti
     */
    function closeAllPopups() {
        const openPopups = document.querySelectorAll(`.${config.showClass}`);
        openPopups.forEach(popup => closePopup(popup));
    }

    /**
     * Restituisce il popup con z-index più alto
     * @returns {HTMLElement|null} Popup più in alto o null
     */
    function getTopMostPopup() {
        const openPopups = Array.from(document.querySelectorAll(`.${config.showClass}`));
        if (openPopups.length === 0) return null;

        return openPopups.reduce((topmost, current) => {
            const currentZ = parseInt(getComputedStyle(current).zIndex) || 0;
            const topmostZ = parseInt(getComputedStyle(topmost).zIndex) || 0;
            return currentZ > topmostZ ? current : topmost;
        });
    }

    /**
     * ============================================
     * FOCUS MANAGEMENT E ACCESSIBILITÀ
     * ============================================
     */

    /**
     * Configura la gestione del focus per il popup
     * @param {HTMLElement} popup - Elemento popup
     */
    function setupPopupFocus(popup) {
        const focusableElements = popup.querySelectorAll(
            'input, button, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        
        if (focusableElements.length > 0) {
            setTimeout(() => focusableElements[0].focus(), 100);
        }

        // Inizializza trappola del focus
        initializeFocusTrap(popup, focusableElements);
    }

    /**
     * Inizializza la trappola del focus per accessibilità
     * @param {HTMLElement} popup - Elemento popup
     * @param {NodeList} focusableElements - Elementi focusabili
     */
    function initializeFocusTrap(popup, focusableElements) {
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        popup.addEventListener('keydown', (event) => {
            if (event.key === 'Tab') {
                if (event.shiftKey) {
                    // Shift + Tab: navigazione indietro
                    if (document.activeElement === firstElement) {
                        event.preventDefault();
                        lastElement.focus();
                    }
                } else {
                    // Tab: navigazione avanti
                    if (document.activeElement === lastElement) {
                        event.preventDefault();
                        firstElement.focus();
                    }
                }
            }
        });
    }

    /**
     * ============================================
     * SISTEMA CAROSELLI INTEGRATO
     * ============================================
     */

    /**
     * Inizializza i caroselli all'interno dei popup
     * @param {HTMLElement} popup - Elemento popup contenente caroselli
     */
    window.initializePopupCarousel = function (popup) {
        if (!popup) return;

        const carousels = popup.querySelectorAll('.carousel, .product-carousel, .image-carousel');

        carousels.forEach(carousel => {
            setupIndividualCarousel(carousel);
        });
    };

    /**
     * Configura un singolo carosello
     * @param {HTMLElement} carousel - Elemento carosello
     */
    function setupIndividualCarousel(carousel) {
        const items = carousel.querySelectorAll('.carousel-item, .product-image-item, .image-item');
        const dots = carousel.querySelectorAll('.carousel-dot, .dot');
        const prevBtn = carousel.querySelector('.carousel-btn.prev, .carousel-control-prev');
        const nextBtn = carousel.querySelector('.carousel-btn.next, .carousel-control-next');
        const thumbnails = carousel.querySelectorAll('.carousel-thumbnails img');

        if (items.length <= 1) return;

        let currentIndex = 0;

        // Funzione di aggiornamento stato carosello
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

        // Event listeners per controlli
        setupCarouselControls(prevBtn, nextBtn, dots, thumbnails, items, () => currentIndex, (newIndex) => {
            currentIndex = newIndex;
            updateCarousel();
        });

        // Inizializza stato
        updateCarousel();
    }

    /**
     * Configura i controlli del carosello
     * @param {HTMLElement} prevBtn - Pulsante precedente
     * @param {HTMLElement} nextBtn - Pulsante successivo
     * @param {NodeList} dots - Dots di navigazione
     * @param {NodeList} thumbnails - Thumbnail di navigazione
     * @param {NodeList} items - Elementi del carosello
     * @param {Function} getCurrentIndex - Getter indice corrente
     * @param {Function} setCurrentIndex - Setter indice corrente
     */
    function setupCarouselControls(prevBtn, nextBtn, dots, thumbnails, items, getCurrentIndex, setCurrentIndex) {
        if (prevBtn) {
            prevBtn.addEventListener('click', (event) => {
                event.stopPropagation();
                const current = getCurrentIndex();
                const newIndex = current > 0 ? current - 1 : items.length - 1;
                setCurrentIndex(newIndex);
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', (event) => {
                event.stopPropagation();
                const current = getCurrentIndex();
                const newIndex = current < items.length - 1 ? current + 1 : 0;
                setCurrentIndex(newIndex);
            });
        }

        dots.forEach((dot, index) => {
            dot.addEventListener('click', (event) => {
                event.stopPropagation();
                setCurrentIndex(index);
            });
        });

        thumbnails.forEach((thumb, index) => {
            thumb.addEventListener('click', (event) => {
                event.stopPropagation();
                setCurrentIndex(index);
            });
        });
    }

    /**
     * ============================================
     * CALLBACKS E EVENTI PERSONALIZZATI
     * ============================================
     */

    /**
     * Esegue callbacks ed eventi personalizzati per i popup
     * @param {HTMLElement} popup - Elemento popup
     * @param {Object} options - Opzioni con callbacks
     * @param {string} callbackName - Nome della callback
     * @param {string} eventName - Nome dell'evento personalizzato
     */
    function executePopupCallbacks(popup, options, callbackName, eventName) {
        // Callback personalizzata
        if (options[callbackName] && typeof options[callbackName] === 'function') {
            options[callbackName](popup);
        }

        // Evento personalizzato
        popup.dispatchEvent(new CustomEvent(eventName, { detail: { popup } }));
    }

    /**
     * ============================================
     * API PUBBLICA
     * ============================================
     */

    /**
     * API pubblica per l'integrazione con altre parti dell'applicazione
     */
    window.PopupManager = {
        open: openPopup,
        close: closePopup,
        closeAll: closeAllPopups,
        getTopMost: getTopMostPopup,
        initializeCarousel: window.initializePopupCarousel
    };

    /**
     * ============================================
     * INIZIALIZZAZIONE AUTOMATICA E OBSERVER
     * ============================================
     */

    // Inizializza quando il DOM è completamente caricato
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializePopups);
    } else {
        initializePopups();
    }

    // Observer per re-inizializzare popup aggiunti dinamicamente
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