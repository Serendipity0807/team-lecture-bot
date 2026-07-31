package com.lecturebot.document.controller;

import com.lecturebot.document.dto.response.DocumentResponse;
import com.lecturebot.document.dto.response.PagedResponse;
import com.lecturebot.document.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * POST /api/v1/documents/{courseSpaceId}
     * Upload a document file for indexing.
     */
    @PostMapping(value = "/{courseSpaceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID courseSpaceId,
            @RequestParam("file") MultipartFile file) {
        // In production, userId would be extracted from JWT Authentication
        // For now, use a placeholder UUID — the API gateway will inject the real user
        UUID userId = getCurrentUserId();
        log.info("Upload request: courseSpaceId={}, fileName={}, size={}",
                courseSpaceId, file.getOriginalFilename(), file.getSize());
        DocumentResponse response = documentService.upload(userId, courseSpaceId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/documents/{courseSpaceId}
     * List documents in a course space with pagination.
     */
    @GetMapping("/{courseSpaceId}")
    public ResponseEntity<PagedResponse<DocumentResponse>> list(
            @PathVariable UUID courseSpaceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("List documents: courseSpaceId={}, page={}, size={}", courseSpaceId, pageable.getPageNumber(), pageable.getPageSize());
        PagedResponse<DocumentResponse> response = documentService.list(courseSpaceId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/documents/{courseSpaceId}/{documentId}
     * Get a single document's metadata.
     */
    @GetMapping("/{courseSpaceId}/{documentId}")
    public ResponseEntity<DocumentResponse> getById(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID documentId) {
        log.debug("Get document: courseSpaceId={}, documentId={}", courseSpaceId, documentId);
        DocumentResponse response = documentService.getById(courseSpaceId, documentId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/documents/{courseSpaceId}/{documentId}
     * Delete a document and its index.
     */
    @DeleteMapping("/{courseSpaceId}/{documentId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable UUID courseSpaceId,
            @PathVariable UUID documentId) {
        UUID userId = getCurrentUserId();
        log.info("Delete document: courseSpaceId={}, documentId={}", courseSpaceId, documentId);
        documentService.delete(userId, courseSpaceId, documentId);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    /**
     * Placeholder for extracting current user ID.
     * In production, this would read from SecurityContextHolder.
     */
    private UUID getCurrentUserId() {
        // TODO: Replace with actual authentication extraction
        return UUID.randomUUID();
    }
}
