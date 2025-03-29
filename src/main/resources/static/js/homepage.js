// Quando il DOM è completamente caricato
document.addEventListener("DOMContentLoaded", function() {
    console.log("Homepage caricata senza barra di ricerca.");
  
    // Aggiungi effetto al bottone "Scopri di più"
    const ctaButton = document.querySelector(".cta-button");
    if (ctaButton) {
      ctaButton.addEventListener("click", function() {
        alert("Stai esplorando le offerte!"); // Mostra un messaggio per simulare un'azione
      });
    }
  
    // Animazione di entrata per le card delle auto
    const carCards = document.querySelectorAll(".car-card");
    carCards.forEach((card, index) => {
      card.style.opacity = 0;
      card.style.transform = "translateY(50px)";
      setTimeout(() => {
        card.style.transition = "opacity 0.5s ease, transform 0.5s ease";
        card.style.opacity = 1;
        card.style.transform = "translateY(0)";
      }, index * 200); // Aggiunge un ritardo per l'animazione sequenziale
    });
  
    // Gestione delle promozioni: rotazione automatica
    const promoCards = document.querySelectorAll(".promo-card");
    let promoIndex = 0;
  
    function rotatePromo() {
      promoCards.forEach((promo, index) => {
        promo.style.opacity = 0;
        promo.style.transform = "translateX(50px)";
      });
  
      promoCards[promoIndex].style.opacity = 1;
      promoCards[promoIndex].style.transform = "translateX(0)";
      promoIndex = (promoIndex + 1) % promoCards.length; // Cicla tra le promozioni
  
      setTimeout(rotatePromo, 3000); // Cambia ogni 3 secondi
    }
    rotatePromo();
  
    // Event listener per il menu mobile (per esempio un hamburger menu)
    const hamburgerMenu = document.querySelector(".hamburger-menu");
    const mobileNav = document.querySelector(".navbar");
  
    if (hamburgerMenu) {
      hamburgerMenu.addEventListener("click", () => {
        mobileNav.classList.toggle("active");
      });
    }
  
    // Aggiungi un effetto di "scroll to top"
    const scrollTopButton = document.createElement("button");
    scrollTopButton.innerText = "↑";
    scrollTopButton.classList.add("scroll-top-btn");
    document.body.appendChild(scrollTopButton);
  
    scrollTopButton.addEventListener("click", () => {
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  
    // Mostra/Nascondi il bottone "scroll to top"
    window.addEventListener("scroll", () => {
      if (window.scrollY > 300) {
        scrollTopButton.style.display = "block";
      } else {
        scrollTopButton.style.display = "none";
      }
    });
  });