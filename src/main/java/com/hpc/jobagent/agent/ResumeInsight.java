package com.hpc.jobagent.agent;

import java.util.List;

public record ResumeInsight(
        List<String> skills,
        List<String> projects,
        List<String> strengths,
        String aiSummary
) {
}
