package com.sencours.repository;

import com.sencours.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    int countByCourseId(Long courseId);
}
