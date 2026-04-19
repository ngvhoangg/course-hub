package com.example.coursehub.lesson;

import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.category.Category;
import com.example.coursehub.common.kafka.event.EntityAction;
import com.example.coursehub.common.kafka.event.EntitySyncEvent;
import com.example.coursehub.common.kafka.producer.EventProducer;
import com.example.coursehub.course.Course;
import com.example.coursehub.lesson.dto.LessonResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.course.CourseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final CourseRepository courseRepository;
    private final EventProducer eventProducer;

    public LessonServiceImpl(LessonRepository lessonRepository, LessonMapper lessonMapper, CourseRepository courseRepository, EventProducer eventProducer) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
        this.courseRepository = courseRepository;
        this.eventProducer = eventProducer;
    }

    @Override
    public List<LessonResponse> getAllLessons(Long courseId) {
        return lessonRepository.findByCourseId(courseId)
            .stream()
            .map(lessonMapper::toLessonResponse)
            .toList();
    }

    @Override
    @Transactional
    public LessonResponse createLesson(Long courseId, String title, String content, Integer orderIndex) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setOrderIndex(orderIndex);
        lesson.setCourse(course);
        lessonRepository.save(lesson);

        sendUpsertEvent(lesson, true);

        return lessonMapper.toLessonResponse(lesson);
    }

    @Override
    @Transactional
    public void updateLesson(Long id, String title, String content, Integer orderIndex) {
        Lesson lesson = lessonRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.LESSON_NOT_FOUND));

        if (title == null && content == null &&  orderIndex == null) {
            throw new UserError(ErrorCode.EMPTY_UPDATE_REQUEST);
        }

        boolean reEmbed = false;

        if (title != null && !title.equals(lesson.getTitle())) {
            lesson.setTitle(title);
            reEmbed = true;
        }

        if (content != null && !content.equals(lesson.getContent())) {
            lesson.setContent(content);
            reEmbed = true;
        }

        if (orderIndex != null) {
            lesson.setOrderIndex(orderIndex);
        }

        lessonRepository.save(lesson);

        sendUpsertEvent(lesson, reEmbed);
    }

    @Override
    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = lessonRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.LESSON_NOT_FOUND));

        sendDeleteEvent(id);

        lessonRepository.delete(lesson);
    }

    // sync for creating, updating lesson
    private void sendUpsertEvent(Lesson lesson, boolean reEmbed) {
        Map<String, Object> metadata = Map.of(
            "course_id", lesson.getCourse().getId(),
            "order_index", lesson.getOrderIndex()
        );

        EntitySyncEvent event = new EntitySyncEvent(
            lesson.getId(),
            EntityType.LESSON,
            EntityAction.UPSERT,
            metadata,
            reEmbed,
            System.currentTimeMillis()
        );

        eventProducer.sendEntitySyncEvent(event);
    }

    // sync for deleting lesson
    private void sendDeleteEvent(Long id) {
        EntitySyncEvent event = new EntitySyncEvent(
            id,
            EntityType.LESSON,
            EntityAction.DELETE,
            Map.of(),
            false,
            System.currentTimeMillis()
        );
        eventProducer.sendEntitySyncEvent(event);
    }
}
