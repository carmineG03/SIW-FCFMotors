document.addEventListener('DOMContentLoaded', () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const getHeaders = () => {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append(csrfHeader, csrfToken);
        return headers;
    };

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
                if (!response.ok) throw new Error(`Errore HTTP: ${response.status}`);
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    showToast('Quantità aggiornata!');
                    window.location.reload();
                } else {
                    showToast(`Errore: ${data.error || 'Impossibile aggiornare.'}`, true);
                }
            })
            .catch(error => {
                console.error('Errore:', error);
                showToast(`Errore: ${error.message}`, true);
            });
        });
    });

    document.querySelectorAll('.remove-button').forEach(button => {
        button.addEventListener('click', (event) => {
            const itemId = event.target.getAttribute('data-item-id') || event.target.parentElement.getAttribute('data-item-id');

            if (confirm('Rimuovere questo abbonamento dal carrello?')) {
                fetch(`/cart/remove/${itemId}`, {
                    method: 'POST',
                    headers: getHeaders()
                })
                .then(response => {
                    if (!response.ok) throw new Error(`Errore HTTP: ${response.status}`);
                    return response.json();
                })
                .then(data => {
                    if (data.success) {
                        showToast('Abbonamento rimosso!');
                        window.location.reload();
                    } else {
                        showToast(`Errore: ${data.error || 'Impossibile rimuovere.'}`, true);
                    }
                })
                .catch(error => {
                    console.error('Errore:', error);
                    showToast(`Errore: ${error.message}`, true);
                });
            }
        });
    });

    function openCheckoutModal() {
        const modal = document.getElementById('checkoutModal');
        const subtotalElement = document.querySelector('#subtotal span');
        if (!modal || !subtotalElement) {
            console.error('Errore: Elementi del modale non trovati.');
            return;
        }

        const subtotal = parseFloat(subtotalElement.textContent.replace(' €', '')) || 0;
        document.getElementById('modalTotal').textContent = subtotal.toFixed(2) + ' €';
        modal.style.display = 'flex';
    }

    function closeCheckoutModal() {
        const modal = document.getElementById('checkoutModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }

    function confirmCheckout() {
        fetch('/cart/checkout-subscriptions', {
            method: 'POST',
            headers: getHeaders()
        })
        .then(response => {
            if (!response.ok) throw new Error(`Errore HTTP: ${response.status}`);
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showToast('Abbonamenti acquistati con successo!');
                closeCheckoutModal();
                setTimeout(() => window.location.href = '/subscriptions-confirmation', 2000);
            } else {
                showToast(`Errore: ${data.error || 'Impossibile completare l\'acquisto.'}`, true);
            }
        })
        .catch(error => {
            console.error('Errore:', error);
            showToast(`Errore: ${error.message}`, true);
        });
    }

    function showToast(message, isError = false) {
        const toast = document.getElementById('toast');
        if (!toast) {
            console.error('Errore: Elemento toast non trovato.');
            return;
        }
        toast.textContent = message;
        toast.className = 'toast show';
        toast.style.backgroundColor = isError ? '#DC3545' : '#28A745';
        setTimeout(() => toast.className = 'toast', 3000);
    }

    document.querySelector('.checkout-button')?.addEventListener('click', openCheckoutModal);
    document.querySelector('.cancel-button')?.addEventListener('click', closeCheckoutModal);
    document.querySelector('.confirm-button')?.addEventListener('click', confirmCheckout);
});