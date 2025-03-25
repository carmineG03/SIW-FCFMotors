package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.Credentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
	Optional<Credentials> findByUsername(String username);
}