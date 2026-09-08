package com.lecturebot.genai.controller;

import com.lecturebot.genai.dto.request.FlashcardGenerateRequest;
import com.lecturebot.genai.dto.request.FlashcardUpdateRequest;
import com.lecturebot.genai.dto.response.FlashcardCardResponse;
import com.lecturebot.genai.dto.response.FlashcardDeckDetailResponse;
import com.lecturebot.genai.dto.response.FlashcardDeckSummaryResponse;
import com.lecturebot.genai.service.FlashcardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/coursespaces/{courseSpaceId}/flashcards")
public class FlashcardController {

    private static final Logger log = LoggerFactory.getLogger(FlashcardController.class);

    private final FlashcardService flashcardService;

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    /**
     * POST /api/coursespaces/{courseSpaceId}/flashcards/generate
     * Generate a new flashcard deck.
     */
    @PostMapping("/generate")
    public ResponseEntity<FlashcardDeckSummaryResponse> generate(
            Principal principal,
            @PathVariable UUID courseSpaceId,
            @RequestBody FlashcardGenerateRequest request) {
        UUID userId = extractUserId(principal);
        log.info("Generate flashcards: userId={}, courseSpaceId={}, mode={}", userId, courseSpaceId, request.getMode());
        FlashcardDeckSummaryResponse response = flashcardService.generate(userId, courseSpaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/coursespaces/{courseSpaceId}/flashcards/decks
     * List all flashcard decks for the user in this course space.
     */
    @GetMapping("/decks")
    public ResponseEntity<List<FlashcardDeckSummaryResponse>> listDecks(
            Principal principal,
            @PathVariable UUID courseSpaceId) {
        UUID userId = extractUserId(principal);
        log.debug("List flashcard decks: userId={}, courseSpaceId={}", userId, courseSpaceId);
        List<FlashcardDeckSummaryResponse> response = flashcardService.listDecks(userId, courseSpaceId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/coursespaces/{courseSpaceId}/flashcards/decks/{deckId}
     * Get a single flashcard deck with all cards.
     */
    @GetMapping("/decks/{deckId}")
    public ResponseEntity<FlashcardDeckDetailResponse> getDeck(
            Principal principal,
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID deckId) {
        UUID userId = extractUserId(principal);
        log.debug("Get flashcard deck: userId={}, courseSpaceId={}, deckId={}", userId, courseSpaceId, deckId);
        FlashcardDeckDetailResponse response = flashcardService.getDeck(userId, courseSpaceId, deckId);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/coursespaces/{courseSpaceId}/flashcards/decks/{deckId}/cards/{cardId}
     * Update a single flashcard.
     */
    @PutMapping("/decks/{deckId}/cards/{cardId}")
    public ResponseEntity<FlashcardCardResponse> updateCard(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID deckId,
            @PathVariable UUID cardId,
            @RequestBody FlashcardUpdateRequest request) {
        log.info("Update flashcard: deckId={}, cardId={}", deckId, cardId);
        FlashcardCardResponse response = flashcardService.updateCard(deckId, cardId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/coursespaces/{courseSpaceId}/flashcards/decks/{deckId}/cards/{cardId}
     * Delete a single flashcard from a deck.
     */
    @DeleteMapping("/decks/{deckId}/cards/{cardId}")
    public ResponseEntity<Map<String, String>> deleteCard(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID deckId,
            @PathVariable UUID cardId) {
        log.info("Delete flashcard: deckId={}, cardId={}", deckId, cardId);
        flashcardService.deleteCard(deckId, cardId);
        return ResponseEntity.ok(Map.of("message", "Flashcard deleted successfully"));
    }

    /**
     * DELETE /api/coursespaces/{courseSpaceId}/flashcards/decks/{deckId}
     * Delete an entire flashcard deck.
     */
    @DeleteMapping("/decks/{deckId}")
    public ResponseEntity<Map<String, String>> deleteDeck(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID deckId) {
        log.info("Delete flashcard deck: deckId={}", deckId);
        flashcardService.deleteDeck(deckId);
        return ResponseEntity.ok(Map.of("message", "Flashcard deck deleted successfully"));
    }

    /**
     * GET /api/coursespaces/{courseSpaceId}/flashcards/decks/{deckId}/export
     * Export a flashcard deck as CSV.
     */
    @GetMapping("/decks/{deckId}/export")
    public ResponseEntity<String> exportDeck(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID deckId,
            @RequestParam(defaultValue = "CSV") String format) {
        log.info("Export flashcard deck: deckId={}, format={}", deckId, format);

        if (!"CSV".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().body("Unsupported export format: " + format + ". Only CSV is supported.");
        }

        String csv = flashcardService.exportDeckAsCsv(deckId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flashcards-" + deckId + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    /**
     * Extract user ID from the security principal.
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
