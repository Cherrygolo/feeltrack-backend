package ld.feeltrack_backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.projection.ReviewCountProjection;
import ld.feeltrack_backend.projection.ReviewTimelineProjection;


public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findAllByOrderByCreatedAtDesc();
    List<Review> findByType(ReviewType type);
    List<Review> findByTypeOrderByCreatedAtDesc(ReviewType type);
    
    boolean existsByCustomerId(int customerId);

    @Query("""
        SELECT r.type AS type, COUNT(r) AS count
        FROM Review r
        GROUP BY r.type
    """)
    List<ReviewCountProjection> countReviewsByType();

    /**
     * Returns the number of reviews grouped by creation date and review type
     * for analytics and dashboard timeline purposes.
     */
    @Query("""
        SELECT
            CAST(r.createdAt AS date) AS createdDate,
            r.type AS type,
            COUNT(r) AS count
        FROM Review r
        WHERE r.createdAt >= :from
        GROUP BY CAST(r.createdAt AS date), r.type
        ORDER BY createdDate, type
    """)
    List<ReviewTimelineProjection> countReviewsByDateAndType(LocalDateTime from);
    

}
