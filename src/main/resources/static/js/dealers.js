document.addEventListener('DOMContentLoaded', function () {
    const dealersList = document.getElementById('dealers-list');
    const dealers = window.dealers || [];

    function renderDealers(dealers) {
        dealersList.innerHTML = '';
        const fragment = document.createDocumentFragment();

        if (!dealers || !Array.isArray(dealers) || dealers.length === 0) {
            dealersList.innerHTML = '<p>Nessun concessionario disponibile.</p>';
            return;
        }

        dealers.forEach(dealer => {
            try {
                const dealerData = {
                    name: dealer.name || 'Nome non disponibile',
                    address: dealer.address || 'Indirizzo non disponibile',
                    contact: dealer.contact || null,
                    description: dealer.description || 'Nessuna descrizione disponibile',
                    imagePath: dealer.imagePath || null
                };

                const dealerItem = document.createElement('div');
                dealerItem.className = 'dealer-item';
                dealerItem.innerHTML = `
                    ${dealerData.imagePath ? `<img src="${dealerData.imagePath}" alt="${dealerData.name} immagine" style="width: 100%; height: auto; border-radius: 5px; margin-bottom: 10px;" />` : ''}
                    <h3>${dealerData.name}</h3>
                    <p>${dealerData.description}</p>
                    <p><i class="fas fa-map-marker-alt"></i> ${dealerData.address}</p>
                    <p><i class="fas fa-phone"></i> ${dealerData.contact ? `<a href="tel:${dealerData.contact}">${dealerData.contact}</a>` : 'Contatto non disponibile'}</p>
                    <p><i class="fas fa-envelope"></i> ${dealerData.contact ? `<a href="mailto:${dealerData.contact}">${dealerData.contact}</a>` : 'Email non disponibile'}</p>
                    ${dealerData.address ? `<a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dealerData.address)}" class="navigate-button" target="_blank" rel="noopener noreferrer"><i class="fas fa-directions"></i> Raggiungi con il navigatore</a>` : ''}
                `;
                fragment.appendChild(dealerItem);
            } catch (error) {
                console.error('Errore elaborazione concessionario:', error);
            }
        });

        dealersList.appendChild(fragment);
    }

    renderDealers(dealers);
});