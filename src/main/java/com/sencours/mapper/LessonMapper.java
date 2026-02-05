package com.sencours.mapper;

import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.response.LessonResponse;
import com.sencours.entity.Lesson;
import com.sencours.entity.Section;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

    public LessonResponse toResponse(Lesson entity) {
        if (entity == null) {
            return null;
        }

        return LessonResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(entity.getType())
                .content(entity.getContent())
                .duration(entity.getDuration())
                .orderIndex(entity.getOrderIndex())
                .isFree(entity.getIsFree())
                .sectionId(entity.getSection() != null ? entity.getSection().getId() : null)
                .build();
    }

    public Lesson toEntity(LessonRequest request, Section section) {
        if (request == null) {
            return null;
        }

        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setType(request.getType());
        lesson.setContent(request.getContent());
        lesson.setDuration(request.getDuration());
        lesson.setIsFree(request.getIsFree() != null ? request.getIsFree() : false);
        lesson.setSection(section);

        return lesson;
    }

    public void updateEntityFromRequest(LessonRequest request, Lesson entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.setTitle(request.getTitle());
        entity.setType(request.getType());
        entity.setContent(request.getContent());
        entity.setDuration(request.getDuration());
        if (request.getIsFree() != null) {
            entity.setIsFree(request.getIsFree());
        }
    }
}
