document.addEventListener('DOMContentLoaded', () => {
    const editForm = document.querySelector('#dealership-form');
    const editButton = document.querySelector('#edit-dealer-button');
    const cancelEditDealerButton = document.querySelector('#cancel-edit-dealer');
    const addCarForm = document.querySelector('#dealer-add-car-form');
    const editProductForm = document.querySelector('#edit-product-form');
    const cancelEditProductButton = document.querySelector('#cancel-edit-product');

    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await saveDealer(editForm, false);
        });
    }

    if (editButton) {
        editButton.addEventListener('click', (e) => {
            e.preventDefault();
            document.querySelector('#display-dealer-details').style.display = 'none';
            document.querySelector('#dealership-form').style.display = 'block';
        });
    }

    if (cancelEditDealerButton) {
        cancelEditDealerButton.addEventListener('click', () => {
            document.querySelector('#dealership-form').style.display = 'none';
            document.querySelector('#display-dealer-details').style.display = 'block';
        });
    }

    if (addCarForm) {
        addCarForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await addCar(addCarForm);
        });
    }

    if (editProductForm) {
        editProductForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await updateCar(editProductForm);
        });
    }

    if (cancelEditProductButton) {
        cancelEditProductButton.addEventListener('click', () => {
            document.querySelector('#edit-product-form').style.display = 'none';
            document.querySelector('#dealer-added-cars').style.display = 'block';
        });
    }

    const editProductLinks = document.querySelectorAll('.edit-product-link');
    editProductLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const productId = link.getAttribute('data-product-id');
            const card = link.closest('.product-card');
            const name = card.querySelector('p:nth-child(1) span').textContent;
            const description = card.querySelector('p:nth-child(2) span').textContent;
            const price = card.querySelector('p:nth-child(3) span').textContent.replace(' €', '');

            document.querySelector('#edit-product-id').value = productId;
            document.querySelector('#edit-product-name').value = name;
            document.querySelector('#edit-product-description').value = description;
            document.querySelector('#edit-product-price').value = parseFloat(price).toFixed(2);

            document.querySelector('#dealer-added-cars').style.display = 'none';
            document.querySelector('#edit-product-form').style.display = 'block';
        });
    });
});

async function saveDealer(form, isCreation) {
    const data = {
        name: form.querySelector('#edit-dealership-name').value,
        description: form.querySelector('#edit-dealership-description').value,
        address: form.querySelector('#edit-dealership-address').value,
        contact: form.querySelector('#edit-dealership-contact').value,
        imagePath: form.querySelector('#edit-dealership-imagePath').value,
        isUpdate: 'true'
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
        showToast('Concessionario aggiornato con successo!', 'success');
        console.log('Reloading /rest/dealers/manage');
        setTimeout(() => {
            window.location.href = '/rest/dealers/manage';
        }, 1000);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nel salvataggio del concessionario', 'error');
    }
}

async function addCar(form) {
    const data = {
        name: form.querySelector('#dealer-car-name').value,
        description: form.querySelector('#dealer-car-description').value,
        price: form.querySelector('#dealer-car-price').value
    };

    console.log('Adding car:', data);

    try {
        const response = await fetch('/rest/api/products', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Errore nell\'aggiunta dell\'auto');
        }

        const result = await response.json();
        console.log('Car added:', result);
        showToast('Auto aggiunta con successo!', 'success');
        form.reset();
        setTimeout(() => {
            window.location.href = '/rest/dealers/manage';
        }, 1000);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nell\'aggiunta dell\'auto', 'error');
    }
}

async function updateCar(form) {
    const productId = form.querySelector('#edit-product-id').value;
    const data = {
        name: form.querySelector('#edit-product-name').value,
        description: form.querySelector('#edit-product-description').value,
        price: form.querySelector('#edit-product-price').value
    };

    console.log('Updating car:', data);

    try {
        const response = await fetch(`/rest/api/products/${productId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Errore nell\'aggiornamento dell\'auto');
        }

        const result = await response.json();
        console.log('Car updated:', result);
        showToast('Auto aggiornata con successo!', 'success');
        setTimeout(() => {
            window.location.href = '/rest/dealers/manage';
        }, 1000);
    } catch (error) {
        console.error('Errore:', error);
        showToast(error.message || 'Errore nell\'aggiornamento dell\'auto', 'error');
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