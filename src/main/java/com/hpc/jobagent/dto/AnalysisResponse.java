package com.hpc.jobagent.dto;

import java.time.LocalDateTime;

import com.hpc.jobagent.domain.AnalysisStatus;
import com.hpc.jobagent.domain.AnalysisTask;

public record AnalysisResponse(
        Long id,
        Long resumeId,
        Long jobId,
        AnalysisStatus status,
        Integer matchScore,
        String summary,
        String resumeInsight,
        String jobInsight,
        String matchDetail,
        String suggestions,
        String interviewQuestions,
        String agentTrace,
        String errorMessage,
        String cacheKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static AnalysisResponse from(AnalysisTask task) {
        return new AnalysisResponse(
                task.getId(),
                task.getResumeId(),
                task.getJobId(),
                task.getStatus(),
                task.getMatchScore(),
                task.getSummary(),
                task.getResumeInsight(),
                task.getJobInsight(),
                task.getMatchDetail(),
                task.getSuggestions(),
                task.getInterviewQuestions(),
                task.getAgentTrace(),
                task.getErrorMessage(),
                task.getCacheKey(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }
}
