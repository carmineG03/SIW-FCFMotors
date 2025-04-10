// Imposta il tuo token di accesso Mapbox
mapboxgl.accessToken = 'pk.eyJ1IjoiZjIzMjMyMyIsImEiOiJjbTliNWYwOXAwOTFuMnJzY3hpZmV1bmU2In0.PV7qt3TpEKGGKdJrAxtvdg'; // Sostituisci con la tua chiave

let map;

function initMap() {
    // Inizializza la mappa centrata sull'Italia
    map = new mapboxgl.Map({
        container: 'map', // ID dell'elemento HTML
        style: 'mapbox://styles/mapbox/streets-v11', // Stile della mappa
        center: [12.5674, 41.8719], // Coordinate [lng, lat] dell'Italia
        zoom: 5
    });
}

function searchDealers(event) {
    event.preventDefault();
    const query = document.getElementById('search-input').value;

    // Usa l'API di geocodifica di Mapbox
    fetch(`https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?country=IT&access_token=${mapboxgl.accessToken}`)
        .then(response => response.json())
        .then(data => {
            if (data.features && data.features.length > 0) {
                const [lng, lat] = data.features[0].center; // Coordinate dal risultato
                map.setCenter([lng, lat]);
                map.setZoom(12);

                // Chiamata AJAX al tuo backend per ottenere i concessionari
                fetch(`/api/dealers?query=${encodeURIComponent(query)}`)
                    .then(response => response.json())
                    .then(dealers => {
                        // Aggiungi marker sulla mappa
                        dealers.forEach(dealer => {
                            new mapboxgl.Marker()
                                .setLngLat([dealer.lng, dealer.lat])
                                .setPopup(new mapboxgl.Popup().setText(dealer.name))
                                .addTo(map);
                        });

                        // Mostra lista concessionari
                        const dealersList = document.getElementById('dealers-list');
                        dealersList.innerHTML = '<h3>Concessionari trovati:</h3>';
                        dealers.forEach(dealer => {
                            dealersList.innerHTML += `<p>${dealer.name} - Lat: ${dealer.lat}, Lng: ${dealer.lng}</p>`;
                        });
                    })
                    .catch(error => {
                        console.error('Errore nella chiamata API backend:', error);
                        alert('Errore nel caricamento dei concessionari');
                    });
            } else {
                alert('Luogo non trovato');
            }
        })
        .catch(error => {
            console.error('Errore nella geocodifica:', error);
            alert('Errore nella ricerca del luogo');
        });
}

// Inizializza la mappa al caricamento della pagina
initMap();