package com.menglang.ticket.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Config {
    private String region = "us-east-1";
    private String bucketPrivate = "rumluos-app";
    private String bucketStatic = "rumluos-app";
    private long presignedExpirySeconds = 3600;
    private String staticUrl = "";
    private String privateUrl = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";

    @Bean
    public S3AsyncClient s3AsyncClient() {
        var builder = S3AsyncClient.builder()
                .region(Region.of(region));

        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            try {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            } catch (Exception e) {
                builder.credentialsProvider(AnonymousCredentialsProvider.create());
            }
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region));

        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        } else {
            try {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            } catch (Exception e) {
                builder.credentialsProvider(AnonymousCredentialsProvider.create());
            }
        }

        return builder.build();
    }
}
