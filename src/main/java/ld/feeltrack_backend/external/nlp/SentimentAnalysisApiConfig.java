package ld.feeltrack_backend.external.nlp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentimentAnalysisApiConfig {

    @Value("${huggingface.token}")
    private String token;

    @Bean
    public SentimentAnalysisApiClient sentimentAnalysisApiClient() {
        return new SentimentAnalysisApiClient(token);
    }
}
