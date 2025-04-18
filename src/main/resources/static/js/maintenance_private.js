document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const toast = document.getElementById('toast');

    if (urlParams.get('success')) {
        if (urlParams.get('success') === 'car_added') {
            toast.textContent = 'Macchina aggiunta con successo!';
        } else if (urlParams.get('success') === 'car_updated') {
            toast.textContent = 'Macchina modificata con successo!';
        } else if (urlParams.get('success') === 'car_deleted') {
            toast.textContent = 'Macchina eliminata con successo!';
        }
        toast.classList.add('show', 'success');
        setTimeout(() => toast.classList.remove('show', 'success'), 3000);
    } else if (urlParams.get('error')) {
        toast.textContent = urlParams.get('error') === 'already_has_car' ? 'Errore: Hai già una macchina associata!' : 'Errore: Non autorizzato!';
        toast.classList.add('show', 'error');
        setTimeout(() => toast.classList.remove('show', 'error'), 3000);
    }
});