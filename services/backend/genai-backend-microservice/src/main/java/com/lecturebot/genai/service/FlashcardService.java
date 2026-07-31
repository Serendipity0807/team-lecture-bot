package com.lecturebot.genai.service;

import com.lecturebot.genai.client.GenAiClient;
import com.lecturebot.genai.dto.request.FlashcardGenerateRequest;
import com.lecturebot.genai.dto.request.FlashcardUpdateRequest;
import com.lecturebot.genai.dto.response.FlashcardCardResponse;
import com.lecturebot.genai.dto.response.FlashcardDeckDetailResponse;
import com.lecturebot.genai.dto.response.FlashcardDeckSummaryResponse;
import com.lecturebot.genai.entity.Flashcard;
import com.lecturebot.genai.entity.FlashcardDeck;
import com.lecturebot.genai.entity.FlashcardDeck.FlashcardMode;
import com.lecturebot.genai.repository.FlashcardDeckRepository;
import com.lecturebot.genai.repository.FlashcardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FlashcardService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final GenAiClient genAiClient;

    public FlashcardService(FlashcardDeckRepository deckRepository,
                            FlashcardRepository flashcardRepository,
                            GenAiClient genAiClient) {
        this.deckRepository = deckRepository;
        this.flashcardRepository = flashcardRepository;
        this.genAiClient = genAiClient;
    }

    /**
     * Generate flashcards by calling the GenAI service.
     */
    @Transactional
    public FlashcardDeckSummaryResponse generate(UUID userId, UUID courseSpaceId, FlashcardGenerateRequest request) {
        log.info("Generating flashcards for user {} in course space {}: mode={}, count={}",
                userId, courseSpaceId, request.getMode(), request.getCount());

        // 1. Build request body for GenAI service
        Map<String, Object> genAiRequestBody = buildGenAiRequest(courseSpaceId, request);

        // 2. Call GenAI service
        Map<String, Object> genAiResponse = genAiClient.generateFlashcards(courseSpaceId, genAiRequestBody);

        // 3. Create a new FlashcardDeck
        FlashcardMode mode = request.getMode() != null ? request.getMode() : FlashcardMode.AUTO;
        FlashcardDeck deck = FlashcardDeck.builder()
                .mode(mode)
                .courseSpaceId(courseSpaceId)
                .userId(userId)
                .cards(new ArrayList<>())
                .build();

        // 4. Extract flashcards from the GenAI response and add to deck
        List<Map<String, Object>> generatedCards = extractCards(genAiResponse);
        for (Map<String, Object> cardData : generatedCards) {
            Flashcard card = Flashcard.builder()
                    .question(getStringValue(cardData, "question", "front"))
                    .answer(getStringValue(cardData, "answer", "back"))
                    .sourceDocumentId(parseUUID(cardData.get("document_id")))
                    .deck(deck)
                    .build();
            deck.getCards().add(card);
        }

        deck.setCardCount(deck.getCards().size());
        deck = deckRepository.save(deck);

        log.info("Flashcard deck {} created with {} cards", deck.getId(), deck.getCardCount());

        return toSummaryResponse(deck);
    }

    /**
     * List all flashcard decks for a user in a course space.
     */
    public List<FlashcardDeckSummaryResponse> listDecks(UUID userId, UUID courseSpaceId) {
        List<FlashcardDeck> decks = deckRepository.findByCourseSpaceIdAndUserId(courseSpaceId, userId);
        return decks.stream().map(this::toSummaryResponse).toList();
    }

    /**
     * Get a single flashcard deck with all its cards.
     */
    public FlashcardDeckDetailResponse getDeck(UUID userId, UUID courseSpaceId, UUID deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Flashcard deck not found: " + deckId));

        // Verify ownership
        if (!deck.getCourseSpaceId().equals(courseSpaceId) || !deck.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to flashcard deck: " + deckId);
        }

        return toDetailResponse(deck);
    }

    /**
     * Update a single flashcard card.
     */
    @Transactional
    public FlashcardCardResponse updateCard(UUID deckId, UUID cardId, FlashcardUpdateRequest request) {
        Flashcard card = flashcardRepository.findByDeckIdAndId(deckId, cardId)
                .orElseThrow(() -> new RuntimeException(
                        "Flashcard not found: " + cardId + " in deck " + deckId));

        if (request.getQuestion() != null) {
            card.setQuestion(request.getQuestion());
        }
        if (request.getAnswer() != null) {
            card.setAnswer(request.getAnswer());
        }
        card = flashcardRepository.save(card);

        return toCardResponse(card);
    }

    /**
     * Delete a single flashcard from a deck.
     */
    @Transactional
    public void deleteCard(UUID deckId, UUID cardId) {
        Flashcard card = flashcardRepository.findByDeckIdAndId(deckId, cardId)
                .orElseThrow(() -> new RuntimeException(
                        "Flashcard not found: " + cardId + " in deck " + deckId));

        FlashcardDeck deck = card.getDeck();
        deck.getCards().remove(card);
        deck.setCardCount(deck.getCards().size());
        flashcardRepository.delete(card);
        deckRepository.save(deck);

        log.info("Deleted flashcard {} from deck {}", cardId, deckId);
    }

    /**
     * Delete an entire flashcard deck and all its cards.
     */
    @Transactional
    public void deleteDeck(UUID deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Flashcard deck not found: " + deckId));

        deckRepository.delete(deck);
        log.info("Deleted flashcard deck {} with {} cards", deckId, deck.getCardCount());
    }

    /**
     * Export a deck as CSV string.
     */
    public String exportDeckAsCsv(UUID deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Flashcard deck not found: " + deckId));

        StringBuilder sb = new StringBuilder();
        sb.append("Question,Answer,SourceDocumentId\n");
        for (Flashcard card : deck.getCards()) {
            sb.append(escapeCsv(card.getQuestion())).append(",");
            sb.append(escapeCsv(card.getAnswer())).append(",");
            sb.append(card.getSourceDocumentId() != null ? card.getSourceDocumentId().toString() : "").append("\n");
        }
        return sb.toString();
    }

    // --- Helper methods ---

    private Map<String, Object> buildGenAiRequest(UUID courseSpaceId, FlashcardGenerateRequest request) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("course_space_id", courseSpaceId.toString());
        body.put("mode", request.getMode() != null ? request.getMode().name() : "AUTO");
        body.put("count", request.getCount());
        if (request.getKeywords() != null) {
            body.put("keywords", request.getKeywords());
        }
        if (request.getDocumentIds() != null) {
            body.put("document_ids", request.getDocumentIds().stream().map(UUID::toString).toList());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractCards(Map<String, Object> genAiResponse) {
        if (genAiResponse == null) {
            return List.of();
        }
        Object cardsObj = genAiResponse.get("cards");
        if (cardsObj instanceof List) {
            return (List<Map<String, Object>>) cardsObj;
        }
        Object flashcardsObj = genAiResponse.get("flashcards");
        if (flashcardsObj instanceof List) {
            return (List<Map<String, Object>>) flashcardsObj;
        }
        return List.of();
    }

    private FlashcardDeckSummaryResponse toSummaryResponse(FlashcardDeck deck) {
        return FlashcardDeckSummaryResponse.builder()
                .deckId(deck.getId())
                .mode(deck.getMode())
                .cardCount(deck.getCardCount())
                .createdAt(deck.getCreatedAt())
                .build();
    }

    private FlashcardDeckDetailResponse toDetailResponse(FlashcardDeck deck) {
        List<FlashcardCardResponse> cards = deck.getCards().stream()
                .map(this::toCardResponse)
                .toList();

        return FlashcardDeckDetailResponse.builder()
                .deckId(deck.getId())
                .courseSpaceId(deck.getCourseSpaceId())
                .mode(deck.getMode())
                .cards(cards)
                .build();
    }

    private FlashcardCardResponse toCardResponse(Flashcard card) {
        return FlashcardCardResponse.builder()
                .id(card.getId())
                .question(card.getQuestion())
                .answer(card.getAnswer())
                .sourceDocumentId(card.getSourceDocumentId())
                .build();
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value.toString();
        }
        return "";
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
