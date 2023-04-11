package br.com.erichiroshi.libraryapi1;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class LibraryApi1Application {

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Scheduled(cron = "0 31 20 1/1 * ?")
	public void testeAgendamentoTarefas() {
		System.out.println("AGENDAMENTO DE TAREFAS FUNCIONANDO COM SUCESSO!");
	}

	public static void main(String[] args) {
		SpringApplication.run(LibraryApi1Application.class, args);
	}

}