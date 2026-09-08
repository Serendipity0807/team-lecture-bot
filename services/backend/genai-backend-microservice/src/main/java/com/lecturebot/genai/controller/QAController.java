package com.lecturebot.genai.controller;

import com.lecturebot.genai.dto.request.AskRequest;
import com.lecturebot.genai.dto.response.AskResponse;
import com.lecturebot.genai.dto.response.PagedResponse;
import com.lecturebot.genai.service.QAService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/coursespaces/{courseSpaceId}")
public class QAController {

    private static final Logger log = LoggerFactory.getLogger(QAController.class);

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    /**
     * POST /api/coursespaces/{courseSpaceId}/ask
     * Ask a question against documents in the course space.
     */
    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(
            Principal principal,
            @PathVariable UUID courseSpaceId,
            @Valid @RequestBody AskRequest request) {
        UUID userId = extractUserId(principal);
        log.info("Q&A ask: userId={}, courseSpaceId={}, question='{}'", userId, courseSpaceId, request.getQuestion());
        AskResponse response = qaService.ask(userId, courseSpaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/coursespaces/{courseSpaceId}/ask/history
     * Get paginated Q&A history.
     */
    @GetMapping("/ask/history")
    public ResponseEntity<PagedResponse<AskResponse>> getHistory(
            Principal principal,
            @PathVariable UUID courseSpaceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = extractUserId(principal);
        log.debug("Q&A history: userId={}, courseSpaceId={}", userId, courseSpaceId);
        PagedResponse<AskResponse> response = qaService.getHistory(userId, courseSpaceId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from the security principal.
     * Expects the principal name to be the user ID string (set by API gateway / auth filter).
     */
    private UUID extractUserId(Principal principal) {
        if (principal == null) {
            log.warn("No principal found, using placeholder user ID");
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Principal name '{}' is not a valid UUID, using placeholder", principal.getName());
            return UUID.randomUUID();
        }
    }
}
