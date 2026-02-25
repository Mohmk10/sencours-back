package com.sencours.dto;

import com.sencours.enums.AppealStatus;
import lombok.Data;

@Data
public class SuspensionAppealReviewRequest {
    private AppealStatus status;
    private String adminResponse;
}
