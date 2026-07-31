package com.lecturebot.genai.repository;

import com.lecturebot.genai.entity.QuestionAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, UUID> {

    Page<QuestionAnswer> findByCourseSpaceIdAndUserId(UUID courseSpaceId, UUID userId, Pageable pageable);
}
