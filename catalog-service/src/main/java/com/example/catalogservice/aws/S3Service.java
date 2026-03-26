package com.example.catalogservice.aws;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.example.catalogservice.aws.exception.AmazonClientException;
import com.example.catalogservice.aws.exception.S3UnavailableException;
import com.example.catalogservice.aws.exception.URIException;
import com.example.catalogservice.aws.utils.ImageProcessingService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class S3Service {

	private static Logger log = LoggerFactory.getLogger(S3Service.class);
	
	 private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		        "image/png",
		        "image/jpeg",
		        "image/jpg",
		        "image/webp"
		    );
	
	private static final long MIN_FILE_SIZE = 1024; // 1KB
	private static final long MAX_FILE_SIZE = (long)10 * 1024 * 1024; // 10MB
	
	@Value("${img.maxWidth}")
	private Integer maxWidth;
	
	private final S3Client s3Client;
	
	private final ImageProcessingService imageProcessingService;

	@Value("${s3.bucket}")
	private String bucketName;

	@CircuitBreaker(name = "s3", fallbackMethod = "uploadFallback")
	public URI uploadFile(MultipartFile file, String folder, String fileName) {
		
		validateFileSize(file);
        validateContentType(file);
        
		try {
			file = imageProcessingService.compressImage(file, maxWidth);
			
			String contentType = file.getContentType();
			String extension = extractExtension(file);
			fileName += "." + extension;
			
			log.info("Iniciando multipart upload");
			
			PutObjectRequest putOb = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(folder + fileName)
					.contentType(contentType)
					.metadata(Map.of(
					        "uploaded-by", "library-api",
					        "original-filename", file.getOriginalFilename(),
					        "upload-timestamp", Instant.now().toString(),
					        "file-size-bytes", String.valueOf(file.getSize()),
					        "content-type-original", contentType
					    ))
					.build();

			s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

			log.info("Upload finalizado");

			return s3Client.utilities()
					.getUrl(GetUrlRequest.builder()
							.bucket(bucketName)
							.key(folder + fileName)
							.build())
					.toURI();
			
		} catch (AwsServiceException | SdkClientException | IOException _) {
			throw new AmazonClientException();
		} catch (Exception _) {
			throw new URIException();
		}
	}
	
	@CircuitBreaker(name = "s3", fallbackMethod = "deleteFallback")
	public void deleteCover(String coverImageUrl) {
		if (coverImageUrl == null || coverImageUrl.isBlank()) {
			log.warn("deleteCover called with null or blank URL — skipping");
			return;
		}

		try {
			// Extrai a key do objeto a partir da URL
			// Ex: https://bucket.s3.region.amazonaws.com/books/book-1.jpg → books/book-1.jpg
			URI uri = URI.create(coverImageUrl);
			String key = uri.getPath().replaceFirst("^/", ""); // remove leading slash

			log.info("Deletando objeto S3: key={}", key);
	 
	        s3Client.deleteObject(builder -> builder
	                .bucket(bucketName)
	                .key(key)
	                .build());
	 
	        log.info("Objeto S3 deletado com sucesso: key={}", key);
	 
	    } catch (AwsServiceException | SdkClientException e) {
	        throw new AmazonClientException();
	    }
	}

	@SuppressWarnings("unused")
	private URI uploadFallback(MultipartFile file, String folder, String fileName, Exception ex) {
		log.error("S3 circuit breaker open | reason={}", ex.getMessage());
		throw new S3UnavailableException("Cover upload temporarily unavailable.");
	}

	@SuppressWarnings("unused")
	private void deleteFallback(String coverImageUrl, Exception ex) {
		log.error("Failed to delete S3 object: {}. Manual cleanup required.", coverImageUrl, ex);
		// não lança exceção — a deleção do livro no banco prossegue
	}

	private String extractExtension(MultipartFile file) {
	    String contentType = file.getContentType();
	    
	    if (contentType == null) {
	        throw new IllegalArgumentException("Content type cannot be null");
	    }
	    
	    // Mapeia content types conhecidos
	    Map<String, String> contentTypeToExtension = Map.of(
	        "image/png", "png",
	        "image/jpeg", "jpg",
	        "image/jpg", "jpg",
	        "image/gif", "gif",
	        "image/webp", "webp",
	        "image/svg+xml", "svg"
	    );
	    
	    String extension = contentTypeToExtension.get(contentType.toLowerCase());
	    if (extension == null) {
	        throw new IllegalArgumentException("Unsupported content type: " + contentType);
	    }
	    
	    return extension;
	}
	
	private void validateFileSize(MultipartFile file) {
		long size = file.getSize();

		if (size < MIN_FILE_SIZE) {
			throw new IllegalArgumentException(
					String.format("File too small: %d bytes (min: %d)", size, MIN_FILE_SIZE));
		}

		if (size > MAX_FILE_SIZE) {
			throw new IllegalArgumentException(
					String.format("File too large: %d bytes (max: %d)", size, MAX_FILE_SIZE));
		}
	}

	private void validateContentType(MultipartFile file) {
	    String contentType = file.getContentType();
	    
	    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
	        throw new IllegalArgumentException(
	            "Invalid content type: " + contentType + 
	            ". Allowed types: " + ALLOWED_CONTENT_TYPES
	        );
	    }
	}

}