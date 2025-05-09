// Verifica il contenitore della mappa
const mapContainer = document.getElementById('map');
if (!mapContainer) {
    console.error('Errore: Contenitore mappa (#map) non trovato.');
    document.body.innerHTML += '<p style="color: red; text-align: center; padding: 20px;">Errore: Contenitore mappa non trovato.</p>';
    return;
}

// Verifica i dati dei concessionari
const dealers = window.dealers || [];
if (!dealers || dealers.length === 0) {
    console.error('Errore: window.dealers non definito o vuoto.', dealers);
    document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Dati dei concessionari non disponibili.</p>';
    return;
}
console.log('Concessionari caricati:', JSON.stringify(dealers, null, 2));

// Verifica se Leaflet è definito
if (typeof L === 'undefined') {
    console.error('Errore: Leaflet non è definito. Verifica il caricamento di leaflet.js.');
    document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Leaflet non caricato.</p>';
    return;
}

// Inizializza la mappa con Leaflet
try {
    console.log('Inizializzazione mappa...');
    const map = L.map('map', {
        center: [41.9028, 12.4964], // Centro su Roma, Italia
        zoom: 5
    });

    // Aggiungi il layer OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    console.log('Mappa inizializzata con successo.');

    // Crea un layer per il clustering
    const markers = L.markerClusterGroup({
        maxClusterRadius: 40, // Distanza in pixel per il clustering
        iconCreateFunction: function(cluster) {
            return L.divIcon({
                html: `<div style="background-color: #F5A623; color: white; width: 30px; height: 30px; border-radius: 50%; text-align: center; line-height: 30px; border: 2px solid white;">${cluster.getChildCount()}</div>`,
                className: 'marker-cluster',
                iconSize: L.point(30, 30)
            });
        }
    });

    // Funzione per validare le coordinate
    function isValidCoordinate(lat, lng) {
        return typeof lat === 'number' && typeof lng === 'number' &&
               lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

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
        console.log('Aggiunta concessionari alla mappa:', dealers.length);
        const dealersList = document.getElementById('dealers-list');
        dealersList.innerHTML = '';

        if (dealers.length === 0) {
            console.warn('Nessun concessionario disponibile.');
            dealersList.innerHTML = '<p>Nessun concessionario disponibile al momento.</p>';
            return;
        }

        const fragment = document.createDocumentFragment();
        let validDealers = 0;

        // Rimuovi i marker esistenti
        markers.clearLayers();

        dealers.forEach(dealer => {
            console.log('Elaborazione dealer:', dealer);
            if (!isValidCoordinate(dealer.lat, dealer.lng)) {
                console.warn(`Concessionario ${dealer.name || 'senza nome'} scartato: lat=${dealer.lat}, lng=${dealer.lng}`);
                return;
            }

            validDealers++;
            console.log(`Aggiungendo marker per ${dealer.name || 'senza nome'}: lat=${dealer.lat}, lng=${dealer.lng}`);

            // Crea il marker
            const marker = L.marker([dealer.lat, dealer.lng], {
                icon: L.icon({
                    iconUrl: '/images/map-marker.svg',
                    iconSize: [32, 32],
                    iconAnchor: [16, 32]
                })
            });

            // Aggiungi popup
            marker.bindPopup(`
                <div style="background: white; padding: 10px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.3); max-width: 300px;">
                    ${dealer.imagePath ? `<img src="${dealer.imagePath}" alt="${dealer.name || 'Concessionario'}" style="width: 100%; height: auto; border-radius: 5px; margin-bottom: 10px;" />` : ''}
                    <h3>${dealer.name || 'Nome non disponibile'}</h3>
                    <p>${dealer.description || 'Nessuna descrizione disponibile'}</p>
                    <p><i class="fas fa-map-marker-alt"></i> ${dealer.address || 'Indirizzo non disponibile'}</p>
                    <p><i class="fas fa-phone"></i> <a href="tel:${dealer.contact}">${dealer.contact || 'Contatto non disponibile'}</a></p>
                    ${dealer.contact ? `<p><i class="fas fa-envelope"></i> <a href="mailto:${dealer.contact}">${dealer.contact}</a></p>` : ''}
                    <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dealer.address || '')}" target="_blank" rel="noopener noreferrer">
                        <i class="fas fa-directions"></i> Raggiungi con il navigatore
                    </a>
                </div>
            `);

            // Aggiungi il marker al cluster
            markers.addLayer(marker);

            // Aggiungi alla lista HTML
            const dealerItem = document.createElement('div');
            dealerItem.className = 'dealer-item';
            dealerItem.innerHTML = `
                ${dealer.imagePath ? `<img src="${dealer.imagePath}" alt="${dealer.name || 'Concessionario'}" style="width: 100%; height: auto; border-radius: 5px; margin-bottom: 10px;" />` : ''}
                <h3>${dealer.name || 'Nome non disponibile'}</h3>
                <p>${dealer.description || 'Nessuna descrizione disponibile'}</p>
                <p><i class="fas fa-map-marker-alt"></i> ${dealer.address || 'Indirizzo non disponibile'}</p>
                ${dealer.distance ? `<p><i class="fas fa-ruler"></i> Distanza: ${dealer.distance.toFixed(1)} km</p>` : ''}
                <p><i class="fas fa-phone"></i> <a href="tel:${dealer.contact}">${dealer.contact || 'Contatto non disponibile'}</a></p>
                ${dealer.contact ? `<p><i class="fas fa-envelope"></i> <a href="mailto:${dealer.contact}">${dealer.contact}</a></p>` : ''}
                <a href="https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dealer.address || '')}" target="_blank" rel="noopener noreferrer" class="navigate-button">
                    <i class="fas fa-directions"></i> Raggiungi con il navigatore
                </a>
            `;
            fragment.appendChild(dealerItem);
        });

        // Aggiungi i marker alla mappa
        map.addLayer(markers);
        dealersList.appendChild(fragment);
        console.log(`Concessionari validi aggiunti: ${validDealers}`);

        if (validDealers === 0) {
            console.warn('Nessun concessionario con coordinate valide.');
            map.setView([41.9028, 12.4964], 5); // Centro su Roma
        } else {
            // Centra la mappa sul primo concessionario valido
            const firstValidDealer = dealers.find(dealer => isValidCoordinate(dealer.lat, dealer.lng));
            if (firstValidDealer) {
                map.setView([firstValidDealer.lat, firstValidDealer.lng], 10);
            }
        }
    }

    // Carica i concessionari iniziali
    console.log('Mappa caricata con successo');
    addDealersToMap(dealers);

    // Cache per le richieste di geocodifica
    const geocodeCache = new Map();

    // Funzione di ricerca
    function searchDealers(event) {
        event.preventDefault();
        const query = document.getElementById('search-input').value.trim().toLowerCase();
        const errorMessage = document.getElementById('search-error');
        const loadingIndicator = document.getElementById('loading-indicator');

        console.log('Ricerca avviata per:', query);
        if (!query) {
            errorMessage.textContent = 'Inserisci un codice postale o città.';
            errorMessage.style.display = 'block';
            return;
        }

        errorMessage.style.display = 'none';
        loadingIndicator.style.display = 'block';

        if (geocodeCache.has(query)) {
            console.log('Risultato geocodifica da cache:', geocodeCache.get(query));
            processGeocodeResult(geocodeCache.get(query));
            loadingIndicator.style.display = 'none';
            return;
        }

        const encodedQuery = encodeURIComponent(query);
        fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodedQuery}&countrycodes=IT&limit=1`, {
            headers: {
                'User-Agent': 'FCFMotors/1.0 (info@fcfmotors.com)'
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Errore API: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                loadingIndicator.style.display = 'none';
                geocodeCache.set(query, data);
                processGeocodeResult(data);
            })
            .catch(error => {
                loadingIndicator.style.display = 'none';
                console.error('Errore nella ricerca:', error);
                errorMessage.textContent = 'Errore durante la ricerca. Verifica la connessione.';
                errorMessage.style.display = 'block';
            });

        function processGeocodeResult(data) {
            console.log('Risultati geocodifica:', data);
            if (data && data.length > 0) {
                const lat = parseFloat(data[0].lat);
                const lng = parseFloat(data[0].lon);
                map.setView([lat, lng], 10);

                // Filtra e ordina i concessionari (max 100 km)
                const filteredDealers = dealers
                    .filter(dealer => {
                        if (!isValidCoordinate(dealer.lat, dealer.lng)) return false;
                        const distance = calculateDistance(lng, lat, dealer.lng, dealer.lat);
                        dealer.distance = distance; // Aggiungi la distanza al dealer
                        return distance < 100;
                    })
                    .sort((a, b) => a.distance - b.distance); // Ordina per distanza

                console.log('Concessionari filtrati:', filteredDealers);
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
        }
    }

    // Aggiungi handler per il form
    const searchForm = document.getElementById('dealer-search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', searchDealers);
    } else {
        console.error('Errore: Form di ricerca (#dealer-search-form) non trovato.');
    }

    // Gestione del pulsante "Torna in cima"
    const backToTopButton = document.getElementById('back-to-top');
    if (backToTopButton) {
        window.addEventListener('scroll', () => {
            backToTopButton.style.display = window.scrollY > 200 ? 'block' : 'none';
        });
        backToTopButton.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
    } else {
        console.warn('Pulsante "Torna in cima" (#back-to-top) non trovato.');
    }
} catch (error) {
    console.error('Errore inizializzazione mappa:', error);
    document.getElementById('map').innerHTML = '<p style="color: red; text-align: center; padding: 20px;">Errore: Impossibile caricare la mappa.</p>';
}