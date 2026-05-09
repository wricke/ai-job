package com.hpc.jobagent.dto;

import java.util.List;

public record JobRecommendationItem(
        String roleTitle,
        int fitScore,
        String fitLevel,
        List<String> reasons,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> searchKeywords,
        List<String> preparationTips,
        Long jobId
) {
}
