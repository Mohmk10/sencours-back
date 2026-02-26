package com.sencours.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {

    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private Boolean completed;
    private LocalDateTime completedAt;
    private Integer watchTimeSeconds;
    private Integer lastPositionSeconds;
}
