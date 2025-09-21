/**
 * ===============================================
 * DROPDOWN MENU MANAGEMENT - FCF MOTORS
 * ===============================================
 * 
 * Sistema di gestione del menu dropdown presente nell'header
 * di tutte le pagine dell'applicazione FCF Motors.
 * 
 * Funzionalità:
 * - Apertura/chiusura menu al click
 * - Chiusura automatica al click esterno
 * - Supporto completo per accessibilità (ARIA, tastiera)
 * - Gestione focus per navigazione con tastiera
 * - Event listeners ottimizzati per performance
 * 
 * @author FCF Motors Team
 * @version 1.2.0
 */

(function () {
    'use strict';

    console.log('🔧 dropdown.js: Script caricato');

    /**
     * Riferimenti agli elementi DOM del dropdown
     * @type {Object}
     */
    let dropdownElements = {
        toggle: null,
        menu: null
    };

    /**
     * Stato corrente del dropdown
     * @type {boolean}
     */
    let isDropdownOpen = false;

    /**
     * ============================================
     * INIZIALIZZAZIONE PRINCIPALE
     * ============================================
     */
    function initializeDropdown() {
        console.log('🔧 dropdown.js: Inizializzazione dropdown...');

        // Trova e memorizza elementi DOM
        if (!findDropdownElements()) {
            console.warn('⚠️ dropdown.js: Elementi dropdown non trovati - inizializzazione annullata');
            return;
        }

        console.log('✅ dropdown.js: Elementi dropdown trovati - configurazione in corso...');

        // Configura event listeners
        setupEventListeners();
        
        // Inizializza attributi ARIA per accessibilità
        initializeARIAAttributes();

        console.log('✅ dropdown.js: Dropdown inizializzato con successo!');
        logEventListeners();
    }

    /**
     * ============================================
     * RICERCA E VALIDAZIONE ELEMENTI DOM
     * ============================================
     */
    function findDropdownElements() {
        dropdownElements.toggle = document.querySelector('.dropdown-toggle');
        dropdownElements.menu = document.querySelector('.dropdown-menu');

        console.log('🔍 dropdown.js: Toggle element trovato:', !!dropdownElements.toggle);
        console.log('🔍 dropdown.js: Menu element trovato:', !!dropdownElements.menu);

        return dropdownElements.toggle && dropdownElements.menu;
    }

    /**
     * ============================================
     * CONFIGURAZIONE EVENT LISTENERS
     * ============================================
     */
    function setupEventListeners() {
        // Click sul pulsante toggle
        dropdownElements.toggle.addEventListener('click', handleToggleClick);
        
        // Click esterno per chiusura automatica
        document.addEventListener('click', handleDocumentClick);
        
        // Gestione tastiera per accessibilità
        dropdownElements.toggle.addEventListener('keydown', handleToggleKeydown);
        dropdownElements.menu.addEventListener('keydown', handleMenuKeydown);
        
        // Chiusura con tasto Escape
        document.addEventListener('keydown', handleEscapeKey);
    }

    /**
     * ============================================
     * GESTORI EVENTI - MOUSE
     * ============================================
     */

    /**
     * Gestisce il click sul pulsante toggle del dropdown
     * @param {Event} event - Evento click
     */
    function handleToggleClick(event) {
        event.preventDefault();
        event.stopPropagation();

        console.log('🖱️ dropdown.js: Click su dropdown toggle');

        // Cambia stato del dropdown
        isDropdownOpen = !isDropdownOpen;
        updateDropdownState();

        console.log('🔧 dropdown.js: Dropdown stato cambiato - isOpen:', isDropdownOpen);
    }

    /**
     * Gestisce i click esterni al dropdown per chiusura automatica
     * @param {Event} event - Evento click del documento
     */
    function handleDocumentClick(event) {
        // Ignora click sul toggle (già gestito)
        if (event.target.closest('.dropdown-toggle')) {
            return;
        }
        
        // Se click è esterno al dropdown, chiudi
        if (!event.target.closest('.dropdown')) {
            console.log('🔧 dropdown.js: Click esterno rilevato - chiusura dropdown');
            closeDropdown();
        }
    }

    /**
     * ============================================
     * GESTORI EVENTI - TASTIERA
     * ============================================
     */

    /**
     * Gestisce la navigazione da tastiera sul pulsante toggle
     * @param {KeyboardEvent} event - Evento tastiera
     */
    function handleToggleKeydown(event) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            console.log('🎹 dropdown.js: Tastiera - Enter/Space premuto su toggle');

            isDropdownOpen ? closeDropdown() : openDropdown();
        }
    }

    /**
     * Gestisce la navigazione da tastiera all'interno del menu
     * @param {KeyboardEvent} event - Evento tastiera
     */
    function handleMenuKeydown(event) {
        if (event.key !== 'Tab') return;

        const focusableElements = getFocusableElements();
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (event.shiftKey) {
            // Shift + Tab: navigazione all'indietro
            if (document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            }
        } else {
            // Tab: navigazione in avanti
            if (document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    }

    /**
     * Gestisce la chiusura con tasto Escape
     * @param {KeyboardEvent} event - Evento tastiera
     */
    function handleEscapeKey(event) {
        if (event.key === 'Escape' && isDropdownOpen) {
            console.log('🎹 dropdown.js: Escape premuto - chiusura dropdown');
            closeDropdown();
        }
    }

    /**
     * ============================================
     * CONTROLLO STATO DROPDOWN
     * ============================================
     */

    /**
     * Apre il dropdown e imposta focus appropriato
     */
    function openDropdown() {
        console.log('🔧 dropdown.js: Apertura dropdown');
        
        isDropdownOpen = true;
        updateDropdownState();

        // Focus sul primo elemento del menu per accessibilità
        const firstMenuItem = dropdownElements.menu.querySelector('a');
        if (firstMenuItem) {
            setTimeout(() => firstMenuItem.focus(), 100);
        }
    }

    /**
     * Chiude il dropdown e ripristina focus
     */
    function closeDropdown() {
        if (!isDropdownOpen) return;

        console.log('🔧 dropdown.js: Chiusura dropdown');
        
        isDropdownOpen = false;
        updateDropdownState();
    }

    /**
     * Aggiorna lo stato visivo del dropdown in base a isDropdownOpen
     */
    function updateDropdownState() {
        // Aggiorna classi CSS
        dropdownElements.menu.classList.toggle('show', isDropdownOpen);
        dropdownElements.toggle.classList.toggle('open', isDropdownOpen);
        
        // Aggiorna attributi ARIA per accessibilità
        dropdownElements.toggle.setAttribute('aria-expanded', isDropdownOpen.toString());
        dropdownElements.menu.setAttribute('aria-hidden', (!isDropdownOpen).toString());
    }

    /**
     * ============================================
     * UTILITÀ E SUPPORTO
     * ============================================
     */

    /**
     * Trova tutti gli elementi focusabili all'interno del menu
     * @returns {NodeList} Lista degli elementi focusabili
     */
    function getFocusableElements() {
        return dropdownElements.menu.querySelectorAll(
            'a, button, [tabindex]:not([tabindex="-1"])'
        );
    }

    /**
     * Inizializza gli attributi ARIA per l'accessibilità
     */
    function initializeARIAAttributes() {
        dropdownElements.toggle.setAttribute('aria-expanded', 'false');
        dropdownElements.toggle.setAttribute('aria-haspopup', 'true');
        dropdownElements.menu.setAttribute('aria-hidden', 'true');
    }

    /**
     * Registra nel console i listener configurati (solo per debug)
     */
    function logEventListeners() {
        console.log('🔧 dropdown.js: Event listeners configurati:', {
            toggle_click: '✓ Apertura/chiusura menu',
            document_click: '✓ Chiusura automatica',
            keyboard_navigation: '✓ Tab, Enter, Space, Escape',
            focus_management: '✓ Gestione focus per accessibilità'
        });
    }

    /**
     * ============================================
     * FUNZIONE DI COMPATIBILITÀ
     * ============================================
     */

    /**
     * Funzione globale per compatibilità con codice esistente
     * @param {Event} event - Evento opzionale
     * @deprecated Usare il sistema di eventi automatico
     */
    window.toggleDropdownMenu = function (event) {
        console.log('🔧 dropdown.js: toggleDropdownMenu (legacy) chiamata');

        if (event) {
            event.stopPropagation();
        }

        if (!dropdownElements.toggle || !dropdownElements.menu) {
            console.warn('⚠️ dropdown.js: Elementi dropdown non disponibili per toggle legacy');
            return;
        }

        isDropdownOpen = !isDropdownOpen;
        updateDropdownState();
        
        console.log('🔧 dropdown.js: Toggle legacy completato - nuovo stato:', isDropdownOpen);
    };

    /**
     * ============================================
     * INIZIALIZZAZIONE AUTOMATICA
     * ============================================
     */

    // Inizializza quando il DOM è completamente caricato
    if (document.readyState === 'loading') {
        console.log('🔧 dropdown.js: DOM in caricamento - attesa DOMContentLoaded');
        document.addEventListener('DOMContentLoaded', initializeDropdown);
    } else {
        console.log('🔧 dropdown.js: DOM già disponibile - inizializzazione immediata');
        initializeDropdown();
    }

    console.log('🔧 dropdown.js: Sistema dropdown completamente configurato');

})();