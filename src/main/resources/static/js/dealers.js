// Token di accesso Mapbox
mapboxgl.accessToken ='pk.eyJ1IjoiZjIzMjMyMyIsImEiOiJjbTlnemJjenUwMDBtMnJzODFlaTIya2VvIn0.PkSIbiGdHDbF7vMBappEuQ';

// Verifica il token
if (!mapboxgl.accessToken) {
    console.error('Errore: Token Mapbox mancante.');
    document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Impossibile caricare la mappa. Token Mapbox mancante.</p>';
    return;
}

// Inizializza la mappa
try {
    const map = new mapboxgl.Map({
        container: 'map', // ID del div della mappa
        style: 'mapbox://styles/mapbox/streets-v12', // Stile aggiornato
        center: [12.4964, 41.9028], // Roma, Italia
        zoom: 5, // Zoom iniziale
        attributionControl: false // Pulizia visiva
    });

    // Aggiungi controlli di navigazione
    map.addControl(new mapboxgl.NavigationControl());

    // Lista di concessionari
    const dealers = [
        {
            name: 'FCF Motors Milano',
            address: 'Via Milano 123, Milano, Italia',
            coordinates: [9.1895, 45.4654],
            phone: '+39 02 123 4567'
        },
        {
            name: 'FCF Motors Roma',
            address: 'Via Roma 456, Roma, Italia',
            coordinates: [12.4964, 41.9028],
            phone: '+39 06 123 4567'
        },
        {
            name: 'FCF Motors Napoli',
            address: 'Via Napoli 789, Napoli, Italia',
            coordinates: [14.2681, 40.8518],
            phone: '+39 081 123 4567'
        }
    ];

    // Funzione per calcolare la distanza (in km)
    function calculateDistance(lng1, lat1, lng2, lat2) {
        const R = 6371; // Raggio della Terra in km
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLng = (lng2 - lng1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                  Math.sin(dLng / 2) * Math.sin(dLng / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Funzione per aggiungere i concessionari
    function addDealersToMap(dealers) {
        const dealersList = document.getElementById('dealers-list');
        dealersList.innerHTML = '';

        dealers.forEach(dealer => {
            // Marker sulla mappa
            const marker = new mapboxgl.Marker({ color: '#F5A623' })
                .setLngLat(dealer.coordinates)
                .setPopup(
                    new mapboxgl.Popup({ offset: 25 })
                        .setHTML(`
                            <h3>${dealer.name}</h3>
                            <p>${dealer.address}</p>
                            <p>Tel: ${dealer.phone}</p>
                        `)
                )
                .addTo(map);

            // Elemento nella lista
            const dealerItem = document.createElement('div');
            dealerItem.className = 'dealer-item';
            dealerItem.innerHTML = `
                <h3>${dealer.name}</h3>
                <p><i class="fas fa-map-marker-alt"></i> ${dealer.address}</p>
                <p><i class="fas fa-phone"></i> ${dealer.phone}</p>
                <a href="tel:${dealer.phone}">Contatta</a>
            `;
            dealersList.appendChild(dealerItem);
        });
    }

    // Carica i concessionari iniziali
    map.on('load', () => {
        console.log('Mappa caricata con successo');
        addDealersToMap(dealers);
    });

    // Gestisci errori di caricamento
    map.on('error', (e) => {
        console.error('Errore Mapbox:', e);
        document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Impossibile caricare la mappa. Controlla la connessione o il token.</p>';
    });

    // Funzione di ricerca
    function searchDealers(event) {
        event.preventDefault();
        const query = document.getElementById('search-input').value.trim();
        const errorMessage = document.getElementById('search-error');

        if (!query) {
            errorMessage.textContent = 'Inserisci un codice postale o città.';
            errorMessage.style.display = 'block';
            return;
        }

        errorMessage.style.display = 'none';

        // Geocodifica
        fetch(`https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${mapboxgl.accessToken}&country=IT&types=postcode,place,locality`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Errore API: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (data.features && data.features.length > 0) {
                    const [lng, lat] = data.features[0].center;
                    map.flyTo({
                        center: [lng, lat],
                        zoom: 10
                    });

                    // Filtra i concessionari (max 100 km)
                    const filteredDealers = dealers.filter(dealer => {
                        const [dealerLng, dealerLat] = dealer.coordinates;
                        const distance = calculateDistance(lng, lat, dealerLng, dealerLat);
                        return distance < 100;
                    });

                    if (filteredDealers.length > 0) {
                        addDealersToMap(filteredDealers);
                    } else {
                        addDealersToMap(dealers);
                        errorMessage.textContent = 'Nessun concessionario trovato nelle vicinanze. Visualizzati tutti i concessionari.';
                        errorMessage.style.display = 'block';
                    }
                } else {
                    errorMessage.textContent = 'CAP o città non trovati. Prova con un altro valore (es. 20121, Milano).';
                    errorMessage.style.display = 'block';
                }
            })
            .catch(error => {
                console.error('Errore nella ricerca:', error);
                errorMessage.textContent = 'Errore durante la ricerca. Verifica la connessione.';
                errorMessage.style.display = 'block';
            });
    }

    // Aggiungi handler per il form
    document.getElementById('dealer-search-form').addEventListener('submit', searchDealers);
} catch (error) {
    console.error('Errore inizializzazione mappa:', error);
    document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Impossibile caricare la mappa.</p>';
}