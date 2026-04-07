package com.example.coursehub.ai.search;

import com.example.coursehub.ai.client.AIClient;
import com.example.coursehub.ai.vector.VectorSearchRepository;
import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseMapper;
import com.example.coursehub.course.dto.CourseListResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class SemanticSearchService {
    private final AIClient aiClient;
    private final VectorSearchRepository vectorSearchRepository;
    private final CourseMapper courseMapper;

    public SemanticSearchService(AIClient aiClient, VectorSearchRepository vectorSearchRepository, CourseMapper courseMapper) {
        this.aiClient = aiClient;
        this.vectorSearchRepository = vectorSearchRepository;
        this.courseMapper = courseMapper;
    }

    public Page<CourseListResponse> search(String query, Pageable pageable) {
        List<Double> queryVector = aiClient.getEmbedding(query);

        return vectorSearchRepository
            .searchSimilarCourses(queryVector.toString(), pageable)
            .map(courseMapper::toListResponse);
    }
}
