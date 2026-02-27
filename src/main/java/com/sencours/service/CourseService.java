package com.sencours.service;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.request.CourseSearchRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.dto.response.PageResponse;
import com.sencours.enums.Status;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {

    CourseResponse create(CourseRequest request);

    CourseResponse getById(Long id);

    List<CourseResponse> getAll();

    List<CourseResponse> getByInstructorId(Long instructorId);

    List<CourseResponse> getByCategoryId(Long categoryId);

    List<CourseResponse> getByStatus(Status status);

    List<CourseResponse> searchByTitle(String title);

    CourseResponse update(Long id, CourseRequest request);

    void delete(Long id);

    CourseResponse publish(Long id);

    CourseResponse archive(Long id);

    CourseResponse updateStatus(Long courseId, String status, String userEmail);

    // Pagination methods
    PageResponse<CourseResponse> getAllPaginated(Pageable pageable);

    PageResponse<CourseResponse> searchByTitlePaginated(String title, Pageable pageable);

    PageResponse<CourseResponse> getByCategoryIdPaginated(Long categoryId, Pageable pageable);

    PageResponse<CourseResponse> getByStatusPaginated(Status status, Pageable pageable);

    PageResponse<CourseResponse> getByInstructorIdPaginated(Long instructorId, Pageable pageable);

    PageResponse<CourseResponse> searchByTitleAndStatusPaginated(String title, Status status, Pageable pageable);

    PageResponse<CourseResponse> getByCategoryIdAndStatusPaginated(Long categoryId, Status status, Pageable pageable);

    // Search methods
    PageResponse<CourseResponse> search(CourseSearchRequest request, Pageable pageable);

    PageResponse<CourseResponse> searchByKeyword(String query, Pageable pageable);

    List<String> getSuggestions(String query);
}
