/**
 * ==========================================================
 * Script di gestione aggiunta auto
 * ==========================================================
 * 
 * Questo file gestisce la logica lato client per la pagina di aggiunta
 * di una nuova auto. Tutte le operazioni JS sono qui, evitando script inline.
 * 
 * Funzionalità principali:
 * - Validazione form e immagini prima dell’invio
 * - Visualizzazione spinner e stato bottone submit
 * - Preview immagini caricate e rimozione immagini selezionate
 * - Visualizzazione toast di feedback (successo/errore)
 * 
 * Sicurezza:
 * - Controllo dimensione e tipo file immagini
 * 
 * Come integrare:
 * - Assicurati che il form e i campi abbiano gli id corretti
 * - Collega questo file JS nella pagina HTML
 * 
 * Dettaglio funzioni:
 * - previewImages: mostra anteprima delle immagini selezionate
 * - removeImage: rimuove una immagine dalla selezione
 * - showToast: mostra messaggi di feedback all’utente
 */
document.addEventListener('DOMContentLoaded', function() {
    // Form validation and submission
    const carForm = document.getElementById('carForm');
    const submitBtn = document.getElementById('submitBtn');
    const submitSpinner = document.getElementById('submitSpinner');

    if (carForm) {
        carForm.addEventListener('submit', function(e) {
            const files = document.getElementById('images').files;
            
            // Validazione immagini
            if (files.length === 0) {
                alert('Devi caricare almeno un\'immagine');
                e.preventDefault();
                return;
            }
            
            if (files.length > 10) {
                alert('Puoi caricare massimo 10 immagini');
                e.preventDefault();
                return;
            }
            
            // Controllo dimensione file
            for (let file of files) {
                if (file.size > 5 * 1024 * 1024) {
                    alert('Tutte le immagini devono essere inferiori a 5MB');
                    e.preventDefault();
                    return;
                }
            }
            
            // Validazione campi obbligatori
            const requiredFields = ['brand', 'model', 'category', 'price', 'year', 'mileage', 'fuelType', 'transmission', 'description'];
            for (let fieldName of requiredFields) {
                const field = document.getElementById(fieldName);
                if (!field.value.trim()) {
                    alert(`Il campo ${field.previousElementSibling.textContent.replace('*', '').trim()} è obbligatorio`);
                    field.focus();
                    e.preventDefault();
                    return;
                }
            }
            
            // Show loading state
            if (submitBtn && submitSpinner) {
                submitBtn.disabled = true;
                submitSpinner.classList.add('active');
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Caricamento... <span class="spinner active"></span>';
            }
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
});

// Image preview functionality
function previewImages(input) {
    const previewContainer = document.getElementById('preview-container');
    const files = Array.from(input.files);
    
    previewContainer.innerHTML = '';
    
    if (files.length > 10) {
        alert('Puoi caricare massimo 10 immagini');
        input.value = '';
        return;
    }
    
    files.forEach((file, index) => {
        if (file.size > 5 * 1024 * 1024) {
            alert(`L'immagine ${file.name} è troppo grande (max 5MB)`);
            return;
        }
        
        if (!file.type.startsWith('image/')) {
            alert(`Il file ${file.name} non è un'immagine valida`);
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            const previewItem = document.createElement('div');
            previewItem.className = 'preview-item';
            previewItem.innerHTML = `
                <img src="${e.target.result}" alt="Anteprima ${index + 1}" style="width: 100%; height: 150px; object-fit: cover; border-radius: 8px;">
                <button type="button" class="remove-btn" onclick="removeImage(${index})" title="Rimuovi immagine">
                    <i class="fas fa-times"></i>
                </button>
            `;
            previewContainer.appendChild(previewItem);
        };
        reader.readAsDataURL(file);
    });
}

function removeImage(index) {
    const fileInput = document.getElementById('images');
    const dt = new DataTransfer();
    const files = Array.from(fileInput.files);
    
    files.forEach((file, i) => {
        if (i !== index) {
            dt.items.add(file);
        }
    });
    
    fileInput.files = dt.files;
    previewImages(fileInput);
    
    // Se non ci sono più file, rimuovi il required constraint temporaneamente
    if (fileInput.files.length === 0) {
        const previewContainer = document.getElementById('preview-container');
        previewContainer.innerHTML = '<p style="text-align: center; color: rgba(255,255,255,0.7); padding: 20px;">Nessuna immagine selezionata</p>';
    }
}

// Show toast notification
function showToast(message, type = 'success') {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            border-radius: 8px;
            color: white;
            font-weight: bold;
            z-index: 3000;
            opacity: 0;
            transition: opacity 0.3s ease;
            transform: translateY(-10px);
        `;
        document.body.appendChild(toast);
    }

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

// Handle form success/error messages
window.addEventListener('load', function() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('success')) {
        
    } else if (urlParams.get('error')) {
        const errorParam = urlParams.get('error');
        let errorMessage = 'Si è verificato un errore';
        
        switch(errorParam) {
            case 'already_has_car': errorMessage = 'Hai già una macchina associata!'; break;
            case 'no_images': errorMessage = 'Devi caricare almeno un\'immagine!'; break;
            case 'missing_brand': errorMessage = 'La marca è obbligatoria!'; break;
            case 'missing_model': errorMessage = 'Il modello è obbligatorio!'; break;
            case 'invalid_price': errorMessage = 'Il prezzo deve essere maggiore di 0!'; break;
            case 'add_failed': errorMessage = 'Impossibile aggiungere l\'auto!'; break;
        }
        
        showToast(errorMessage, 'error');
    }
});