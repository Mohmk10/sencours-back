package com.sencours.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponse {

    private Long id;
    private String title;
    private Integer orderIndex;
    private Long courseId;
    private String courseTitle;

    @Builder.Default
    private List<LessonResponse> lessons = new ArrayList<>();
}
