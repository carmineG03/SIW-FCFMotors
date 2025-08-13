package it.uniroma3.siwprogetto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurazione MVC dell'applicazione.
 * Gestisce la configurazione delle risorse statiche e del mapping degli upload.
 * 
 * @author FCF Motors Team
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Logger per eventi di configurazione */
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    /**
     * Configura i gestori delle risorse statiche.
     * Mappa la directory temporanea per servire i file caricati dagli utenti.
     * 
     * I file vengono salvati nella directory temporanea del sistema e
     * serviti attraverso l'endpoint /uploads/**
     * 
     * @param registry Registry per la configurazione dei resource handler
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ottiene la directory temporanea del sistema
        String tempDir = System.getProperty("java.io.tmpdir");
        
        // Costruisce il percorso completo per gli upload
        String uploadPath = "file:" + tempDir + "/uploads/";
        
        // Log della configurazione per debugging
        logger.info("Configurazione upload path: {}", uploadPath);
        
        // Registra il resource handler per servire i file caricati
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
        
        logger.info("Resource handler configurato: /uploads/** -> {}", uploadPath);
    }
}