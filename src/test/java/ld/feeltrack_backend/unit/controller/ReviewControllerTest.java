package ld.feeltrack_backend.unit.controller;

import java.time.LocalDate;
import java.util.List;

import org.aspectj.weaver.ast.And;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.analytics.Granularity;
import ld.feeltrack_backend.controller.ReviewController;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.dto.ReviewTimelineItemDTO;
import ld.feeltrack_backend.dto.ReviewTimelineResponseDTO;
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



    //region ---------- GET REVIEW STATS ----------
    
    @Test
	void getReviewStats_shouldReturnStatsWhenReviewsExist() throws Exception {

		ReviewStatsDTO stats =
			new ReviewStatsDTO(5, 2, 1);

		when(reviewService.getReviewStats())
			.thenReturn(stats);

		mockMvc.perform(get("/review/stats"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.positive").value(5))
			.andExpect(jsonPath("$.negative").value(2))
			.andExpect(jsonPath("$.neutral").value(1));

		verify(reviewService).getReviewStats();
	}
	
	//endregion

	//region ---------- GET REVIEW TIMELINE ----------

        void getTimeline_shouldReturnTimeline_withDefaultDays() throws Exception {

                ReviewTimelineResponseDTO mockResponse =
                        new ReviewTimelineResponseDTO(Granularity.DAY, List.of());

                when(reviewService.getReviewTimeline(30, null))
                        .thenReturn(mockResponse);

                mockMvc.perform(get("/review/stats/timeline"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.granularity").value("DAY"))
                        .andExpect(jsonPath("$.data").isArray());

                verify(reviewService).getReviewTimeline(30, null);
        }

	@Test
    void getTimeline_shouldPassDaysParameter_toService() throws Exception {

        ReviewTimelineResponseDTO mockResponse =
                new ReviewTimelineResponseDTO(Granularity.WEEK, List.of());

        when(reviewService.getReviewTimeline(7, null))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/review/stats/timeline")
                .param("days", "7"))
                .andExpect(status().isOk());

        verify(reviewService).getReviewTimeline(7, null);
    }

	@Test
    void getTimeline_shouldReturnCorrectJsonStructure() throws Exception {

        List<ReviewTimelineItemDTO> items = List.of(
            new ReviewTimelineItemDTO(LocalDate.of(2026, 1, 1), 1, 2, 3),
            new ReviewTimelineItemDTO(LocalDate.of(2026, 1, 2), 0, 0, 1)
        );

        ReviewTimelineResponseDTO mockResponse =
            new ReviewTimelineResponseDTO(Granularity.DAY, items);

        when(reviewService.getReviewTimeline(anyInt(), any()))
            .thenReturn(mockResponse);

        mockMvc.perform(get("/review/stats/timeline")
                .param("days", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.granularity").value("DAY"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].positive").value(1))
            .andExpect(jsonPath("$.data[0].negative").value(2))
            .andExpect(jsonPath("$.data[0].neutral").value(3))
            .andExpect(jsonPath("$.data[1].positive").value(0))
            .andExpect(jsonPath("$.data[1].negative").value(0))
            .andExpect(jsonPath("$.data[1].neutral").value(1));
    }
	//endregion

}
