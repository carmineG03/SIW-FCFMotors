$(document).ready(function() {
    // Carica i dati dal file JSON
    $.getJSON('/js/cars.json', function(data) {
        // Autocompletamento per Categoria
        $('#category').autocomplete({
            source: data.categories,
            minLength: 1
        });

        // Autocompletamento per Marca
        $('#brand').autocomplete({
            source: data.brands.map(brand => brand.name),
            minLength: 1,
            select: function(event, ui) {
                // Quando si seleziona una marca, aggiorna i modelli disponibili
                const selectedBrand = ui.item.value;
                const brand = data.brands.find(b => b.name === selectedBrand);
                $('#model').autocomplete({
                    source: brand ? brand.models : [],
                    minLength: 1
                });
            }
        });

        // Autocompletamento per Modello (inizialmente vuoto)
        $('#model').autocomplete({
            source: [],
            minLength: 1
        });

        // Autocompletamento per Anno
        $('#year').autocomplete({
            source: data.years.map(String), // Converti in stringhe per l'autocompletamento
            minLength: 1
        });

        // Autocompletamento per Tipo di carburante
        $('#fuelType').autocomplete({
            source: data.fuelTypes,
            minLength: 1
        });
    }).fail(function() {
        console.error('Errore nel caricamento di cars.json');
    });
});