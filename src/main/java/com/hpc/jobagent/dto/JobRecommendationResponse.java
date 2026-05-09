package com.hpc.jobagent.dto;

import java.time.LocalDateTime;
import java.util.List;

public record JobRecommendationResponse(
        Long resumeId,
        String resumeTitle,
        String summary,
        List<JobRecommendationItem> recommendations,
        List<String> overallGaps,
        List<String> searchKeywords,
        String rawAdvice,
        LocalDateTime generatedAt
) {
}
