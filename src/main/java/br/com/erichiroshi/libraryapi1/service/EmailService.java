package br.com.erichiroshi.libraryapi1.service;

import java.util.List;

public interface EmailService {

	void sendMails(String message, List<String> mailsList);
}