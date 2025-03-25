package it.uniroma3.siwprogetto.service;

import it.uniroma3.siwprogetto.model.Credentials;
import it.uniroma3.siwprogetto.repository.CredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CredentialsService {
	@Autowired
	private CredentialsRepository credentialsRepository;

	public Credentials getCredentials(Long id) {
		return credentialsRepository.findById(id).orElse(null);
	}

	public Credentials getCredentials(String username) {
		return credentialsRepository.findByUsername(username).orElse(null);
	}

	public Credentials saveCredentials(Credentials credentials) {
		return credentialsRepository.save(credentials);
	}
}
