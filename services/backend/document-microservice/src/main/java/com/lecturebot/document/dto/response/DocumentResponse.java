package com.lecturebot.document.dto.response;

import com.lecturebot.document.entity.Document.DocumentStatus;
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
public class DocumentResponse {

    private UUID id;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private UUID courseSpaceId;
    private DocumentStatus status;
    private int chunkCount;
    private LocalDateTime createdAt;
}
