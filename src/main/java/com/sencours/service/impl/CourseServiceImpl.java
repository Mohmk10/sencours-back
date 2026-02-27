package com.sencours.service.impl;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.request.CourseSearchRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.dto.response.PageResponse;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.User;
import com.sencours.enums.Role;
import com.sencours.enums.Status;
import com.sencours.exception.BadRequestException;
import com.sencours.exception.ForbiddenException;
import com.sencours.exception.InstructorNotFoundException;
import com.sencours.exception.InvalidInstructorRoleException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.CourseMapper;
import com.sencours.repository.CategoryRepository;
import com.sencours.repository.CourseRepository;
import com.sencours.repository.UserRepository;
import com.sencours.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    @Override
    public CourseResponse create(CourseRequest request) {
        log.info("Création d'un nouveau cours: {}", request.getTitle());

        User instructor = validateAndGetInstructor(request.getInstructorId());
        Category category = validateAndGetCategory(request.getCategoryId());

        Course course = courseMapper.toEntity(request, instructor, category);
        course.setStatus(Status.DRAFT);

        Course savedCourse = courseRepository.save(course);

        log.info("Cours créé avec succès. ID: {}", savedCourse.getId());
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        log.debug("Recherche du cours avec ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", id));

        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAll() {
        log.debug("Récupération de tous les cours");

        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getByInstructorId(Long instructorId) {
        log.debug("Récupération des cours de l'instructeur ID: {}", instructorId);

        if (!userRepository.existsById(instructorId)) {
            throw new InstructorNotFoundException(instructorId);
        }

        return courseRepository.findByInstructorId(instructorId)
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getByCategoryId(Long categoryId) {
        log.debug("Récupération des cours de la catégorie ID: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Catégorie", "id", categoryId);
        }

        return courseRepository.findByCategoryId(categoryId)
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getByStatus(Status status) {
        log.debug("Récupération des cours avec status: {}", status);

        return courseRepository.findByStatus(status)
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> searchByTitle(String title) {
        log.debug("Recherche des cours par titre: {}", title);

        return courseRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    @Override
    public CourseResponse update(Long id, CourseRequest request) {
        log.info("Mise à jour du cours avec ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", id));

        User instructor = null;
        if (!course.getInstructor().getId().equals(request.getInstructorId())) {
            instructor = validateAndGetInstructor(request.getInstructorId());
        }

        Category category = null;
        if (!course.getCategory().getId().equals(request.getCategoryId())) {
            category = validateAndGetCategory(request.getCategoryId());
        }

        courseMapper.updateEntityFromRequest(request, course, instructor, category);
        Course updatedCourse = courseRepository.save(course);

        log.info("Cours mis à jour avec succès. ID: {}", updatedCourse.getId());
        return courseMapper.toResponse(updatedCourse);
    }

    @Override
    public void delete(Long id) {
        log.info("Suppression du cours avec ID: {}", id);

        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cours", "id", id);
        }

        courseRepository.deleteById(id);
        log.info("Cours supprimé avec succès. ID: {}", id);
    }

    @Override
    public CourseResponse publish(Long id) {
        log.info("Publication du cours avec ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", id));

        course.setStatus(Status.PUBLISHED);
        Course updatedCourse = courseRepository.save(course);

        log.info("Cours publié avec succès. ID: {}", id);
        return courseMapper.toResponse(updatedCourse);
    }

    @Override
    public CourseResponse archive(Long id) {
        log.info("Archivage du cours avec ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", id));

        course.setStatus(Status.ARCHIVED);
        Course updatedCourse = courseRepository.save(course);

        log.info("Cours archivé avec succès. ID: {}", id);
        return courseMapper.toResponse(updatedCourse);
    }

    @Override
    public CourseResponse updateStatus(Long courseId, String status, String userEmail) {
        log.info("Changement de statut du cours ID: {} vers {}", courseId, status);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Cours", "id", courseId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!course.getInstructor().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN
                && user.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Vous n'avez pas les droits sur ce cours");
        }

        try {
            course.setStatus(Status.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut invalide: " + status);
        }

        Course savedCourse = courseRepository.save(course);
        log.info("Statut du cours mis à jour avec succès. ID: {}, Nouveau statut: {}", courseId, status);
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllPaginated(Pageable pageable) {
        log.debug("Récupération de tous les cours avec pagination");
        Page<Course> page = courseRepository.findAll(pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> searchByTitlePaginated(String title, Pageable pageable) {
        log.debug("Recherche des cours par titre avec pagination: {}", title);
        Page<Course> page = courseRepository.findByTitleContainingIgnoreCase(title, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getByCategoryIdPaginated(Long categoryId, Pageable pageable) {
        log.debug("Récupération des cours de la catégorie ID avec pagination: {}", categoryId);
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Catégorie", "id", categoryId);
        }
        Page<Course> page = courseRepository.findByCategoryId(categoryId, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getByStatusPaginated(Status status, Pageable pageable) {
        log.debug("Récupération des cours par status avec pagination: {}", status);
        Page<Course> page = courseRepository.findByStatus(status, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getByInstructorIdPaginated(Long instructorId, Pageable pageable) {
        log.debug("Récupération des cours de l'instructeur ID avec pagination: {}", instructorId);
        if (!userRepository.existsById(instructorId)) {
            throw new InstructorNotFoundException(instructorId);
        }
        Page<Course> page = courseRepository.findByInstructorId(instructorId, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> searchByTitleAndStatusPaginated(String title, Status status, Pageable pageable) {
        log.debug("Recherche des cours par titre et status avec pagination: {} - {}", title, status);
        Page<Course> page = courseRepository.findByTitleContainingIgnoreCaseAndStatus(title, status, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getByCategoryIdAndStatusPaginated(Long categoryId, Status status, Pageable pageable) {
        log.debug("Récupération des cours par catégorie et status avec pagination: {} - {}", categoryId, status);
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Catégorie", "id", categoryId);
        }
        Page<Course> page = courseRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> search(CourseSearchRequest request, Pageable pageable) {
        log.debug("Recherche avancée de cours: {}", request);

        BigDecimal minPrice = request.getMinPrice() != null ?
                BigDecimal.valueOf(request.getMinPrice()) : null;
        BigDecimal maxPrice = request.getMaxPrice() != null ?
                BigDecimal.valueOf(request.getMaxPrice()) : null;

        Page<Course> page = courseRepository.search(
                request.getQuery(),
                request.getCategoryId(),
                minPrice,
                maxPrice,
                request.getFree(),
                pageable
        );

        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> searchByKeyword(String query, Pageable pageable) {
        log.debug("Recherche rapide de cours: {}", query);

        Page<Course> page;
        if (query == null || query.trim().isEmpty()) {
            page = courseRepository.findByStatus(Status.PUBLISHED, pageable);
        } else {
            page = courseRepository.searchByKeyword(query.trim(), pageable);
        }

        List<CourseResponse> content = page.getContent().stream()
                .map(courseMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSuggestions(String query) {
        log.debug("Suggestions de recherche pour: {}", query);

        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }

        return courseRepository.findTitleSuggestions(
                query.trim(),
                org.springframework.data.domain.PageRequest.of(0, 5)
        );
    }

    private User validateAndGetInstructor(Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new InstructorNotFoundException(instructorId));

        if (instructor.getRole() != Role.INSTRUCTEUR) {
            throw new InvalidInstructorRoleException(instructorId);
        }

        return instructor;
    }

    private Category validateAndGetCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", categoryId));
    }
}
