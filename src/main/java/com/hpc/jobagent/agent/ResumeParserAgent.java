package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.ResumeProfile;
import org.springframework.stereotype.Component;

@Component
public class ResumeParserAgent {

    private final TextAnalyzer textAnalyzer;
    private final AiClient aiClient;

    public ResumeParserAgent(TextAnalyzer textAnalyzer, AiClient aiClient) {
        this.textAnalyzer = textAnalyzer;
        this.aiClient = aiClient;
    }

    public ResumeInsight analyze(ResumeProfile resume) {
        List<String> skills = textAnalyzer.findSkills(resume.getContent());
        List<String> projects = textAnalyzer.extractProjectSignals(resume.getContent());
        List<String> strengths = new ArrayList<>();
        if (skills.contains("Java") && skills.contains("Spring Boot")) {
            strengths.add("具备 Java Web 后端开发基础");
        }
        if (skills.contains("MySQL") && skills.contains("Redis")) {
            strengths.add("有数据库和缓存相关项目表达");
        }
        if (skills.contains("RabbitMQ") || skills.contains("高并发")) {
            strengths.add("项目能延展到异步处理或高并发问题");
        }
        if (strengths.isEmpty()) {
            strengths.add("简历中已有可用于岗位匹配的项目和技能信息");
        }
        String aiSummary = aiClient.complete(
                "resume-parser: summarize a resume for backend internship matching in Chinese. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                resume.getContent()
        );
        return new ResumeInsight(skills, projects, strengths, aiSummary);
    }
}
