package com.lecturebot.genai.dto.response;

import com.lecturebot.genai.entity.FlashcardDeck.FlashcardMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardDeckSummaryResponse {

    private UUID deckId;
    private FlashcardMode mode;
    private int cardCount;
    private LocalDateTime createdAt;
}
