package com.lecturebot.genai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskRequest {

    @NotBlank(message = "Question must not be blank")
    private String question;

    @Builder.Default
    private int topK = 5;
}
