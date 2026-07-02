package ld.feeltrack_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.dto.ReviewTimelineResponseDTO;
import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.service.ReviewService;



@RestController
@RequestMapping( path = "review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Review> createReview (@Valid @RequestBody Review review) {
        Review createdReview = this.reviewService.createReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }
    
    @GetMapping
    public List<Review> findReviews(@RequestParam(required = false) ReviewType type) {
        return this.reviewService.findReviews(type);
    }

    @GetMapping(path = "stats")
    public ResponseEntity<ReviewStatsDTO> getReviewStats() {
        ReviewStatsDTO stats = reviewService.getReviewStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping(path ="/stats/timeline")
    public ResponseEntity<ReviewTimelineResponseDTO> getTimeline(
            @RequestParam(defaultValue = "30") int days) {
        ReviewTimelineResponseDTO timeline = reviewService.getReviewTimeline(days, null);
        return ResponseEntity.ok(timeline);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "{id}")
    public void deleteReview(@PathVariable int id) {
        this.reviewService.deleteReview(id);
    }
    

}
