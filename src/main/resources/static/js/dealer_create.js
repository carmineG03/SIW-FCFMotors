document.addEventListener('DOMContentLoaded', () => {
    const creationForm = document.querySelector('#dealership-creation-form');
    const submitButton = creationForm.querySelector('.modern-button');
    const spinner = submitButton.querySelector('.spinner');

    if (creationForm) {
        creationForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            // Validazione client-side
            const name = creationForm.querySelector('#create-dealership-name').value.trim();
            if (!name) {
                showToast('Il nome del concessionario è obbligatorio!', 'error');
                return;
            }

            // Mostra lo spinner e disabilita il pulsante
            submitButton.disabled = true;
            spinner.style.display = 'inline-block';

            await saveDealer(creationForm, true);

            // Nasconde lo spinner e riabilita il pulsante
            submitButton.disabled = false;
            spinner.style.display = 'none';
        });
    }
});

async function saveDealer(form, isCreation) {
    const data = {
        name: form.querySelector('#create-dealership-name').value.trim(),
        description: form.querySelector('#create-dealership-description').value.trim(),
        address: form.querySelector('#create-dealership-address').value.trim(),
        contact: form.querySelector('#create-dealership-contact').value.trim(),
        imagePath: form.querySelector('#create-dealership-imagePath').value.trim(),
        isUpdate: 'false'
    };

    console.log('Sending dealer data:', data);

    try {
        const response = await fetch('/rest/api/dealers', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Errore nel salvataggio del concessionario');
        }

        const result = await response.json();
        console.log('Dealer saved:', result);
        showToast('Concessionario salvato con successo!', 'success');

        // Transizione fluida prima del redirect
        document.body.style.transition = 'opacity 0.5s ease';
        document.body.style.opacity = '0';
        setTimeout(() => {
            window.location.href = '/rest/dealers/manage';
        }, 500);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nel salvataggio del concessionario', 'error');
    }
}

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