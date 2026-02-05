package com.sencours.service;

import com.sencours.dto.request.LessonRequest;
import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.response.LessonResponse;

import java.util.List;

public interface LessonService {

    LessonResponse create(Long sectionId, LessonRequest request);

    LessonResponse getById(Long id);

    List<LessonResponse> getBySectionId(Long sectionId);

    List<LessonResponse> getFreeLessons();

    LessonResponse update(Long id, LessonRequest request);

    void delete(Long id);

    List<LessonResponse> reorder(Long sectionId, ReorderRequest request);
}
