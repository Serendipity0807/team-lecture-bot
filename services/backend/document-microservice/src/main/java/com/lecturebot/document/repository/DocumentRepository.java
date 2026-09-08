package com.lecturebot.document.repository;

import com.lecturebot.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByCourseSpaceId(UUID courseSpaceId, Pageable pageable);

    Optional<Document> findByIdAndCourseSpaceId(UUID id, UUID courseSpaceId);

    void deleteByCourseSpaceIdAndId(UUID courseSpaceId, UUID id);
}
