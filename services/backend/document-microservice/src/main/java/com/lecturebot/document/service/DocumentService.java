package com.lecturebot.document.service;

import com.lecturebot.document.client.GenAiClient;
import com.lecturebot.document.dto.response.DocumentResponse;
import com.lecturebot.document.dto.response.PagedResponse;
import com.lecturebot.document.entity.Document;
import com.lecturebot.document.entity.Document.DocumentStatus;
import com.lecturebot.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final GenAiClient genAiClient;
    private final Path uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           GenAiClient genAiClient,
                           @Value("${app.upload.dir:uploads}") String uploadDirPath) {
        this.documentRepository = documentRepository;
        this.genAiClient = genAiClient;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Upload a document, store it locally, extract text, and index via GenAI.
     */
    @Transactional
    public DocumentResponse upload(UUID userId, UUID courseSpaceId, MultipartFile file) {
        log.info("Uploading file '{}' for user {} in course space {}", file.getOriginalFilename(), userId, courseSpaceId);

        // 1. Create and save Document entity with UPLOADING status
        Document document = Document.builder()
                .title(file.getOriginalFilename())
                .fileName(file.getOriginalFilename())
                .fileType(getFileType(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .status(DocumentStatus.UPLOADING)
                .courseSpaceId(courseSpaceId)
                .userId(userId)
                .build();

        document = documentRepository.save(document);
        log.debug("Document entity created with id {}", document.getId());

        // 2. Store file to local filesystem
        try {
            String storedFileName = document.getId() + "_" + file.getOriginalFilename();
            Path targetPath = uploadDir.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            document.setStoragePath(targetPath.toString());
            log.debug("File stored at {}", targetPath);
        } catch (IOException e) {
            document.setStatus(DocumentStatus.ERROR);
            documentRepository.save(document);
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        // 3. Extract text content
        String extractedText;
        try {
            document.setStatus(DocumentStatus.INDEXING);
            documentRepository.save(document);

            extractedText = extractText(file);
            log.debug("Extracted {} characters of text", extractedText.length());
        } catch (Exception e) {
            document.setStatus(DocumentStatus.ERROR);
            documentRepository.save(document);
            throw new RuntimeException("Failed to extract text from file: " + e.getMessage(), e);
        }

        // 4. Index document via GenAI service
        try {
            genAiClient.indexDocument(courseSpaceId, document.getId(), extractedText);
        } catch (Exception e) {
            document.setStatus(DocumentStatus.ERROR);
            documentRepository.save(document);
            log.error("GenAI indexing failed for document {}", document.getId(), e);
            throw new RuntimeException("Failed to index document: " + e.getMessage(), e);
        }

        // 5. Estimate chunk count and mark as READY
        int estimatedChunks = Math.max(1, extractedText.length() / 500);
        document.setChunkCount(estimatedChunks);
        document.setStatus(DocumentStatus.READY);
        document = documentRepository.save(document);

        log.info("Document {} uploaded and indexed successfully", document.getId());
        return toResponse(document);
    }

    /**
     * List documents in a course space with pagination.
     */
    public PagedResponse<DocumentResponse> list(UUID courseSpaceId, Pageable pageable) {
        Page<Document> page = documentRepository.findByCourseSpaceId(courseSpaceId, pageable);
        return PagedResponse.<DocumentResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    /**
     * Get a single document by course space ID and document ID.
     */
    public DocumentResponse getById(UUID courseSpaceId, UUID documentId) {
        Document document = documentRepository.findByIdAndCourseSpaceId(documentId, courseSpaceId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found: " + documentId + " in course space " + courseSpaceId));
        return toResponse(document);
    }

    /**
     * Delete a document: remove vector index, delete file, then delete DB record.
     */
    @Transactional
    public void delete(UUID userId, UUID courseSpaceId, UUID documentId) {
        Document document = documentRepository.findByIdAndCourseSpaceId(documentId, courseSpaceId)
                .orElseThrow(() -> new RuntimeException(
                        "Document not found: " + documentId + " in course space " + courseSpaceId));

        log.info("Deleting document {} from course space {}", documentId, courseSpaceId);

        // 1. Delete vector index from GenAI
        try {
            genAiClient.deleteIndex(courseSpaceId, documentId);
        } catch (Exception e) {
            log.warn("Failed to delete GenAI index for document {}: {}", documentId, e.getMessage());
            // Continue with deletion even if GenAI call fails
        }

        // 2. Delete file from local storage
        if (document.getStoragePath() != null) {
            try {
                Files.deleteIfExists(Path.of(document.getStoragePath()));
                log.debug("Deleted file at {}", document.getStoragePath());
            } catch (IOException e) {
                log.warn("Failed to delete file for document {}: {}", documentId, e.getMessage());
            }
        }

        // 3. Delete database record
        documentRepository.delete(document);
        log.info("Document {} deleted successfully", documentId);
    }

    /**
     * Simple text extraction. Supports plain text files; for PDFs returns a placeholder
     * indicating PDFBox integration is needed.
     */
    private String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return "";
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".csv")) {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        if (lowerName.endsWith(".pdf")) {
            // PDFBox integration placeholder — in production, use Apache PDFBox:
            // try (PDDocument pdDoc = PDDocument.load(file.getInputStream())) {
            //     return new PDFTextStripper().getText(pdDoc);
            // }
            log.warn("PDF text extraction requires Apache PDFBox dependency. Returning placeholder.");
            return "[PDF content placeholder - add PDFBox dependency for full text extraction] "
                    + "File: " + fileName + ", Size: " + file.getSize() + " bytes";
        }

        // For other binary files, return a minimal placeholder
        log.warn("Unsupported file type for text extraction: {}", fileName);
        return "[Binary file: " + fileName + "]";
    }

    /**
     * Determine file type from extension.
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return "unknown";
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Convert Document entity to DocumentResponse DTO.
     */
    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .courseSpaceId(document.getCourseSpaceId())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
