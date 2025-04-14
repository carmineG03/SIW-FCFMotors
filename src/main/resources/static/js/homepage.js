document.addEventListener('DOMContentLoaded', () => {
    const carousel = document.getElementById('carousel');
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const categoryItems = document.querySelectorAll('.category-item');

    // Verifica che gli elementi siano trovati
    if (!carousel || !prevBtn || !nextBtn || categoryItems.length === 0) {
        console.error('Errore: uno o più elementi del carosello non sono stati trovati.');
        return;
    } else {
        console.log('Carosello inizializzato con', categoryItems.length, 'elementi.');
    }

    const itemCount = categoryItems.length;
    let currentIndex = 0;

    // Calcola il numero di elementi visibili in base alla larghezza dello schermo
    const getVisibleItems = () => {
        if (window.innerWidth <= 768) {
            return 2; // Su schermi piccoli, mostra 2 elementi (50% ciascuno)
        }
        return 4; // Su schermi grandi, mostra 4 elementi (25% ciascuno)
    };

    let visibleItems = getVisibleItems();

    // Calcola la larghezza di un elemento in percentuale
    const itemWidthPercentage = 100 / visibleItems;

    // Aggiorna la posizione del carosello
    const updateCarousel = () => {
        const maxIndex = itemCount - visibleItems;
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex > maxIndex) currentIndex = maxIndex;

        const offset = -currentIndex * itemWidthPercentage;
        carousel.style.transform = `translateX(${offset}%)`;

        // Disabilita i pulsanti se necessario
        prevBtn.disabled = currentIndex === 0;
        nextBtn.disabled = currentIndex >= maxIndex;

        console.log('Indice corrente:', currentIndex, 'Elementi visibili:', visibleItems);
    };

    // Gestisci il click sul pulsante "Precedente"
    prevBtn.addEventListener('click', () => {
        console.log('Pulsante Precedente cliccato');
        if (currentIndex > 0) {
            currentIndex--;
            updateCarousel();
        }
    });

    // Gestisci il click sul pulsante "Successivo"
    nextBtn.addEventListener('click', () => {
        console.log('Pulsante Successivo cliccato');
        if (currentIndex < itemCount - visibleItems) {
            currentIndex++;
            updateCarousel();
        }
    });

    // Aggiungi evento di click per filtrare per categoria
    categoryItems.forEach(item => {
        item.addEventListener('click', () => {
            const category = item.getAttribute('data-category');
            window.location.href = `/products?category=${category}`;
        });
    });

    // Aggiorna il numero di elementi visibili quando la finestra viene ridimensionata
    window.addEventListener('resize', () => {
        const newVisibleItems = getVisibleItems();
        if (newVisibleItems !== visibleItems) {
            visibleItems = newVisibleItems;
            currentIndex = 0; // Resetta l'indice per evitare problemi
            updateCarousel();
        }
    });

    // Inizializza il carosello
    updateCarousel();

    // Funzione per mostrare un toast
    const showToast = (message, isError = false) => {
        let toast = document.getElementById('toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'toast';
            toast.className = 'toast';
            document.body.appendChild(toast);
        }
        toast.textContent = message;
        toast.className = 'toast show';
        toast.style.backgroundColor = isError ? '#DC3545' : '#28A745';
        setTimeout(() => toast.className = 'toast', 3000);
    };

    // Validazione del form di ricerca
    window.validateSearch = function() {
        const searchInput = document.getElementById('search-input').value.trim();
        if (searchInput.length < 3) {
            showToast('Inserisci almeno 3 caratteri per la ricerca.', true);
            return false;
        }
        return true;
    };


    // Effetti visivi sul form di ricerca
    const searchInput = document.getElementById('search-input');
    searchInput.addEventListener('focus', function() {
        this.parentElement.parentElement.style.transform = 'scale(1.05)';
        this.parentElement.parentElement.style.transition = 'transform 0.3s ease';
    });
    searchInput.addEventListener('blur', function() {
        this.parentElement.parentElement.style.transform = 'scale(1)';
    });
});

// Gestione dello sfondo trasparente per il titolo durante lo scroll
document.addEventListener("scroll", function () {
    const header = document.getElementById("main-header");
    if (window.scrollY > 50) {
        header.classList.add("header-transparent");
    } else {
        header.classList.remove("header-transparent");
    }
});