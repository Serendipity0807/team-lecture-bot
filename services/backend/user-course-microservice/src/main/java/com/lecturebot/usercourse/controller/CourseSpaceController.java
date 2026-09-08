package com.lecturebot.usercourse.controller;

import com.lecturebot.usercourse.dto.request.CreateCourseSpaceRequest;
import com.lecturebot.usercourse.dto.request.UpdateCourseSpaceRequest;
import com.lecturebot.usercourse.dto.response.CourseSpaceResponse;
import com.lecturebot.usercourse.dto.response.MessageResponse;
import com.lecturebot.usercourse.service.CourseSpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/coursespaces")
@RequiredArgsConstructor
public class CourseSpaceController {

    private final CourseSpaceService courseSpaceService;

    @PostMapping
    public ResponseEntity<CourseSpaceResponse> create(Authentication authentication,
                                                       @Valid @RequestBody CreateCourseSpaceRequest request) {
        UUID userId = extractUserId(authentication);
        log.info("Creating course space for user: {}", userId);
        CourseSpaceResponse response = courseSpaceService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CourseSpaceResponse>> list(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = extractUserId(authentication);
        log.info("Listing course spaces for user: {}", userId);
        Page<CourseSpaceResponse> response = courseSpaceService.list(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseSpaceResponse> getById(Authentication authentication,
                                                        @PathVariable UUID id) {
        UUID userId = extractUserId(authentication);
        log.info("Fetching course space {} for user: {}", id, userId);
        CourseSpaceResponse response = courseSpaceService.getById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseSpaceResponse> update(Authentication authentication,
                                                       @PathVariable UUID id,
                                                       @Valid @RequestBody UpdateCourseSpaceRequest request) {
        UUID userId = extractUserId(authentication);
        log.info("Updating course space {} for user: {}", id, userId);
        CourseSpaceResponse response = courseSpaceService.update(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(Authentication authentication,
                                                   @PathVariable UUID id) {
        UUID userId = extractUserId(authentication);
        log.info("Deleting course space {} for user: {}", id, userId);
        courseSpaceService.delete(id, userId);
        return ResponseEntity.ok(new MessageResponse("Course space deleted successfully"));
    }

    private UUID extractUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return UUID.fromString(userDetails.getUsername());
    }
}
