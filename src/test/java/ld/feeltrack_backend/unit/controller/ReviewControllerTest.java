package ld.feeltrack_backend.unit.controller;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import ld.feeltrack_backend.controller.ReviewController;
import ld.feeltrack_backend.dto.ReviewStatsDTO;
import ld.feeltrack_backend.dto.ReviewTimelineItemDTO;
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

	@Test
	void getReviewTimeline_shouldReturnEmptyList_whenNoReviews() throws Exception {
		when(reviewService.getReviewTimeline(any(Integer.class))).thenReturn(List.of());

		mockMvc.perform(get("/review/stats/timeline")
						.param("days", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getReviewTimeline_shouldReturnTimelineData_whenReviewsExist() throws Exception {
		List<TimelineDayData> timelineData = List.of(
			new TimelineDayData(5, 2, 1),
			new TimelineDayData(3, 1, 0)
		);
		ReviewTimelineItemDTO day1 = new ReviewTimelineItemDTO(LocalDate.now(), timelineData.get(0).positive(), timelineData.get(0).negative(), timelineData.get(0).neutral());
		ReviewTimelineItemDTO day2 = new ReviewTimelineItemDTO(LocalDate.now().minusDays(1), timelineData.get(1).positive(), timelineData.get(1).negative(), timelineData.get(1).neutral());

		when(reviewService.getReviewTimeline(any(Integer.class)))
				.thenReturn(List.of(day1, day2));
				

		ResultActions result = mockMvc.perform(get("/review/stats/timeline")
						.param("days", "30"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$").isArray())
						.andExpect(jsonPath("$.length()").value(2));

		System.out.println("=== RESPONSE ===");
		System.out.println(result.andReturn().getResponse().getContentAsString());

		for ( Integer i = 0; i < 2; i++) {
			String path = "$[" + i + "]";
			LocalDate expectedDate = LocalDate.now().minusDays(i);
			result.andExpect(jsonPath(path + ".createdDate").exists())		
			// java.lang.AssertionError: Got a list of values [2026,5,21] instead of the expected single value 2026-05-21	
				.andExpect(jsonPath(path + ".createdDate").value(expectedDate.toString()))
				.andExpect(jsonPath(path + ".positive").value(timelineData.get(i).positive()))
				.andExpect(jsonPath(path + ".negative").value(timelineData.get(i).negative()))
				.andExpect(jsonPath(path + ".neutral").value(timelineData.get(i).neutral()));

		}
	}
	
	//endregion

}
