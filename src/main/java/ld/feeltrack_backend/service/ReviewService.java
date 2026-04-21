package ld.feeltrack_backend.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.entity.Customer;
import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.external.nlp.FeelingAnalyser;
import ld.feeltrack_backend.projection.ReviewCountProjection;
import ld.feeltrack_backend.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerService customerService;

    public ReviewService(CustomerService customerService, ReviewRepository reviewRepository) {
        this.customerService = customerService;
        this.reviewRepository = reviewRepository;
    }
    
    public Review createReview(Review review) {

        if (review.getText() == null || review.getText().isBlank()) {
            throw new IllegalArgumentException("Review text cannot be null or empty.");
        }

        Customer customer = review.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer info must be provided.");
        }

        // Si l'ID est présent, customer existant
        if (customer.getId() != null) {
            customer = customerService.getCustomerById(customer.getId());
        } else {
            // Sinon, création du customer si email présent
            if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email is required to create a new review");
            }
            customer = customerService.findOrCreateCustomer(customer);
        }

        review.setCustomer(customer);

        // Analyse du type
        review.setType(FeelingAnalyser.analyzeFeelingType(review.getText()));

        return reviewRepository.save(review);
    }
    
    public List<Review> findReviews(ReviewType reviewType) {

        if (reviewType == null) {
            //Tri par date décroissante pour afficher les avis les plus récents en premier
            return this.reviewRepository.findAllByOrderByCreatedAtDesc();
        }
        
        return this.reviewRepository.findByTypeOrderByCreatedAtDesc(reviewType);
    }

    public ReviewStatsDTO getReviewStats() {

    List<ReviewCountProjection> results = reviewRepository.countReviewsByType();

        Map<ReviewType, Long> reviewsCountByType = new EnumMap<>(ReviewType.class);

        for (ReviewCountProjection row : results) {
            reviewsCountByType.put(row.getType(), row.getCount());
        }

        return new ReviewStatsDTO(
            reviewsCountByType.getOrDefault(ReviewType.POSITIVE, 0L),
            reviewsCountByType.getOrDefault(ReviewType.NEGATIVE, 0L),
            reviewsCountByType.getOrDefault(ReviewType.NEUTRAL, 0L)
        );
    }

    public void deleteReview(int id) {
        // Vérification que l'avis existe avant de tenter de le supprimer
        if (!this.reviewRepository.existsById(id)) {
            throw new EntityNotFoundException("No review found with the ID : " + id + ".");
        }
        this.reviewRepository.deleteById(id);
    }

}