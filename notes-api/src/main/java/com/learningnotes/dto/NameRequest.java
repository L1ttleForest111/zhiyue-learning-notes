package com.learningnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NameRequest(@NotBlank(message = "名称不能为空") @Size(max = 50, message = "名称不能超过 50 个字符") String name) {}
