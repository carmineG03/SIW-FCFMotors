/**
 * ==========================================================
 * Script di gestione modifica auto
 * ==========================================================
 * 
 * Questo file gestisce la logica lato client per la pagina di modifica
 * di una auto esistente. Tutte le operazioni JS sono qui, evitando script inline.
 * 
 * Funzionalità principali:
 * - Mostra/nasconde il form di modifica
 * - Validazione campi form prima dell’invio
 * - Visualizzazione spinner e stato bottone submit
 * - Gestione carosello immagini auto
 * - Preview nuove immagini caricate e rimozione immagini selezionate
 * - Visualizzazione toast di feedback (successo/errore)
 * - Eliminazione immagini già caricate tramite form POST
 * 
 * Sicurezza:
 * - Conferma eliminazione immagini
 * 
 * Come integrare:
 * - Assicurati che il form e i campi abbiano gli id corretti
 * - Collega questo file JS nella pagina HTML
 * 
 * Dettaglio funzioni:
 * - showToast: mostra messaggi di feedback all’utente
 * - previewNewImages: mostra anteprima delle nuove immagini selezionate
 * - removeNewImage: rimuove una nuova immagine dalla selezione
 * - deleteImage: elimina una immagine già caricata
 * - Carosello immagini: gestione navigazione e selezione
 */
document.addEventListener('DOMContentLoaded', function() {
    // Elements
    const editBtn = document.getElementById('edit-btn');
    const cancelBtn = document.getElementById('cancel-btn');
    const carOverview = document.getElementById('car-overview');
    const editForm = document.getElementById('edit-form');
    const saveSpinner = document.getElementById('saveSpinner');
    const form = editForm?.querySelector('form');

    // Show/hide edit form
    if (editBtn && carOverview && editForm) {
        editBtn.addEventListener('click', function() {
            carOverview.style.display = 'none';
            editForm.style.display = 'block';
        });
    }

    if (cancelBtn && carOverview && editForm) {
        cancelBtn.addEventListener('click', function() {
            editForm.style.display = 'none';
            carOverview.style.display = 'block';
        });
    }

    // Form validation and submission
    if (form) {
        form.addEventListener('submit', function(e) {
            const price = parseFloat(document.getElementById('price').value);
            if (isNaN(price) || price <= 0) {
                showToast('Il prezzo deve essere maggiore di 0', 'error');
                e.preventDefault();
                return false;
            }

            const year = parseInt(document.getElementById('year').value);
            const currentYear = new Date().getFullYear();
            if (isNaN(year) || year < 1900 || year > currentYear + 1) {
                showToast(`L'anno deve essere compreso tra 1900 e ${currentYear + 1}`, 'error');
                e.preventDefault();
                return false;
            }

            const mileage = parseInt(document.getElementById('mileage').value);
            if (isNaN(mileage) || mileage < 0) {
                showToast('Il chilometraggio non può essere negativo', 'error');
                e.preventDefault();
                return false;
            }

            // Show loading state
            if (saveSpinner) {
                saveSpinner.classList.add('active');
                const submitBtn = form.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Salvando... <span class="spinner active"></span>';
                }
            }

            return true;
        });
    }

    // Auto-trim form fields
    ['brand', 'model', 'description'].forEach(fieldName => {
        const field = document.getElementById(fieldName);
        if (field) {
            field.addEventListener('blur', function() {
                this.value = this.value.trim();
            });
        }
    });

    // Initialize carousel
    initializeCarousels();
});

// Toast notification function
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (toast) {
        toast.textContent = message;
        toast.style.background = type === 'success' ? 
            'linear-gradient(45deg, #28a745, #218838)' : 
            'linear-gradient(45deg, #dc3545, #c82333)';
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
        
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-10px)';
        }, 3000);
    }
}

// Carousel functionality for car images
let currentCarImageIndex = 0;
const carImages = document.querySelectorAll('.image-carousel .image-item');
const carDots = document.querySelectorAll('.carousel-dot');

function initializeCarousels() {
    // Initialize main car carousel
    updateCarImageCarousel();
}

function updateCarImageCarousel() {
    carImages.forEach((img, index) => {
        img.classList.toggle('active', index === currentCarImageIndex);
    });
    carDots.forEach((dot, index) => {
        dot.classList.toggle('active', index === currentCarImageIndex);
    });
}

function changeCarImage(direction) {
    if (carImages.length > 1) {
        currentCarImageIndex += direction;
        if (currentCarImageIndex >= carImages.length) currentCarImageIndex = 0;
        if (currentCarImageIndex < 0) currentCarImageIndex = carImages.length - 1;
        updateCarImageCarousel();
    }
}

function setCarImage(index) {
    if (carImages.length > 1) {
        currentCarImageIndex = index;
        updateCarImageCarousel();
    }
}

// Delete image function
function deleteImage(imageId) {
    if (confirm('Sei sicuro di voler eliminare questa immagine?')) {
        // Create hidden form
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/private/images/delete/${imageId}`;
        
        // Add CSRF token
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = document.querySelector('meta[name="_csrf_parameter"]')?.getAttribute('content') || '_csrf';
        csrfInput.value = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        form.appendChild(csrfInput);
        
        // Submit form
        document.body.appendChild(form);
        form.submit();
    }
}

// Preview new images function
function previewNewImages(input) {
    const previewContainer = document.getElementById('new-images-preview');
    const files = Array.from(input.files);
    
    previewContainer.innerHTML = '';
    
    if (files.length > 10) {
        showToast('Puoi caricare massimo 10 immagini', 'error');
        input.value = '';
        return;
    }
    
    files.forEach((file, index) => {
        if (file.size > 5 * 1024 * 1024) {
            showToast(`L'immagine ${file.name} è troppo grande (max 5MB)`, 'error');
            return;
        }
        
        if (!file.type.startsWith('image/')) {
            showToast(`Il file ${file.name} non è un'immagine valida`, 'error');
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            const previewItem = document.createElement('div');
            previewItem.className = 'preview-item';
            previewItem.innerHTML = `
                <img src="${e.target.result}" alt="Anteprima ${index + 1}" style="width: 100%; height: 150px; object-fit: cover; border-radius: 8px;">
                <button type="button" class="remove-btn" onclick="removeNewImage(${index})" title="Rimuovi immagine">
                    <i class="fas fa-times"></i>
                </button>
            `;
            previewContainer.appendChild(previewItem);
        };
        reader.readAsDataURL(file);
    });

    // Update label text
    const label = document.querySelector('label[for="new-images"]');
    if (files.length > 0 && label) {
        label.innerHTML = `<i class="fas fa-check-circle" style="font-size: 2rem; margin-bottom: 10px; color: #4caf50;"></i><br>
                          ${files.length} immagine${files.length > 1 ? 'i' : ''} selezionata${files.length > 1 ? 'e' : ''}<br>
                          <small>Clicca per cambiare selezione</small>`;
    }
}

// Remove new image from preview
function removeNewImage(index) {
    const fileInput = document.getElementById('new-images');
    const dt = new DataTransfer();
    const files = Array.from(fileInput.files);
    
    files.forEach((file, i) => {
        if (i !== index) {
            dt.items.add(file);
        }
    });
    
    fileInput.files = dt.files;
    previewNewImages(fileInput);
    
    // Reset label if no files
    if (fileInput.files.length === 0) {
        const label = document.querySelector('label[for="new-images"]');
        if (label) {
            label.innerHTML = `<i class="fas fa-cloud-upload-alt"></i><br>
                              Clicca per aggiungere o sostituire immagini<br>
                              <small>Formati: JPG, PNG, GIF (max 5MB ciascuna, max 10 totali)</small>`;
        }
    }
}

// Handle success/error messages on page load
window.addEventListener('load', function() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('success')) {
        showToast('Auto aggiornata con successo!', 'success');
    } else if (urlParams.get('error')) {
        const errorParam = urlParams.get('error');
        let errorMessage = 'Si è verificato un errore';
        
        switch(errorParam) {
            case 'not_authorized': errorMessage = 'Non sei autorizzato a modificare questa auto!'; break;
            case 'update_failed': errorMessage = 'Impossibile aggiornare l\'auto!'; break;
            case 'invalid_price': errorMessage = 'Il prezzo deve essere maggiore di 0!'; break;
            case 'invalid_year': errorMessage = 'Anno non valido!'; break;
            case 'invalid_mileage': errorMessage = 'Chilometraggio non valido!'; break;
            case 'missing_fields': errorMessage = 'Compila tutti i campi obbligatori!'; break;
            case 'image_error': errorMessage = 'Errore durante l\'elaborazione delle immagini!'; break;
        }
        
        showToast(errorMessage, 'error');
    }
});