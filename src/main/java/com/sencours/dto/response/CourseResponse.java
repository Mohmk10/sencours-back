package com.sencours.dto.response;

import com.sencours.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String thumbnailUrl;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long instructorId;
    private String instructorFirstName;
    private String instructorLastName;
    private String instructorName;
    private String instructorEmail;

    private Long categoryId;
    private String categoryName;

    @Builder.Default
    private List<SectionResponse> sections = new ArrayList<>();

    private Integer totalLessons;
    private Integer totalDuration;
    private Integer totalStudents;
    private Double averageRating;
}
