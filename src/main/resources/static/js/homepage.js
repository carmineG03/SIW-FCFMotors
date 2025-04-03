// Validazione del form di ricerca
function validateSearch() {
    const searchInput = document.getElementById('search-input').value.trim();
    if (searchInput.length < 3) {
        alert('Inserisci almeno 3 caratteri per la ricerca.');
        return false;
    }
    return true;
}

// Filtro dinamico dei prodotti
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

// Animazione del form di ricerca al focus
const searchInput = document.getElementById('search-input');
searchInput.addEventListener('focus', function() {
    this.parentElement.parentElement.style.transform = 'scale(1.05)';
    this.parentElement.parentElement.style.transition = 'transform 0.3s ease';
});
searchInput.addEventListener('blur', function() {
    this.parentElement.parentElement.style.transform = 'scale(1)';
});

// Carosello delle categorie
const carousel = document.getElementById('carousel');
const prevBtn = document.getElementById('prev-btn');
const nextBtn = document.getElementById('next-btn');
const categoryItems = document.querySelectorAll('.category-item');
const itemWidth = categoryItems[0].offsetWidth + 20; // Larghezza item + gap
let currentIndex = 0;

function updateCarousel() {
    const maxIndex = categoryItems.length - 4; // Mostra 4 elementi per volta
    if (currentIndex < 0) currentIndex = 0;
    if (currentIndex > maxIndex) currentIndex = maxIndex;
    carousel.style.transform = `translateX(-${currentIndex * itemWidth}px)`;
}

nextBtn.addEventListener('click', () => {
    currentIndex++;
    updateCarousel();
});

prevBtn.addEventListener('click', () => {
    currentIndex--;
    updateCarousel();
});

// Clic su categoria (esempio di interazione)
categoryItems.forEach(item => {
    item.addEventListener('click', () => {
        const category = item.getAttribute('data-category');
        alert(`Hai selezionato la categoria: ${category}`);
        // Qui puoi aggiungere logica per filtrare i prodotti o reindirizzare
    });
});