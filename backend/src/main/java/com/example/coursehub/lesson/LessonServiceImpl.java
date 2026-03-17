package com.example.coursehub.lesson;

import com.example.coursehub.course.Course;
import com.example.coursehub.lesson.dto.LessonResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.course.CourseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final CourseRepository courseRepository;

    public LessonServiceImpl(LessonRepository lessonRepository, LessonMapper lessonMapper, CourseRepository courseRepository) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
        this.courseRepository = courseRepository;
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

        return lessonMapper.toLessonResponse(lesson);
    }

    @Override
    @Transactional
    public void updateLesson(Long id, String title, String content, Integer orderIndex) {
        Lesson lesson = lessonRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.LESSON_NOT_FOUND));

        if (title == null &&
            content == null &&
            orderIndex == null)
        {
            throw new UserError(ErrorCode.EMPTY_UPDATE_REQUEST);
        }
        if(title != null){
            lesson.setTitle(title);
        }
        if(content != null){
            lesson.setContent(content);
        }
        if(orderIndex != null){
            lesson.setOrderIndex(orderIndex);
        }

        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = lessonRepository.findById(id)
            .orElseThrow(() -> new UserError(ErrorCode.LESSON_NOT_FOUND));

        lessonRepository.delete(lesson);
    }
}
