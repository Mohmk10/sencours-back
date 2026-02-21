package com.sencours.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorApplicationCreateRequest {

    @NotBlank(message = "La motivation est obligatoire")
    @Size(min = 50, max = 500, message = "La motivation doit contenir entre 50 et 500 caractères")
    private String motivation;

    @Size(max = 500, message = "L'expertise ne peut pas dépasser 500 caractères")
    private String expertise;

    @Size(max = 255)
    private String linkedinUrl;

    @Size(max = 255)
    private String portfolioUrl;
}
