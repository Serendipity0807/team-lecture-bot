package com.lecturebot.genai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
public class GenAiClient {

    private static final Logger log = LoggerFactory.getLogger(GenAiClient.class);

    private final RestTemplate restTemplate;
    private final String genaiServiceUrl;
    private final ObjectMapper objectMapper;

    public GenAiClient(RestTemplate restTemplate,
                       @Value("${genai.service.url}") String genaiServiceUrl,
                       ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.genaiServiceUrl = genaiServiceUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * Query documents from the GenAI vector store for RAG-based Q&A.
     * Returns the raw response body as a Map.
     */
    public Map<String, Object> queryDocuments(UUID courseSpaceId, String query, int topK) {
        String url = genaiServiceUrl + "/api/v1/query";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "course_space_id", courseSpaceId.toString(),
                    "query", query,
                    "top_k", topK
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            log.info("Query documents in course space {}: status={}", courseSpaceId, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = response.getBody();
            return result;
        } catch (RestClientException e) {
            log.error("Failed to query documents in course space {}: {}", courseSpaceId, e.getMessage(), e);
            throw new RuntimeException("Failed to query documents: " + e.getMessage(), e);
        }
    }

    /**
     * Generate flashcards via the GenAI service.
     * Returns the raw response body as a Map.
     */
    public Map<String, Object> generateFlashcards(UUID courseSpaceId, Object requestBody) {
        String url = genaiServiceUrl + "/api/v1/flashcards/generate";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            log.info("Generate flashcards in course space {}: status={}", courseSpaceId, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> result = response.getBody();
            return result;
        } catch (RestClientException e) {
            log.error("Failed to generate flashcards in course space {}: {}", courseSpaceId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate flashcards: " + e.getMessage(), e);
        }
    }
}
