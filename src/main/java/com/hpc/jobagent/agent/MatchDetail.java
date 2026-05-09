package com.hpc.jobagent.agent;

import java.util.List;

public record MatchDetail(
        int score,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> risks,
        String reason
) {
}
