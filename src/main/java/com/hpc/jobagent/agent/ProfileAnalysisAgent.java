package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfileAnalysisAgent {

    private final TextAnalyzer textAnalyzer;
    private final AiClient aiClient;

    public ProfileAnalysisAgent(TextAnalyzer textAnalyzer, AiClient aiClient) {
        this.textAnalyzer = textAnalyzer;
        this.aiClient = aiClient;
    }

    public ProfileAnalysis analyze(ResumeProfile resume, JobPosting job) {
        return new ProfileAnalysis(analyzeResume(resume), analyzeJob(job));
    }

    private ResumeInsight analyzeResume(ResumeProfile resume) {
        List<String> skills = textAnalyzer.findSkills(resume.getContent());
        List<String> projects = textAnalyzer.extractProjectSignals(resume.getContent());
        List<String> strengths = new ArrayList<>();
        if (resume.getTargetRole() != null && !resume.getTargetRole().isBlank()) {
            strengths.add("目标岗位方向较明确：" + resume.getTargetRole().strip());
        }
        if (!skills.isEmpty()) {
            strengths.add("简历明确体现的能力关键词：" + String.join("、", limit(skills, 6)));
        }
        if (!projects.isEmpty()) {
            strengths.add("已有可用于岗位匹配的经历证据：" + shortText(projects.get(0), 70));
        }
        if (strengths.isEmpty()) {
            strengths.add("简历中已有可用于岗位匹配的经历信息，建议继续补充目标岗位关键词和代表性项目。");
        }
        String aiSummary = aiClient.complete(
                "profile-analysis: summarize a resume for job matching in Chinese. Focus only on the candidate's actual major, target role, tools, projects and experience. Do not assume any role direction unless it is clearly present in the resume. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "targetRole=" + resume.getTargetRole() + "\nresume=\n" + resume.getContent()
        );
        return new ResumeInsight(skills, projects, strengths, aiSummary);
    }

    private JobInsight analyzeJob(JobPosting job) {
        List<String> requiredSkills = textAnalyzer.findSkills(job.getDescription());
        List<String> responsibilities = textAnalyzer.extractResponsibilities(job.getDescription());
        List<String> bonusItems = textAnalyzer.extractBonusItems(job.getDescription());
        String aiSummary = aiClient.complete(
                "profile-analysis: summarize a job description for job matching in Chinese. Extract the real role direction, responsibilities, tools and requirements from the JD. Do not assume any role direction unless it is clearly present in the JD. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "jobTitle=" + job.getTitle() + "\njobDescription=\n" + job.getDescription()
        );
        return new JobInsight(requiredSkills, responsibilities, bonusItems, aiSummary);
    }

    private List<String> limit(List<String> values, int limit) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.strip());
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private String shortText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
