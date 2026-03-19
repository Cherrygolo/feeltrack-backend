/**
 * Factory de données de test.
 *
 * Fournit des objets de test standards lorsque la personnalisation n'est pas nécessaire.
 */


package ld.sa_backend.testutils;

import java.util.ArrayList;
import java.util.List;

import ld.sa_backend.entity.Customer;
import ld.sa_backend.entity.Review;
import ld.sa_backend.enums.ReviewType;

public class TestDataFactory {

    //region ------------ TEST DATA GENERATION METHODS FOR CUSTOMER ------------
    
    
    public static Customer createDefaultCustomer() {
        return CustomerTestBuilder.aCustomer().build();
    }
    
    public static Customer createCustomerWithEmail(String email) {
        return CustomerTestBuilder.aCustomer()
            .withEmail(email)
            .build();
    }
    
    public static Customer createCompleteCustomerWithId(int id) {
        return CustomerTestBuilder.aCustomer()
            .withId(id)
            .withEmail("customer" + id + "@test.com")
            .withPhone("060000000" + id)
            .build();
    }
    
    public static List<Customer> createCustomerList(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customers.add(createCompleteCustomerWithId(i + 1));
        }
        return customers;
    }
    
    // Pour les cas spécifiques de tests
    public static Customer createCustomerForDuplicateEmailTest() {
        return CustomerTestBuilder.aCustomer()
            .withEmail("duplicate@test.com")
            .withId(999)
            .build();
    }

    //#endregion

    //region ------------ TEST DATA GENERATION METHODS FOR REVIEW ------------

    public static Review createDefaultReview() {
        return ReviewTestBuilder.aReview().build();
    }

    public static Review createReviewWithType(ReviewType type) {
        return ReviewTestBuilder.aReview()
            .withType(type)
            .withText("Review with type " + type.name())
            .build();
    }

    public static List<Review> createReviewListWithDifferentTypes(int positiveNumber, int negativeNumber, int neutralNumber) {
        List<Review> reviews = new ArrayList<>();
        for (int i = 0 ; i < positiveNumber ; i++) {
            reviews.add(createReviewWithType(ReviewType.POSITIVE));
        }
        for (int i = 0 ; i < negativeNumber ; i++) {
            reviews.add(createReviewWithType(ReviewType.NEGATIVE));
        }
        for (int i = 0 ; i < neutralNumber ; i++) {
            reviews.add(createReviewWithType(ReviewType.NEUTRAL));
        }
        // Assigner un customer par défaut à chaque review pour éviter les nulls
        reviews.forEach(r -> r.setCustomer(createDefaultCustomer()));
        
        return reviews;
    }

    //#endregion
}