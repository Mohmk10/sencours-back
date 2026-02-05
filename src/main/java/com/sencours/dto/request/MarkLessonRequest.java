package com.sencours.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkLessonRequest {

    @NotNull(message = "L'ID de la leçon est obligatoire")
    private Long lessonId;

    @NotNull(message = "Le statut de complétion est obligatoire")
    private Boolean completed;
}
