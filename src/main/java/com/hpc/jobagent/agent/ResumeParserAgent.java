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
        if (skills.contains("平面设计") || skills.contains("视觉设计") || skills.contains("Photoshop")) {
            strengths.add("具备视觉传达和平面设计基础");
        }
        if (skills.contains("交互设计") || skills.contains("UI 设计")) {
            strengths.add("具备界面和交互体验设计意识");
        }
        if (skills.contains("三维建模") || skills.contains("SketchUp") || skills.contains("3ds Max") || skills.contains("Maya")) {
            strengths.add("具备三维建模和空间表现能力");
        }
        if (skills.contains("VR 交互")) {
            strengths.add("具备 VR 交互或数字媒体项目经历");
        }
        if (skills.contains("Premiere") || skills.contains("After Effects") || skills.contains("视频剪辑")) {
            strengths.add("具备视频剪辑和后期制作能力");
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
                "resume-parser: summarize a resume for job matching in Chinese. Focus only on the candidate's actual major, target role, tools, projects and experience. Do not assume backend/software direction unless the resume clearly contains it. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "targetRole=" + resume.getTargetRole() + "\nresume=\n" + resume.getContent()
        );
        return new ResumeInsight(skills, projects, strengths, aiSummary);
    }
}
