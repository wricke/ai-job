package com.hpc.jobagent.dto;

import jakarta.validation.constraints.NotNull;

public record CreateAnalysisRequest(
        @NotNull(message = "简历 ID 不能为空") Long resumeId,
        @NotNull(message = "岗位 ID 不能为空") Long jobId
) {
}
