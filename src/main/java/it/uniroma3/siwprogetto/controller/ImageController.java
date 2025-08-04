package it.uniroma3.siwprogetto.controller;

import it.uniroma3.siwprogetto.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImageController {

	@Autowired
	private ImageRepository imageRepository;

	@GetMapping("/images/{id}")
	public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
		return imageRepository.findById(id)
				.map(image -> ResponseEntity.ok()
						.contentType(MediaType.IMAGE_JPEG) // Cambia il tipo in base al formato
						.body(image.getData()))
				.orElse(ResponseEntity.notFound().build());
	}
}