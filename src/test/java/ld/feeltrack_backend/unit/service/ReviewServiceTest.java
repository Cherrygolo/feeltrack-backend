package ld.feeltrack_backend.unit.service;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.entity.Customer;
import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.external.nlp.FeelingAnalyser;
import ld.feeltrack_backend.projection.ReviewCountProjection;
import ld.feeltrack_backend.repository.ReviewRepository;
import ld.feeltrack_backend.service.CustomerService;
import ld.feeltrack_backend.service.ReviewService;
import ld.feeltrack_backend.testutils.CustomerTestBuilder;
import ld.feeltrack_backend.testutils.ReviewTestBuilder;
import ld.feeltrack_backend.testutils.TestDataFactory;

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


    //region ------------ CREATE REVIEW ------------

    @Test
    void createReview_shouldThrowException_whenCustomerIsNull() {
        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withText("Super expérience")
            .withCustomer(null)
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewService.createReview(reviewToCreate)
        );

        assertEquals("Customer info must be provided.", exception.getMessage());
        verifyNoInteractions(customerService, reviewRepository);
    }

    @Test
    void createReview_shouldThrowException_whenCustomerIdAndEmailAreMissing() {
        Customer incompleteCustomer = CustomerTestBuilder.aCustomer()
            .withId(null)
            .withEmail(null)
            .build();

        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withText("Super expérience")
            .withCustomer(incompleteCustomer)
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewService.createReview(reviewToCreate)
        );

        assertEquals("Email is required to create a new review", exception.getMessage());
        verifyNoInteractions(customerService, reviewRepository);
    }

    @Test
    void createReview_shouldThrowException_whenCustomerIdDoesNotExist() {
        Customer nonExistentCustomer = CustomerTestBuilder.aCustomer()
            .withId(99)
            .withEmail("test@example.com")
            .build();

        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withText("Super expérience")
            .withCustomer(nonExistentCustomer)
            .build();

        when(customerService.getCustomerById(99))
            .thenThrow(new RuntimeException("Customer not found"));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> reviewService.createReview(reviewToCreate)
        );

        assertEquals("Customer not found", exception.getMessage());
        verify(customerService).getCustomerById(99);
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void createReview_shouldThrowException_whenTextIsNull() {
        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withText(null)
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewService.createReview(reviewToCreate)
        );

        assertEquals("Review text cannot be null or empty.", exception.getMessage());
        verifyNoInteractions(customerService, reviewRepository);
    }

    @Test
    void createReview_shouldThrowException_whenTextIsBlank() {
        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withText("   ")
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> reviewService.createReview(reviewToCreate)
        );

        assertEquals("Review text cannot be null or empty.", exception.getMessage());
        verifyNoInteractions(customerService, reviewRepository);
    }

    @Test
    void createReview_shouldCreateReviewSuccessfully_whenDataIsValid() {
        Customer customerFromRequest = TestDataFactory.createDefaultCustomer();

        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withCustomer(customerFromRequest)
            .withText("Très bonne expérience !")
            .build();

        Customer savedCustomer = CustomerTestBuilder.aCustomer()
            .withId(1)
            .withEmail(customerFromRequest.getEmail())
            .build();

        when(customerService.findOrCreateCustomer(customerFromRequest))
            .thenReturn(savedCustomer);

        try (var mockedFeelingAnalyser = Mockito.mockStatic(FeelingAnalyser.class)) {
            mockedFeelingAnalyser
                .when(() -> FeelingAnalyser.analyzeFeelingType(reviewToCreate.getText()))
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
            verify(reviewRepository).save(reviewToCreate);
        }
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
}