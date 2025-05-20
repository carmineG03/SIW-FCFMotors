package it.uniroma3.siwprogetto.repository;

import it.uniroma3.siwprogetto.model.AccountInformation;
import it.uniroma3.siwprogetto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountInformationRepository extends JpaRepository<AccountInformation, Long> {
	Optional<AccountInformation> findByUser(User user);

	Optional<AccountInformation> findByUserId(Long id);

	void deleteById(Long id);
}