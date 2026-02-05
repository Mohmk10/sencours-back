package com.sencours.service.impl;

import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.Enrollment;
import com.sencours.entity.Progress;
import com.sencours.exception.EnrollmentNotFoundException;
import com.sencours.exception.ProgressNotFoundException;
import com.sencours.mapper.ProgressMapper;
import com.sencours.repository.EnrollmentRepository;
import com.sencours.repository.ProgressRepository;
import com.sencours.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository progressRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ProgressMapper progressMapper;

    @Override
    public ProgressResponse markLessonCompleted(Long enrollmentId, Long lessonId) {
        log.info("Marquage de la leçon {} comme complétée pour l'inscription {}", lessonId, enrollmentId);

        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseThrow(() -> new ProgressNotFoundException(enrollmentId, lessonId));

        if (!progress.getCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progress = progressRepository.save(progress);

            checkAndUpdateCourseCompletion(enrollmentId);
        }

        log.info("Leçon {} marquée comme complétée pour l'inscription {}", lessonId, enrollmentId);
        return progressMapper.toResponse(progress);
    }

    @Override
    public ProgressResponse markLessonIncomplete(Long enrollmentId, Long lessonId) {
        log.info("Marquage de la leçon {} comme non complétée pour l'inscription {}", lessonId, enrollmentId);

        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseThrow(() -> new ProgressNotFoundException(enrollmentId, lessonId));

        if (progress.getCompleted()) {
            progress.setCompleted(false);
            progress.setCompletedAt(null);
            progress = progressRepository.save(progress);

            Enrollment enrollment = progress.getEnrollment();
            if (enrollment.getCompletedAt() != null) {
                enrollment.setCompletedAt(null);
                enrollmentRepository.save(enrollment);
            }
        }

        log.info("Leçon {} marquée comme non complétée pour l'inscription {}", lessonId, enrollmentId);
        return progressMapper.toResponse(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgressResponse> getProgressByEnrollment(Long enrollmentId) {
        log.debug("Récupération des progressions pour l'inscription {}", enrollmentId);

        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
        }

        return progressRepository.findByEnrollmentId(enrollmentId).stream()
                .map(progressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Long enrollmentId, Long lessonId) {
        log.debug("Récupération de la progression pour l'inscription {} et la leçon {}", enrollmentId, lessonId);

        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseThrow(() -> new ProgressNotFoundException(enrollmentId, lessonId));

        return progressMapper.toResponse(progress);
    }

    private void checkAndUpdateCourseCompletion(Long enrollmentId) {
        int totalLessons = progressRepository.countByEnrollmentId(enrollmentId);
        int completedLessons = progressRepository.countByEnrollmentIdAndCompletedTrue(enrollmentId);

        if (totalLessons > 0 && totalLessons == completedLessons) {
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
                enrollmentRepository.save(enrollment);
                log.info("Cours complété pour l'inscription {}", enrollmentId);
            }
        }
    }
}
