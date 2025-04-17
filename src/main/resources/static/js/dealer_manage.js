document.addEventListener('DOMContentLoaded', () => {
    const placeholder = 'https://via.placeholder.com/300x200?text=Immagine+Non+Disponibile';

    // Funzione per validare gli URL
    function isValidUrl(string) {
        try {
            new URL(string);
            return true;
        } catch (_) {
            return false;
        }
    }

    // Funzione per gestire lo spinner
    function manageSpinner(submitButton, show) {
        const spinner = submitButton.querySelector('.spinner') || document.createElement('span');
        spinner.className = 'spinner';
        spinner.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        if (!submitButton.contains(spinner)) submitButton.appendChild(spinner);
        spinner.classList.toggle('active', show);
        submitButton.disabled = show;
    }

    // Funzione per evidenziare i campi non validi
    function highlightInvalidField(field) {
        field.classList.add('invalid');
        setTimeout(() => {
            field.classList.remove('invalid');
        }, 3000);
    }

    // Funzione per gestire gli errori di caricamento delle immagini
    function setupImageErrorHandling() {
        document.querySelectorAll('.product-image img, .dealer-image').forEach(img => {
            img.onerror = () => {
                img.src = placeholder;
            };
        });
    }

    // Funzione per mostrare il toast
    function showToast(message, type) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `toast ${type} show`;
        toast.setAttribute('aria-label', message);
        toast.style.display = 'block';
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(20px)';
        setTimeout(() => {
            toast.style.opacity = '1';
            toast.style.transform = 'translateY(0)';
        }, 10);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(20px)';
            setTimeout(() => {
                toast.style.display = 'none';
            }, 300);
        }, 3000);
    }

    // Funzione per gestire la transizione tra form e visualizzazione
    function toggleFormVisibility(showForm, formId, displayId) {
        const form = document.getElementById(formId);
        const display = document.getElementById(displayId);
        if (showForm) {
            display.style.opacity = '0';
            setTimeout(() => {
                display.style.display = 'none';
                form.style.display = 'block';
                setTimeout(() => {
                    form.style.opacity = '1';
                }, 10);
            }, 300);
        } else {
            form.style.opacity = '0';
            setTimeout(() => {
                form.style.display = 'none';
                display.style.display = 'block';
                setTimeout(() => {
                    display.style.opacity = '1';
                }, 10);
            }, 300);
        }
    }

    // Gestione del form di modifica del concessionario
    const editForm = document.querySelector('#dealership-form');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitButton = editForm.querySelector('button[type="submit"]');
            manageSpinner(submitButton, true);

            const name = editForm.querySelector('#edit-dealership-name').value.trim();
            const imagePath = editForm.querySelector('#edit-dealership-imagePath').value.trim();

            if (!name) {
                showToast('Il nome del concessionario è obbligatorio!', 'error');
                highlightInvalidField(editForm.querySelector('#edit-dealership-name'));
                manageSpinner(submitButton, false);
                return;
            }

            if (imagePath && !isValidUrl(imagePath)) {
                showToast('URL immagine non valido!', 'error');
                highlightInvalidField(editForm.querySelector('#edit-dealership-imagePath'));
                manageSpinner(submitButton, false);
                return;
            }

            await saveDealer(editForm, false);
            manageSpinner(submitButton, false);
        });
    }

    // Gestione del pulsante Modifica Concessionario
    const editDealerButton = document.querySelector('#edit-dealer-button');
    if (editDealerButton) {
        editDealerButton.addEventListener('click', (e) => {
            e.preventDefault();
            toggleFormVisibility(true, 'dealership-form', 'display-dealer-details');
        });
    }

    // Gestione del pulsante Annulla Modifica Concessionario
    const cancelEditDealerButton = document.querySelector('#cancel-edit-dealer');
    if (cancelEditDealerButton) {
        cancelEditDealerButton.addEventListener('click', () => {
            toggleFormVisibility(false, 'dealership-form', 'display-dealer-details');
        });
    }

    // Funzione per salvare il concessionario
    async function saveDealer(form, isCreation) {
        const data = {
            name: form.querySelector('#edit-dealership-name').value.trim(),
            description: form.querySelector('#edit-dealership-description').value.trim(),
            address: form.querySelector('#edit-dealership-address').value.trim(),
            contact: form.querySelector('#edit-dealership-contact').value.trim(),
            imagePath: form.querySelector('#edit-dealership-imagePath').value.trim() || null,
            isUpdate: 'true'
        };

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            if (!csrfToken) {
                throw new Error('Token CSRF mancante');
            }

            const response = await fetch('/rest/api/dealers', {
                method: isCreation ? 'POST' : 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Errore nel salvataggio del concessionario');
            }

            showToast('Concessionario salvato con successo!', 'success');
            window.location.href = '/rest/dealers/manage';
        } catch (error) {
            console.error('Errore:', error);
            showToast(error.message || 'Errore nel salvataggio del concessionario', 'error');
        }
    }

    // Gestione del form di aggiunta auto
    const addCarForm = document.querySelector('#dealer-add-car-form');
    if (addCarForm) {
        addCarForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitButton = addCarForm.querySelector('button[type="submit"]');
            manageSpinner(submitButton, true);

            const name = addCarForm.querySelector('#dealer-car-name').value.trim();
            const price = addCarForm.querySelector('#dealer-car-price').value;
            const imageUrl = addCarForm.querySelector('#dealer-car-imageUrl').value.trim();

            if (!name) {
                showToast('Il nome dell\'auto è obbligatorio!', 'error');
                highlightInvalidField(addCarForm.querySelector('#dealer-car-name'));
                manageSpinner(submitButton, false);
                return;
            }

            if (!price || parseFloat(price) <= 0) {
                showToast('Il prezzo deve essere maggiore di zero!', 'error');
                highlightInvalidField(addCarForm.querySelector('#dealer-car-price'));
                manageSpinner(submitButton, false);
                return;
            }

            if (imageUrl && !isValidUrl(imageUrl)) {
                showToast('URL immagine non valido!', 'error');
                highlightInvalidField(addCarForm.querySelector('#dealer-car-imageUrl'));
                manageSpinner(submitButton, false);
                return;
            }

            await addCar(addCarForm);
            manageSpinner(submitButton, false);
        });
    }

    // Funzione per aggiungere un'auto
    async function addCar(form) {
        const data = {
            name: form.querySelector('#dealer-car-name').value.trim(),
            description: form.querySelector('#dealer-car-description').value.trim(),
            price: parseFloat(form.querySelector('#dealer-car-price').value).toFixed(2),
            imageUrl: form.querySelector('#dealer-car-imageUrl').value.trim() || null
        };

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            if (!csrfToken) {
                throw new Error('Token CSRF mancante');
            }

            const response = await fetch('/rest/api/products', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Errore nell'aggiunta dell'auto");
            }

            const result = await response.json();
            const productGrid = document.querySelector('#dealer-added-cars .product-grid');
            const newCard = document.createElement('div');
            newCard.className = 'product-card animated-card';
            newCard.innerHTML = `
                <div class="product-image">
                    <img src="${result.imageUrl || placeholder}" alt="Immagine Auto">
                    <span class="product-badge"><i class="fas fa-car"></i></span>
                </div>
                <div class="product-info">
                    <div class="info-row"><span class="label"><strong>Nome:</strong></span><span class="value">${result.name}</span></div>
                    <div class="info-row"><span class="label"><strong>Descrizione:</strong></span><span class="value">${result.description || ''}</span></div>
                    <div class="info-row"><span class="label"><strong>Prezzo:</strong></span><span class="value">${result.price} €</span></div>
                    <div class="info-row"><span class="label"><strong>URL Immagine:</strong></span><span class="value">${result.imageUrl || 'Non specificato'}</span></div>
                    <div class="form-actions">
                        <a href="#" data-product-id="${result.id}" class="modern-button edit-product-link"><i class="fas fa-edit"></i> Modifica</a>
                        <a href="#" data-product-id="${result.id}" class="modern-button delete-product-link cancel-button"><i class="fas fa-trash"></i> Elimina</a>
                    </div>
                </div>
            `;
            productGrid.appendChild(newCard);
            showToast('Auto aggiunta con successo!', 'success');
            form.reset();
            setupImageErrorHandling();

            // Aggiungi event listener per i nuovi pulsanti
            newCard.querySelector('.edit-product-link').addEventListener('click', handleEditProductClick);
            newCard.querySelector('.delete-product-link').addEventListener('click', handleDeleteProductClick);

            // Rimuovi il messaggio "Nessuna macchina aggiunta" se presente
            const noProducts = productGrid.parentElement.querySelector('.no-products');
            if (noProducts) {
                noProducts.style.display = 'none';
            }
        } catch (error) {
            console.error('Errore:', error);
            showToast(error.message || "Errore nell'aggiunta dell'auto", 'error');
        }
    }

    // Gestione del form di modifica prodotto
    const editProductForm = document.querySelector('#edit-product-form');
    if (editProductForm) {
        editProductForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitButton = editProductForm.querySelector('button[type="submit"]');
            manageSpinner(submitButton, true);

            const name = editProductForm.querySelector('#edit-product-name').value.trim();
            const price = editProductForm.querySelector('#edit-product-price').value;
            const imageUrl = editProductForm.querySelector('#edit-product-imageUrl').value.trim();

            if (!name) {
                showToast('Il nome dell\'auto è obbligatorio!', 'error');
                highlightInvalidField(editProductForm.querySelector('#edit-product-name'));
                manageSpinner(submitButton, false);
                return;
            }

            if (!price || parseFloat(price) <= 0) {
                showToast('Il prezzo deve essere maggiore di zero!', 'error');
                highlightInvalidField(editProductForm.querySelector('#edit-product-price'));
                manageSpinner(submitButton, false);
                return;
            }

            if (imageUrl && !isValidUrl(imageUrl)) {
                showToast('URL immagine non valido!', 'error');
                highlightInvalidField(editProductForm.querySelector('#edit-product-imageUrl'));
                manageSpinner(submitButton, false);
                return;
            }

            await updateCar(editProductForm);
            manageSpinner(submitButton, false);
        });
    }

    // Gestione del pulsante Annulla Modifica Prodotto
    const cancelEditProductButton = document.querySelector('#cancel-edit-product');
    if (cancelEditProductButton) {
        cancelEditProductButton.addEventListener('click', () => {
            toggleFormVisibility(false, 'edit-product-form', 'dealer-added-cars');
            editProductForm.closest('.hero-content').style.display = 'none';
        });
    }

    // Funzione per gestire il click su Modifica Prodotto
    function handleEditProductClick(e) {
        e.preventDefault();
        const productId = e.target.closest('a').getAttribute('data-product-id');
        fetch(`/rest/api/products/${productId}`)
            .then(response => response.json())
            .then(product => {
                editProductForm.querySelector('#edit-product-id').value = product.id;
                editProductForm.querySelector('#edit-product-name').value = product.name;
                editProductForm.querySelector('#edit-product-description').value = product.description || '';
                editProductForm.querySelector('#edit-product-price').value = product.price;
                editProductForm.querySelector('#edit-product-imageUrl').value = product.imageUrl || '';
                toggleFormVisibility(true, 'edit-product-form', 'dealer-added-cars');
                editProductForm.closest('.hero-content').style.display = 'block';
            })
            .catch(error => {
                console.error('Errore:', error);
                showToast('Errore nel caricamento dei dati del prodotto', 'error');
            });
    }

    // Funzione per gestire il click su Elimina Prodotto
    function handleDeleteProductClick(e) {
        e.preventDefault();
        const productId = e.target.closest('a').getAttribute('data-product-id');
        if (confirm('Sei sicuro di voler eliminare questo prodotto?')) {
            deleteCar(productId);
        }
    }

    // Aggiungi event listener per i pulsanti di modifica ed eliminazione
    document.querySelectorAll('.edit-product-link').forEach(link => {
        link.addEventListener('click', handleEditProductClick);
    });

    document.querySelectorAll('.delete-product-link').forEach(link => {
        link.addEventListener('click', handleDeleteProductClick);
    });

    // Funzione per aggiornare un'auto
    async function updateCar(form) {
        const data = {
            id: form.querySelector('#edit-product-id').value,
            name: form.querySelector('#edit-product-name').value.trim(),
            description: form.querySelector('#edit-product-description').value.trim(),
            price: parseFloat(form.querySelector('#edit-product-price').value).toFixed(2),
            imageUrl: form.querySelector('#edit-product-imageUrl').value.trim() || null
        };

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            if (!csrfToken) {
                throw new Error('Token CSRF mancante');
            }

            const response = await fetch(`/rest/api/products/${data.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Errore nell'aggiornamento dell'auto");
            }

            showToast('Auto aggiornata con successo!', 'success');
            window.location.href = '/rest/dealers/manage';
        } catch (error) {
            console.error('Errore:', error);
            showToast(error.message || "Errore nell'aggiornamento dell'auto", 'error');
        }
    }

    // Funzione per eliminare un'auto
    async function deleteCar(productId) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            if (!csrfToken) {
                throw new Error('Token CSRF mancante');
            }

            const response = await fetch(`/rest/api/products/${productId}`, {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': csrfToken
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || "Errore nell'eliminazione dell'auto");
            }

            showToast('Auto eliminata con successo!', 'success');
            window.location.href = '/rest/dealers/manage';
        } catch (error) {
            console.error('Errore:', error);
            showToast(error.message || "Errore nell'eliminazione dell'auto", 'error');
        }
    }

    // Animazioni delle sezioni
    const sections = document.querySelectorAll('.animated-section');
    sections.forEach((section, index) => {
        setTimeout(() => {
            section.classList.add('visible');
            if (section.classList.contains('products-section')) {
                const cards = section.querySelectorAll('.product-card');
                cards.forEach((card, cardIndex) => {
                    setTimeout(() => {
                        card.classList.add('visible');
                    }, cardIndex * 100);
                });
            }
        }, index * 200);
    });

    // Inizializza la gestione degli errori delle immagini
    setupImageErrorHandling();
});