document.addEventListener('DOMContentLoaded', function() {
    // Elementi principali del DOM
    const addCarForm = document.getElementById('dealer-add-car-form');
    const editProductForm = document.getElementById('edit-product-form');
    const toast = document.getElementById('toast');
    const isFeaturedCheckbox = document.getElementById('dealer-car-highlighted');
    const featureDurationField = document.getElementById('dealer-car-feature-duration-field');
    const editIsFeaturedCheckbox = document.getElementById('edit-product-highlighted');
    const editFeatureDurationField = document.getElementById('edit-highlight-duration-field');

    // Funzione per mostrare il toast
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

    // Verifica il limite di prodotti in evidenza
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

                // Aggiorna lo stato dei pulsanti "Metti in evidenza"
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

                // Aggiorna lo stato del checkbox nel form di aggiunta
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
                showToast(data.message || 'Errore nel recupero del limite di evidenza', 'error');
                return { success: false };
            }
        } catch (error) {
            console.error('Errore durante il recupero del limite di evidenza:', error);
            showToast('Errore di rete durante il recupero del limite', 'error');
            return { success: false };
        }
    }

    // Esegui il controllo del limite al caricamento della pagina
    checkFeaturedLimit();

    // Gestione del form per aggiungere un'auto
    if (addCarForm) {
        addCarForm.addEventListener('submit', async function(event) {
            event.preventDefault();

            const modelInput = document.getElementById('dealer-car-model');
            const brandInput = document.getElementById('dealer-car-brand');
            const categoryInput = document.getElementById('dealer-car-category');
            const descriptionInput = document.getElementById('dealer-car-description');
            const priceInput = document.getElementById('dealer-car-price');
            const mileageInput = document.getElementById('dealer-car-mileage');
            const yearInput = document.getElementById('dealer-car-year');
            const fuelTypeInput = document.getElementById('dealer-car-fuelType');
            const transmissionInput = document.getElementById('dealer-car-transmission');
            const imageUrlInput = document.getElementById('dealer-car-imageUrl');
            const dealerIdInput = addCarForm.querySelector('input[name="dealerId"]');
            const spinner = addCarForm.querySelector('.spinner');

            if (!modelInput || !priceInput || !spinner || !dealerIdInput) {
                showToast('Errore: Elementi del form non trovati.', 'error');
                return;
            }

            const model = modelInput.value.trim();
            const brand = brandInput ? brandInput.value.trim() : '';
            const category = categoryInput ? categoryInput.value.trim() : '';
            const description = descriptionInput ? descriptionInput.value.trim() : '';
            const price = priceInput.value;
            const mileage = mileageInput ? mileageInput.value : '';
            const year = yearInput ? yearInput.value : '';
            const fuelType = fuelTypeInput ? fuelTypeInput.value : '';
            const transmission = transmissionInput ? transmissionInput.value : '';
            const imageUrl = imageUrlInput ? imageUrlInput.value.trim() : '';
            const dealerId = dealerIdInput.value;

            let isValid = true;
            if (!model) {
                modelInput.classList.add('invalid');
                isValid = false;
            } else {
                modelInput.classList.remove('invalid');
            }

            if (!price || parseFloat(price) <= 0) {
                priceInput.classList.add('invalid');
                isValid = false;
            } else {
                priceInput.classList.remove('invalid');
            }

            if (!isValid) {
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            const carData = {
                dealerId,
                model,
                brand,
                category,
                description,
                price,
                mileage,
                year,
                fuelType,
                transmission,
                imageUrl
            };

            console.log('Payload inviato:', carData);

            spinner.classList.add('active');
            const submitButton = addCarForm.querySelector('button[type="submit"]');
            submitButton.disabled = true;

            try {
                const response = await fetch(addCarForm.action, {
                    method: 'POST',
                    body: JSON.stringify(carData),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                if (response.ok) {
                    showToast(responseData.message || 'Auto aggiunta con successo!', 'success');
                    addCarForm.reset();
                    await checkFeaturedLimit();
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    showToast(responseData.message || 'Errore durante l\'aggiunta dell\'auto.', 'error');
                    await checkFeaturedLimit();
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    }

    // Gestione della visibilità del campo "Durata Evidenza" nel form di aggiunta
    if (isFeaturedCheckbox && featureDurationField) {
        isFeaturedCheckbox.addEventListener('change', function() {
            featureDurationField.style.display = this.checked && !this.disabled ? 'block' : 'none';
        });
    }

    // Gestione della visibilità del campo "Durata Evidenza" nel form di modifica
    if (editIsFeaturedCheckbox && editFeatureDurationField) {
        editIsFeaturedCheckbox.addEventListener('change', function() {
            editFeatureDurationField.style.display = this.checked ? 'block' : 'none';
        });
    }

    // Gestione del pulsante "Modifica Concessionario"
    const editDealerButton = document.getElementById('edit-dealer-button');
    const displayDealerDetails = document.getElementById('display-dealer-details');
    const dealershipForm = document.getElementById('dealership-form');
    const cancelEditDealerButton = document.getElementById('cancel-edit-dealer');

    if (editDealerButton && displayDealerDetails && dealershipForm && cancelEditDealerButton) {
        editDealerButton.addEventListener('click', function(event) {
            event.preventDefault();
            displayDealerDetails.style.display = 'none';
            dealershipForm.style.display = 'block';
        });

        cancelEditDealerButton.addEventListener('click', function() {
            displayDealerDetails.style.display = 'block';
            dealershipForm.style.display = 'none';
        });
    }

    // Gestione del form di modifica del concessionario
    if (dealershipForm) {
        dealershipForm.addEventListener('submit', async function(event) {
            event.preventDefault();

            const idInput = dealershipForm.querySelector('input[name="id"]');
            const nameInput = document.getElementById('edit-dealership-name');
            const descriptionInput = document.getElementById('edit-dealership-description');
            const addressInput = document.getElementById('edit-dealership-address');
            const phoneInput = document.getElementById('edit-dealership-phone');
            const emailInput = document.getElementById('edit-dealership-email');
            const imagePathInput = document.getElementById('edit-dealership-imagePath');
            const spinner = dealershipForm.querySelector('.spinner');
            const submitButton = dealershipForm.querySelector('button[type="submit"]');

            if (!idInput || !nameInput) {
                showToast('Errore: Campi del form di modifica concessionario non trovati.', 'error');
                return;
            }

            const dealerData = {
                id: idInput.value,
                name: nameInput.value.trim(),
                description: descriptionInput ? descriptionInput.value.trim() : '',
                address: addressInput ? addressInput.value.trim() : '',
                phone: phoneInput ? phoneInput.value.trim() : '',
                email: emailInput ? emailInput.value.trim() : '',
                imagePath: imagePathInput ? imagePathInput.value.trim() : '',
                isUpdate: "true"
            };

            let isValid = true;
            if (!dealerData.name) {
                nameInput.classList.add('invalid');
                isValid = false;
            } else {
                nameInput.classList.remove('invalid');
            }

            if (!isValid) {
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            spinner.classList.add('active');
            submitButton.disabled = true;

            try {
                const response = await fetch(dealershipForm.action, {
                    method: 'POST',
                    body: JSON.stringify(dealerData),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const responseData = await response.json();
                if (response.ok) {
                    showToast(responseData.message || 'Concessionario modificato con successo!', 'success');
                    dealershipForm.style.display = 'none';
                    displayDealerDetails.style.display = 'block';
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    showToast(responseData.message || 'Errore durante la modifica del concessionario.', 'error');
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    }

    // Gestione del form di modifica del prodotto
    const cancelEditProductButton = document.getElementById('cancel-edit-product');
    const editProductLinks = document.querySelectorAll('.edit-product-link');
    const deleteProductLinks = document.querySelectorAll('.delete-product-link');
    const highlightProductLinks = document.querySelectorAll('.highlight-product-link');
    const removeHighlightProductLinks = document.querySelectorAll('.remove-highlight-product-link');

    // Funzione per popolare il form di modifica con i dati del prodotto
    async function populateEditForm(productId) {
        try {
            const response = await fetch(`/rest/api/products/${productId}`, {
                method: 'GET',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            });

            if (response.ok) {
                const product = await response.json();
                document.getElementById('edit-product-id').value = product.id || '';
                document.getElementById('edit-product-model').value = product.model || '';
                document.getElementById('edit-product-brand').value = product.brand || '';
                document.getElementById('edit-product-category').value = product.category || '';
                document.getElementById('edit-product-description').value = product.description || '';
                document.getElementById('edit-product-price').value = product.price || '';
                document.getElementById('edit-product-mileage').value = product.mileage || '';
                document.getElementById('edit-product-year').value = product.year || '';
                document.getElementById('edit-product-fuelType').value = product.fuelType || '';
                document.getElementById('edit-product-transmission').value = product.transmission || '';
                document.getElementById('edit-product-imageUrl').value = product.imageUrl || '';
                document.getElementById('edit-product-highlighted').checked = product.highlighted || false;
                document.getElementById('edit-product-highlight-duration').value = product.highlightExpiration ?
                    Math.ceil((new Date(product.highlightExpiration) - new Date()) / (1000 * 60 * 60 * 24)) : '';
                if (editFeatureDurationField) {
                    editFeatureDurationField.style.display = product.highlighted ? 'block' : 'none';
                }
            } else {
                showToast('Errore durante il recupero dei dati del prodotto.', 'error');
            }
        } catch (error) {
            console.error('Errore di rete:', error);
            showToast('Errore di rete: ' + error.message, 'error');
        }
    }

    // Gestione del click su "Modifica"
    if (editProductLinks && editProductForm && cancelEditProductButton) {
        editProductLinks.forEach(link => {
            link.addEventListener('click', async function(event) {
                event.preventDefault();
                const productId = this.getAttribute('data-product-id');
                await populateEditForm(productId);
                const heroContent = editProductForm.closest('.hero-content');
                if (heroContent) heroContent.style.display = 'block';
                editProductForm.style.display = 'block';
            });
        });

        cancelEditProductButton.addEventListener('click', function() {
            editProductForm.style.display = 'none';
            const heroContent = editProductForm.closest('.hero-content');
            if (heroContent) heroContent.style.display = 'none';
            editProductForm.reset();
        });
    }

    // Gestione dell'invio del form di modifica
    if (editProductForm) {
        editProductForm.addEventListener('submit', async function(event) {
            event.preventDefault();

            const productIdInput = document.getElementById('edit-product-id');
            const modelInput = document.getElementById('edit-product-model');
            const brandInput = document.getElementById('edit-product-brand');
            const categoryInput = document.getElementById('edit-product-category');
            const descriptionInput = document.getElementById('edit-product-description');
            const priceInput = document.getElementById('edit-product-price');
            const mileageInput = document.getElementById('edit-product-mileage');
            const yearInput = document.getElementById('edit-product-year');
            const fuelTypeInput = document.getElementById('edit-product-fuelType');
            const transmissionInput = document.getElementById('edit-product-transmission');
            const imageUrlInput = document.getElementById('edit-product-imageUrl');
            const isFeaturedInput = document.getElementById('edit-product-highlighted');
            const featureDurationInput = document.getElementById('edit-product-highlight-duration');
            const spinner = editProductForm.querySelector('.spinner');
            const submitButton = editProductForm.querySelector('button[type="submit"]');

            if (!productIdInput || !modelInput || !priceInput) {
                showToast('Errore: Campi del form di modifica non trovati.', 'error');
                return;
            }

            const productData = {
                id: productIdInput.value,
                model: modelInput.value.trim(),
                brand: brandInput ? brandInput.value.trim() : '',
                category: categoryInput ? categoryInput.value.trim() : '',
                description: descriptionInput ? descriptionInput.value.trim() : '',
                price: priceInput.value,
                mileage: mileageInput ? mileageInput.value : '',
                year: yearInput ? yearInput.value : '',
                fuelType: fuelTypeInput ? fuelTypeInput.value : '',
                transmission: transmissionInput ? transmissionInput.value : '',
                imageUrl: imageUrlInput ? imageUrlInput.value.trim() : '',
                isFeatured: isFeaturedInput ? isFeaturedInput.checked : false,
                featuredUntil: isFeaturedInput && isFeaturedInput.checked && featureDurationInput ? featureDurationInput.value : ''
            };

            let isValid = true;
            if (!productData.model) {
                modelInput.classList.add('invalid');
                isValid = false;
            } else {
                modelInput.classList.remove('invalid');
            }

            if (!productData.price || parseFloat(productData.price) <= 0) {
                priceInput.classList.add('invalid');
                isValid = false;
            } else {
                priceInput.classList.remove('invalid');
            }

            if (productData.isFeatured && (!productData.featuredUntil || parseInt(productData.featuredUntil) <= 0)) {
                featureDurationInput.classList.add('invalid');
                isValid = false;
            } else if (featureDurationInput) {
                featureDurationInput.classList.remove('invalid');
            }

            if (!isValid) {
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            console.log('Payload modifica prodotto:', productData);

            spinner.classList.add('active');
            submitButton.disabled = true;

            try {
                const response = await fetch(`/rest/api/products/${productData.id}`, {
                    method: 'PUT',
                    body: JSON.stringify(productData),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const responseData = await response.json();
                console.log('Risposta del server:', responseData);
                if (response.ok) {
                    showToast(responseData.message || 'Auto modificata con successo!', 'success');
                    editProductForm.style.display = 'none';
                    const heroContent = editProductForm.closest('.hero-content');
                    if (heroContent) heroContent.style.display = 'none';
                    editProductForm.reset();
                    await checkFeaturedLimit();
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    showToast(responseData.message || 'Errore durante la modifica dell\'auto.', 'error');
                    await checkFeaturedLimit();
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                spinner.classList.remove('active');
                submitButton.disabled = false;
            }
        });
    }

    // Gestione del click su "Elimina"
    deleteProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            const productId = this.getAttribute('data-product-id');
            if (!confirm('Sei sicuro di voler eliminare questo prodotto?')) return;

            try {
                const response = await fetch(`/rest/api/products/${productId}`, {
                    method: 'DELETE',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                if (response.ok) {
                    showToast('Auto eliminata con successo!', 'success');
                    await checkFeaturedLimit();
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    const responseData = await response.json();
                    showToast(responseData.message || 'Errore durante l\'eliminazione dell\'auto.', 'error');
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Gestione del click su "Metti in Evidenza"
    highlightProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            if (this.classList.contains('disabled')) {
                showToast('Limite massimo di prodotti in evidenza raggiunto.', 'error');
                return;
            }

            const productId = this.getAttribute('data-product-id');
            const durationInput = prompt('Inserisci la durata in giorni per l\'evidenza (es. 7):');
            const duration = parseInt(durationInput);

            if (isNaN(duration) || duration <= 0) {
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

                const responseData = await response.json();
                if (response.ok) {
                    showToast(responseData.message || 'Auto messa in evidenza con successo!', 'success');
                    await checkFeaturedLimit();
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    showToast(responseData.message || 'Errore durante la messa in evidenza.', 'error');
                    await checkFeaturedLimit();
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Gestione del click su "Rimuovi Evidenza"
    removeHighlightProductLinks.forEach(link => {
        link.addEventListener('click', async function(event) {
            event.preventDefault();
            const productId = this.getAttribute('data-product-id');
            console.log(`Tentativo di rimozione evidenza per prodotto ID: ${productId}`);

            try {
                const response = await fetch(`/rest/api/products/${productId}/remove-highlight`, {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                const responseData = await response.json();
                console.log('Risposta da /rest/api/products/remove-highlight:', responseData);

                if (response.ok) {
                    showToast(responseData.message || 'Evidenza rimossa con successo!', 'success');
                    const result = await checkFeaturedLimit();
                    console.log('Risultato di checkFeaturedLimit dopo rimozione:', result);
                    if (result.success && !result.isLimitReached) {
                        console.log('Limite non raggiunto, pulsanti e checkbox dovrebbero essere attivi');
                    }
                    setTimeout(() => window.location.reload(), 1500);
                } else {
                    showToast(responseData.message || 'Errore durante la rimozione dell\'evidenza.', 'error');
                }
            } catch (error) {
                console.error('Errore di rete durante la rimozione dell\'evidenza:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            }
        });
    });

    // Animazioni per le sezioni e le card
    const animatedSections = document.querySelectorAll('.animated-section');
    animatedSections.forEach(section => section.classList.add('visible'));

    const productCards = document.querySelectorAll('.product-card');
    productCards.forEach((card, index) => {
        setTimeout(() => card.classList.add('visible'), index * 100);
    });
});