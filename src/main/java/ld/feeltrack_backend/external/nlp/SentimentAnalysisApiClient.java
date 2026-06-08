package ld.feeltrack_backend.external.nlp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import ld.feeltrack_backend.dto.SentimentAnalysisRequest;
import ld.feeltrack_backend.exception.ExternalApiException;

public class SentimentAnalysisApiClient {

    private static final String MODEL_URL =
            "https://router.huggingface.co/hf-inference/models/nlptown/bert-base-multilingual-uncased-sentiment";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String token;

    public SentimentAnalysisApiClient(String token) {
        this.token = token;
    }

    public String analyze(String textToAnalyse) {

        SentimentAnalysisRequest requestBody = new SentimentAnalysisRequest(textToAnalyse);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MODEL_URL))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Sentiment Analysis API error: " + response.body());
            }

            return response.body();

        } catch (IOException e) {
            throw new ExternalApiException(502, "IO error calling Sentiment Analysis API : " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(503, "Sentiment Analysis API Request interrupted : " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return token != null && !token.isBlank();
    }
}
