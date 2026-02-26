package com.sencours.dto.response;

import com.sencours.enums.LessonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {

    private Long id;
    private String title;
    private LessonType type;
    private String content;
    private Integer duration;
    private Integer orderIndex;
    private Boolean isFree;
    private String videoUrl;
    private String filePath;
    private String quizData;
    private String thumbnailUrl;
    private Long sectionId;
}
