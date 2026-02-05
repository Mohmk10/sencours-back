package com.sencours.service.impl;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.dto.response.PageResponse;
import com.sencours.entity.Category;
import com.sencours.exception.ResourceAlreadyExistsException;
import com.sencours.exception.ResourceNotFoundException;
import com.sencours.mapper.CategoryMapper;
import com.sencours.repository.CategoryRepository;
import com.sencours.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse create(CategoryRequest request) {
        log.info("Création d'une nouvelle catégorie: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Catégorie", "nom", request.getName());
        }

        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);

        log.info("Catégorie créée avec succès. ID: {}", savedCategory.getId());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        log.debug("Recherche de la catégorie avec ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        log.debug("Récupération de toutes les catégories");

        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        log.info("Mise à jour de la catégorie avec ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));

        // Vérifier si le nouveau nom existe déjà pour une autre catégorie
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Catégorie", "nom", request.getName());
        }

        categoryMapper.updateEntityFromRequest(request, category);
        Category updatedCategory = categoryRepository.save(category);

        log.info("Catégorie mise à jour avec succès. ID: {}", updatedCategory.getId());
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void delete(Long id) {
        log.info("Suppression de la catégorie avec ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie", "id", id);
        }

        categoryRepository.deleteById(id);
        log.info("Catégorie supprimée avec succès. ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllPaginated(Pageable pageable) {
        log.debug("Récupération de toutes les catégories avec pagination");
        Page<Category> page = categoryRepository.findAll(pageable);
        List<CategoryResponse> content = page.getContent().stream()
                .map(categoryMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> searchByNamePaginated(String name, Pageable pageable) {
        log.debug("Recherche des catégories par nom avec pagination: {}", name);
        Page<Category> page = categoryRepository.findByNameContainingIgnoreCase(name, pageable);
        List<CategoryResponse> content = page.getContent().stream()
                .map(categoryMapper::toResponse)
                .toList();
        return PageResponse.of(page, content);
    }
}
