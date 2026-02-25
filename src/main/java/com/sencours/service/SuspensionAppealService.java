package com.sencours.service;

import com.sencours.dto.SuspensionAppealRequest;
import com.sencours.dto.SuspensionAppealResponse;
import com.sencours.dto.SuspensionAppealReviewRequest;

import java.util.List;

public interface SuspensionAppealService {

    SuspensionAppealResponse submitAppeal(SuspensionAppealRequest request, String userEmail);

    List<SuspensionAppealResponse> getUserAppeals(String userEmail);

    List<SuspensionAppealResponse> getPendingAppeals();

    SuspensionAppealResponse reviewAppeal(Long appealId, SuspensionAppealReviewRequest request, String adminEmail);
}
