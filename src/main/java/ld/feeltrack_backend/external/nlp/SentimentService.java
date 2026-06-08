package ld.feeltrack_backend.external.nlp;

import java.util.Arrays;

import org.springframework.stereotype.Service;

import ld.feeltrack_backend.enums.ReviewType;

@Service
public class SentimentService {

    private final SentimentAnalysisApiClient apiClient;
    private final SentimentAnalysisApiResponseMapper sentimentAnalysisApiResponseMapper;

    public SentimentService(SentimentAnalysisApiClient apiClient, SentimentAnalysisApiResponseMapper sentimentAnalysisApiResponseMapper) {
        this.apiClient = apiClient;
        this.sentimentAnalysisApiResponseMapper = sentimentAnalysisApiResponseMapper;
    }

    public ReviewType analyze(String textToAnalyse) {

        if (!apiClient.isAvailable()) {
            return analyseTextFeelingTypeBasicly(textToAnalyse);
        }

        String response = apiClient.analyze(textToAnalyse);
        return sentimentAnalysisApiResponseMapper.toReviewType(response);
    }

    /**
     * Baseline simple sentiment analysis using keyword matching. This is a fallback method when the API is not available.
     * @param textToAnalyse : the text to analyze
     * @return ReviewType.POSITIVE if the text contains more positive markers, ReviewType.NEGATIVE if it contains more negative markers, 
     * otherwise ReviewType.NEUTRAL
     */
    private static ReviewType analyseTextFeelingTypeBasicly(String textToAnalyse) {
        // Simple sentiment markers
        String[] negations = {"ne", "n'", "pas", "jamais", "aucun", "sans"};
        String[] simplePositiveWords = {"bon", "bien", "ok", "positif", "super", "excellent"};
        String[] simpleNegativeWords = {"mal", "non", "nul", "négatif", "horrible", "terrible"};

        int score = 0;
        boolean negateNext = false;

        String[] words = textToAnalyse.toLowerCase().split("\\W+");
        for (String word : words) {
            if (Arrays.asList(negations).contains(word)) {
                negateNext = true;
                continue;
            }
            if (Arrays.asList(simplePositiveWords).contains(word)) {
                score += negateNext ? -1 : 1;
            } else if (Arrays.asList(simpleNegativeWords).contains(word)) {
                score += negateNext ? 1 : -1;
            }
            negateNext = false; // une négation ne s'applique qu'au mot suivant
        }

        return score >= 0 ? ReviewType.POSITIVE : ReviewType.NEGATIVE;
    }

}