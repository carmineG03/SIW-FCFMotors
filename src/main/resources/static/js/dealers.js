document.addEventListener('DOMContentLoaded', function () {
    const dealersList = document.getElementById('dealers-list');
    // Dati iniettati da Thymeleaf
    const dealers = window.dealersData || [];

    // Log per debug
    console.log('Dati concessionari ricevuti:', dealers);

    // Verifica se Thymeleaf ha già renderizzato i concessionari
    if (dealersList.children.length > 0 && !dealersList.classList.contains('js-rendered')) {
        console.log('Rendering Thymeleaf già presente, nessun intervento JavaScript necessario');
        return;
    }

    function renderDealers(dealers) {
        dealersList.innerHTML = '';
        const fragment = document.createDocumentFragment();

        if (!dealers || !Array.isArray(dealers) || dealers.length === 0) {
            dealersList.innerHTML = '<p id="no-dealers-message">Nessun concessionario disponibile.</p>';
            return;
        }

        dealers.forEach(dealer => {
            try {
                const dealerData = {
                    id: dealer.id || 'unknown',
                    name: dealer.name || 'Nome non disponibile',
                    address: dealer.address || 'Indirizzo non disponibile',
                    phone: dealer.phone || null,
                    email: dealer.email || null,
                    description: dealer.description || 'Nessuna descrizione disponibile',
                    imagePath: dealer.imagePath || null
                };

                const dealerItem = document.createElement('div');
                dealerItem.className = 'dealer-item';
                dealerItem.id = `dealer-item-${dealerData.id}`;
                dealerItem.innerHTML = `
                    ${dealerData.imagePath ? `<img src="${dealerData.imagePath}" alt="${dealerData.name} immagine" style="width: 100%; height: auto; border-radius: 5px; margin-bottom: 10px;" />` : ''}
                    <h3>${dealerData.name}</h3>
                    <p>${dealerData.description}</p>
                    <p><i class="fas fa-map-marker-alt"></i> ${dealerData.address}</p>
                    <p><i class="fas fa-phone"></i> ${dealerData.phone ? `<a href="tel:${dealerData.phone}" aria-label="Chiama ${dealerData.phone}">${dealerData.phone}</a>` : 'Telefono non disponibile'}</p>
                    <p><i class="fas fa-envelope"></i> ${dealerData.email ? `<a href="mailto:${dealerData.email}" aria-label="Invia email a ${dealerData.email}">${dealerData.email}</a>` : 'Email non disponibile'}</p>
                    ${dealerData.address ? `<a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dealerData.address)}" class="navigate-button" target="_blank" rel="noopener noreferrer"><i class="fas fa-directions"></i> Raggiungi con il navigatore</a>` : ''}
                `;
                fragment.appendChild(dealerItem);
            } catch (error) {
                console.error('Errore elaborazione concessionario:', error);
            }
        });

        dealersList.appendChild(fragment);
        dealersList.classList.add('js-rendered');
    }

    // Renderizza solo se i dati sono disponibili e Thymeleaf non ha già renderizzato
    if (dealers.length > 0 && !dealersList.classList.contains('js-rendered')) {
        renderDealers(dealers);
    }
});