package com.sencours.mapper;

import com.sencours.dto.response.ProgressResponse;
import com.sencours.entity.Progress;
import org.springframework.stereotype.Component;

@Component
public class ProgressMapper {

    public ProgressResponse toResponse(Progress progress) {
        return ProgressResponse.builder()
                .id(progress.getId())
                .lessonId(progress.getLesson().getId())
                .lessonTitle(progress.getLesson().getTitle())
                .lessonOrderIndex(progress.getLesson().getOrderIndex())
                .completed(progress.getCompleted())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
