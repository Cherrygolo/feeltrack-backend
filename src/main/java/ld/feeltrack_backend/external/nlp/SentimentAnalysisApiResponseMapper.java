package ld.feeltrack_backend.external.nlp;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ld.feeltrack_backend.enums.ReviewType;
import ld.feeltrack_backend.wrapper.Rating;

/* 
 * Mapper for converting the API response to a ReviewType.
 */

@Component
public class SentimentAnalysisApiResponseMapper {

private final ObjectMapper objectMapper = new ObjectMapper();

    public ReviewType toReviewType(String rawResponse) {

        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            if (!root.isArray() || root.isEmpty()) {
                throw new IllegalStateException("Empty or invalid Sentiment Analysis API response");
            }

            JsonNode inner = root.get(0);

            Rating[] ratingsArray =
                    objectMapper.treeToValue(inner, Rating[].class);

            List<Rating> ratings = Arrays.asList(ratingsArray);

            Rating best = ratings.stream()
                    .max((a, b) -> Double.compare(a.getScore(), b.getScore()))
                    .orElse(null);

            if (best == null || best.getLabel() == null) {
                return ReviewType.NEUTRAL;
            }

            int stars = Integer.parseInt(best.getLabel().split(" ")[0]);

            if (stars <= 2) return ReviewType.NEGATIVE;
            if (stars == 3) return ReviewType.NEUTRAL;
            return ReviewType.POSITIVE;

        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse Sentiment Analysis API response", ex);
        }
    }
}
