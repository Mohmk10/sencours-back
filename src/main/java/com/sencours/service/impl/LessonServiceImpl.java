package com.sencours.service.impl;

import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.response.LessonResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Lesson;
import com.sencours.entity.Section;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.LessonNotFoundException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.exception.SectionNotFoundException;
import com.sencours.mapper.LessonMapper;
import com.sencours.repository.LessonRepository;
import com.sencours.repository.SectionRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.FileStorageService;
import com.sencours.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final LessonMapper lessonMapper;

    @Override
    public LessonResponse create(Long sectionId, LessonRequest request) {
        log.info("Création d'une nouvelle leçon pour la section ID: {}", sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));

        Lesson lesson = lessonMapper.toEntity(request, section);

        int nextOrderIndex = lessonRepository.countBySectionId(sectionId) + 1;
        lesson.setOrderIndex(nextOrderIndex);

        Lesson savedLesson = lessonRepository.save(lesson);

        log.info("Leçon créée avec succès. ID: {}, orderIndex: {}", savedLesson.getId(), savedLesson.getOrderIndex());
        return lessonMapper.toResponse(savedLesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getById(Long id) {
        log.debug("Recherche de la leçon avec ID: {}", id);

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));

        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getBySectionId(Long sectionId) {
        log.debug("Récupération des leçons de la section ID: {}", sectionId);

        if (!sectionRepository.existsById(sectionId)) {
            throw new SectionNotFoundException(sectionId);
        }

        return lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId)
                .stream()
                .map(lessonMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getFreeLessons() {
        log.debug("Récupération des leçons gratuites");

        return lessonRepository.findByIsFreeTrue()
                .stream()
                .map(lessonMapper::toResponse)
                .toList();
    }

    @Override
    public LessonResponse update(Long id, LessonRequest request) {
        log.info("Mise à jour de la leçon avec ID: {}", id);

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));

        lessonMapper.updateEntityFromRequest(request, lesson);
        Lesson updatedLesson = lessonRepository.save(lesson);

        log.info("Leçon mise à jour avec succès. ID: {}", updatedLesson.getId());
        return lessonMapper.toResponse(updatedLesson);
    }

    @Override
    public void delete(Long id) {
        log.info("Suppression de la leçon avec ID: {}", id);

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));

        Long sectionId = lesson.getSection().getId();
        int deletedOrderIndex = lesson.getOrderIndex();

        lessonRepository.delete(lesson);

        List<Lesson> remainingLessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
        for (Lesson l : remainingLessons) {
            if (l.getOrderIndex() > deletedOrderIndex) {
                l.setOrderIndex(l.getOrderIndex() - 1);
                lessonRepository.save(l);
            }
        }

        log.info("Leçon supprimée avec succès. ID: {}", id);
    }

    @Override
    public void delete(Long id, String userEmail) {
        log.info("Suppression de la leçon avec ID: {} par {}", id, userEmail);

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException(id));

        Course course = lesson.getSection().getCourse();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        boolean isOwner = course.getInstructor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Vous n'avez pas les droits pour supprimer cette leçon");
        }

        if (lesson.getFilePath() != null && !lesson.getFilePath().isEmpty()) {
            fileStorageService.deleteFile(lesson.getFilePath());
        }

        Long sectionId = lesson.getSection().getId();
        int deletedOrderIndex = lesson.getOrderIndex();

        lessonRepository.delete(lesson);

        List<Lesson> remainingLessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
        for (Lesson l : remainingLessons) {
            if (l.getOrderIndex() > deletedOrderIndex) {
                l.setOrderIndex(l.getOrderIndex() - 1);
                lessonRepository.save(l);
            }
        }

        log.info("Leçon supprimée avec succès. ID: {}", id);
    }

    @Override
    public List<LessonResponse> reorder(Long sectionId, ReorderRequest request) {
        log.info("Réorganisation des leçons de la section ID: {}", sectionId);

        if (!sectionRepository.existsById(sectionId)) {
            throw new SectionNotFoundException(sectionId);
        }

        List<Lesson> lessons = new ArrayList<>();
        int orderIndex = 1;

        for (Long lessonId : request.getOrderedIds()) {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new LessonNotFoundException(lessonId));

            if (!lesson.getSection().getId().equals(sectionId)) {
                throw new IllegalArgumentException(
                        String.format("La leçon %d n'appartient pas à la section %d", lessonId, sectionId));
            }

            lesson.setOrderIndex(orderIndex++);
            lessons.add(lessonRepository.save(lesson));
        }

        log.info("Leçons réorganisées avec succès pour la section ID: {}", sectionId);
        return lessons.stream()
                .map(lessonMapper::toResponse)
                .toList();
    }
}
