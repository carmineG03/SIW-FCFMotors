document.addEventListener('DOMContentLoaded', function() {
    // Main DOM elements
    const addCarForm = document.getElementById('dealer-add-car-form');
    const editProductForm = document.getElementById('edit-product-form');
    const editProductPopup = document.getElementById('edit-product-popup');
    const closeEditProductPopupButton = document.getElementById('close-edit-product-popup');
    const cancelEditProductButton = document.getElementById('cancel-edit-product');
    const toast = document.getElementById('toast');
    const isFeaturedCheckbox = document.getElementById('dealer-car-highlighted');
    const featureDurationField = document.getElementById('dealer-car-feature-duration-field');
    const productsSection = document.getElementById('dealer-added-cars');

    // Debug: Check if products section exists
    console.log('Sezione prodotti trovata:', productsSection ? 'Sì' : 'No');

    // Function to update select labels
    function updateSelectLabel(selectElement) {
        const label = selectElement.parentElement.querySelector('label');
        if (label && selectElement.value) {
            label.style.top = '-10px';
            label.style.left = '10px';
            label.style.fontSize = '12px';
            label.style.color = '#F5A623';
            label.style.background = 'rgba(0, 0, 0, 0.8)';
            label.style.padding = '2px 6px';
            label.style.borderRadius = '4px';
            label.style.maxWidth = 'fit-content';
        } else if (label) {
            label.style.top = '50%';
            label.style.left = '15px';
            label.style.transform = 'translateY(-50%)';
            label.style.fontSize = '14px';
            label.style.color = 'rgba(255, 255, 255, 0.9)';
            label.style.background = 'none';
            label.style.padding = '0';
            label.style.borderRadius = '0';
            label.style.maxWidth = 'calc(100% - 30px)';
        }
    }

    // Function to initialize select labels
    function initializeSelectLabels(form) {
        const selects = form.querySelectorAll('select');
        selects.forEach(select => {
            updateSelectLabel(select);
            select.addEventListener('change', () => updateSelectLabel(select));
            select.addEventListener('focus', () => updateSelectLabel(select));
        });
    }

    // Initialize select labels for forms
    if (addCarForm) initializeSelectLabels(addCarForm);
    if (editProductForm) initializeSelectLabels(editProductForm);

    // Function to show toast notification
    function showToast(message, type) {
        if (!toast) {
            console.error('Elemento toast non trovato nel DOM');
            return;
        }
        toast.textContent = message;
        toast.className = `toast ${type}`;
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(20px)';
        }, 3000);
    }

    // Function to open edit product popup
    function openEditProductPopup() {
        if (editProductPopup && editProductForm) {
            console.log('Apertura popup di modifica');
            editProductPopup.style.display = 'flex';
        } else {
            console.error('Pop-up o form di modifica non trovati nel DOM');
            showToast('Errore: Pop-up o form di modifica non trovati', 'error');
        }
    }

    // Function to close edit product popup
    function closeEditPopup() {
        if (editProductPopup) {
            console.log('Chiusura popup di modifica');
            editProductPopup.style.display = 'none';
            if (editProductForm) {
                editProductForm.reset();
                editProductForm.action = ''; // Reset action
                initializeSelectLabels(editProductForm);
            }
        } else {
            console.error('Pop-up di modifica non trovato nel DOM');
        }
    }

    // Function to check featured products limit
    async function checkFeaturedLimit() {
        try {
            console.log('Esecuzione di checkFeaturedLimit');
            const response = await fetch('/rest/api/users/featured-limit', {
                method: 'GET',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            });
            const data = await response.json();
            console.log('Risposta da /rest/api/users/featured-limit:', data);

            if (response.ok) {
                const { currentFeaturedCount, maxFeaturedProducts } = data;
                console.log(`currentFeaturedCount: ${currentFeaturedCount}, maxFeaturedProducts: ${maxFeaturedProducts}`);
                const isLimitReached = currentFeaturedCount >= maxFeaturedProducts;

                const highlightLinks = document.querySelectorAll('.highlight-product-link');
                console.log(`Trovati ${highlightLinks.length} pulsanti .highlight-product-link`);
                highlightLinks.forEach(link => {
                    if (isLimitReached) {
                        console.log(`Disabilitazione del pulsante con data-product-id: ${link.getAttribute('data-product-id')}`);
                        link.classList.add('disabled');
                        link.style.pointerEvents = 'none';
                        link.style.opacity = '0.5';
                        link.title = 'Limite massimo di prodotti in evidenza raggiunto';
                    } else {
                        console.log(`Abilitazione del pulsante con data-product-id: ${link.getAttribute('data-product-id')}`);
                        link.classList.remove('disabled');
                        link.style.pointerEvents = 'auto';
                        link.style.opacity = '1';
                        link.title = '';
                    }
                });

                if (isFeaturedCheckbox) {
                    if (isLimitReached) {
                        console.log('Disabilitazione del checkbox isFeaturedCheckbox');
                        isFeaturedCheckbox.disabled = true;
                        isFeaturedCheckbox.title = 'Limite massimo di prodotti in evidenza raggiunto';
                        featureDurationField.style.display = 'none';
                    } else {
                        console.log('Abilitazione del checkbox isFeaturedCheckbox');
                        isFeaturedCheckbox.disabled = false;
                        isFeaturedCheckbox.title = '';
                        featureDurationField.style.display = isFeaturedCheckbox.checked ? 'block' : 'none';
                    }
                } else {
                    console.warn('isFeaturedCheckbox non trovato nel DOM');
                }

                return { success: true, isLimitReached };
            } else {
                console.error('Errore nella risposta del server:', data.message);
                if (data.message === 'Nessun abbonamento associato. Contatta l\'assistenza.') {
                    showToast('Nessun abbonamento attivo. Alcune funzionalità potrebbero essere limitate.', 'error');
                    return { success: false, isLimitReached: true };
                } else {
                    showToast(data.message || 'Errore nel recupero del limite di evidenza', 'error');
                    return { success: false };
                }
            }
        } catch (error) {
            console.error('Errore durante il recupero del limite di evidenza:', error);
            showToast('Errore di rete durante il recupero del limite', 'error');
            return { success: false };
        }
    }

    checkFeaturedLimit();

    // 1. Submit aggiunta auto
    if (addCarForm) {
        addCarForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            console.log('Invio form di aggiunta auto');

            const modelInput = document.getElementById('dealer-car-model');
            const brandInput = document.getElementById('dealer-car-brand');
            const categoryInput = document.getElementById('dealer-car-category');
            const descriptionInput = document.getElementById('dealer-car-description');
            const priceInput = document.getElementById('dealer-car-price');
            const mileageInput = document.getElementById('dealer-car-mileage');
            const yearInput = document.getElementById('dealer-car-year');
            const fuelTypeInput = document.getElementById('dealer-car-fuelType');
            const transmissionInput = document.getElementById('dealer-car-transmission');
            const imageInputs = addCarForm.querySelectorAll('input[name="images"]');
            const dealerIdInput = addCarForm.querySelector('input[name="dealerId"]');
            const isFeaturedInput = document.getElementById('dealer-car-highlighted');
            const featureDurationInput = document.getElementById('dealer-car-feature-duration');
            const spinner = addCarForm.querySelector('.spinner');
            const submitButton = addCarForm.querySelector('button[type="submit"]');

            if (!modelInput || !priceInput || !spinner || !dealerIdInput) {
                console.error('Elementi del form non trovati:', { modelInput, priceInput, spinner, dealerIdInput });
                showToast('Errore: Elementi del form non trovati.', 'error');
                return;
            }

            const formData = new FormData();
            formData.append('dealerId', dealerIdInput.value);
            formData.append('model', modelInput.value.trim());
            if (brandInput.value) formData.append('brand', brandInput.value.trim());
            if (categoryInput.value) formData.append('category', categoryInput.value.trim());
            if (descriptionInput.value) formData.append('description', descriptionInput.value.trim());
            formData.append('price', priceInput.value);
            if (mileageInput.value) formData.append('mileage', mileageInput.value);
            if (yearInput.value) formData.append('year', yearInput.value);
            if (fuelTypeInput.value) formData.append('fuelType', fuelTypeInput.value);
            if (transmissionInput.value) formData.append('transmission', transmissionInput.value);
            const imageFiles = Array.from(imageInputs).flatMap(input => Array.from(input.files)).filter(file => file);
            console.log('Numero di immagini selezionate:', imageFiles.length);
            imageFiles.forEach((file, index) => {
                console.log(`Immagine ${index + 1}: ${file.name}, size: ${file.size} bytes, type: ${file.type}`);
                formData.append('images', file);
            });
            formData.append('isFeatured', isFeaturedInput ? isFeaturedInput.checked : false);
            if (isFeaturedInput.checked && featureDurationInput.value) {
                formData.append('featuredUntil', featureDurationInput.value);
            }

            let isValid = true;
            if (!modelInput.value.trim()) {
                modelInput.classList.add('invalid');
                isValid = false;
            } else {
                modelInput.classList.remove('invalid');
            }

            if (!priceInput.value || parseFloat(priceInput.value) <= 0) {
                priceInput.classList.add('invalid');
                isValid = false;
            } else {
                priceInput.classList.remove('invalid');
            }

            if (isFeaturedInput.checked && (!featureDurationInput.value || parseInt(featureDurationInput.value) <= 0)) {
                featureDurationInput.classList.add('invalid');
                isValid = false;
            } else if (featureDurationInput) {
                featureDurationInput.classList.remove('invalid');
            }

            if (imageFiles.length > 10) {
                showToast('Puoi caricare un massimo di 10 immagini per prodotto', 'error');
                isValid = false;
            }

            if (imageFiles.length === 0) {
                console.warn('Nessuna immagine selezionata');
                showToast('Seleziona almeno un\'immagine per il prodotto', 'error');
                isValid = false;
            }

            if (!isValid) {
                console.error('Validazione fallita:', Object.fromEntries(formData));
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            console.log('Payload inviato:', Object.fromEntries(formData));

            spinner.classList.add('active');
            submitButton.disabled = true;

            try {
                const response = await fetch(addCarForm.action, {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    console.error('Errore server:', errorData);
                    showToast(errorData.message || 'Errore durante l\'aggiunta dell\'auto.', 'error');
                    throw new Error(errorData.message || 'Errore durante l\'aggiunta dell\'auto.');
                }

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                showToast(responseData.message || 'Auto aggiunta con successo!', 'success');
                addCarForm.reset();
                initializeSelectLabels(addCarForm);
                await checkFeaturedLimit();
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    } else {
        console.error('Form di aggiunta auto non trovato nel DOM');
    }

    // Handle featured checkbox toggle
    if (isFeaturedCheckbox && featureDurationField) {
        isFeaturedCheckbox.addEventListener('change', function() {
            featureDurationField.style.display = this.checked && !this.disabled ? 'block' : 'none';
        });
    }

    // Handle dealer edit form toggle
    const editDealerButton = document.getElementById('edit-dealer-button');
    const displayDealerDetails = document.getElementById('display-dealer-details');
    const dealershipForm = document.getElementById('dealership-form');
    const cancelEditDealerButton = document.getElementById('cancel-edit-dealer');

    if (editDealerButton && displayDealerDetails && dealershipForm && cancelEditDealerButton) {
        editDealerButton.addEventListener('click', function(event) {
            event.preventDefault();
            console.log('Mostra form di modifica concessionario');
            displayDealerDetails.style.display = 'none';
            dealershipForm.style.display = 'block';
        });

        cancelEditDealerButton.addEventListener('click', function() {
            console.log('Nascondi form di modifica concessionario');
            displayDealerDetails.style.display = 'block';
            dealershipForm.style.display = 'none';
        });
    }

    // Handle dealer edit form submission
    if (dealershipForm) {
        dealershipForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            console.log('Invio form di modifica concessionario');

            const idInput = dealershipForm.querySelector('input[name="id"]');
            const nameInput = document.getElementById('edit-dealership-name');
            const descriptionInput = document.getElementById('edit-dealership-description');
            const addressInput = document.getElementById('edit-dealership-address');
            const phoneInput = document.getElementById('edit-dealership-phone');
            const emailInput = document.getElementById('edit-dealership-email');
            const imageInputs = dealershipForm.querySelectorAll('input[name="images"]');
            const spinner = dealershipForm.querySelector('.spinner');
            const submitButton = dealershipForm.querySelector('button[type="submit"]');

            if (!idInput || !nameInput) {
                console.error('Campi del form di modifica concessionario non trovati');
                showToast('Errore: Campi del form di modifica concessionario non trovati.', 'error');
                return;
            }

            const dealerId = idInput.value;
            if (!dealerId || dealerId === 'undefined') {
                console.error('Dealer ID non valido:', dealerId);
                showToast('Errore: ID del concessionario non valido.', 'error');
                return;
            }

            const formData = new FormData();
            formData.append('name', nameInput.value.trim());
            if (descriptionInput.value) formData.append('description', descriptionInput.value.trim());
            if (addressInput.value) formData.append('address', addressInput.value.trim());
            if (phoneInput.value) formData.append('phone', phoneInput.value.trim());
            if (emailInput.value) formData.append('email', emailInput.value.trim());
            const imageFiles = Array.from(imageInputs).map(input => input.files[0]).filter(file => file);
            imageFiles.forEach((file, index) => {
                console.log(`Immagine ${index + 1}: ${file.name}, size: ${file.size} bytes, type: ${file.type}`);
                formData.append('images', file);
            });

            let isValid = true;
            if (!nameInput.value.trim()) {
                nameInput.classList.add('invalid');
                isValid = false;
            } else {
                nameInput.classList.remove('invalid');
            }

            const phonePattern = /^\+?[0-9\s\-]{6,15}$/;
            if (phoneInput.value && !phonePattern.test(phoneInput.value.trim())) {
                phoneInput.classList.add('invalid');
                isValid = false;
                showToast('Inserisci un numero di telefono valido', 'error');
            } else {
                phoneInput.classList.remove('invalid');
            }

            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (emailInput.value && !emailPattern.test(emailInput.value.trim())) {
                emailInput.classList.add('invalid');
                isValid = false;
                showToast('Inserisci un indirizzo email valido', 'error');
            } else {
                emailInput.classList.remove('invalid');
            }

            if (imageFiles.length > 4) {
                showToast('Puoi caricare un massimo di 4 immagini per concessionario', 'error');
                isValid = false;
            }

            if (!isValid) {
                console.error('Validazione fallita:', Object.fromEntries(formData));
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            console.log('Payload modifica concessionario:', Object.fromEntries(formData));

            spinner.classList.add('active');
            submitButton.disabled = true;

            try {
                const response = await fetch(`/rest/api/dealers/${dealerId}`, {
                    method: 'PUT',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (e) {
                        console.error('Risposta non JSON:', await response.text());
                        throw new Error('Errore del server: risposta non valida.');
                    }
                    console.error('Errore server:', errorData);
                    throw new Error(errorData.message || 'Errore durante la modifica del concessionario.');
                }

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                showToast(responseData.message || 'Concessionario modificato con successo!', 'success');
                dealershipForm.style.display = 'none';
                displayDealerDetails.style.display = 'block';
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    } else {
        console.error('Form di modifica concessionario non trovato nel DOM');
    }

    // 2. Popolamento form modifica auto
    const editProductLinks = document.querySelectorAll('.edit-product-link');
    const deleteProductLinks = document.querySelectorAll('.delete-product-link');
    const highlightProductLinks = document.querySelectorAll('.highlight-product-link');
    const removeHighlightProductLinks = document.querySelectorAll('.remove-highlight-product-link');

    console.log(`Trovati ${editProductLinks.length} pulsanti .edit-product-link`);

    async function populateEditForm(productId) {
        console.log(`Inizio populateEditForm per productId: ${productId}`);
        try {
            const response = await fetch(`/rest/api/products/${productId}`, {
                method: 'GET',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            });

            console.log(`Stato risposta API: ${response.status}`);
            if (response.ok) {
                const product = await response.json();
                console.log('Dati prodotto ricevuti:', product);

                // Set form action dynamically
                if (editProductForm) {
                    editProductForm.action = `/rest/api/products/${productId}`;
                }

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
                    if (element) {
                        element.value = value;
                    } else {
                        console.error(`Elemento con ID ${id} non trovato nel DOM`);
                    }
                }

                initializeSelectLabels(editProductForm);
                openEditProductPopup();
            } else {
                const errorData = await response.json();
                console.error('Errore API:', errorData);
                showToast(errorData.message || 'Errore durante il recupero dei dati del prodotto.', 'error');
            }
        } catch (error) {
            console.error('Errore di rete in populateEditForm:', error);
            showToast('Errore di rete: ' + error.message, 'error');
        }
    }

    if (editProductLinks && editProductForm && cancelEditProductButton && closeEditProductPopupButton) {
        editProductLinks.forEach(link => {
            link.addEventListener('click', async function(event) {
                event.preventDefault();
                const productId = this.getAttribute('data-product-id');
                console.log(`Click su Modifica per productId: ${productId}`);
                await populateEditForm(productId);
            });
        });

        cancelEditProductButton.addEventListener('click', closeEditPopup);
        closeEditProductPopupButton.addEventListener('click', closeEditPopup);
    } else {
        console.error('Elementi per la modifica del prodotto non trovati:', {
            editProductLinks: editProductLinks.length,
            editProductForm: !!editProductForm,
            cancelEditProductButton: !!cancelEditProductButton,
            closeEditProductPopupButton: !!closeEditProductPopupButton
        });
    }

    // 3. Submit modifica auto
    if (editProductForm) {
        editProductForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            console.log('Invio form di modifica prodotto');

            const modelInput = document.getElementById('edit-product-model');
            const priceInput = document.getElementById('edit-product-price');
            const imageInputs = editProductForm.querySelectorAll('input[name="images"]');
            const spinner = editProductForm.querySelector('.spinner');
            const submitButton = editProductForm.querySelector('button[type="submit"]');

            const formData = new FormData();
            formData.append('id', document.getElementById('edit-product-id').value);
            formData.append('model', modelInput.value.trim());
            formData.append('brand', document.getElementById('edit-product-brand').value.trim());
            formData.append('category', document.getElementById('edit-product-category').value.trim());
            formData.append('description', document.getElementById('edit-product-description').value.trim());
            formData.append('price', priceInput.value);
            formData.append('mileage', document.getElementById('edit-product-mileage').value);
            formData.append('year', document.getElementById('edit-product-year').value);
            formData.append('fuelType', document.getElementById('edit-product-fuelType').value);
            formData.append('transmission', document.getElementById('edit-product-transmission').value);
            const imageFiles = Array.from(imageInputs).map(input => input.files[0]).filter(file => file);
            imageFiles.forEach((file, index) => {
                console.log(`Immagine prodotto ${index + 1}: ${file.name}, size: ${file.size} bytes, type: ${file.type}`);
                formData.append('images', file);
            });

            let isValid = true;
            if (!modelInput.value.trim()) {
                modelInput.classList.add('invalid');
                isValid = false;
            } else {
                modelInput.classList.remove('invalid');
            }

            if (!priceInput.value || parseFloat(priceInput.value) <= 0) {
                priceInput.classList.add('invalid');
                isValid = false;
            } else {
                priceInput.classList.remove('invalid');
            }

            if (imageFiles.length > 10) {
                showToast('Puoi caricare un massimo di 10 immagini per prodotto', 'error');
                isValid = false;
            }

            if (!isValid) {
                console.error('Validazione fallita:', Object.fromEntries(formData));
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            console.log('Payload modifica prodotto:', Object.fromEntries(formData));

            spinner.classList.add('active');
            submitButton.disabled = true;

            try {
                const response = await fetch(editProductForm.action, {
                    method: 'PUT',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (e) {
                        console.error('Risposta non JSON:', await response.text());
                        throw new Error('Errore del server: risposta non valida.');
                    }
                    console.error('Errore server:', errorData);
                    throw new Error(errorData.message || 'Errore durante la modifica dell\'auto.');
                }

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                showToast(responseData.message || 'Auto modificata con successo!', 'success');
                closeEditPopup();
                await checkFeaturedLimit();
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    } else {
        console.error('Form di modifica prodotto non trovato nel DOM');
    }

    // Handle delete product links
    deleteProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            const productId = this.getAttribute('data-product-id');
            console.log(`Click su Elimina per productId: ${productId}`);
            if (!confirm('Sei sicuro di voler eliminare questo prodotto?')) return;

            try {
                const response = await fetch(`/rest/api/products/${productId}`, {
                    method: 'DELETE',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (e) {
                        console.error('Risposta non JSON:', await response.text());
                        throw new Error('Errore del server: risposta non valida.');
                    }
                    console.error('Errore server:', errorData);
                    throw new Error(errorData.message || 'Errore durante l\'eliminazione dell\'auto.');
                }

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                showToast(responseData.message || 'Auto eliminata con successo!', 'success');
                await checkFeaturedLimit();
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Handle highlight product links
    highlightProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            const productId = this.getAttribute('data-product-id');
            console.log(`Click su Metti in Evidenza per productId: ${productId}`);
            if (this.classList.contains('disabled')) {
                showToast('Limite massimo di prodotti in evidenza raggiunto.', 'error');
                return;
            }

            const durationInput = prompt('Inserisci la durata in giorni per l\'evidenza (es. 7):');
            const duration = parseInt(durationInput);

            if (isNaN(duration) || duration <= 0) {
                console.error('Durata non valida:', durationInput);
                showToast('Inserisci una durata valida (numero di giorni maggiore di 0).', 'error');
                return;
            }

            try {
                const response = await fetch(`/rest/api/products/${productId}/highlight`, {
                    method: 'POST',
                    body: JSON.stringify({ duration }),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (e) {
                        console.error('Risposta non JSON:', await response.text());
                        throw new Error('Errore del server: risposta non valida.');
                    }
                    console.error('Errore server:', errorData);
                    throw new Error(errorData.message || 'Errore durante la messa in evidenza.');
                }

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                showToast(responseData.message || 'Auto messa in evidenza con successo!', 'success');
                await checkFeaturedLimit();
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Handle remove highlight product links
    removeHighlightProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            const productId = this.getAttribute('data-product-id');
            console.log(`Tentativo di rimozione evidenza per prodotto ID: ${productId}`);

            try {
                const response = await fetch(`/rest/api/products/${productId}/remove-highlight`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (!response.ok) {
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (e) {
                        console.error('Risposta non JSON:', await response.text());
                        throw new Error('Errore del server: risposta non valida.');
                    }
                    console.error('Errore server:', errorData);
                    throw new Error(errorData.message || 'Errore durante la rimozione dell\'evidenza.');
                }

                const responseData = await response.json();
                console.log('Risposta da /rest/api/products/remove-highlight:', responseData);
                showToast(responseData.message || 'Evidenza rimossa con successo!', 'success');
                await checkFeaturedLimit();
                setTimeout(() => {
                    const baseUrl = window.location.pathname;
                    window.location.href = baseUrl + '?t=' + new Date().getTime();
                }, 1500);
            } catch (error) {
                console.error('Errore di rete durante la rimozione dell\'evidenza:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Handle carousel navigation for dealer images
    const dealerCarousel = document.querySelector('.dealer-carousel');
    if (dealerCarousel) {
        const items = dealerCarousel.querySelectorAll('.dealer-image-item');
        const dots = dealerCarousel.querySelectorAll('.dots .dot');
        const prevBtn = dealerCarousel.querySelector('.carousel-btn.prev');
        const nextBtn = dealerCarousel.querySelector('.carousel-btn.next');

        if (items.length > 1 && prevBtn && nextBtn) {
            let currentIndex = 0;

            const updateCarousel = () => {
                items.forEach((item, index) => {
                    item.classList.toggle('active', index === currentIndex);
                    if (dots[index]) dots[index].classList.toggle('active', index === currentIndex);
                });
                dealerCarousel.style.transform = `translateX(-${currentIndex * 100}%)`;
            };

            prevBtn.addEventListener('click', () => {
                currentIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
                updateCarousel();
            });

            nextBtn.addEventListener('click', () => {
                currentIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0;
                updateCarousel();
            });

            dots.forEach((dot, index) => {
                dot.addEventListener('click', () => {
                    currentIndex = index;
                    updateCarousel();
                });
            });

            updateCarousel();
        } else {
            console.log('Dealer carousel non inizializzato: una sola immagine o pulsanti di navigazione mancanti');
        }
    } else {
        console.log('Dealer carousel non trovato nel DOM');
    }

    // Handle carousel navigation for product images
    document.querySelectorAll('.car-grid .carousel').forEach(carousel => {
        const items = carousel.querySelectorAll('.category-item');
        const dots = carousel.querySelectorAll('.dots .dot');
        const prevBtn = carousel.querySelector('.carousel-btn.prev');
        const nextBtn = carousel.querySelector('.carousel-btn.next');

        if (items.length > 1 && prevBtn && nextBtn) {
            let currentIndex = 0;

            const updateProductCarousel = () => {
                items.forEach((item, index) => {
                    item.classList.toggle('active', index === currentIndex);
                    if (dots[index]) dots[index].classList.toggle('active', index === currentIndex);
                });
                carousel.style.transform = `translateX(-${currentIndex * 100}%)`;
            };

            prevBtn.addEventListener('click', () => {
                currentIndex = currentIndex > 0 ? currentIndex - 1 : items.length - 1;
                updateProductCarousel();
            });

            nextBtn.addEventListener('click', () => {
                currentIndex = currentIndex < items.length - 1 ? currentIndex + 1 : 0;
                updateProductCarousel();
            });

            dots.forEach((dot, index) => {
                dot.addEventListener('click', () => {
                    currentIndex = index;
                    updateProductCarousel();
                });
            });

            updateProductCarousel();
        } else {
            console.log('Product carousel non inizializzato: una sola immagine o pulsanti di navigazione mancanti');
        }
    });

    // Animate sections and product cards
    const animatedSections = document.querySelectorAll('.animated-section');
    animatedSections.forEach(section => section.classList.add('visible'));

    const productCards = document.querySelectorAll('.car-card');
    console.log(`Trovate ${productCards.length} card di prodotti`);
    productCards.forEach((card, index) => {
        setTimeout(() => card.classList.add('visible'), index * 100);
    });
});