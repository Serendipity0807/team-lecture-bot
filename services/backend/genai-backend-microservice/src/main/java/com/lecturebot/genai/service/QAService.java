package com.lecturebot.genai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecturebot.genai.client.GenAiClient;
import com.lecturebot.genai.dto.request.AskRequest;
import com.lecturebot.genai.dto.response.AskResponse;
import com.lecturebot.genai.dto.response.PagedResponse;
import com.lecturebot.genai.dto.response.SourceInfo;
import com.lecturebot.genai.entity.QuestionAnswer;
import com.lecturebot.genai.repository.QuestionAnswerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QAService {

    private static final Logger log = LoggerFactory.getLogger(QAService.class);

    private final QuestionAnswerRepository qaRepository;
    private final GenAiClient genAiClient;
    private final ObjectMapper objectMapper;

    public QAService(QuestionAnswerRepository qaRepository,
                     GenAiClient genAiClient,
                     ObjectMapper objectMapper) {
        this.qaRepository = qaRepository;
        this.genAiClient = genAiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Ask a question: query the GenAI RAG service, persist the Q&A record, and return the answer.
     */
    @Transactional
    public AskResponse ask(UUID userId, UUID courseSpaceId, AskRequest request) {
        log.info("User {} asking in course space {}: '{}'", userId, courseSpaceId, request.getQuestion());

        // 1. Call GenAI service to query documents
        Map<String, Object> genAiResponse = genAiClient.queryDocuments(
                courseSpaceId, request.getQuestion(), request.getTopK());

        // 2. Extract answer and sources from the GenAI response
        String answer = extractAnswer(genAiResponse);
        List<SourceInfo> sources = extractSources(genAiResponse);

        // 3. Serialize sources to JSON for persistence
        String sourcesJson;
        try {
            sourcesJson = objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sources to JSON", e);
            sourcesJson = "[]";
        }

        // 4. Persist the Q&A record
        QuestionAnswer qa = QuestionAnswer.builder()
                .question(request.getQuestion())
                .answer(answer)
                .sources(sourcesJson)
                .courseSpaceId(courseSpaceId)
                .userId(userId)
                .build();
        qa = qaRepository.save(qa);

        // 5. Return response
        return AskResponse.builder()
                .id(qa.getId())
                .question(qa.getQuestion())
                .answer(qa.getAnswer())
                .sources(sources)
                .createdAt(qa.getCreatedAt())
                .build();
    }

    /**
     * Get paginated Q&A history for a user in a course space.
     */
    public PagedResponse<AskResponse> getHistory(UUID userId, UUID courseSpaceId, Pageable pageable) {
        Page<QuestionAnswer> page = qaRepository.findByCourseSpaceIdAndUserId(courseSpaceId, userId, pageable);

        List<AskResponse> content = page.getContent().stream()
                .map(this::toAskResponse)
                .toList();

        return PagedResponse.<AskResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Extract the answer text from the GenAI service response.
     */
    @SuppressWarnings("unchecked")
    private String extractAnswer(Map<String, Object> genAiResponse) {
        if (genAiResponse == null) {
            return "No response from GenAI service.";
        }
        Object answerObj = genAiResponse.get("answer");
        if (answerObj != null) {
            return answerObj.toString();
        }
        // Fallback: try "response" field
        Object responseObj = genAiResponse.get("response");
        if (responseObj != null) {
            return responseObj.toString();
        }
        return "Unable to extract answer from GenAI response.";
    }

    /**
     * Extract source references from the GenAI service response.
     */
    @SuppressWarnings("unchecked")
    private List<SourceInfo> extractSources(Map<String, Object> genAiResponse) {
        if (genAiResponse == null) {
            return List.of();
        }
        Object sourcesObj = genAiResponse.get("sources");
        if (sourcesObj instanceof List) {
            List<Map<String, Object>> sourcesList = (List<Map<String, Object>>) sourcesObj;
            return sourcesList.stream()
                    .map(this::mapToSourceInfo)
                    .toList();
        }
        // Fallback: try "results" field
        Object resultsObj = genAiResponse.get("results");
        if (resultsObj instanceof List) {
            List<Map<String, Object>> resultsList = (List<Map<String, Object>>) resultsObj;
            return resultsList.stream()
                    .map(this::mapToSourceInfo)
                    .toList();
        }
        return List.of();
    }

    /**
     * Map a raw source map to a SourceInfo DTO.
     */
    private SourceInfo mapToSourceInfo(Map<String, Object> source) {
        return SourceInfo.builder()
                .documentId(parseUUID(source.get("document_id")))
                .documentTitle(getStringValue(source, "document_title", "title"))
                .chunkText(getStringValue(source, "chunk_text", "text", "content"))
                .score(parseDouble(source.get("score")))
                .build();
    }

    /**
     * Convert a QuestionAnswer entity to an AskResponse DTO.
     */
    private AskResponse toAskResponse(QuestionAnswer qa) {
        List<SourceInfo> sources;
        try {
            if (qa.getSources() != null && !qa.getSources().isEmpty()) {
                sources = objectMapper.readValue(qa.getSources(), new TypeReference<List<SourceInfo>>() {});
            } else {
                sources = List.of();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize sources for Q&A {}", qa.getId(), e);
            sources = List.of();
        }

        return AskResponse.builder()
                .id(qa.getId())
                .question(qa.getQuestion())
                .answer(qa.getAnswer())
                .sources(sources)
                .createdAt(qa.getCreatedAt())
                .build();
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value.toString();
        }
        return "";
    }
}
