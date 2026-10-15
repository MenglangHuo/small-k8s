package com.menglang.ticket.infrastructure.storage;

import com.menglang.ticket.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3StorageAdapter implements StoragePort {

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final AwsS3Config config;

    @Override
    public Mono<String> upload(String key, FilePart filePart, String contentType) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> uploadBase64(key, bytes, contentType));
    }

    @Override
    public Mono<String> uploadBase64(String key, byte[] data, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(config.getBucketPrivate())
                .key(key)
                .contentType(contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream")
                .contentLength((long) data.length)
                .build();

        return Mono.fromFuture(() -> s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromBytes(data)))
                .map(response -> key)
                .doOnError(e -> log.error("Error uploading to S3 with key {}: {}", key, e.getMessage()));
    }

    @Override
    public String generatePresignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(config.getPresignedExpirySeconds()))
                    .getObjectRequest(b -> b.bucket(config.getBucketPrivate()).key(key))
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.warn("Could not generate presigned URL for key {}: {}", key, e.getMessage());
            return String.format("https://%s.s3.%s.amazonaws.com/%s", config.getBucketPrivate(), config.getRegion(), key);
        }
    }
}
