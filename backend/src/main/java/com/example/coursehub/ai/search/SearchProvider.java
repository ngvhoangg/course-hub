package com.example.coursehub.ai.search;

import com.example.coursehub.ai.embedding.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SearchProvider<T> {
    EntityType getEntityType();

    Page<T> performSearch(String query, String vector, SearchMode mode, Pageable pageable);

    Object mapToResponse(Object entity);
}
