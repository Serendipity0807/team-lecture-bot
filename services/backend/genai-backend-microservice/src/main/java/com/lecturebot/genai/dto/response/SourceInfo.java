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
public class SourceInfo {

    private UUID documentId;
    private String documentTitle;
    private String chunkText;
    private double score;
}
