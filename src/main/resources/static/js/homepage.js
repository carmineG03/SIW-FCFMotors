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

    const itemWidth = categoryItems[0].offsetWidth + 20; // Larghezza item + gap
    let currentIndex = 0;

    function updateCarousel() {
        const maxIndex = categoryItems.length - 4; // Mostra 4 elementi per volta
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex > maxIndex) currentIndex = maxIndex;
        carousel.style.transform = `translateX(-${currentIndex * itemWidth}px)`;
        console.log('Indice corrente:', currentIndex); // Debug
    }

    prevBtn.addEventListener('click', () => {
        console.log('Pulsante Precedente cliccato');
        currentIndex--;
        updateCarousel();
    });

    nextBtn.addEventListener('click', () => {
        console.log('Pulsante Successivo cliccato');
        currentIndex++;
        updateCarousel();
    });

    // Altre funzioni
    function validateSearch() {
        const searchInput = document.getElementById('search-input').value.trim();
        if (searchInput.length < 3) {
            alert('Inserisci almeno 3 caratteri per la ricerca.');
            return false;
        }
        return true;
    }

    function filterProducts() {
        const filterValue = document.getElementById('product-filter').value.toLowerCase();
        const products = document.querySelectorAll('.product-item');
        products.forEach(product => {
            const productName = product.getAttribute('data-name').toLowerCase();
            if (productName.includes(filterValue)) {
                product.style.display = 'block';
            } else {
                product.style.display = 'none';
            }
        });
    }

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