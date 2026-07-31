package com.lecturebot.genai.dto.response;

import com.lecturebot.genai.entity.FlashcardDeck.FlashcardMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardDeckDetailResponse {

    private UUID deckId;
    private UUID courseSpaceId;
    private FlashcardMode mode;
    private List<FlashcardCardResponse> cards;
}
