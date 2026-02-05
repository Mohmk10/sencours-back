package com.sencours.service;

import com.sencours.dto.response.ProgressResponse;

import java.util.List;

public interface ProgressService {

    ProgressResponse markLessonCompleted(Long enrollmentId, Long lessonId);

    ProgressResponse markLessonIncomplete(Long enrollmentId, Long lessonId);

    List<ProgressResponse> getProgressByEnrollment(Long enrollmentId);

    ProgressResponse getProgress(Long enrollmentId, Long lessonId);
}
