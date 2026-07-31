package com.lecturebot.usercourse.service;

import com.lecturebot.usercourse.dto.request.CreateCourseSpaceRequest;
import com.lecturebot.usercourse.dto.request.UpdateCourseSpaceRequest;
import com.lecturebot.usercourse.dto.response.CourseSpaceResponse;
import com.lecturebot.usercourse.entity.CourseSpace;
import com.lecturebot.usercourse.entity.User;
import com.lecturebot.usercourse.exception.ResourceNotFoundException;
import com.lecturebot.usercourse.repository.CourseSpaceRepository;
import com.lecturebot.usercourse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSpaceService {

    private final CourseSpaceRepository courseSpaceRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseSpaceResponse create(UUID userId, CreateCourseSpaceRequest request) {
        log.info("Creating course space for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        CourseSpace courseSpace = CourseSpace.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(user)
                .build();

        courseSpace = courseSpaceRepository.save(courseSpace);
        log.info("Course space created with id: {}", courseSpace.getId());

        return toCourseSpaceResponse(courseSpace);
    }

    @Transactional(readOnly = true)
    public Page<CourseSpaceResponse> list(UUID userId, Pageable pageable) {
        log.debug("Listing course spaces for user: {}, page: {}", userId, pageable);
        return courseSpaceRepository.findByUserId(userId, pageable)
                .map(this::toCourseSpaceResponse);
    }

    @Transactional(readOnly = true)
    public CourseSpaceResponse getById(UUID id, UUID userId) {
        log.debug("Fetching course space {} for user: {}", id, userId);

        CourseSpace courseSpace = courseSpaceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course space not found with id: " + id + " for user: " + userId));

        return toCourseSpaceResponse(courseSpace);
    }

    @Transactional
    public CourseSpaceResponse update(UUID id, UUID userId, UpdateCourseSpaceRequest request) {
        log.info("Updating course space {} for user: {}", id, userId);

        CourseSpace courseSpace = courseSpaceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course space not found with id: " + id + " for user: " + userId));

        courseSpace.setTitle(request.getTitle());
        courseSpace.setDescription(request.getDescription());

        courseSpace = courseSpaceRepository.save(courseSpace);
        log.info("Course space updated: {}", id);

        return toCourseSpaceResponse(courseSpace);
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        log.info("Deleting course space {} for user: {}", id, userId);

        CourseSpace courseSpace = courseSpaceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course space not found with id: " + id + " for user: " + userId));

        courseSpaceRepository.delete(courseSpace);
        log.info("Course space deleted: {}", id);
    }

    private CourseSpaceResponse toCourseSpaceResponse(CourseSpace courseSpace) {
        return CourseSpaceResponse.builder()
                .id(courseSpace.getId())
                .title(courseSpace.getTitle())
                .description(courseSpace.getDescription())
                .documentCount(0L)
                .createdAt(courseSpace.getCreatedAt())
                .updatedAt(courseSpace.getUpdatedAt())
                .build();
    }
}
