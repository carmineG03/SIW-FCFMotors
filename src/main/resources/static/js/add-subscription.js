document.addEventListener('DOMContentLoaded', () => {
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const getHeaders = () => {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append(csrfHeader, csrfToken);
        return headers;
    };

    document.querySelectorAll('.add-subscription-button').forEach(button => {
        button.addEventListener('click', () => {
            const productId = button.getAttribute('data-product-id');
            const subscriptionId = button.getAttribute('data-subscription-id');

            fetch(`/cart/add-subscription/${productId}?subscriptionId=${subscriptionId}`, {
                method: 'POST',
                headers: getHeaders()
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('Abbonamento aggiunto al carrello!');
                    window.location.href = '/cart';
                } else {
                    alert(`Errore: ${data.error}`);
                    if (data.error.includes("ruolo SELLER")) {
                        window.location.href = '/products';
                    }
                }
            })
            .catch(error => alert(`Errore: ${error.message}`));
        });
    });
});