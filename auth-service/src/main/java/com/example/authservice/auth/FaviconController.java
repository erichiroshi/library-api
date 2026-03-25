package com.example.authservice.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {
	@GetMapping("favicon.ico")
	void returnNoFavicon() {
		// Método vazio para responder com 200 OK e evitar logs de erro
	}
}
