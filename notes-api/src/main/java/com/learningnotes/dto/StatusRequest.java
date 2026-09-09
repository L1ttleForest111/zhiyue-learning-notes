package com.learningnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record StatusRequest(@NotBlank(message = "学习状态不能为空") @Size(max = 20, message = "学习状态不合法") String status, LocalDateTime nextReviewAt) {}
