package com.sencours.controller;

import com.sencours.entity.Course;
import com.sencours.repository.CourseRepository;
import com.sencours.service.YouTubeThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/migration")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class MigrationController {

    private final CourseRepository courseRepository;
    private final YouTubeThumbnailService youTubeThumbnailService;

    /**
     * Migre les thumbnails des cours existants vers les thumbnails YouTube
     * Pour les cours qui ont des videos YouTube mais pas de thumbnail valide
     */
    @PostMapping("/thumbnails")
    public ResponseEntity<Map<String, Object>> migrateThumbnails() {
        List<Course> courses = courseRepository.findAll();
        int updated = 0;
        int skipped = 0;

        for (Course course : courses) {
            String currentThumbnail = course.getThumbnailUrl();
            boolean hasValidThumbnail = currentThumbnail != null
                    && (currentThumbnail.startsWith("https://res.cloudinary.com")
                    || currentThumbnail.startsWith("https://img.youtube.com"));

            if (hasValidThumbnail) {
                skipped++;
                continue;
            }

            String youtubeUrl = findFirstYouTubeUrlInCourse(course);

            if (youtubeUrl != null) {
                String youtubeThumbnail = youTubeThumbnailService.getThumbnailUrl(youtubeUrl);
                if (youtubeThumbnail != null) {
                    course.setThumbnailUrl(youtubeThumbnail);
                    courseRepository.save(course);
                    updated++;
                    log.info("Updated thumbnail for course {}: {}", course.getId(), youtubeThumbnail);
                }
            } else {
                skipped++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCourses", courses.size());
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("message", "Migration terminee avec succes");

        return ResponseEntity.ok(result);
    }

    private String findFirstYouTubeUrlInCourse(Course course) {
        if (course.getSections() == null) {
            return null;
        }

        return course.getSections().stream()
                .filter(section -> section.getLessons() != null)
                .flatMap(section -> section.getLessons().stream())
                .filter(lesson -> lesson.getVideoUrl() != null
                        && youTubeThumbnailService.isYouTubeUrl(lesson.getVideoUrl()))
                .map(lesson -> lesson.getVideoUrl())
                .findFirst()
                .orElse(null);
    }
}
