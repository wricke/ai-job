package com.hpc.jobagent.agent;

import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.JobPosting;
import org.springframework.stereotype.Component;

@Component
public class JobAnalyzerAgent {

    private final TextAnalyzer textAnalyzer;
    private final AiClient aiClient;

    public JobAnalyzerAgent(TextAnalyzer textAnalyzer, AiClient aiClient) {
        this.textAnalyzer = textAnalyzer;
        this.aiClient = aiClient;
    }

    public JobInsight analyze(JobPosting job) {
        List<String> requiredSkills = textAnalyzer.findSkills(job.getDescription());
        List<String> responsibilities = textAnalyzer.extractResponsibilities(job.getDescription());
        List<String> bonusItems = textAnalyzer.extractBonusItems(job.getDescription());
        String aiSummary = aiClient.complete(
                "jd-analyzer: summarize a job description for job matching in Chinese. Extract the real role direction, responsibilities, tools and requirements from the JD. Do not assume backend/software direction unless the JD clearly contains it. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "jobTitle=" + job.getTitle() + "\njobDescription=\n" + job.getDescription()
        );
        return new JobInsight(requiredSkills, responsibilities, bonusItems, aiSummary);
    }
}
