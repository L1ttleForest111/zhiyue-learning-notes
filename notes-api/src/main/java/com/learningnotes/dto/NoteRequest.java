package com.learningnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record NoteRequest(
        @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题不能超过 200 个字符") String title,
        @NotNull(message = "内容不能为空") String content,
        Long categoryId,
        @Size(max = 20, message = "状态不合法") String status,
        List<Long> tagIds,
        LocalDateTime nextReviewAt
) {}
