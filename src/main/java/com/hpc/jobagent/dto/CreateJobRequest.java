package com.hpc.jobagent.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
        String company,
        @NotBlank(message = "岗位名称不能为空") String title,
        String source,
        @NotBlank(message = "岗位要求内容不能为空") String description
) {
}
