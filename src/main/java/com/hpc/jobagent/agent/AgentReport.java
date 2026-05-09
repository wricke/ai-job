package com.hpc.jobagent.agent;

import java.util.List;

public record AgentReport(
        String summary,
        ResumeInsight resumeInsight,
        JobInsight jobInsight,
        MatchDetail matchDetail,
        SuggestionPack suggestions,
        InterviewPack interviewQuestions,
        List<AgentStepTrace> trace
) {
}
