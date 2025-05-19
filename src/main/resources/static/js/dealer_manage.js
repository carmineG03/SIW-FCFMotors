document.addEventListener('DOMContentLoaded', function() {
    const addCarForm = document.getElementById('dealer-add-car-form');
    const editProductForm = document.getElementById('edit-product-form');
    const toast = document.getElementById('toast');
    const highlightedCheckbox = document.getElementById('dealer-car-highlighted');
    const featureDurationField = document.getElementById('edit-feature-duration-field');

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

    // Debug: Verifica che il form esista
    if (!addCarForm) {
        console.error('Form #dealer-add-car-form non trovato nel DOM');
        showToast('Errore: Form per aggiungere auto non trovato.', 'error');
        return;
    }

    // Gestione dell'invio del form per aggiungere un'auto
    addCarForm.addEventListener('submit', async function(event) {
        event.preventDefault();

        const nameInput = document.getElementById('dealer-car-name');
        const priceInput = document.getElementById('dealer-car-price');
        const descriptionInput = document.getElementById('dealer-car-description');
        const imageUrlInput = document.getElementById('dealer-car-imageUrl');
        const highlightedInput = document.getElementById('dealer-car-highlighted');
        const featureDurationInput = document.getElementById('edit-product-feature-duration');
        const dealerIdInput = addCarForm.querySelector('input[name="dealerId"]');
        const spinner = addCarForm.querySelector('.spinner');

        if (!nameInput || !priceInput || !spinner || !dealerIdInput) {
            console.error('Uno o più elementi del form non sono stati trovati:', { nameInput, priceInput, spinner, dealerIdInput });
            showToast('Errore: Elementi del form non trovati.', 'error');
            return;
        }

        const name = nameInput.value.trim();
        const price = parseFloat(priceInput.value);
        const description = descriptionInput ? descriptionInput.value.trim() : '';
        const imageUrl = imageUrlInput ? imageUrlInput.value.trim() : '';
        const highlighted = highlightedInput ? highlightedInput.checked : false;
        const featureDuration = featureDurationInput && highlighted ? parseInt(featureDurationInput.value) || 0 : 0;
        const dealerId = dealerIdInput.value;

        let isValid = true;

        if (!name) {
            nameInput.classList.add('invalid');
            isValid = false;
        } else {
            nameInput.classList.remove('invalid');
        }

        if (!price || price <= 0) {
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
            dealerId: dealerId,
            name: name,
            description: description,
            price: price,
            imageUrl: imageUrl,
            highlighted: highlighted,
            featureDuration: featureDuration
        };

        spinner.classList.add('active');
        const submitButton = addCarForm.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.disabled = true;
        }

        try {
            const response = await fetch(addCarForm.action, {
                method: 'POST',
                body: JSON.stringify(carData),
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            });

            if (response.ok) {
                showToast('Auto aggiunta con successo!', 'success');
                addCarForm.reset();
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            } else if (response.status === 404) {
                showToast('Errore: Endpoint per aggiungere auto non trovato (404). Contatta l\'amministratore.', 'error');
            } else if (response.status === 415) {
                showToast('Errore: Formato dei dati non supportato (415). Contatta l\'amministratore.', 'error');
            } else {
                const errorText = await response.text();
                showToast('Errore durante l\'aggiunta dell\'auto: ' + errorText, 'error');
            }
        } catch (error) {
            console.error('Errore di rete:', error);
            showToast('Errore di rete: ' + error.message, 'error');
        } finally {
            spinner.classList.remove('active');
            if (submitButton) {
                submitButton.disabled = false;
            }
        }
    });

    // Gestione della visibilità del campo "Durata Evidenza" nel form di aggiunta
    if (highlightedCheckbox && featureDurationField) {
        highlightedCheckbox.addEventListener('change', function() {
            featureDurationField.style.display = this.checked ? 'block' : 'none';
        });
    } else {
        console.warn('Elementi per la gestione della durata evidenza non trovati:', { highlightedCheckbox, featureDurationField });
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

    // Gestione dell'invio del form di modifica del concessionario
    if (dealershipForm) {
        dealershipForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            console.log('Form di modifica concessionario inviato'); // Debug

            const idInput = dealershipForm.querySelector('input[name="id"]');
            const nameInput = document.getElementById('edit-dealership-name');
            const descriptionInput = document.getElementById('edit-dealership-description');
            const addressInput = document.getElementById('edit-dealership-address');
            const phoneInput = document.getElementById('edit-dealership-phone'); // Nuovo campo
            const emailInput = document.getElementById('edit-dealership-email'); // Nuovo campo
            const imagePathInput = document.getElementById('edit-dealership-imagePath');

            if (!idInput || !nameInput || !phoneInput || !emailInput) {
                console.error('Campi del form di modifica concessionario non trovati');
                showToast('Errore: Campi del form di modifica concessionario non trovati.', 'error');
                return;
            }

            const id = idInput.value;
            const name = nameInput.value.trim();
            const description = descriptionInput ? descriptionInput.value.trim() : '';
            const address = addressInput ? addressInput.value.trim() : '';
            const phone = phoneInput ? phoneInput.value.trim() : '';
            const email = emailInput ? emailInput.value.trim() : '';
            const imagePath = imagePathInput ? imagePathInput.value.trim() : '';

            let isValid = true;

            if (!name) {
                nameInput.classList.add('invalid');
                isValid = false;
                console.log('Validazione fallita: Nome concessionario non valido');
            } else {
                nameInput.classList.remove('invalid');
            }

            // Validazione opzionale per email e telefono
            if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                emailInput.classList.add('invalid');
                isValid = false;
                console.log('Validazione fallita: Email non valida');
            } else {
                emailInput.classList.remove('invalid');
            }

            if (phone && !/\+?[0-9\s\-]{6,15}/.test(phone)) {
                phoneInput.classList.add('invalid');
                isValid = false;
                console.log('Validazione fallita: Telefono non valido');
            } else {
                phoneInput.classList.remove('invalid');
            }

            if (!isValid) {
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            const dealerData = {
                id: id,
                name: name,
                description: description,
                address: address,
                phone: phone,
                email: email,
                imagePath: imagePath,
                isUpdate: "true"
            };

            console.log('Dati del form di modifica concessionario:', dealerData); // Debug

            const spinner = dealershipForm.querySelector('.spinner');
            if (spinner) {
                spinner.classList.add('active');
            }

            const submitButton = dealershipForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            try {
                console.log('Invio richiesta POST a /rest/api/dealers'); // Debug
                const response = await fetch(`/rest/api/dealers`, {
                    method: 'POST',
                    body: JSON.stringify(dealerData),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                console.log('Risposta ricevuta:', response.status, response.statusText); // Debug

                if (response.ok) {
                    const responseData = await response.json();
                    showToast(responseData.message || 'Concessionario modificato con successo!', 'success');
                    dealershipForm.style.display = 'none';
                    document.getElementById('display-dealer-details').style.display = 'block';
                    setTimeout(() => {
                        window.location.reload();
                    }, 1500);
                } else {
                    const errorData = await response.json();
                    console.error('Errore dal server:', errorData);
                    showToast(errorData.message || 'Errore durante la modifica del concessionario.', 'error');
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                if (spinner) {
                    spinner.classList.remove('active');
                }
                if (submitButton) {
                    submitButton.disabled = false;
                }
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
                document.getElementById('edit-product-name').value = product.name || '';
                document.getElementById('edit-product-description').value = product.description || '';
                document.getElementById('edit-product-price').value = product.price || '';
                document.getElementById('edit-product-imageUrl').value = product.imageUrl || '';
                document.getElementById('edit-product-highlighted').checked = product.highlighted || false;
                const editHighlightDurationField = document.getElementById('edit-highlight-duration-field');
                if (editHighlightDurationField) {
                    editHighlightDurationField.style.display = product.highlighted ? 'block' : 'none';
                }
                document.getElementById('edit-product-highlight-duration').value = product.featureDuration || '';
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
                console.log('Cliccato "Modifica" per il prodotto con ID:', productId); // Debug
                try {
                    await populateEditForm(productId);
                    // Rendi visibile il contenitore genitore
                    const heroContent = editProductForm.closest('.hero-content');
                    if (heroContent) {
                        heroContent.style.display = 'block';
                    } else {
                        console.warn('Contenitore .hero-content non trovato');
                    }
                    // Rendi visibile il form
                    editProductForm.style.display = 'block';
                    console.log('Form #edit-product-form reso visibile'); // Debug

                    // Listener per il pulsante "Salva" (debug)
                    const submitButton = editProductForm.querySelector('button[type="submit"]');
                    if (submitButton) {
                        submitButton.addEventListener('click', function(e) {
                            console.log('Pulsante "Salva" cliccato nel form di modifica'); // Debug
                        });
                    } else {
                        console.error('Pulsante "Salva" non trovato nel form #edit-product-form');
                    }

                    // Listener globale per i click nel form (debug)
                    editProductForm.addEventListener('click', function(e) {
                        console.log('Click rilevato nel form #edit-product-form, target:', e.target); // Debug
                    });
                } catch (error) {
                    console.error('Errore durante il popolamento del form di modifica:', error);
                    showToast('Errore durante il caricamento del form di modifica.', 'error');
                }
            });
        });

        cancelEditProductButton.addEventListener('click', function() {
            console.log('Cliccato "Annulla" nel form di modifica'); // Debug
            editProductForm.style.display = 'none';
            const heroContent = editProductForm.closest('.hero-content');
            if (heroContent) {
                heroContent.style.display = 'none';
            } else {
                console.warn('Contenitore .hero-content non trovato');
            }
            editProductForm.reset();
        });
    }

    // Gestione dell'invio del form di modifica
    if (editProductForm) {
        console.log('Form #edit-product-form trovato nel DOM'); // Debug
        editProductForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            console.log('Form di modifica inviato'); // Debug

            const productIdInput = document.getElementById('edit-product-id');
            const nameInput = document.getElementById('edit-product-name');
            const priceInput = document.getElementById('edit-product-price');
            const descriptionInput = document.getElementById('edit-product-description');
            const imageUrlInput = document.getElementById('edit-product-imageUrl');
            const highlightedInput = document.getElementById('edit-product-highlighted');
            const featureDurationInput = document.getElementById('edit-product-highlight-duration');

            if (!productIdInput || !nameInput || !priceInput) {
                console.error('Campi del form di modifica non trovati:', { productIdInput, nameInput, priceInput });
                showToast('Errore: Campi del form di modifica non trovati.', 'error');
                return;
            }

            const productId = productIdInput.value;
            const name = nameInput.value.trim();
            const price = parseFloat(priceInput.value);
            const description = descriptionInput ? descriptionInput.value.trim() : '';
            const imageUrl = imageUrlInput ? imageUrlInput.value.trim() : '';
            const highlighted = highlightedInput ? highlightedInput.checked : false;
            const featureDuration = highlighted && featureDurationInput ? parseInt(featureDurationInput.value) || 0 : 0;

            console.log('Dati del form di modifica:', { productId, name, price, description, imageUrl, highlighted, featureDuration }); // Debug

            let isValid = true;

            if (!name) {
                nameInput.classList.add('invalid');
                isValid = false;
                console.log('Validazione fallita: Nome non valido');
            } else {
                nameInput.classList.remove('invalid');
            }

            if (isNaN(price) || price <= 0) {
                priceInput.classList.add('invalid');
                isValid = false;
                console.log('Validazione fallita: Prezzo non valido');
            } else {
                priceInput.classList.remove('invalid');
            }

            if (!isValid) {
                showToast('Compila tutti i campi obbligatori correttamente.', 'error');
                return;
            }

            const productData = {
                id: productId,
                name: name,
                description: description,
                price: price,
                imageUrl: imageUrl,
                highlighted: highlighted,
                featureDuration: featureDuration
            };

            const spinner = editProductForm.querySelector('.spinner');
            if (spinner) {
                spinner.classList.add('active');
            } else {
                console.warn('Spinner non trovato nel form di modifica');
            }

            const submitButton = editProductForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            try {
                console.log('Invio richiesta PUT a /rest/api/products/' + productId); // Debug
                const response = await fetch(`/rest/api/products/${productId}`, {
                    method: 'PUT',
                    body: JSON.stringify(productData),
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                    }
                });

                console.log('Risposta ricevuta:', response.status, response.statusText); // Debug

                if (response.ok) {
                    const responseData = await response.json();
                    showToast(responseData.message || 'Auto modificata con successo!', 'success');
                    editProductForm.style.display = 'none';
                    const heroContent = editProductForm.closest('.hero-content');
                    if (heroContent) {
                        heroContent.style.display = 'none';
                    } else {
                        console.warn('Contenitore .hero-content non trovato');
                    }
                    editProductForm.reset();
                    setTimeout(() => {
                        window.location.reload();
                    }, 1500);
                } else {
                    const errorData = await response.json();
                    console.error('Errore dal server:', errorData);
                    showToast(errorData.message || 'Errore durante la modifica dell\'auto.', 'error');
                }
            } catch (error) {
                console.error('Errore di rete:', error);
                showToast('Errore di rete: ' + error.message, 'error');
            } finally {
                if (spinner) {
                    spinner.classList.remove('active');
                }
                if (submitButton) {
                    submitButton.disabled = false;
                }
            }
        });
    } else {
        console.error('Form #edit-product-form non trovato nel DOM'); // Debug
    }

    // Gestione del click su "Elimina"
    if (deleteProductLinks) {
        deleteProductLinks.forEach(link => {
            link.addEventListener('click', async function(event) {
                event.preventDefault();
                const productId = this.getAttribute('data-product-id');
                const confirmDelete = confirm('Sei sicuro di voler eliminare questo prodotto?');
                if (!confirmDelete) return;

                try {
                    const response = await fetch(`/rest/api/products/${productId}`, {
                        method: 'DELETE',
                        headers: {
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                        }
                    });

                    if (response.ok) {
                        showToast('Auto eliminata con successo!', 'success');
                        setTimeout(() => {
                            window.location.reload();
                        }, 1500);
                    } else {
                        const errorText = await response.text();
                        showToast('Errore durante l\'eliminazione dell\'auto: ' + errorText, 'error');
                    }
                } catch (error) {
                    console.error('Errore di rete:', error);
                    showToast('Errore di rete: ' + error.message, 'error');
                }
            });
        });
    }

    // Gestione del click su "Metti in Evidenza"
    if (highlightProductLinks) {
        highlightProductLinks.forEach(link => {
            link.addEventListener('click', async function(event) {
                event.preventDefault();
                const productId = this.getAttribute('data-product-id');

                const durationInput = prompt('Inserisci la durata in giorni per l\'evidenza (es. 7):');
                const duration = parseInt(durationInput);

                console.log('Durata inserita:', durationInput, 'Parsata:', duration); // Debug

                if (isNaN(duration) || duration <= 0) {
                    showToast('Inserisci una durata valida (numero di giorni maggiore di 0).', 'error');
                    return;
                }

                const highlightData = {
                    duration: duration.toString()
                };

                console.log('Dati inviati per l\'evidenza:', highlightData); // Debug

                try {
                    const response = await fetch(`/rest/api/products/${productId}/highlight`, {
                        method: 'POST',
                        body: JSON.stringify(highlightData),
                        headers: {
                            'Content-Type': 'application/json',
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                        }
                    });

                    console.log('Risposta ricevuta:', response.status, response.statusText); // Debug

                    if (response.ok) {
                        const responseData = await response.json();
                        showToast(responseData.message || 'Auto messa in evidenza con successo!', 'success');
                        setTimeout(() => {
                            window.location.reload();
                        }, 1500);
                    } else {
                        const errorData = await response.json();
                        console.error('Errore dal server:', errorData);
                        showToast(errorData.message || 'Errore durante la messa in evidenza.', 'error');
                    }
                } catch (error) {
                    console.error('Errore di rete:', error);
                    showToast('Errore di rete: ' + error.message, 'error');
                }
            });
        });
    }

    // Gestione del click su "Rimuovi Evidenza"
    if (removeHighlightProductLinks) {
        removeHighlightProductLinks.forEach(link => {
            link.addEventListener('click', async function(event) {
                event.preventDefault();
                const productId = this.getAttribute('data-product-id');

                try {
                    const response = await fetch(`/rest/api/products/${productId}/remove-highlight`, {
                        method: 'POST',
                        headers: {
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                        }
                    });

                    if (response.ok) {
                        showToast('Evidenza rimossa con successo!', 'success');
                        setTimeout(() => {
                            window.location.reload();
                        }, 1500);
                    } else {
                        const errorText = await response.text();
                        showToast('Errore durante la rimozione dell\'evidenza: ' + errorText, 'error');
                    }
                } catch (error) {
                    console.error('Errore di rete:', error);
                    showToast('Errore di rete: ' + error.message, 'error');
                }
            });
        });
    }

    // Animazioni per le sezioni e le card
    const animatedSections = document.querySelectorAll('.animated-section');
    animatedSections.forEach(section => {
        section.classList.add('visible');
    });

    const productCards = document.querySelectorAll('.product-card');
    productCards.forEach((card, index) => {
        setTimeout(() => {
            card.classList.add('visible');
        }, index * 100);
    });
});