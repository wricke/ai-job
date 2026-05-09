package com.hpc.jobagent.service;

import java.util.List;

import com.hpc.jobagent.agent.JobRecommendationAgent;
import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.JobRecommendationItem;
import com.hpc.jobagent.dto.JobRecommendationResponse;
import com.hpc.jobagent.dto.JobResponse;
import org.springframework.stereotype.Service;

@Service
public class JobRecommendationService {

    private final ResumeService resumeService;
    private final JobRecommendationAgent jobRecommendationAgent;
    private final JobPostingService jobPostingService;

    public JobRecommendationService(ResumeService resumeService,
                                    JobRecommendationAgent jobRecommendationAgent,
                                    JobPostingService jobPostingService) {
        this.resumeService = resumeService;
        this.jobRecommendationAgent = jobRecommendationAgent;
        this.jobPostingService = jobPostingService;
    }

    public JobRecommendationResponse recommend(Long resumeId) {
        ResumeProfile resume = resumeService.getEntity(resumeId);
        JobRecommendationResponse report = jobRecommendationAgent.recommend(resume);
        List<JobRecommendationItem> savedRecommendations = report.recommendations().stream()
                .map(item -> withSavedJobId(resume, item))
                .toList();
        return new JobRecommendationResponse(
                report.resumeId(),
                report.resumeTitle(),
                report.summary(),
                savedRecommendations,
                report.overallGaps(),
                report.searchKeywords(),
                report.rawAdvice(),
                report.generatedAt()
        );
    }

    private JobRecommendationItem withSavedJobId(ResumeProfile resume, JobRecommendationItem item) {
        JobResponse job = jobPostingService.saveRecommendation(resume, item);
        return new JobRecommendationItem(
                item.roleTitle(),
                item.fitScore(),
                item.fitLevel(),
                item.reasons(),
                item.matchedSkills(),
                item.missingSkills(),
                item.searchKeywords(),
                item.preparationTips(),
                job.id()
        );
    }
}
