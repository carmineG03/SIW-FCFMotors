document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const toast = document.getElementById('toast');

    if (urlParams.get('success')) {
        toast.textContent = urlParams.get('success') === 'car_added' ? 'Macchina aggiunta con successo!' : 'Macchina modificata con successo!';
        toast.classList.add('show', 'success');
        setTimeout(() => toast.classList.remove('show', 'success'), 3000);
    } else if (urlParams.get('error')) {
        toast.textContent = urlParams.get('error') === 'already_has_car' ? 'Errore: Hai già una macchina associata!' : 'Errore: Non autorizzato!';
        toast.classList.add('show', 'error');
        setTimeout(() => toast.classList.remove('show', 'error'), 3000);
    }
});