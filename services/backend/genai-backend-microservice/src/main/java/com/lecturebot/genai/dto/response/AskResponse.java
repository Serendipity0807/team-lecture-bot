package com.lecturebot.genai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskResponse {

    private UUID id;
    private String question;
    private String answer;
    private List<SourceInfo> sources;
    private LocalDateTime createdAt;
}
