package com.sencours.service;

import com.sencours.dto.request.ProgressRequest;
import com.sencours.dto.response.ProgressResponse;

import java.util.List;

public interface ProgressService {

    ProgressResponse updateProgress(Long lessonId, ProgressRequest request, String userEmail);

    ProgressResponse getProgress(Long lessonId, String userEmail);

    List<ProgressResponse> getCourseProgress(Long courseId, String userEmail);

    void markAsCompleted(Long lessonId, String userEmail);
}
