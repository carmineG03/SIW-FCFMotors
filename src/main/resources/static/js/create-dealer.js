/**
 * ==========================================================
 * Script di gestione creazione concessionario
 * ==========================================================
 * 
 * Questo file gestisce la logica lato client per la pagina di creazione
 * di un nuovo concessionario. Tutte le operazioni JS sono qui, evitando
 * script inline nell'HTML.
 * 
 * Funzionalità principali:
 * - Invio del form tramite AJAX con fetch e CSRF token
 * - Gestione spinner e stato del bottone submit
 * - Visualizzazione toast di feedback (successo/errore)
 * - Preview immagini caricate e rimozione immagini selezionate
 * 
 * Sicurezza:
 * - Invio del token CSRF nell'header della richiesta
 * 
 * Come integrare:
 * - Assicurati che il form abbia id e campi corrispondenti
 * - Collega questo file JS nella pagina HTML
 * 
 * Dettaglio funzioni:
 * - showToast: mostra messaggi di feedback all’utente
 * - previewImages: mostra anteprima delle immagini selezionate
 * - removeImage: rimuove una immagine dalla selezione
 */
document.addEventListener('DOMContentLoaded', function() {
        // Form submission with AJAX
        const form = document.querySelector('form');
        const submitSpinner = document.getElementById('submitSpinner');

        form.addEventListener('submit', async function(e) {
            e.preventDefault(); // Blocca il submit normale
            
            if (submitSpinner) submitSpinner.classList.add('active');
            const submitButton = document.querySelector('button[type="submit"]');
            if (submitButton) submitButton.disabled = true;

            try {
                const formData = new FormData(form);
                
                const response = await fetch(form.action, {
                    method: 'POST',
                    body: formData,
                    headers: {
                        'X-CSRF-TOKEN': document.querySelector('input[name="_csrf"]').value
                    }
                });

                // Controlla se la risposta è JSON
                const contentType = response.headers.get('content-type');
                let data;
                
                if (contentType && contentType.includes('application/json')) {
                    data = await response.json();
                } else {
                    // Se non è JSON, probabilmente è una risposta di errore HTML
                    const text = await response.text();
                    console.error('Non-JSON response:', text);
                    showToast('Errore del server: risposta non valida', 'error');
                    return;
                }

                if (response.ok && data.id) {
                    // Successo: mostra messaggio e reindirizza a manage dealer
                    showToast(data.message || 'Concessionario creato con successo!', 'success');
                    
                    // Reindirizza alla pagina di gestione del concessionario
                    setTimeout(() => {
                        window.location.href = `/rest/dealers/manage`;
                    }, 1500);
                } else {
                    // Errore: mostra messaggio di errore
                    showToast(data.message || 'Errore durante la creazione del concessionario', 'error');
                    if (submitButton) submitButton.disabled = false;
                }
            } catch (error) {
                console.error('Errore:', error);
                showToast('Errore di rete durante la creazione: ' + error.message, 'error');
                if (submitButton) submitButton.disabled = false;
            } finally {
                if (submitSpinner) submitSpinner.classList.remove('active');
            }
        });

                // Funzione per mostrare toast
                function showToast(message, type = 'success') {
                    // Crea il toast se non esiste
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
            });

            // Gestione preview immagini
            function previewImages(input) {
                const previewContainer = document.getElementById('imagePreview');
                previewContainer.innerHTML = '';

                if (input.files) {
                    Array.from(input.files).forEach((file, index) => {
                        if (file.type.startsWith('image/')) {
                            const reader = new FileReader();
                            reader.onload = function(e) {
                                const previewDiv = document.createElement('div');
                                previewDiv.className = 'preview-item';
                                previewDiv.innerHTML = `
                                    <img src="${e.target.result}" alt="Preview ${index + 1}" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;">
                                    <button type="button" class="remove-btn" onclick="removeImage(${index})" style="position: absolute; top: 5px; right: 5px; background: rgba(220,53,69,0.8); color: white; border: none; border-radius: 50%; width: 25px; height: 25px; cursor: pointer;">
                                        <i class="fas fa-times"></i>
                                    </button>
                                `;
                                previewDiv.style.cssText = 'position: relative; display: inline-block; margin: 10px;';
                                previewContainer.appendChild(previewDiv);
                            };
                            reader.readAsDataURL(file);
                        }
                    });
                }
            }

            function removeImage(index) {
                const input = document.getElementById('dealerImages');
                const dt = new DataTransfer();

                for (let i = 0; i < input.files.length; i++) {
                    if (i !== index) {
                        dt.items.add(input.files[i]);
                    }
                }

                input.files = dt.files;
                previewImages(input);
            }