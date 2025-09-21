package it.uniroma3.siwprogetto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "it.uniroma3.siwprogetto")
@EnableScheduling
public class SiwProgettoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SiwProgettoApplication.class, args);
	}

}
