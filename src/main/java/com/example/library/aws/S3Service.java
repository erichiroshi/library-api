package com.example.library.aws;

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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.example.library.aws.exception.AmazonClientException;
import com.example.library.aws.exception.URIException;
import com.example.library.aws.utils.ImageProcessingService;

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
	
	private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid content type: " + contentType + 
                ". Allowed types: " + ALLOWED_CONTENT_TYPES
            );
        }
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

}