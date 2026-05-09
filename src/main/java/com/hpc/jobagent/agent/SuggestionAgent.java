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
        resumeImprovements.add("把项目描述改成：业务背景 -> 技术方案 -> 结果指标，避免只罗列技术栈。");
        resumeImprovements.add("针对 JD 中的高频词，在技能和项目中补齐同义表达，例如接口开发、缓存、数据库优化。");
        if (!match.missingSkills().isEmpty()) {
            resumeImprovements.add("缺口技能需要单独补一条学习或项目实践：" + String.join("、", match.missingSkills()));
        }

        List<String> projectRewriteTips = new ArrayList<>();
        projectRewriteTips.add("为每个项目准备 1 个能讲 3 分钟的技术难点，例如缓存击穿、事务一致性、接口性能优化。");
        projectRewriteTips.add("补充量化结果，例如 QPS 降低、接口耗时下降、错误率降低、重复调用减少。");
        projectRewriteTips.add("如果写 AI Agent 项目，重点说明 Agent 如何拆步骤、如何存储分析结果、如何减少重复调用。");

        List<String> learningPlan = new ArrayList<>();
        for (String skill : match.missingSkills()) {
            learningPlan.add("补强 " + skill + "：先理解核心概念，再写到项目的具体功能点中。");
        }
        if (learningPlan.isEmpty()) {
            learningPlan.add("继续打磨已有项目，重点准备数据库、缓存、并发和异常处理追问。");
        }

        String aiAdvice = aiClient.complete(
                "suggestion: generate resume improvement advice for a backend internship candidate in Chinese.",
                "matched=" + match.matchedSkills() + ", missing=" + match.missingSkills()
        );
        return new SuggestionPack(resumeImprovements, projectRewriteTips, learningPlan, aiAdvice);
    }
}
