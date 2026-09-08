package com.lecturebot.genai.repository;

import com.lecturebot.genai.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    List<Flashcard> findByDeckId(UUID deckId);

    Optional<Flashcard> findByDeckIdAndId(UUID deckId, UUID cardId);

    void deleteByDeckId(UUID deckId);
}
