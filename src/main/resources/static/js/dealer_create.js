document.addEventListener('DOMContentLoaded', () => {
    const creationForm = document.querySelector('#dealership-creation-form');

    if (creationForm) {
        creationForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveDealer(creationForm, true);
        });
    }
});

async function saveDealer(form, isCreation) {
    const data = {
        name: form.querySelector('#create-dealership-name').value,
        description: form.querySelector('#create-dealership-description').value,
        address: form.querySelector('#create-dealership-address').value,
        contact: form.querySelector('#create-dealership-contact').value,
        imagePath: form.querySelector('#create-dealership-imagePath').value,
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
        console.log('Redirecting to /rest/dealers/manage');
        setTimeout(() => {
            window.location.href = '/rest/dealers/manage';
        }, 1000);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nel salvataggio del concessionario', 'error');
    }
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.style.display = 'block';
    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}