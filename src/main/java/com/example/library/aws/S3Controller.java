package com.example.library.aws;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/s3")
public class S3Controller {

	private final S3Service service;

	public S3Controller(S3Service service) {
		this.service = service;
	}

	@PostMapping("/picture")
    public ResponseEntity<Void> uploadProfilePicture(@RequestPart("file") MultipartFile file) {
		URI uri = service.uploadFile(file);
		return ResponseEntity.created(uri).build();
	}
}
