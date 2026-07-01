package com.example.coursehub.ai.embedding;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ChunkIdGenerator {

    public Long generate(Long lessonId, int chunkIndex) {
        String key = "LESSON_CHUNK:" + lessonId + ":" + chunkIndex;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(key.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by JVM spec, this never throws
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
