package br.com.erichiroshi.libraryapi1.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import br.com.erichiroshi.libraryapi1.service.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	@Value("${application.mail.default-remetent}")
	private String remetent = "mail@library-api.com";

	private JavaMailSender javaMailSender;

	@Override
	public void sendMails(String message, List<String> mailsList) {
		javaMailSender = new JavaMailSenderImpl();

		String[] mails = mailsList.toArray(new String[mailsList.size()]);

		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setFrom(remetent);
		mailMessage.setSubject("Livro com empréstimo atrasado");
		mailMessage.setText(message);
		mailMessage.setTo(mails);

		javaMailSender.send(mailMessage);
	}
}