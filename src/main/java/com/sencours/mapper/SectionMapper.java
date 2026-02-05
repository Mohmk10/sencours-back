package com.sencours.mapper;

import com.sencours.dto.request.SectionRequest;
import com.sencours.dto.response.SectionResponse;
import com.sencours.entity.Course;
import com.sencours.entity.Section;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SectionMapper {

    private final LessonMapper lessonMapper;

    public SectionResponse toResponse(Section entity) {
        if (entity == null) {
            return null;
        }

        SectionResponse.SectionResponseBuilder builder = SectionResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .orderIndex(entity.getOrderIndex());

        if (entity.getCourse() != null) {
            builder.courseId(entity.getCourse().getId())
                    .courseTitle(entity.getCourse().getTitle());
        }

        if (entity.getLessons() != null && !entity.getLessons().isEmpty()) {
            builder.lessons(entity.getLessons().stream()
                    .map(lessonMapper::toResponse)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public SectionResponse toResponseWithoutLessons(Section entity) {
        if (entity == null) {
            return null;
        }

        SectionResponse.SectionResponseBuilder builder = SectionResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .orderIndex(entity.getOrderIndex());

        if (entity.getCourse() != null) {
            builder.courseId(entity.getCourse().getId())
                    .courseTitle(entity.getCourse().getTitle());
        }

        return builder.build();
    }

    public Section toEntity(SectionRequest request, Course course) {
        if (request == null) {
            return null;
        }

        Section section = new Section();
        section.setTitle(request.getTitle());
        section.setCourse(course);

        return section;
    }

    public void updateEntityFromRequest(SectionRequest request, Section entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.setTitle(request.getTitle());
    }
}
