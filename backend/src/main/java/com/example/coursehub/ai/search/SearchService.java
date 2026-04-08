package com.example.coursehub.ai.search;

import com.example.coursehub.ai.client.AIClient;
import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.SystemError;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SearchService {
    private final ObjectProvider<AIClient> aiClientProvider;
    // collect implemented SearchProvider classes automatically
    private final List<SearchProvider<?>> providers;

    public SearchService(ObjectProvider<AIClient> aiClientProvider, List<SearchProvider<?>> providers) {
        this.aiClientProvider = aiClientProvider;
        this.providers = providers;
    }

    public Page<?> search(String query, EntityType entityType, SearchMode mode, Pageable pageable) {
        SearchProvider<?> provider = providers.stream()
            .filter(p -> p.getEntityType() == entityType)
            .findFirst()
            .orElseThrow(() -> new SystemError(ErrorCode.UNSUPPORTED_ENTITY_TYPE));

        // get vector
        String vector = null;
        if (mode == SearchMode.SEMANTIC || mode == SearchMode.HYBRID) {
            AIClient aiClient = aiClientProvider.getIfAvailable();
            if (aiClient == null) {
                log.warn("AI client not available. Falling back to KEYWORD search");
                mode = SearchMode.KEYWORD;
            } else {
                try {
                    vector = aiClient.getEmbedding(query).toString();
                } catch (Exception e) {
                    log.error("AI service failed. Fallback to KEYWORD search", e);
                    mode = SearchMode.KEYWORD;
                }
            }
        }

        Page<?> results = provider.performSearch(query, vector, mode, pageable);

        return results.map(provider::mapToResponse);
    }
}
