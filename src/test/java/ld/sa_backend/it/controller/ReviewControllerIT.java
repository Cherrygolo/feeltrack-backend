/**
 * ReviewControllerIT - Tests d'intégration pour le ReviewController.
 *
 * Vérifie le comportement des endpoints REST liés aux avis (reviews),
 * incluant l'ajout, la mise à jour, la récupération et la suppression.
 * Les tests s'exécutent dans un environnement complet pour s'assurer que les
 * interactions avec la base de données et les entités liées fonctionnent correctement.
 */

package ld.sa_backend.it.controller;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ld.sa_backend.entity.Customer;
import ld.sa_backend.entity.Review;
import ld.sa_backend.enums.ReviewType;
import ld.sa_backend.repository.CustomerRepository;
import ld.sa_backend.repository.ReviewRepository;
import ld.sa_backend.testutils.CustomerTestBuilder;
import ld.sa_backend.testutils.ReviewTestBuilder;
import ld.sa_backend.testutils.TestDataFactory;

/**
 * Integration tests for {@link ld.sa_backend.controller.ReviewController}.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ReviewControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer persistedCustomer;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerRepository.deleteAll();

        // Utilisation du builder pour respecter le constructeur protégé
        persistedCustomer = customerRepository.save(
            CustomerTestBuilder.aCustomer()
                .withEmail("customer@test.com")
                .withPhone("0600000000")
                .build()
        );
    }

    //region ------------ CREATE REVIEW ------------

    @Test
    void createReview_shouldCreateReviewAndAssignTypeAutomatically() throws Exception {
        Review reviewToCreate = ReviewTestBuilder.aReview()
            .withCustomer(persistedCustomer)
            .withText("Très bonne expérience !")
            .build(); // pas d'ID, la DB le gère

        mockMvc.perform(post("/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewToCreate)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.text").value("Très bonne expérience !"))
            .andExpect(jsonPath("$.customer.email").value("customer@test.com"))
            .andExpect(jsonPath("$.type").value("POSITIVE")); // vérifie l'analyse automatique
    }

    //endregion

    //region ------------ FIND REVIEWS ------------

    @Test
    void findReviews_shouldReturnAllReviews_whenNoTypeIsProvided() throws Exception {
        reviewRepository.save(
            ReviewTestBuilder.aReview().withCustomer(persistedCustomer).withText("Avis positif").build()
        );
        reviewRepository.save(
            ReviewTestBuilder.aReview().withCustomer(persistedCustomer).withText("Avis négatif").build()
        );

        mockMvc.perform(get("/review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findReviews_shouldReturnOnlyNegativeReviews_whenTypeIsProvided() throws Exception {
        Review positiveReview = ReviewTestBuilder.aReview()
            .withCustomer(persistedCustomer)
            .withText("Très bon service")
            .withType(ReviewType.POSITIVE)
            .build();

        Review negativeReview = ReviewTestBuilder.aReview()
            .withCustomer(persistedCustomer)
            .withText("Je ne suis pas satisfait")
            .withType(ReviewType.NEGATIVE)
            .build();

        reviewRepository.save(positiveReview);
        reviewRepository.save(negativeReview);

        mockMvc.perform(get("/review")
                .param("type", "NEGATIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("NEGATIVE"))
            .andExpect(jsonPath("$[0].text").value("Je ne suis pas satisfait"));
    }

    @Test
    void findReviews_shouldReturnEnumValueInvalid_whenTypeIsInvalid() throws Exception {
        mockMvc.perform(get("/review").param("type", "INVALID"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENUM_VALUE_INVALID"));
    }

    //endregion

    //region ------------ GET REVIEW STATS ------------
    @Test
    void getReviewStats_shouldReturnReviewStats() throws Exception {

        List<Review> reviews = TestDataFactory.createReviewListWithDifferentTypes(5, 3, 2);
        reviewRepository.saveAll(reviews);

        mockMvc.perform(get("/review/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.positive").value(5))
            .andExpect(jsonPath("$.negative").value(3))
            .andExpect(jsonPath("$.neutral").value(2));
    }

    @Test
    void getReviewStats_shouldReturnZeroWhenNoReviews() throws Exception {

        mockMvc.perform(get("/review/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.positive").value(0))
            .andExpect(jsonPath("$.negative").value(0))
            .andExpect(jsonPath("$.neutral").value(0));
    }

    //region ------------ DELETE REVIEW ------------

    @Test
    void deleteReview_shouldDeleteReviewAndReturnNoContentStatus() throws Exception {
        Review persistedReview = reviewRepository.save(
            ReviewTestBuilder.aReview()
                .withCustomer(persistedCustomer)
                .withText("Avis à supprimer")
                .build()
        );

        mockMvc.perform(delete("/review/{id}", persistedReview.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/review"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteReview_shouldReturnNotFound_whenReviewDoesNotExist() throws Exception {
        mockMvc.perform(delete("/review/{id}", 999))
            .andExpect(status().isNotFound());
    }

    //endregion
}