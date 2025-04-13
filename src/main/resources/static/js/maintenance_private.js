document.getElementById('add-car-form').addEventListener('submit', function(event) {
    event.preventDefault();

    const name = document.getElementById('car-name').value;
    const description = document.getElementById('car-description').value;
    const price = document.getElementById('car-price').value;
    const imageFile = document.getElementById('car-image').files[0];

    if (!imageFile) {
        alert('Per favore, seleziona un\'immagine.');
        return;
    }

    const fee = 10; // 10€ per aggiunta
    console.log(`Prezzo pagato: ${fee}€`);

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

    document.getElementById('added-cars').appendChild(carItem);

    const toast = document.getElementById('toast');
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);

    this.reset();
});