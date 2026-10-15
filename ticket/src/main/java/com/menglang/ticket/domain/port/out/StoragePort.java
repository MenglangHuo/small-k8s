package com.menglang.ticket.domain.port.out;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface StoragePort {
    Mono<String> upload(String key, FilePart filePart, String contentType);
    Mono<String> uploadBase64(String key, byte[] data, String contentType);
    String generatePresignedUrl(String key);
}
