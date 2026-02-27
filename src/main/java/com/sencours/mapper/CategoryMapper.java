package com.sencours.mapper;

import com.sencours.dto.request.CategoryRequest;
import com.sencours.dto.response.CategoryResponse;
import com.sencours.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category entity) {
        if (entity == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .courseCount(entity.getCourses() != null ? (long) entity.getCourses().size() : 0L)
                .build();
    }

    public Category toEntity(CategoryRequest request) {
        if (request == null) {
            return null;
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return category;
    }

    public void updateEntityFromRequest(CategoryRequest request, Category entity) {
        if (request == null || entity == null) {
            return;
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
    }
}
