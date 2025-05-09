document.addEventListener('DOMContentLoaded', () => {
    const carousel = document.querySelector('.carousel');
    const items = document.querySelectorAll('.category-item');
    const prevBtn = document.querySelector('.carousel-btn.prev');
    const nextBtn = document.querySelector('.carousel-btn.next');
    const dotsContainer = document.querySelector('.dots');
    let currentIndex = 0;
    let itemsPerSlide = 3;

    // Determina il numero di elementi per slide in base alla larghezza dello schermo
    function updateItemsPerSlide() {
        if (window.innerWidth <= 480) {
            itemsPerSlide = 1;
        } else if (window.innerWidth <= 768) {
            itemsPerSlide = 2;
        } else {
            itemsPerSlide = 3;
        }
        currentIndex = Math.min(currentIndex, Math.ceil(items.length / itemsPerSlide) - 1);
        updateDots();
        updateCarousel();
    }

    // Crea o aggiorna i pallini di navigazione
    function updateDots() {
        dotsContainer.innerHTML = '';
        const totalSlides = Math.ceil(items.length / itemsPerSlide);
        for (let i = 0; i < totalSlides; i++) {
            const dot = document.createElement('span');
            dot.classList.add('dot');
            if (i === currentIndex) dot.classList.add('active');
            dot.addEventListener('click', () => goToSlide(i));
            dotsContainer.appendChild(dot);
        }
    }

    // Aggiorna la posizione del carosello
    function updateCarousel() {
        const totalSlides = Math.ceil(items.length / itemsPerSlide);
        if (currentIndex >= totalSlides) currentIndex = totalSlides - 1;
        if (currentIndex < 0) currentIndex = 0;

        const itemWidthPercent = 100 / itemsPerSlide;
        const translateX = -(currentIndex * itemWidthPercent * itemsPerSlide);
        carousel.style.transform = `translateX(${translateX}%)`;

        const dots = document.querySelectorAll('.dot');
        dots.forEach((dot, index) => {
            dot.classList.toggle('active', index === currentIndex);
        });
    }

    // Vai a una slide specifica
    function goToSlide(index) {
        const totalSlides = Math.ceil(items.length / itemsPerSlide);
        currentIndex = index;
        if (currentIndex >= totalSlides) currentIndex = 0;
        if (currentIndex < 0) currentIndex = totalSlides - 1;
        updateCarousel();
    }

    // Event listeners per i pulsanti
    prevBtn.addEventListener('click', () => goToSlide(currentIndex - 1));
    nextBtn.addEventListener('click', () => goToSlide(currentIndex + 1));

    // Aggiorna itemsPerSlide al caricamento e al ridimensionamento
    updateItemsPerSlide();
    window.addEventListener('resize', updateItemsPerSlide);

    // Verifica il caricamento delle immagini
    items.forEach((item, index) => {
        const bgImage = item.style.backgroundImage;
        if (!bgImage || bgImage === 'url("")') {
            console.warn(`Immagine di sfondo mancante per l'elemento ${index + 1}: ${item.querySelector('p').textContent}`);
            item.style.backgroundColor = '#ccc';
        }
    });

    // Funzione per il toast
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

    // Validazione della ricerca
    window.validateSearch = function() {
        const searchInput = document.getElementById('search-input').value.trim();
        if (searchInput.length < 3) {
            showToast('Inserisci almeno 3 caratteri per la ricerca.', true);
            return false;
        }
        return true;
    };

    // Effetti per l'input di ricerca
    const searchInput = document.getElementById('search-input');
    searchInput.addEventListener('focus', function() {
        this.parentElement.parentElement.style.transform = 'scale(1.05)';
        this.parentElement.parentElement.style.transition = 'transform 0.3s ease';
    });
    searchInput.addEventListener('blur', function() {
        this.parentElement.parentElement.style.transform = 'scale(1)';
    });

    // Effetto header trasparente allo scroll
    /*document.addEventListener("scroll", function () {
        const header = document.getElementById("main-header");
        if (window.scrollY > 50) {
            header.classList.add("header-transparent");
        } else {
            header.classList.remove("header-transparent");
        }
    });*/
});