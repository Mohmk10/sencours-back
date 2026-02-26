package com.sencours.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressRequest {

    private Boolean completed;
    private Integer watchTimeSeconds;
    private Integer lastPositionSeconds;
}
