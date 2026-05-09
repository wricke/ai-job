package com.hpc.jobagent.domain;

import java.time.LocalDateTime;

public class AnalysisCache {

    private String cacheKey;
    private Integer matchScore;
    private String summary;
    private String resumeInsight;
    private String jobInsight;
    private String matchDetail;
    private String suggestions;
    private String interviewQuestions;
    private String agentTrace;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getResumeInsight() {
        return resumeInsight;
    }

    public void setResumeInsight(String resumeInsight) {
        this.resumeInsight = resumeInsight;
    }

    public String getJobInsight() {
        return jobInsight;
    }

    public void setJobInsight(String jobInsight) {
        this.jobInsight = jobInsight;
    }

    public String getMatchDetail() {
        return matchDetail;
    }

    public void setMatchDetail(String matchDetail) {
        this.matchDetail = matchDetail;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public String getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(String interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }

    public String getAgentTrace() {
        return agentTrace;
    }

    public void setAgentTrace(String agentTrace) {
        this.agentTrace = agentTrace;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
