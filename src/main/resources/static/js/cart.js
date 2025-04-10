document.addEventListener('DOMContentLoaded', () => {
    // Recupera il token CSRF e l'intestazione dai meta tag
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Funzione per creare gli header con il token CSRF
    const getHeaders = () => {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append(csrfHeader, csrfToken);
        return headers;
    };

    // Aggiorna quantità
    document.querySelectorAll('.quantity').forEach(select => {
        select.addEventListener('change', (event) => {
            const itemId = event.target.getAttribute('data-item-id');
            const quantity = event.target.value;

            console.log(`Aggiornamento quantità - Item ID: ${itemId}, Quantità: ${quantity}`);

            fetch(`/cart/update/${itemId}`, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ quantity: parseInt(quantity) })
            })
            .then(response => {
                console.log('Risposta dal server (update):', response);
                if (!response.ok) {
                    throw new Error(`Errore HTTP: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dati ricevuti (update):', data);
                if (data.success) {
                    showToast('Quantità aggiornata!');
                    window.location.reload();
                } else {
                    showToast(`Errore: ${data.error || 'Impossibile aggiornare la quantità.'}`, true);
                }
            })
            .catch(error => {
                console.error('Errore durante l\'aggiornamento:', error);
                showToast(`Errore: ${error.message}`, true);
            });
        });
    });

    // Rimuovi prodotto
    document.querySelectorAll('.remove-button').forEach(button => {
        button.addEventListener('click', (event) => {
            const itemId = event.target.getAttribute('data-item-id') || event.target.parentElement.getAttribute('data-item-id');

            console.log(`Rimozione prodotto - Item ID: ${itemId}`);

            if (confirm('Sei sicuro di voler rimuovere questo prodotto dal carrello?')) {
                fetch(`/cart/remove/${itemId}`, {
                    method: 'POST',
                    headers: getHeaders()
                })
                .then(response => {
                    console.log('Risposta dal server (remove):', response);
                    if (!response.ok) {
                        throw new Error(`Errore HTTP: ${response.status} ${response.statusText}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Dati ricevuti (remove):', data);
                    if (data.success) {
                        showToast('Prodotto rimosso dal carrello!');
                        window.location.reload();
                    } else {
                        showToast(`Errore: ${data.error || 'Impossibile rimuovere il prodotto.'}`, true);
                    }
                })
                .catch(error => {
                    console.error('Errore durante la rimozione:', error);
                    showToast(`Errore: ${error.message}`, true);
                });
            }
        });
    });

    // Salva per dopo
    document.querySelectorAll('.save-for-later').forEach(button => {
        button.addEventListener('click', (event) => {
            const itemId = event.target.getAttribute('data-item-id') || event.target.parentElement.getAttribute('data-item-id');

            console.log(`Salvataggio per dopo - Item ID: ${itemId}`);

            fetch(`/cart/save-for-later/${itemId}`, {
                method: 'POST',
                headers: getHeaders()
            })
            .then(response => {
                console.log('Risposta dal server (save-for-later):', response);
                if (!response.ok) {
                    throw new Error(`Errore HTTP: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dati ricevuti (save-for-later):', data);
                if (data.success) {
                    showToast('Prodotto salvato per dopo!');
                    window.location.reload();
                } else {
                    showToast(`Errore: ${data.error || 'Impossibile salvare il prodotto.'}`, true);
                }
            })
            .catch(error => {
                console.error('Errore durante il salvataggio:', error);
                showToast(`Errore: ${error.message}`, true);
            });
        });
    });

    // Aggiungi al carrello (prodotti suggeriti)
    document.querySelectorAll('.add-to-cart').forEach(button => {
        button.addEventListener('click', (event) => {
            const productId = event.target.getAttribute('data-product-id') || event.target.parentElement.getAttribute('data-product-id');

            console.log(`Aggiunta al carrello - Product ID: ${productId}`);

            fetch(`/cart/add/${productId}`, {
                method: 'POST',
                headers: getHeaders()
            })
            .then(response => {
                console.log('Risposta dal server (add):', response);
                if (!response.ok) {
                    throw new Error(`Errore HTTP: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dati ricevuti (add):', data);
                if (data.success) {
                    showToast('Prodotto aggiunto al carrello!');
                    window.location.reload();
                } else {
                    showToast(`Errore: ${data.error || 'Impossibile aggiungere il prodotto.'}`, true);
                }
            })
            .catch(error => {
                console.error('Errore durante l\'aggiunta:', error);
                showToast(`Errore: ${error.message}`, true);
            });
        });
    });

    // Aggiungi di nuovo al carrello (da salvati per dopo)
    document.querySelectorAll('.add-back-to-cart').forEach(button => {
        button.addEventListener('click', (event) => {
            const itemId = event.target.getAttribute('data-item-id') || event.target.parentElement.getAttribute('data-item-id');

            console.log(`Aggiunta di nuovo al carrello - Item ID: ${itemId}`);

            fetch(`/cart/add-back/${itemId}`, {
                method: 'POST',
                headers: getHeaders()
            })
            .then(response => {
                console.log('Risposta dal server (add-back):', response);
                if (!response.ok) {
                    throw new Error(`Errore HTTP: ${response.status} ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                console.log('Dati ricevuti (add-back):', data);
                if (data.success) {
                    showToast('Prodotto aggiunto di nuovo al carrello!');
                    window.location.reload();
                } else {
                    showToast(`Errore: ${data.error || 'Impossibile aggiungere il prodotto.'}`, true);
                }
            })
            .catch(error => {
                console.error('Errore durante l\'aggiunta:', error);
                showToast(`Errore: ${error.message}`, true);
            });
        });
    });
});

// Funzione per mostrare il toast
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast show';
    toast.style.backgroundColor = isError ? '#DC3545' : '#28A745';
    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}

// Funzione per aprire il modale di checkout
function openCheckoutModal() {
    console.log('openCheckoutModal called');
    const modal = document.getElementById('checkoutModal');
    if (!modal) {
        console.error('Modale non trovato!');
        return;
    }

    const subtotalElement = document.querySelector('#subtotal span');
    if (!subtotalElement) {
        console.error('Subtotale non trovato!');
        showToast('Errore: Impossibile calcolare il subtotale.', true);
        return;
    }

    const subtotalText = subtotalElement.textContent;
    const subtotal = parseFloat(subtotalText.replace(' €', '')) || 0;
    const total = subtotal; // Spedizione gratuita, quindi totale = subtotale

    // Aggiorna il totale nel modale
    const modalTotalElement = document.getElementById('modalTotal');
    if (modalTotalElement) {
        modalTotalElement.textContent = total.toFixed(2) + ' €';
    } else {
        console.error('Elemento modalTotal non trovato!');
    }

    // Mostra il modale
    modal.style.display = 'flex';
}

// Funzione per chiudere il modale di checkout
function closeCheckoutModal() {
    const modal = document.getElementById('checkoutModal');
    modal.style.display = 'none';
}

// Funzione per confermare l'acquisto
function confirmCheckout() {
    // Simula la conferma dell'acquisto (puoi aggiungere una richiesta al server qui)
    showToast('Acquisto confermato con successo!');
    closeCheckoutModal();

    // Opzionale: Reindirizza a una pagina di conferma o svuota il carrello
    setTimeout(() => {
        window.location.href = '/order-confirmation'; // Reindirizza a una pagina di conferma (da creare)
    }, 2000);
}