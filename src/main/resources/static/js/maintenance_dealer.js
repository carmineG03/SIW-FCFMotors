let dealershipCreated = false;

document.getElementById('dealership-form')?.addEventListener('submit', function(event) {
    event.preventDefault();

    const name = document.getElementById('dealership-name')?.value;
    const description = document.getElementById('dealership-description')?.value;
    const address = document.getElementById('dealership-address')?.value;
    const contact = document.getElementById('dealership-contact')?.value;
    const imageFile = document.getElementById('dealership-image')?.files[0];

    if (!imageFile) {
        alert('Per favore, seleziona un\'immagine.');
        return;
    }

    const imageUrl = URL.createObjectURL(imageFile);

    document.getElementById('dealership-image-preview').innerHTML = `<img src="${imageUrl}" alt="${name}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px; margin-bottom: 15px;">`;
    document.getElementById('display-name').textContent = name;
    document.getElementById('display-description').textContent = description;
    document.getElementById('display-address').textContent = address;
    document.getElementById('display-contact').textContent = contact;

    document.getElementById('dealership-form-section').style.display = 'none';
    document.getElementById('dealership-details').style.display = 'block';
    document.getElementById('add-car-section').style.display = 'block';

    showToast('Concessionario salvato con successo!');
});document.getElementById('dealership-form')?.addEventListener('submit', function(event) {
    event.preventDefault();

    const name = document.getElementById('dealership-name')?.value;
    const description = document.getElementById('dealership-description')?.value;
    const address = document.getElementById('dealership-address')?.value;
    const contact = document.getElementById('dealership-contact')?.value;
    const imageFile = document.getElementById('dealership-image')?.files[0];

    if (!imageFile) {
        alert('Per favore, seleziona un\'immagine.');
        return;
    }

    const imageUrl = URL.createObjectURL(imageFile);

    document.getElementById('dealership-image-preview').innerHTML = `<img src="${imageUrl}" alt="${name}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px; margin-bottom: 15px;">`;
    document.getElementById('display-name').textContent = name;
    document.getElementById('display-description').textContent = description;
    document.getElementById('display-address').textContent = address;
    document.getElementById('display-contact').textContent = contact;

    document.getElementById('dealership-form-section').style.display = 'none';
    document.getElementById('dealership-details').style.display = 'block';
    document.getElementById('add-car-section').style.display = 'block';

    showToast('Concessionario salvato con successo!');
});

document.getElementById('dealer-add-car-form').addEventListener('submit', function(event) {
    event.preventDefault();

    const name = document.getElementById('dealer-car-name').value;
    const description = document.getElementById('dealer-car-description').value;
    const price = document.getElementById('dealer-car-price').value;
    const imageFile = document.getElementById('dealer-car-image').files[0];

    if (!imageFile) {
        alert('Per favore, seleziona un\'immagine.');
        return;
    }

    const imageUrl = URL.createObjectURL(imageFile);

    const carItem = document.createElement('div');
    carItem.classList.add('product-item');
    carItem.innerHTML = `
        <div class="product-image">
            <img src="${imageUrl}" alt="${name}">
        </div>
        <div class="product-details">
            <h3>${name}</h3>
            <p class="description">${description}</p>
            <p class="price">${price}€</p>
        </div>
    `;

    document.getElementById('dealer-added-cars').appendChild(carItem);

    showToast('Macchina aggiunta con successo!');

    this.reset();
});

function editDealership() {
    document.getElementById('dealership-form-section').style.display = 'block';
    document.getElementById('dealership-details').style.display = 'none';
    document.getElementById('add-car-section').style.display = 'none';
}

function showToast(message) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}