package com.hpc.jobagent.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateResumeRequest(
        @NotBlank(message = "标题不能为空") String title,
        String ownerName,
        String targetRole,
        @NotBlank(message = "简历内容不能为空") String content
) {
}
