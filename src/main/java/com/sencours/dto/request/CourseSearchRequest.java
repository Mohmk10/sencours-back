package com.sencours.dto.request;

import lombok.Data;

@Data
public class CourseSearchRequest {
    private String query;
    private Long categoryId;
    private Double minPrice;
    private Double maxPrice;
    private Double minRating;
    private Boolean free;
    private String sortBy;
    private String sortDirection;
}
