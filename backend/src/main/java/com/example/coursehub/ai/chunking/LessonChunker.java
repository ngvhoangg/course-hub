package com.example.coursehub.ai.chunking;

import com.example.coursehub.ai.chunking.dto.ChunkResult;

import java.util.List;

public interface LessonChunker {
    List<ChunkResult> chunk(String content);
}
