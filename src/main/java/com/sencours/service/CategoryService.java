package com.sencours.service;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse getById(Long id);

    List<CategoryResponse> getAll();

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    boolean existsByName(String name);

    // Pagination methods
    PageResponse<CategoryResponse> getAllPaginated(Pageable pageable);

    PageResponse<CategoryResponse> searchByNamePaginated(String name, Pageable pageable);
}
