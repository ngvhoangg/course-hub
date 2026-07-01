package com.example.coursehub.ai.chunking;

import com.example.coursehub.ai.chunking.dto.ChunkResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FixedSizeLessonChunker implements LessonChunker {

    private static final double TOKEN_MULTIPLIER = 1.3;
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    private final ChunkingProperties chunkingProperties;

    public FixedSizeLessonChunker(ChunkingProperties chunkingProperties) {
        this.chunkingProperties = chunkingProperties;
    }

    @Override
    public List<ChunkResult> chunk(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> sentences = splitSentences(content);
        List<ChunkResult> chunks = new ArrayList<>();

        int sentenceStart = 0; // index into sentences list for current chunk window
        int chunkIndex = 0;

        while (sentenceStart < sentences.size()) {
            List<String> window = new ArrayList<>();
            int tokenCount = 0;
            int i = sentenceStart;

            // Accumulate sentences until token budget is exceeded
            while (i < sentences.size()) {
                String sentence = sentences.get(i);
                int sentenceTokens = approximateTokens(sentence);
                if (!window.isEmpty() && tokenCount + sentenceTokens > chunkingProperties.getMaxTokens()) {
                    break;
                }
                window.add(sentence);
                tokenCount += sentenceTokens;
                i++;
            }

            // Edge case: single sentence already exceeds maxTokens — include it anyway to avoid infinite loop
            if (window.isEmpty()) {
                window.add(sentences.get(sentenceStart));
                tokenCount = approximateTokens(window.get(0));
                i = sentenceStart + 1;
            }

            String chunkContent = String.join(" ", window);
            int startOffset = findOffset(content, sentences, sentenceStart);
            int endOffset = startOffset + chunkContent.length();

            chunks.add(new ChunkResult(chunkIndex++, chunkContent, tokenCount, startOffset, endOffset));

            // Advance window: next chunk starts (overlapSentences) before end of current window
            int consumed = i - sentenceStart;
            int advance = Math.max(1, consumed - chunkingProperties.getOverlapSentences());
            sentenceStart += advance;
        }

        return chunks;
    }

    private List<String> splitSentences(String content) {
        String[] parts = SENTENCE_SPLIT.split(content.trim());
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private int approximateTokens(String text) {
        String[] words = text.trim().split("\\s+");
        return (int) Math.ceil(words.length * TOKEN_MULTIPLIER);
    }

    private int findOffset(String content, List<String> sentences, int sentenceIndex) {
        int offset = 0;
        for (int i = 0; i < sentenceIndex; i++) {
            int found = content.indexOf(sentences.get(i), offset);
            if (found >= 0) {
                offset = found + sentences.get(i).length();
            }
        }
        // Find where this sentence actually starts
        if (sentenceIndex < sentences.size()) {
            int found = content.indexOf(sentences.get(sentenceIndex), offset);
            if (found >= 0) return found;
        }
        return offset;
    }
}
