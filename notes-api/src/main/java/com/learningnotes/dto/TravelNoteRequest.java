package com.learningnotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TravelNoteRequest(
        @NotBlank(message = "旅行标题不能为空") @Size(max = 200, message = "标题最多 200 字") String title,
        @Size(max = 100, message = "目的地最多 100 字") String destination,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 1000, message = "摘要最多 1000 字") String summary,
        @Size(max = 100000, message = "心得最多 100000 字") String journalContent,
        @NotBlank(message = "请选择 HTML 攻略文件") @Size(max = 2097152, message = "HTML 文件最多 2 MiB") String htmlContent,
        @NotBlank(message = "文件名不能为空") @Size(max = 255, message = "文件名过长") String originalFilename,
        @NotBlank(message = "导入标识不能为空")
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", message = "导入标识格式不正确") String importKey
) {}
