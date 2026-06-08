package ld.feeltrack_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.dto.ReviewTimelineDTO;
import ld.feeltrack_backend.entity.Customer;
import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.external.nlp.SentimentService;
import ld.feeltrack_backend.projection.ReviewCountProjection;
import ld.feeltrack_backend.projection.ReviewTimelineProjection;
import ld.feeltrack_backend.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerService customerService;
    private final SentimentService sentimentService;

    public ReviewService(CustomerService customerService, ReviewRepository reviewRepository, SentimentService sentimentService) {
        this.customerService = customerService;
        this.reviewRepository = reviewRepository;
        this.sentimentService = sentimentService;
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

        // Analyse du type de sentiment du texte de l'avis
       review.setType(sentimentService.analyze(review.getText()));

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

    // Get review counts grouped by day and type for the last N days
    public List<ReviewTimelineDTO> getReviewTimeline(int days) {

        LocalDateTime from = LocalDateTime.now().minusDays(days);

        List<ReviewTimelineProjection> raw = reviewRepository.countReviewsByDateAndType(from);

        Map<LocalDate, ReviewTimelineDTO> map = new LinkedHashMap<>();

        for (ReviewTimelineProjection row : raw) {

            /* Process each row to populate the timeline data */

            LocalDate date = row.getCreatedDate();
            // Ensure there's a DTO for this date in the map, initialize counts to 0
            map.putIfAbsent(
                date,
                new ReviewTimelineDTO(date, 0, 0, 0)
            );
            // Update the counts in the DTO based on the review type
            ReviewTimelineDTO dto = map.get(date);
            switch (row.getType()) {
                case POSITIVE -> dto.addPositive(row.getCount());
                case NEGATIVE -> dto.addNegative(row.getCount());
                case NEUTRAL -> dto.addNeutral(row.getCount());
            }
        }

        return new ArrayList<>(map.values());
    }

    public void deleteReview(int id) {
        // Vérification que l'avis existe avant de tenter de le supprimer
        if (!this.reviewRepository.existsById(id)) {
            throw new EntityNotFoundException("No review found with the ID : " + id + ".");
        }
        this.reviewRepository.deleteById(id);
    }

}