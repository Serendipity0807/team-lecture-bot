package com.lecturebot.usercourse.repository;

import com.lecturebot.usercourse.entity.CourseSpace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseSpaceRepository extends JpaRepository<CourseSpace, UUID> {

    Page<CourseSpace> findByUserId(UUID userId, Pageable pageable);

    Optional<CourseSpace> findByIdAndUserId(UUID id, UUID userId);
}
