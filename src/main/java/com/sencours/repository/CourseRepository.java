package com.sencours.repository;

import com.sencours.entity.Course;
import com.sencours.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructorId(Long instructorId);

    List<Course> findByCategoryId(Long categoryId);

    List<Course> findByStatus(Status status);

    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByStatusAndCategoryId(Status status, Long categoryId);

    // Pagination methods
    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Course> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Course> findByStatus(Status status, Pageable pageable);

    Page<Course> findByTitleContainingIgnoreCaseAndStatus(String title, Status status, Pageable pageable);

    Page<Course> findByCategoryIdAndStatus(Long categoryId, Status status, Pageable pageable);

    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

    // Recherche avancée avec filtres
    @Query("SELECT c FROM Course c " +
           "LEFT JOIN c.instructor i " +
           "LEFT JOIN c.category cat " +
           "WHERE c.status = 'PUBLISHED' " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(cat.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(CONCAT(i.firstName, ' ', i.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:categoryId IS NULL OR cat.id = :categoryId) " +
           "AND (:minPrice IS NULL OR c.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR c.price <= :maxPrice) " +
           "AND (:free IS NULL OR (:free = true AND c.price = 0) OR (:free = false AND c.price > 0))")
    Page<Course> search(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("free") Boolean free,
            Pageable pageable
    );

    // Recherche simple par mot-clé
    @Query("SELECT c FROM Course c " +
           "LEFT JOIN c.instructor i " +
           "LEFT JOIN c.category cat " +
           "WHERE c.status = 'PUBLISHED' " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(cat.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(CONCAT(i.firstName, ' ', i.lastName)) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Course> searchByKeyword(@Param("query") String query, Pageable pageable);

    // Suggestions de recherche (autocomplete)
    @Query("SELECT DISTINCT c.title FROM Course c " +
           "WHERE c.status = 'PUBLISHED' " +
           "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY c.title")
    List<String> findTitleSuggestions(@Param("query") String query, Pageable pageable);
}
