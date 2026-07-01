package com.example.coursehub.ai.chunking;

import com.example.coursehub.ai.chunking.dto.ChunkResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedSizeLessonChunkerTest {

    private ChunkingProperties chunkingProperties;
    private FixedSizeLessonChunker chunker;

    @BeforeEach
    void setUp() {
        chunkingProperties = new ChunkingProperties();
        chunkingProperties.setMaxTokens(7);
        chunkingProperties.setOverlapSentences(1);
        chunker = new FixedSizeLessonChunker(chunkingProperties);
    }

    @Test
    void chunk_shouldSplitNormalLessonContentIntoMultipleChunks() {
        String content = "Alpha. Beta. Gamma.\n\nDelta. Epsilon. Zeta.";

        List<ChunkResult> chunks = chunker.chunk(content);

        assertThat(chunks).hasSize(4);
        assertThat(chunks).extracting(ChunkResult::chunkIndex).containsExactly(0, 1, 2, 3);

        assertThat(chunks.get(0).content()).isEqualTo("Alpha. Beta. Gamma.");
        assertThat(chunks.get(1).content()).isEqualTo("Gamma. Delta. Epsilon.");
        assertThat(chunks.get(2).content()).isEqualTo("Epsilon. Zeta.");
        assertThat(chunks.get(3).content()).isEqualTo("Zeta.");

        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.tokenCount()).isPositive();
            assertThat(chunk.endOffset()).isGreaterThan(chunk.startOffset());
        });

        assertThat(chunks.get(0).startOffset()).isEqualTo(0);
        assertThat(chunks.get(0).endOffset()).isGreaterThan(chunks.get(0).startOffset());
        assertThat(chunks.get(1).startOffset()).isGreaterThan(chunks.get(0).startOffset());
        assertThat(chunks.get(2).startOffset()).isGreaterThan(chunks.get(1).startOffset());
        assertThat(chunks.get(3).startOffset()).isGreaterThan(chunks.get(2).startOffset());
    }

    @Test
    void chunk_shouldKeepLongSentenceAsSingleChunkWithoutLooping() {
        chunkingProperties.setMaxTokens(1);
        chunkingProperties.setOverlapSentences(2);
        chunker = new FixedSizeLessonChunker(chunkingProperties);

        String content = "This sentence contains many words and exceeds the configured token budget.";

        List<ChunkResult> chunks = chunker.chunk(content);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo(content);
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(chunkingProperties.getMaxTokens());
    }

    @Test
    void chunk_shouldReturnEmptyListForNullOrBlankContent() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("   ")).isEmpty();
    }

    @Test
    void chunk_shouldUseConfiguredOverlapSentencesBetweenChunks() {
        chunkingProperties.setMaxTokens(6);
        chunkingProperties.setOverlapSentences(2);
        chunker = new FixedSizeLessonChunker(chunkingProperties);

        String content = "One. Two. Three. Four. Five.";

        List<ChunkResult> chunks = chunker.chunk(content);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.get(0).content()).contains("One.", "Two.", "Three.");
        assertThat(chunks.get(1).content()).contains("Two.", "Three.", "Four.");
    }
}
