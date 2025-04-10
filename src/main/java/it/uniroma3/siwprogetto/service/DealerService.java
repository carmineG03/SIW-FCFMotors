package it.uniroma3.siwprogetto.service;


import org.springframework.stereotype.Service;

import it.uniroma3.siwprogetto.model.Dealer;

import java.util.ArrayList;
import java.util.List;

@Service
public class DealerService {

    // Metodo di esempio: restituisce una lista fittizia di concessionari
    public List<Dealer> findByLocation(String query) {
        // Qui dovresti implementare la logica reale (es. query al database o API esterna)
        List<Dealer> dealers = new ArrayList<>();
        
        // Dati fittizi per test
        dealers.add(new Dealer("Concessionario " + query, 41.9028, 12.4964)); // Roma
        dealers.add(new Dealer("Concessionario Milano", 45.4642, 9.1900));   // Milano
        
        return dealers;
    }
}