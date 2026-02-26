package com.sencours.dto.request;

import com.sencours.enums.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {

    @NotBlank(message = "Le titre de la leçon est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;

    @NotNull(message = "Le type de leçon est obligatoire")
    private LessonType type;

    @Size(max = 50000, message = "Le contenu ne peut pas dépasser 50000 caractères")
    private String content;

    @Min(value = 0, message = "La durée doit être positive")
    private Integer duration;

    private Boolean isFree;

    private String videoUrl;

    private String quizData;

    private String thumbnailUrl;
}
