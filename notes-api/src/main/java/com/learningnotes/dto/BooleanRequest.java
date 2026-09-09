package com.learningnotes.dto;

import jakarta.validation.constraints.NotNull;

public record BooleanRequest(@NotNull(message = "状态不能为空") Boolean value) {}
