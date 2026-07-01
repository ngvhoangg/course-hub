package com.example.coursehub.ai.chunking;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.chunking")
public class ChunkingProperties {
    private int maxTokens = 200;
    private int overlapSentences = 2;
}
