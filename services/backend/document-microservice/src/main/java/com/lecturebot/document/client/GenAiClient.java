package com.lecturebot.document.client;

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

    public GenAiClient(RestTemplate restTemplate,
                       @Value("${genai.service.url}") String genaiServiceUrl) {
        this.restTemplate = restTemplate;
        this.genaiServiceUrl = genaiServiceUrl;
    }

    /**
     * Index a document into the GenAI vector store.
     */
    public void indexDocument(UUID courseSpaceId, UUID documentId, String text) {
        String url = genaiServiceUrl + "/api/v1/index";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "course_space_id", courseSpaceId.toString(),
                    "document_id", documentId.toString(),
                    "text", text
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("Indexing document {} in course space {}: status={}",
                    documentId, courseSpaceId, response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Failed to index document {} in course space {}: {}",
                    documentId, courseSpaceId, e.getMessage(), e);
            throw new RuntimeException("Failed to index document: " + e.getMessage(), e);
        }
    }

    /**
     * Delete the vector index for a document.
     */
    public void deleteIndex(UUID courseSpaceId, UUID documentId) {
        String url = genaiServiceUrl + "/api/v1/index/" + courseSpaceId + "/" + documentId;
        try {
            restTemplate.delete(url);
            log.info("Deleted index for document {} in course space {}", documentId, courseSpaceId);
        } catch (RestClientException e) {
            log.error("Failed to delete index for document {} in course space {}: {}",
                    documentId, courseSpaceId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete document index: " + e.getMessage(), e);
        }
    }
}
