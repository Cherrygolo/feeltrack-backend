package ld.feeltrack_backend.unit.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.analytics.Granularity;
import ld.feeltrack_backend.analytics.GranularityResolver;
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
import ld.feeltrack_backend.service.CustomerService;
import ld.feeltrack_backend.service.ReviewService;
import ld.feeltrack_backend.testutils.CustomerTestBuilder;
import ld.feeltrack_backend.testutils.ReviewTestBuilder;
import ld.feeltrack_backend.testutils.TestDataFactory;
import ld.feeltrack_backend.unit.service.ReviewServiceTest.ReviewTimelineProjectionImpl;

/**
 * Classe de test unitaire pour ReviewService.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private SentimentService sentimentService;


    //region ------------ CREATE REVIEW ------------

    @Test
    void createReview_shouldCreateReviewSuccessfully_whenDataIsValid() {

        Customer customerFromRequest = TestDataFactory.createDefaultCustomer();

        Review reviewToCreate = ReviewTestBuilder.aReview()
                .withCustomer(customerFromRequest)
                .withText("Très bonne expérience !\nJe recommande vivement.\n-Bob")
                .build();

        Customer savedCustomer = CustomerTestBuilder.aCustomer()
                .withId(1)
                .withEmail(customerFromRequest.getEmail())
                .build();

        when(customerService.findOrCreateCustomer(customerFromRequest))
                .thenReturn(savedCustomer);

        when(sentimentService.analyze(reviewToCreate.getText()))
                .thenReturn(ReviewType.POSITIVE);

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> {
                    Review savedReview = invocation.getArgument(0);
                    savedReview.setId(99);
                    return savedReview;
                });

        Review createdReview = reviewService.createReview(reviewToCreate);

        assertNotNull(createdReview);
        assertEquals(99, createdReview.getId());
        assertEquals(savedCustomer, createdReview.getCustomer());
        assertEquals(ReviewType.POSITIVE, createdReview.getType());

        verify(customerService).findOrCreateCustomer(customerFromRequest);
        verify(sentimentService).analyze(reviewToCreate.getText());
        verify(reviewRepository).save(any(Review.class));
    }

    //endregion

    //region ------------ FIND REVIEWS ------------

    @Test
    void findReviews_shouldReturnAllReviews_whenTypeIsNull() {
        Customer customer = TestDataFactory.createDefaultCustomer();

        Review positiveReview = ReviewTestBuilder.aReview()
            .withId(1)
            .withCustomer(customer)
            .withText("Excellent service")
            .withType(ReviewType.POSITIVE)
            .build();

        Review negativeReview = ReviewTestBuilder.aReview()
            .withId(2)
            .withCustomer(customer)
            .withText("Très déçu")
            .withType(ReviewType.NEGATIVE)
            .build();

        List<Review> existingReviews = List.of(positiveReview, negativeReview);

        when(reviewRepository.findAllByOrderByCreatedAtDesc()).thenReturn(existingReviews);

        List<Review> foundReviews = reviewService.findReviews(null);

        assertEquals(2, foundReviews.size());
        verify(reviewRepository).findAllByOrderByCreatedAtDesc();
        verify(reviewRepository, never()).findByTypeOrderByCreatedAtDesc(any());
    }

    @Test
    void findReviews_shouldReturnFilteredReviews_whenTypeIsProvided() {
        Customer customer = TestDataFactory.createDefaultCustomer();

        Review negativeReview1 = ReviewTestBuilder.aReview()
            .withId(1)
            .withCustomer(customer)
            .withText("Très déçu")
            .withType(ReviewType.NEGATIVE)
            .build();

        Review negativeReview2 = ReviewTestBuilder.aReview()
            .withId(2)
            .withCustomer(customer)
            .withText("Service médiocre")
            .withType(ReviewType.NEGATIVE)
            .build();

        Review positiveReview = ReviewTestBuilder.aReview()
            .withId(3)
            .withCustomer(customer)
            .withText("Excellent")
            .withType(ReviewType.POSITIVE)
            .build();

        when(reviewRepository.findByTypeOrderByCreatedAtDesc(ReviewType.NEGATIVE))
            .thenReturn(List.of(negativeReview1, negativeReview2));

        List<Review> foundReviews = reviewService.findReviews(ReviewType.NEGATIVE);

        assertEquals(2, foundReviews.size());
        assertTrue(foundReviews.stream().allMatch(
            review -> review.getType() == ReviewType.NEGATIVE
        ));

        verify(reviewRepository).findByTypeOrderByCreatedAtDesc(ReviewType.NEGATIVE);
        verify(reviewRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    //endregion

    //region ------------ GET REVIEW STATS ------------
    @Test
    void getReviewStats_shouldReturnCorrectStats() {
        List<ReviewCountProjection> mockData = List.of(
            new ReviewCountProjectionImpl(ReviewType.POSITIVE, 5),
            new ReviewCountProjectionImpl(ReviewType.NEGATIVE, 2)
        );

        when(reviewRepository.countReviewsByType()).thenReturn(mockData);

        ReviewStatsDTO result = reviewService.getReviewStats();

        assertEquals(5, result.getPositive());
        assertEquals(2, result.getNegative());
        assertEquals(0, result.getNeutral());
    }

    @Test
    void getReviewStats_shouldReturnZeroWhenNoReviews() {

        when(reviewRepository.countReviewsByType()).thenReturn(Collections.emptyList());

        ReviewStatsDTO result = reviewService.getReviewStats();

        assertEquals(0, result.getPositive());
        assertEquals(0, result.getNegative());
        assertEquals(0, result.getNeutral());
    }

    //endregion

    //region ------------ GET REVIEW TIMELINE ------------

    @Test
    void getReviewTimeline_shouldReturnZeroFilledTimeline_whenNoReviews() {
        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        ReviewTimelineResponseDTO timeline = reviewService.getReviewTimeline(30, Granularity.DAY);

        assertNotNull(timeline);

         List<ReviewTimelineItemDTO> data = timeline.data();
        assertEquals(31, data.size());
        assertTrue(data.stream().allMatch(
            item -> item.getPositive() == 0L && 
            item.getNegative() == 0L && 
            item.getNeutral() == 0L
        ));
        verify(reviewRepository).countReviewsByDateAndType(any(LocalDateTime.class));
    }

    @Test
    void getReviewTimeline_shouldReturnReviews_whenReviewsExist() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        List<ReviewTimelineProjection> mockData = List.of(
            new ReviewTimelineProjectionImpl(today, ReviewType.POSITIVE, 3L),
            new ReviewTimelineProjectionImpl(yesterday, ReviewType.NEGATIVE, 1L),
            new ReviewTimelineProjectionImpl(yesterday, ReviewType.NEGATIVE, 2L)
        );

        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class))).thenReturn(mockData);

        ReviewTimelineResponseDTO timeline = reviewService.getReviewTimeline(30, Granularity.DAY);

        assertNotNull(timeline);
        assertNotNull(timeline.granularity());
        assertFalse(timeline.data().isEmpty());
        verify(reviewRepository).countReviewsByDateAndType(any(LocalDateTime.class));
    }

    @Test
    void getReviewTimeline_shouldUseProvidedGranularity() {

        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        ReviewTimelineResponseDTO result =
            reviewService.getReviewTimeline(10, Granularity.WEEK);

        assertEquals(Granularity.WEEK, result.granularity());
    }

    @Test
    void getReviewTimeline_shouldUseGranularityResolver_whenGranularityIsNull() {
        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

        try (MockedStatic<GranularityResolver> mocked =
                Mockito.mockStatic(GranularityResolver.class)) {

            mocked.when(() -> GranularityResolver.resolve(40))
                .thenReturn(Granularity.WEEK);

            ReviewTimelineResponseDTO result =
                reviewService.getReviewTimeline(40, null);

            assertEquals(Granularity.WEEK, result.granularity());

            mocked.verify(() -> GranularityResolver.resolve(40));
        }
    }
    
    @Test
    void getReviewTimeline_shouldAggregateReviewsCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastWeek = today.minusWeeks(1);

        List<ReviewTimelineProjection> mockData = List.of(
            new ReviewTimelineProjectionImpl(today, ReviewType.POSITIVE, 2L),
            new ReviewTimelineProjectionImpl(yesterday, ReviewType.POSITIVE, 3L),
            new ReviewTimelineProjectionImpl(yesterday, ReviewType.NEGATIVE, 1L),
            new ReviewTimelineProjectionImpl(lastWeek, ReviewType.NEGATIVE, 4L)
        );

        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class)))
            .thenReturn(mockData);

        ReviewTimelineResponseDTO result =
            reviewService.getReviewTimeline(14, Granularity.WEEK);

        List<ReviewTimelineItemDTO> data = result.data();

        // Data should be aggregated into weeks, so we expect at most 3 entries (current week, last week, and possibly the week before)
        assertTrue(data.size() <= 3);

        // Verify the counts for the current week
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY); // Current week starting date
        ReviewTimelineItemDTO currentWeekData = data.stream()
            .filter(item -> item.getStartingPeriodDate().isEqual(currentWeekStart))
            .findFirst()
            .orElse(null);
        assertEquals(5L, currentWeekData.getPositive());
        assertEquals(1L, currentWeekData.getNegative());

        // Verify the counts for the last week
        LocalDate lastWeekStart = lastWeek.with(DayOfWeek.MONDAY); // Last week starting date
        ReviewTimelineItemDTO lastWeekData = data.stream()
            .filter(item -> item.getStartingPeriodDate().isEqual(lastWeekStart))
            .findFirst()
            .orElse(null);
        assertEquals(0L, lastWeekData.getPositive());
        assertEquals(4L, lastWeekData.getNegative());
    }

    @Test
    void getReviewTimeline_shouldEnsureContinuityOfTimeline() {

        when(reviewRepository.countReviewsByDateAndType(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        ReviewTimelineResponseDTO result =
            reviewService.getReviewTimeline(5, Granularity.DAY);

        List<LocalDate> dates = result.data().stream()
            .map(ReviewTimelineItemDTO::getStartingPeriodDate)
            .toList();

        // verify that the dates are in order
        List<LocalDate> sorted = new ArrayList<>(dates);
        sorted.sort(Comparator.naturalOrder());
        assertEquals(sorted, dates);

        // verify that the dates are continuous (no gaps)
        for (int i = 1; i < dates.size(); i++) {
            assertEquals(1, ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
        }
    }

    //endregion
    
    //region ------------ DELETE REVIEW ------------

    @Test
    void deleteReview_shouldThrowException_whenReviewDoesNotExist() {
        int nonExistentReviewId = 123;

        when(reviewRepository.existsById(nonExistentReviewId)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> reviewService.deleteReview(nonExistentReviewId)
        );

        assertEquals(
            "No review found with the ID : " + nonExistentReviewId + ".",
            exception.getMessage()
        );

        verify(reviewRepository).existsById(nonExistentReviewId);
        verify(reviewRepository, never()).deleteById(anyInt());
    }

    @Test
    void deleteReview_shouldDeleteReview_whenReviewExists() {
        int existingReviewId = 1;

        when(reviewRepository.existsById(existingReviewId)).thenReturn(true);

        reviewService.deleteReview(existingReviewId);

        verify(reviewRepository).existsById(existingReviewId);
        verify(reviewRepository).deleteById(existingReviewId);
    }

    //endregion

    static class ReviewCountProjectionImpl implements ReviewCountProjection {
        private final ReviewType type;
        private final long count;

        ReviewCountProjectionImpl(ReviewType type, long count) {
            this.type = type;
            this.count = count;
        }

        public ReviewType getType() { return type; }
        public long getCount() { return count; }
    }

    static class ReviewTimelineProjectionImpl implements ReviewTimelineProjection {
        private final LocalDate createdDate;
        private final ReviewType type;
        private final Long count;

        ReviewTimelineProjectionImpl(LocalDate createdDate, ReviewType type, Long count) {
            this.createdDate = createdDate;
            this.type = type;
            this.count = count;
        }

        public LocalDate getCreatedDate() { return createdDate; }
        public ReviewType getType() { return type; }
        public Long getCount() { return count; }
    }
}