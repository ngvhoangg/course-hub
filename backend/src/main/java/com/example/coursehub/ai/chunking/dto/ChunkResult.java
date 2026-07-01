package com.example.coursehub.ai.chunking.dto;

public record ChunkResult(
    int chunkIndex,
    String content,
    int tokenCount,
    int startOffset,
    int endOffset
) {}
