package com.sencours.service.impl;

import com.sencours.dto.request.ProgressRequest;
import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.*;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.repository.*;
import com.sencours.service.EnrollmentService;
import com.sencours.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;

    @Override
    @Transactional
    public ProgressResponse updateProgress(Long lessonId, ProgressRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Leçon non trouvée"));

        Course course = lesson.getSection().getCourse();

        if (!lesson.getIsFree()) {
            if (!enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
                throw new BadRequestException("Vous devez être inscrit au cours pour accéder à cette leçon");
            }
        }

        Progress progress = progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElse(Progress.builder()
                        .user(user)
                        .lesson(lesson)
                        .completed(false)
                        .watchTimeSeconds(0)
                        .lastPositionSeconds(0)
                        .build());

        if (request.getCompleted() != null) {
            progress.setCompleted(request.getCompleted());
            if (request.getCompleted() && progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        }

        if (request.getWatchTimeSeconds() != null) {
            progress.setWatchTimeSeconds(request.getWatchTimeSeconds());
        }

        if (request.getLastPositionSeconds() != null) {
            progress.setLastPositionSeconds(request.getLastPositionSeconds());
        }

        progress = progressRepository.save(progress);

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            enrollmentService.updateProgress(course.getId(), userEmail);
        }

        return mapToResponse(progress);
    }

    @Override
    public ProgressResponse getProgress(Long lessonId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return progressRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .map(this::mapToResponse)
                .orElse(ProgressResponse.builder()
                        .lessonId(lessonId)
                        .completed(false)
                        .watchTimeSeconds(0)
                        .lastPositionSeconds(0)
                        .build());
    }

    @Override
    public List<ProgressResponse> getCourseProgress(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return progressRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsCompleted(Long lessonId, String userEmail) {
        ProgressRequest request = new ProgressRequest();
        request.setCompleted(true);
        updateProgress(lessonId, request, userEmail);
    }

    private ProgressResponse mapToResponse(Progress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson().getId())
                .lessonTitle(progress.getLesson().getTitle())
                .completed(progress.getCompleted())
                .completedAt(progress.getCompletedAt())
                .watchTimeSeconds(progress.getWatchTimeSeconds())
                .lastPositionSeconds(progress.getLastPositionSeconds())
                .build();
    }
}
