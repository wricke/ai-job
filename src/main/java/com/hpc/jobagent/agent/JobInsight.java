package com.hpc.jobagent.agent;

import java.util.List;

public record JobInsight(
        List<String> requiredSkills,
        List<String> responsibilities,
        List<String> bonusItems,
        String aiSummary
) {
}
