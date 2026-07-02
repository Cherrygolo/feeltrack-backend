package ld.feeltrack_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.analytics.Granularity;
import ld.feeltrack_backend.analytics.GranularityResolver;
import ld.feeltrack_backend.analytics.TimeSeriesGranularity;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.dto.ReviewTimelineItemDTO;
import ld.feeltrack_backend.dto.ReviewTimelineResponseDTO;
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

        Customer customer = review.getCustomer();

        customer = customerService.findOrCreateCustomer(customer);

        review.setCustomer(customer);

       // Analyze the review text to determine its sentiment type (POSITIVE, NEGATIVE, NEUTRAL)
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

    // Get review counts grouped by day and type for the last N days with flexible granularity (day, week, month)
    public ReviewTimelineResponseDTO getReviewTimeline(
        int days,
        Granularity granularity
    ) {

        LocalDateTime from = LocalDateTime.now().minusDays(days);
        LocalDate to = LocalDate.now();

        List<ReviewTimelineProjection> countReviewsByTypeAndDateResult =
            reviewRepository.countReviewsByDateAndType(from);

        Granularity finalGranularity =
            (granularity != null)
                ? granularity
                : GranularityResolver.resolve(days);

        /*
         * Aggregation of review counts by normalized date and type 
         * to fill the DTO timeline later.
         */

        Map<LocalDate, Map<ReviewType, Long>> reviewsCountByTypeAndDate = new HashMap<>();

        for (ReviewTimelineProjection row : countReviewsByTypeAndDateResult) {

            //normalize the date according to the granularity (day, week, month)
            LocalDate normalizedDate =
                TimeSeriesGranularity.normalize(row.getCreatedDate(), finalGranularity);

            reviewsCountByTypeAndDate
                .putIfAbsent(normalizedDate, new EnumMap<>(ReviewType.class));
            
            Map<ReviewType, Long> reviewCountByTypeForDate = reviewsCountByTypeAndDate.get(normalizedDate);

            // Increment the count for the specific review type and date
            reviewCountByTypeForDate.put(
                row.getType(),
                reviewCountByTypeForDate.getOrDefault(row.getType(), 0L) + row.getCount()
            );
        }

        /*
         * Building the complete timeline with all dates to ensure continuity in the data, 
         * inclusive of both start and end dates
        */

        List<LocalDate> timelineDates = new ArrayList<>();

        LocalDate currentDate =
            TimeSeriesGranularity.normalize(from.toLocalDate(), finalGranularity);

        LocalDate endDate =
            TimeSeriesGranularity.normalize(to, finalGranularity);

        while (!currentDate.isAfter(endDate)) {
            timelineDates.add(currentDate);
            currentDate = TimeSeriesGranularity.increment(currentDate, finalGranularity);
        }

        /*
         * Building the final DTO timeline with counts for each date and review type, 
         * filling missing dates with zeros to ensure continuity in the data for the 
         * frontend timeline visualization.
         */

        List<ReviewTimelineItemDTO> result = new ArrayList<>();

        for (LocalDate date : timelineDates) {

            Map<ReviewType, Long> reviewCountByTypeForDate =
                reviewsCountByTypeAndDate.getOrDefault(date, Map.of());

            ReviewTimelineItemDTO dto =
                new ReviewTimelineItemDTO(date, 0, 0, 0);

            dto.addPositive(reviewCountByTypeForDate.getOrDefault(ReviewType.POSITIVE, 0L));
            dto.addNegative(reviewCountByTypeForDate.getOrDefault(ReviewType.NEGATIVE, 0L));
            dto.addNeutral(reviewCountByTypeForDate.getOrDefault(ReviewType.NEUTRAL, 0L));

            result.add(dto);
        }

        return new ReviewTimelineResponseDTO(finalGranularity, result);
    }

    public void deleteReview(int id) {
        // Vérification que l'avis existe avant de tenter de le supprimer
        if (!this.reviewRepository.existsById(id)) {
            throw new EntityNotFoundException("No review found with the ID : " + id + ".");
        }
        this.reviewRepository.deleteById(id);
    }

}