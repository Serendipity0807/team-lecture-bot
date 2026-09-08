package com.lecturebot.genai.repository;

import com.lecturebot.genai.entity.FlashcardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, UUID> {

    List<FlashcardDeck> findByCourseSpaceIdAndUserId(UUID courseSpaceId, UUID userId);
}
