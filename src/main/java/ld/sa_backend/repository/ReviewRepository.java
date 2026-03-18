package ld.sa_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ld.sa_backend.entity.Review;
import ld.sa_backend.enums.ReviewType;


public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findAllByOrderByIdDesc();
    List<Review> findByType(ReviewType type);
    List<Review> findByTypeOrderByIdDesc(ReviewType type);
    
    boolean existsByCustomerId(int customerId);

    @Query("SELECT r.type, COUNT(r) FROM Review r GROUP BY r.type")
    List<Object[]> countReviewsByType();

}
