package br.com.erichiroshi.libraryapi1;

import java.util.Arrays;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.com.erichiroshi.libraryapi1.service.EmailService;

@SpringBootApplication
@EnableScheduling
public class LibraryApi1Application {

	@Autowired
	private EmailService emailService;

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			List<String> emails = Arrays.asList("library-api-831326@inbox.mailtrap.io");
			emailService.sendMails("Testando serviço de emails.", emails);
			System.out.println("EMAILS ENVIADOS");
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(LibraryApi1Application.class, args);
	}

}