
// Initialize form submission handler
document.addEventListener('DOMContentLoaded', () => {
    const creationForm = document.querySelector('#dealership-creation-form');
    const submitButton = creationForm.querySelector('.modern-button');
    const spinner = submitButton.querySelector('.spinner');

    if (creationForm) {
        creationForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const name = creationForm.querySelector('#create-dealership-name').value.trim();
            if (!name) {
                showToast('Il nome del concessionario è obbligatorio!', 'error');
                return;
            }

            submitButton.disabled = true;
            spinner.style.display = 'inline-block';

            await saveDealer(creationForm);
        });
    }
});

// Handle dealer form submission
async function saveDealer(form) {
    const submitButton = form.querySelector('.modern-button');
    const spinner = submitButton.querySelector('.spinner');
    const errorMessageDiv = document.getElementById('errorMessage');
    const successMessageDiv = document.getElementById('successMessage');

    try {
        // Collect form data
        const formData = new FormData(form);

        // Validate phone number
        const phone = form.querySelector('#create-dealership-phone').value.trim();
        const phonePattern = /^\+?[0-9\s\-]{6,15}$/;
        if (phone && !phonePattern.test(phone)) {
            showToast('Inserisci un numero di telefono valido', 'error');
            errorMessageDiv.textContent = 'Inserisci un numero di telefono valido (es. +39 123 456 7890)';
            errorMessageDiv.style.display = 'block';
            successMessageDiv.style.display = 'none';
            return;
        }

        // Validate email
        const email = form.querySelector('#create-dealership-email').value.trim();
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (email && !emailPattern.test(email)) {
            showToast('Inserisci un indirizzo email valido', 'error');
            errorMessageDiv.textContent = 'Inserisci un indirizzo email valido';
            errorMessageDiv.style.display = 'block';
            successMessageDiv.style.display = 'none';
            return;
        }

        // Validate image files
        const imageFiles = Array.from(form.querySelectorAll('input[type="file"]'))
            .map(input => input.files[0])
            .filter(file => file); // Remove undefined/null files
        if (imageFiles.length > 10) {
            showToast('Puoi caricare un massimo di 10 immagini', 'error');
            errorMessageDiv.textContent = 'Puoi caricare un massimo di 10 immagini';
            errorMessageDiv.style.display = 'block';
            successMessageDiv.style.display = 'none';
            return;
        }

        // Send form data via AJAX
        const response = await fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: {
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            }
        });

        if (!response.ok) {
            const errorData = await response.text();
            throw new Error(errorData || 'Errore nel salvataggio del concessionario');
        }

        const result = await response.json();
        console.log('Dealer saved:', result);
        showToast('Concessionario salvato con successo!', 'success');
        errorMessageDiv.style.display = 'none';
        successMessageDiv.textContent = 'Concessionario creato con successo!';
        successMessageDiv.style.display = 'block';

        // Fade out and redirect
        document.body.style.transition = 'opacity 0.5s ease';
        document.body.style.opacity = '0';
        setTimeout(() => {
            window.location.href = `/rest/dealer/manage/${result.id}`;
        }, 500);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nel salvataggio del concessionario', 'error');
        errorMessageDiv.textContent = error.message || 'Errore nel salvataggio del concessionario';
        errorMessageDiv.style.display = 'block';
        successMessageDiv.style.display = 'none';
    } finally {
        submitButton.disabled = false;
        spinner.style.display = 'none';
    }
}

// Show toast notification
function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    toast.style.display = 'block';
    setTimeout(() => {
        toast.classList.remove('show');
        toast.style.opacity = '0';
        setTimeout(() => {
            toast.style.display = 'none';
            toast.style.opacity = '1';
        }, 300);
    }, 3000);
}