package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}