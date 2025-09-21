/**
 * ==========================================================
 * Script di gestione della pagina "Gestione Concessionario"
 * ==========================================================
 * 
 * Questo file contiene tutta la logica JS per la pagina di gestione
 * del concessionario, separando lo script inline da manage_dealer.html.
 * 
 * Funzionalità principali:
 * - Gestione modali per modifica concessionario e auto
 * - Gestione caroselli immagini (preview e navigazione)
 * - Gestione toast di feedback per l'utente
 * - Gestione delle azioni su auto: aggiunta, modifica, eliminazione, evidenziazione
 * - Gestione delle azioni sul concessionario: modifica, eliminazione, richieste preventivo
 * - Integrazione con endpoint REST tramite fetch e CSRF token
 * - Gestione preview immagini per upload
 * 
 * Dettaglio delle sezioni:
 * - Inizializzazione: attende il DOM pronto e logga l'avvio.
 * - Selezione elementi principali (bottoni, form, toast, ecc).
 * - Funzione showToast: mostra messaggi di feedback.
 * - Event listeners per i bottoni principali (modifica, aggiungi, elimina, preventivi).
 * - Gestione checkbox "in evidenza" e campo durata.
 * - Gestione submit dei form (modifica concessionario, aggiungi auto, modifica auto).
 * - Gestione azioni sulle auto (modifica, elimina, metti in evidenza, rimuovi evidenza).
 * - Funzioni per preview immagini (dealer, auto, modifica auto).
 * - Gestione caroselli immagini con PopupManager.
 * 
 * Sicurezza:
 * - Tutte le chiamate fetch usano il token CSRF.
 * - Conferme per azioni distruttive (eliminazione).
 * 
 * Responsive:
 * - Funziona con i modali e i caroselli previsti dal CSS.
 * 
 * Debug:
 * - Log di stato per facilitare il debug.
 * 
 * Ultima riga: log di completamento inizializzazione.
 */

document.addEventListener('DOMContentLoaded', function () {

    console.log('🔧 Inizializzazione manage_dealer.html');


    // Elements
    const editDealerButton = document.getElementById('edit-dealer-button');
    const addCarButton = document.getElementById('add-car-button');
    const viewQuotesButton = document.getElementById('view-quotes-button');
    const deleteDealerButton = document.getElementById('delete-dealer-button');
    const toast = document.getElementById('toast');
    const isFeaturedCheckbox = document.getElementById('dealer-car-highlighted');
    const featureDurationField = document.getElementById('dealer-car-feature-duration-field');

    // Toast function
    function showToast(message, type = 'success') {
        if (toast) {
            toast.textContent = message;
            toast.style.background = type === 'success' ?
                'linear-gradient(45deg, #28a745, #218838)' :
                'linear-gradient(45deg, #dc3545, #c82333)';
            toast.style.opacity = '1';
            toast.style.transform = 'translateY(0)';

            setTimeout(() => {
                toast.style.opacity = '0';
                toast.style.transform = 'translateY(-10px)';
            }, 3000);
        }
    }

    // Event listeners per i pulsanti principali
    if (editDealerButton) {
        editDealerButton.addEventListener('click', function () {
            console.log('🖱️ Click su modifica dealer');
            openEditDealerModal();
        });
    }

    if (addCarButton) {
        addCarButton.addEventListener('click', function () {
            console.log('🖱️ Click su aggiungi auto');
            openAddCarModal();
        });
    }

    if (viewQuotesButton) {
        viewQuotesButton.addEventListener('click', function () {
            const dealerId = document.querySelector('input[name="dealerId"]').value;
            window.location.href = `/rest/dealer/quote-requests/${dealerId}`;
        });
    }

    if (deleteDealerButton) {
        deleteDealerButton.addEventListener('click', function () {
            if (confirm('Sei sicuro di voler eliminare definitivamente questo concessionario? Questa azione non può essere annullata.')) {
                const dealerId = document.querySelector('input[name="dealerId"]').value;
                window.location.href = `/rest/manutenzione/dealer/delete_dealer/${dealerId}`;
            }
        });
    }

    // Featured checkbox functionality
    if (isFeaturedCheckbox && featureDurationField) {
        isFeaturedCheckbox.addEventListener('change', function () {
            featureDurationField.style.display = this.checked ? 'block' : 'none';
        });
    }

    // Form submissions
    const editDealerForm = document.getElementById('edit-dealer-form');
    const addCarForm = document.getElementById('dealer-add-car-form');
    const editProductForm = document.getElementById('edit-product-form');

    if (editDealerForm) {
        editDealerForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const formData = new FormData(this);
            const spinner = this.querySelector('.spinner');
            const submitButton = this.querySelector('button[type="submit"]');

            if (spinner) spinner.classList.add('active');
            if (submitButton) submitButton.disabled = true;

            try {
                const response = await fetch(this.action, {
                    method: 'PUT',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const data = await response.json();

                if (response.ok) {
                    showToast(data.message || 'Concessionario modificato con successo!', 'success');
                    closeModal('edit-dealer-modal');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.message || 'Errore durante la modifica', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                if (spinner) spinner.classList.remove('active');
                if (submitButton) submitButton.disabled = false;
            }
        });
    }

    if (addCarForm) {
        addCarForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const formData = new FormData(this);
            const spinner = this.querySelector('.spinner');
            const submitButton = this.querySelector('button[type="submit"]');

            if (spinner) spinner.classList.add('active');
            if (submitButton) submitButton.disabled = true;

            try {
                const response = await fetch(this.action, {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const data = await response.json();

                if (response.ok) {
                    showToast(data.message || 'Auto aggiunta con successo!', 'success');
                    closeModal('add-car-modal');
                    this.reset();
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.message || 'Errore durante l\'aggiunta', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                if (spinner) spinner.classList.remove('active');
                if (submitButton) submitButton.disabled = false;
            }
        });
    }

    // Product management
    const editProductLinks = document.querySelectorAll('.edit-product-link');
    const deleteProductLinks = document.querySelectorAll('.delete-product-link');
    const highlightProductLinks = document.querySelectorAll('.highlight-product-link');
    const removeHighlightProductLinks = document.querySelectorAll('.remove-highlight-product-link');

    // Edit product functionality
    editProductLinks.forEach(link => {
        link.addEventListener('click', async function (e) {
            e.preventDefault();
            const productId = this.getAttribute('data-product-id');
            if (productId) {
                await populateEditForm(productId);
            }
        });
    });

    async function populateEditForm(productId) {
        try {
            const response = await fetch(`/rest/api/products/${productId}`, {
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            });

            if (response.ok) {
                const product = await response.json();

                if (editProductForm) {
                    editProductForm.action = `/rest/api/products/${productId}`;

                    const fields = {
                        'edit-product-id': product.id || '',
                        'edit-product-model': product.model || '',
                        'edit-product-brand': product.brand || '',
                        'edit-product-category': product.category || '',
                        'edit-product-description': product.description || '',
                        'edit-product-price': product.price || '',
                        'edit-product-mileage': product.mileage || '',
                        'edit-product-year': product.year || '',
                        'edit-product-fuelType': product.fuelType || '',
                        'edit-product-transmission': product.transmission || ''
                    };

                    for (const [id, value] of Object.entries(fields)) {
                        const element = document.getElementById(id);
                        if (element) element.value = value;
                    }

                    showModal('edit-product-modal');
                }
            } else {
                const data = await response.json();
                showToast(data.message || 'Errore nel caricamento dei dati', 'error');
            }
        } catch (error) {
            showToast('Errore di rete: ' + error.message, 'error');
        }
    }

    // Delete, highlight, remove highlight functionality...
    deleteProductLinks.forEach(link => {
        link.addEventListener('click', async function (e) {
            e.preventDefault();
            const productId = this.getAttribute('data-product-id');

            if (!confirm('Sei sicuro di voler eliminare questo prodotto?')) return;

            try {
                const response = await fetch(`/rest/api/products/${productId}`, {
                    method: 'DELETE',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const data = await response.json();

                if (response.ok) {
                    showToast(data.message || 'Auto eliminata con successo!', 'success');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.message || 'Errore durante l\'eliminazione', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Image preview functions
    window.previewDealerImages = function (input) {
        const previewContainer = document.getElementById('dealer-image-preview');

        if (input.files && input.files.length > 0) {
            previewContainer.innerHTML = '';

            Array.from(input.files).forEach((file, index) => {
                if (file.type.startsWith('image/')) {
                    const reader = new FileReader();
                    reader.onload = function (e) {
                        const imageItem = document.createElement('div');
                        imageItem.className = index === 0 ? 'image-item active' : 'image-item';
                        imageItem.innerHTML = `<img src="${e.target.result}" alt="Preview ${index + 1}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px;">`;
                        previewContainer.appendChild(imageItem);

                        if (index === input.files.length - 1) {
                            if (input.files.length > 1) {
                                const nav = document.createElement('div');
                                nav.className = 'carousel-nav';
                                nav.innerHTML = `
                                    <button class="carousel-btn prev" type="button"><i class="fas fa-chevron-left"></i></button>
                                    <button class="carousel-btn next" type="button"><i class="fas fa-chevron-right"></i></button>
                                `;
                                previewContainer.appendChild(nav);

                                const dots = document.createElement('div');
                                dots.className = 'carousel-dots';
                                for (let i = 0; i < input.files.length; i++) {
                                    dots.innerHTML += `<span class="carousel-dot ${i === 0 ? 'active' : ''}"></span>`;
                                }
                                previewContainer.appendChild(dots);
                            }

                            // Reinitialize carousel con PopupManager
                            PopupManager.initializeCarousel(previewContainer);
                        }
                    };
                    reader.readAsDataURL(file);
                }
            });
        }
    };

    window.previewCarImages = function (input) {
        const previewContainer = document.getElementById('car-image-preview');

        if (input.files && input.files.length > 0) {
            previewContainer.innerHTML = '';

            Array.from(input.files).forEach((file, index) => {
                if (file.type.startsWith('image/')) {
                    const reader = new FileReader();
                    reader.onload = function (e) {
                        const imageItem = document.createElement('div');
                        imageItem.className = index === 0 ? 'image-item active' : 'image-item';
                        imageItem.innerHTML = `<img src="${e.target.result}" alt="Preview ${index + 1}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px;">`;
                        previewContainer.appendChild(imageItem);

                        if (index === input.files.length - 1) {
                            if (input.files.length > 1) {
                                const nav = document.createElement('div');
                                nav.className = 'carousel-nav';
                                nav.innerHTML = `
                                    <button class="carousel-btn prev" type="button"><i class="fas fa-chevron-left"></i></button>
                                    <button class="carousel-btn next" type="button"><i class="fas fa-chevron-right"></i></button>
                                `;
                                previewContainer.appendChild(nav);

                                const dots = document.createElement('div');
                                dots.className = 'carousel-dots';
                                for (let i = 0; i < input.files.length; i++) {
                                    dots.innerHTML += `<span class="carousel-dot ${i === 0 ? 'active' : ''}"></span>`;
                                }
                                previewContainer.appendChild(dots);
                            }

                            PopupManager.initializeCarousel(previewContainer);
                        }
                    };
                    reader.readAsDataURL(file);
                }
            });
        }
    };

    window.previewEditCarImages = function (input) {
        const previewContainer = document.getElementById('edit-car-image-preview');

        if (input.files && input.files.length > 0) {
            previewContainer.innerHTML = '';

            Array.from(input.files).forEach((file, index) => {
                if (file.type.startsWith('image/')) {
                    const reader = new FileReader();
                    reader.onload = function (e) {
                        const imageItem = document.createElement('div');
                        imageItem.className = index === 0 ? 'image-item active' : 'image-item';
                        imageItem.innerHTML = `<img src="${e.target.result}" alt="Preview ${index + 1}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px;">`;
                        previewContainer.appendChild(imageItem);

                        if (index === input.files.length - 1) {
                            if (input.files.length > 1) {
                                const nav = document.createElement('div');
                                nav.className = 'carousel-nav';
                                nav.innerHTML = `
                                    <button class="carousel-btn prev" type="button"><i class="fas fa-chevron-left"></i></button>
                                    <button class="carousel-btn next" type="button"><i class="fas fa-chevron-right"></i></button>
                                `;
                                previewContainer.appendChild(nav);

                                const dots = document.createElement('div');
                                dots.className = 'carousel-dots';
                                for (let i = 0; i < input.files.length; i++) {
                                    dots.innerHTML += `<span class="carousel-dot ${i === 0 ? 'active' : ''}"></span>`;
                                }
                                previewContainer.appendChild(dots);
                            }

                            PopupManager.initializeCarousel(previewContainer);
                        }
                    };
                    reader.readAsDataURL(file);
                }
            });
        }
    };
    // AGGIUNGI QUESTO CODICE PRIMA DI: console.log('✅ manage_dealer.html inizializzato completamente!');

    // Edit product form submission - MANCAVA QUESTO!
    if (editProductForm) {
        editProductForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            const formData = new FormData(this);
            const productId = document.getElementById('edit-product-id').value;
            const spinner = this.querySelector('.spinner');
            const submitButton = this.querySelector('button[type="submit"]');

            if (spinner) spinner.classList.add('active');
            if (submitButton) submitButton.disabled = true;

            try {
                // USA IL METODO PUT CHE GIÀ ESISTE NEL CONTROLLER
                const response = await fetch(`/rest/api/products/${productId}`, {
                    method: 'PUT',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const data = await response.json();

                if (response.ok) {
                    showToast(data.message || 'Auto modificata con successo!', 'success');
                    closeModal('edit-product-modal');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.message || 'Errore durante la modifica', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                if (spinner) spinner.classList.remove('active');
                if (submitButton) submitButton.disabled = false;
            }
        });
    }
    // Remove highlight functionality - MANCAVA QUESTO!
    removeHighlightProductLinks.forEach(link => {
        link.addEventListener('click', async function (e) {
            e.preventDefault();
            const productId = this.getAttribute('data-product-id');

            try {
                const response = await fetch(`/rest/products/${productId}/remove-featured`, {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content,
                        'Content-Type': 'application/json'
                    }
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    showToast(data.message || 'Evidenza rimossa!', 'success');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.error || 'Errore durante la rimozione evidenziazione', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });
    // Highlight product functionality - MANCAVA QUESTO!
    highlightProductLinks.forEach(link => {
        link.addEventListener('click', async function (e) {
            e.preventDefault();
            const productId = this.getAttribute('data-product-id');

            try {
                // USA L'ENDPOINT CHE GIÀ ESISTE
                const response = await fetch(`/rest/products/${productId}/set-featured`, {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content,
                        'Content-Type': 'application/json'
                    }
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    showToast(data.message || 'Prodotto messo in evidenza!', 'success');
                    setTimeout(() => location.reload(), 1500);
                } else {
                    showToast(data.error || 'Errore durante l\'evidenziazione', 'error');
                }
            } catch (error) {
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });


    console.log('✅ manage_dealer.html inizializzato completamente!');
});
