package ld.feeltrack_backend.unit.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.controller.ReviewController;
import ld.feeltrack_backend.entity.Review;
import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.service.ReviewService;
import ld.feeltrack_backend.testutils.ReviewTestBuilder;
import ld.feeltrack_backend.testutils.TestDataFactory;
import ld.feeltrack_backend.testutils.TimelineDayData;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    private Review testReview;

    @BeforeEach
    void setup() {

        testReview = TestDataFactory.createDefaultReview();
    }

    //region ---------- CREATE REVIEW ----------

    @Test
    void createReview_shouldReturn201_whenReviewIsValid() throws Exception {
        when(reviewService.createReview(any(Review.class))).thenReturn(testReview);

        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReview)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value(testReview.getText()))
                .andExpect(jsonPath("$.id").value(testReview.getId()));
    }

    //endregion

    //region ---------- FIND REVIEWS ----------

    @Test
    void findReviews_shouldReturnAllReviews_whenTypeIsNull() throws Exception {
        Review positiveReview = ReviewTestBuilder.aReview()
                .withText("Super !")
                .withType(ReviewType.POSITIVE)
                .build();

        Review negativeReview = ReviewTestBuilder.aReview()
                .withText("Pas top")
                .withType(ReviewType.NEGATIVE)
                .build();

        List<Review> allReviews = List.of(positiveReview, negativeReview);

        when(reviewService.findReviews(null)).thenReturn(allReviews);

        mockMvc.perform(get("/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Super !"))
                .andExpect(jsonPath("$[0].type").value("POSITIVE"))
                .andExpect(jsonPath("$[1].text").value("Pas top"))
                .andExpect(jsonPath("$[1].type").value("NEGATIVE"));
    }

    @Test
    void findReviews_shouldReturnFilteredReviews_whenTypeIsProvided() throws Exception {
        Review positiveReview = ReviewTestBuilder.aReview()
                .withText("Super !")
                .withType(ReviewType.POSITIVE)
                .build();

        Review negativeReview1 = ReviewTestBuilder.aReview()
                .withText("Pas top")
                .withType(ReviewType.NEGATIVE)
                .build();

        Review negativeReview2 = ReviewTestBuilder.aReview()
                .withText("Décevant")
                .withType(ReviewType.NEGATIVE)
                .build();

        // Service mocké : retourne uniquement les avis négatifs
        when(reviewService.findReviews(ReviewType.NEGATIVE))
                .thenReturn(List.of(negativeReview1, negativeReview2));

        mockMvc.perform(get("/review")
                        .param("type", "NEGATIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NEGATIVE"))
                .andExpect(jsonPath("$[0].text").value("Pas top"))
                .andExpect(jsonPath("$[1].type").value("NEGATIVE"))
                .andExpect(jsonPath("$[1].text").value("Décevant"));
    }

    //endregion

    //region ---------- DELETE REVIEW ----------

    @Test
    void deleteReview_shouldReturn204_whenReviewExists() throws Exception {
        mockMvc.perform(delete("/review/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1);
    }

    @Test
    void deleteReview_shouldReturn404_whenReviewDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Review not found"))
                .when(reviewService).deleteReview(99);

        mockMvc.perform(delete("/review/99"))
                .andExpect(status().isNotFound());
    }

    //endregion
}
