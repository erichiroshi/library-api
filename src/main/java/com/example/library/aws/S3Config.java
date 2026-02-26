package com.example.library.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

	@Value("${aws.access-key-id}")
	private String awsId;

	@Value("${aws.secret-access-key}")
	private String awsKey;

	@Value("${s3.region}")
	private String region;
	
	@PostConstruct
	public void validate() {
		Assert.hasText(awsId, "AWS Access Key ID must not be empty");
		Assert.hasText(awsKey, "AWS Secret Access Key must not be empty");
		Assert.hasText(region, "S3 region must not be empty");

		// Valida se região é válida
		try {
			Region.of(region);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("Invalid AWS region: " + region, e);
		}
	}

	@Bean
	S3Client s3client() {

		AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(awsId, awsKey));
		
		return S3Client
				.builder()
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider)
				.build();
	}

}