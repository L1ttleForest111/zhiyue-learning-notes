package com.learningnotes.vo;

import com.learningnotes.entity.Tag;
import java.time.LocalDateTime;
import java.util.List;

public record NoteView(Long id, String title, String content, Long categoryId, String categoryName,
                       String status, Boolean pinned, Boolean favorite, LocalDateTime nextReviewAt,
                       LocalDateTime createdAt, LocalDateTime updatedAt, List<Tag> tags) {}
