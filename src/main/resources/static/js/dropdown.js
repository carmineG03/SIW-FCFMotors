/**
 * Dropdown Menu Management - FCF Motors
 * Gestisce il menu a tendina presente in tutte le pagine
 */

(function() {
    'use strict';

    // Inizializza il dropdown quando il DOM è caricato
    function initializeDropdown() {
        const dropdownToggle = document.querySelector('.dropdown-toggle');
        const dropdownMenu = document.querySelector('.dropdown-menu');

        // Verifica che gli elementi esistano
        if (!dropdownToggle || !dropdownMenu) {
            console.warn('Dropdown elements not found');
            return;
        }

        // Gestione click sul pulsante toggle
        dropdownToggle.addEventListener('click', function(event) {
            event.stopPropagation();
            
            const isOpen = dropdownMenu.classList.contains('show');
            dropdownMenu.classList.toggle('show');
            dropdownToggle.classList.toggle('open', !isOpen);
            
            // Aggiungi attributi ARIA per accessibilità
            dropdownToggle.setAttribute('aria-expanded', !isOpen);
            dropdownMenu.setAttribute('aria-hidden', isOpen);
        });

        // Chiusura dropdown quando si clicca fuori
        document.addEventListener('click', function(event) {
            if (!dropdownToggle.contains(event.target) && !dropdownMenu.contains(event.target)) {
                closeDropdown();
            }
        });

        // Gestione tastiera per accessibilità
        dropdownToggle.addEventListener('keydown', function(event) {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                const isOpen = dropdownMenu.classList.contains('show');
                
                if (isOpen) {
                    closeDropdown();
                } else {
                    openDropdown();
                }
            }
        });

        // Chiusura con tasto Escape
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape') {
                closeDropdown();
            }
        });

        // Gestione focus per accessibilità
        dropdownMenu.addEventListener('keydown', function(event) {
            const focusableElements = dropdownMenu.querySelectorAll('a, button, [tabindex]:not([tabindex="-1"])');
            const firstElement = focusableElements[0];
            const lastElement = focusableElements[focusableElements.length - 1];

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

        // Funzioni helper
        function openDropdown() {
            dropdownMenu.classList.add('show');
            dropdownToggle.classList.add('open');
            dropdownToggle.setAttribute('aria-expanded', 'true');
            dropdownMenu.setAttribute('aria-hidden', 'false');
            
            // Focus sul primo elemento del menu
            const firstMenuItem = dropdownMenu.querySelector('a');
            if (firstMenuItem) {
                firstMenuItem.focus();
            }
        }

        function closeDropdown() {
            dropdownMenu.classList.remove('show');
            dropdownToggle.classList.remove('open');
            dropdownToggle.setAttribute('aria-expanded', 'false');
            dropdownMenu.setAttribute('aria-hidden', 'true');
        }

        // Inizializza attributi ARIA
        dropdownToggle.setAttribute('aria-expanded', 'false');
        dropdownToggle.setAttribute('aria-haspopup', 'true');
        dropdownMenu.setAttribute('aria-hidden', 'true');
    }

    // Funzione di fallback per compatibilità con codice esistente
    window.toggleDropdownMenu = function(event) {
        if (event) {
            event.stopPropagation();
        }
        
        const dropdownMenu = document.querySelector('.dropdown-menu');
        const dropdownToggle = document.querySelector('.dropdown-toggle');
        
        if (dropdownMenu && dropdownToggle) {
            const isOpen = dropdownMenu.classList.contains('show');
            dropdownMenu.classList.toggle('show');
            dropdownToggle.classList.toggle('open', !isOpen);
        }
    };

    // Inizializza quando il DOM è pronto
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeDropdown);
    } else {
        initializeDropdown();
    }
})();