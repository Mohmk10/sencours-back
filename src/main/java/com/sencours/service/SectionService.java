package com.sencours.service;

import com.sencours.dto.request.ReorderRequest;
import com.sencours.dto.request.SectionRequest;
import com.sencours.dto.response.SectionResponse;

import java.util.List;

public interface SectionService {

    SectionResponse create(Long courseId, SectionRequest request);

    SectionResponse getById(Long id);

    List<SectionResponse> getByCourseId(Long courseId);

    SectionResponse update(Long id, SectionRequest request);

    void delete(Long id);

    List<SectionResponse> reorder(Long courseId, ReorderRequest request);
}
