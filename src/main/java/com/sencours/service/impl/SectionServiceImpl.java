package com.sencours.service.impl;

import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.request.SectionRequest;
import com.sencours.dto.response.SectionResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Section;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.exception.SectionNotFoundException;
import com.sencours.mapper.SectionMapper;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.SectionRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.SectionService;
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
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SectionMapper sectionMapper;

    @Override
    public SectionResponse create(Long courseId, SectionRequest request) {
        log.info("Création d'une nouvelle section pour le cours ID: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", courseId));

        Section section = sectionMapper.toEntity(request, course);

        int nextOrderIndex = sectionRepository.countByCourseId(courseId) + 1;
        section.setOrderIndex(nextOrderIndex);

        Section savedSection = sectionRepository.save(section);

        log.info("Section créée avec succès. ID: {}, orderIndex: {}", savedSection.getId(), savedSection.getOrderIndex());
        return sectionMapper.toResponseWithoutLessons(savedSection);
    }

    @Override
    @Transactional(readOnly = true)
    public SectionResponse getById(Long id) {
        log.debug("Recherche de la section avec ID: {}", id);

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        return sectionMapper.toResponse(section);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getByCourseId(Long courseId) {
        log.debug("Récupération des sections du cours ID: {}", courseId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        return sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(sectionMapper::toResponse)
                .toList();
    }

    @Override
    public SectionResponse update(Long id, SectionRequest request) {
        log.info("Mise à jour de la section avec ID: {}", id);

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        sectionMapper.updateEntityFromRequest(request, section);
        Section updatedSection = sectionRepository.save(section);

        log.info("Section mise à jour avec succès. ID: {}", updatedSection.getId());
        return sectionMapper.toResponse(updatedSection);
    }

    @Override
    public void delete(Long id) {
        log.info("Suppression de la section avec ID: {}", id);

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        Long courseId = section.getCourse().getId();
        int deletedOrderIndex = section.getOrderIndex();

        sectionRepository.delete(section);

        List<Section> remainingSections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (Section s : remainingSections) {
            if (s.getOrderIndex() > deletedOrderIndex) {
                s.setOrderIndex(s.getOrderIndex() - 1);
                sectionRepository.save(s);
            }
        }

        log.info("Section supprimée avec succès. ID: {}", id);
    }

    @Override
    public void delete(Long id, String userEmail) {
        log.info("Suppression de la section avec ID: {} par {}", id, userEmail);

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new SectionNotFoundException(id));

        Course course = section.getCourse();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        boolean isOwner = course.getInstructor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Vous n'avez pas les droits pour supprimer cette section");
        }

        Long courseId = course.getId();
        int deletedOrderIndex = section.getOrderIndex();

        sectionRepository.delete(section);

        List<Section> remainingSections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        for (Section s : remainingSections) {
            if (s.getOrderIndex() > deletedOrderIndex) {
                s.setOrderIndex(s.getOrderIndex() - 1);
                sectionRepository.save(s);
            }
        }

        log.info("Section supprimée avec succès. ID: {}", id);
    }

    @Override
    public List<SectionResponse> reorder(Long courseId, ReorderRequest request) {
        log.info("Réorganisation des sections du cours ID: {}", courseId);

        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Cours", "id", courseId);
        }

        List<Section> sections = new ArrayList<>();
        int orderIndex = 1;

        for (Long sectionId : request.getOrderedIds()) {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new SectionNotFoundException(sectionId));

            if (!section.getCourse().getId().equals(courseId)) {
                throw new IllegalArgumentException(
                        String.format("La section %d n'appartient pas au cours %d", sectionId, courseId));
            }

            section.setOrderIndex(orderIndex++);
            sections.add(sectionRepository.save(section));
        }

        log.info("Sections réorganisées avec succès pour le cours ID: {}", courseId);
        return sections.stream()
                .map(sectionMapper::toResponseWithoutLessons)
                .toList();
    }
}
