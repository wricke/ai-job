package com.hpc.jobagent.agent;

import java.util.List;

public record SuggestionPack(
        List<String> resumeImprovements,
        List<String> projectRewriteTips,
        List<String> learningPlan,
        String aiAdvice
) {
}
