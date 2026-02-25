package com.example.library.aws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.library.aws.exception.AmazonClientException;
import com.example.library.aws.exception.URIException;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RequiredArgsConstructor
@Service
public class S3Service {

	private static Logger log = LoggerFactory.getLogger(S3Service.class);

	private final S3Client s3Client;

	@Value("${s3.bucket}")
	private String bucketName;

	public URI uploadFile(MultipartFile file) {
		try {
			String fileName = file.getOriginalFilename();
			String contentType = file.getContentType();
			
			log.info("Iniciando multipart upload");
			
			PutObjectRequest putOb = PutObjectRequest.builder()
					.bucket(bucketName)
					.key("books/" + fileName)	
					.contentType(contentType)
					.build();

			s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

			log.info("Upload finalizado");

			return s3Client.utilities()
					.getUrl(GetUrlRequest.builder()
							.bucket(bucketName)
							.key(fileName)
							.build())
					.toURI();
			} catch (AwsServiceException | SdkClientException | IOException _) {
				throw new AmazonClientException();
			} catch (URISyntaxException _) {
				throw new URIException();
			}

		}

	}