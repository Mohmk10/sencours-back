package com.sencours.dto.request;

import com.sencours.enums.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Le titre du cours est obligatoire")
    @Size(min = 5, max = 200, message = "Le titre doit contenir entre 5 et 200 caractères")
    private String title;

    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix doit être supérieur ou égal à 0")
    private BigDecimal price;

    private String thumbnailUrl;

    private Status status;

    @NotNull(message = "L'ID de l'instructeur est obligatoire")
    private Long instructorId;

    @NotNull(message = "L'ID de la catégorie est obligatoire")
    private Long categoryId;
}
