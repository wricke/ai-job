package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import org.springframework.stereotype.Component;

@Component
public class SuggestionAgent {

    private final AiClient aiClient;

    public SuggestionAgent(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public SuggestionPack generate(ResumeInsight resume, JobInsight job, MatchDetail match) {
        List<String> resumeImprovements = new ArrayList<>();
        resumeImprovements.add("根据目标岗位重排简历重点，把最相关的技能、作品、项目或实习经历放在前面。");
        resumeImprovements.add("把经历描述改成：背景目标 -> 个人职责 -> 使用工具/方法 -> 产出结果，避免只罗列经历。");
        resumeImprovements.add("针对 JD 中的高频词，在技能、作品和项目描述中补齐同义表达。");
        if (!match.missingSkills().isEmpty()) {
            resumeImprovements.add("缺口能力需要通过课程、作品、项目或实习经历补充体现：" + String.join("、", match.missingSkills()));
        }

        List<String> projectRewriteTips = new ArrayList<>();
        projectRewriteTips.add("为每个作品或项目准备 1 个能讲 3 分钟的核心亮点，例如设计取舍、用户反馈、制作难点或结果指标。");
        projectRewriteTips.add("补充可验证结果，例如获奖、排名、播放量、用户反馈、交付物数量、效率提升或最终展示效果。");
        projectRewriteTips.add("如果是设计/媒体类经历，重点说明调研、构思、工具使用、迭代过程和最终视觉/交互效果。");

        List<String> learningPlan = new ArrayList<>();
        for (String skill : match.missingSkills()) {
            learningPlan.add("补强 " + skill + "：先补基础概念，再用一个小作品或练习证明能力。");
        }
        if (learningPlan.isEmpty()) {
            learningPlan.add("继续打磨已有作品或项目，重点准备过程、职责、结果和复盘问题。");
        }

        String aiAdvice = aiClient.complete(
                "suggestion: generate resume improvement advice for a candidate in Chinese. Base advice on the actual matched and missing skills. Do not assume backend/software direction unless requiredSkills clearly contains backend technologies. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "requiredSkills=" + job.requiredSkills() + ", matched=" + match.matchedSkills() + ", missing=" + match.missingSkills() + ", risks=" + match.risks()
        );
        return new SuggestionPack(resumeImprovements, projectRewriteTips, learningPlan, aiAdvice);
    }
}
