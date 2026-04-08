package com.example.coursehub.course;

import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.ai.search.SearchMode;
import com.example.coursehub.ai.search.SearchProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class CourseSearchProvider implements SearchProvider<Course> {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseSearchProvider(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.COURSE;
    }

    @Override
    public Page<Course> performSearch(String query, String vector, SearchMode mode, Pageable pageable) {
        return switch (mode) {
            case KEYWORD -> courseRepository.searchByKeyword(query, pageable);
            case SEMANTIC -> courseRepository.searchSemantic(vector, pageable);
            case HYBRID -> courseRepository.searchHybrid(query, vector, pageable);
        };
    }

    @Override
    public Object mapToResponse(Object entity) {
        return courseMapper.toListResponse((Course) entity);
    }
}
