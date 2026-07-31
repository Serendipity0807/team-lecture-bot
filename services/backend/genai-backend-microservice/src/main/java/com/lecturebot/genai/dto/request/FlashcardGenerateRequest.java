package com.lecturebot.genai.dto.request;

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
public class FlashcardGenerateRequest {

    private FlashcardMode mode;

    private List<String> keywords;

    private List<UUID> documentIds;

    @Builder.Default
    private int count = 10;
}
