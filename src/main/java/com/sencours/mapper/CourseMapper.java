package com.sencours.mapper;

import com.sencours.dto.request.CourseRequest;
import com.sencours.dto.response.CourseResponse;
import com.sencours.dto.response.SectionResponse;
import com.sencours.entity.Category;
import com.sencours.entity.Course;
import com.sencours.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseMapper {

    private final SectionMapper sectionMapper;

    public CourseResponse toResponse(Course entity) {
        if (entity == null) {
            return null;
        }

        CourseResponse.CourseResponseBuilder builder = CourseResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .thumbnailUrl(entity.getThumbnailUrl())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getInstructor() != null) {
            User instructor = entity.getInstructor();
            builder.instructorId(instructor.getId())
                    .instructorFirstName(instructor.getFirstName())
                    .instructorLastName(instructor.getLastName())
                    .instructorName(instructor.getFirstName() + " " + instructor.getLastName())
                    .instructorEmail(instructor.getEmail());
        }

        if (entity.getCategory() != null) {
            builder.categoryId(entity.getCategory().getId())
                    .categoryName(entity.getCategory().getName());
        }

        if (entity.getEnrollments() != null) {
            int count = entity.getEnrollments().size();
            builder.totalStudents(count);
            builder.enrollmentCount(count);
        } else {
            builder.totalStudents(0);
            builder.enrollmentCount(0);
        }

        if (entity.getReviews() != null && !entity.getReviews().isEmpty()) {
            double avg = entity.getReviews().stream()
                    .mapToInt(r -> r.getRating())
                    .average()
                    .orElse(0.0);
            builder.averageRating(Math.round(avg * 10.0) / 10.0);
        } else {
            builder.averageRating(0.0);
        }

        if (entity.getSections() != null && !entity.getSections().isEmpty()) {
            List<SectionResponse> sectionResponses = entity.getSections().stream()
                    .map(sectionMapper::toResponse)
                    .collect(Collectors.toList());
            builder.sections(sectionResponses);

            int totalLessons = entity.getSections().stream()
                    .mapToInt(s -> s.getLessons() != null ? s.getLessons().size() : 0)
                    .sum();
            int totalDuration = entity.getSections().stream()
                    .flatMap(s -> s.getLessons() != null ? s.getLessons().stream() : java.util.stream.Stream.empty())
                    .mapToInt(l -> l.getDuration() != null ? l.getDuration() : 0)
                    .sum();
            builder.totalLessons(totalLessons);
            builder.totalDuration(totalDuration);
        }

        return builder.build();
    }

    public Course toEntity(CourseRequest request, User instructor, Category category) {
        if (request == null) {
            return null;
        }

        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setInstructor(instructor);
        course.setCategory(category);

        return course;
    }

    public void updateEntityFromRequest(CourseRequest request, Course entity, User instructor, Category category) {
        if (request == null || entity == null) {
            return;
        }

        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setThumbnailUrl(request.getThumbnailUrl());

        if (instructor != null) {
            entity.setInstructor(instructor);
        }

        if (category != null) {
            entity.setCategory(category);
        }
    }
}
