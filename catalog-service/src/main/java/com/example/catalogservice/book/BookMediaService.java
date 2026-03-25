package com.example.catalogservice.book;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.catalogservice.aws.S3Service;
import com.example.catalogservice.book.exception.BookNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookMediaService {

	private static final Logger log = LoggerFactory.getLogger(BookMediaService.class);
	private static final String S3_FOLDER_NAME = "books/";

	private final BookService bookService;
	private final S3Service s3Service;

	@Value("${img.prefix.book}")
	private String prefix;

	@Transactional
	public URI uploadCover(Long bookId, MultipartFile file) {
		try {
			bookService.find(bookId);

			String fileName = prefix + bookId;
			URI uri = s3Service.uploadFile(file, S3_FOLDER_NAME, fileName);

			bookService.updateCoverImageUrl(bookId, fileName);

			log.info("Cover uploaded for bookId={} uri={}", bookId, uri);
			return uri;
		} catch (BookNotFoundException _) {
			log.warn("Book not found for cover upload: {}", bookId);
			return null;
		}
	}
}