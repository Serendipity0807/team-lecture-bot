package com.lecturebot.genai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardCardResponse {

    private UUID id;
    private String question;
    private String answer;
    private UUID sourceDocumentId;
}
