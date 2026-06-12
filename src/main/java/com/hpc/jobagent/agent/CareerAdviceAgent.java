package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import org.springframework.stereotype.Component;

@Component
public class CareerAdviceAgent {

    private final AiClient aiClient;

    public CareerAdviceAgent(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public CareerAdvice generate(ResumeInsight resume, JobInsight job, MatchDetail match) {
        return new CareerAdvice(generateSuggestions(resume, job, match), generateInterview(resume, job, match));
    }

    private SuggestionPack generateSuggestions(ResumeInsight resume, JobInsight job, MatchDetail match) {
        List<String> resumeImprovements = new ArrayList<>();
        resumeImprovements.add("围绕目标岗位要求重排简历重点：" + joinOrDefault(job.requiredSkills(), "先补充岗位关键词"));
        resumeImprovements.add("把经历描述改成：背景目标 -> 个人职责 -> 使用工具/方法 -> 产出结果，避免只罗列经历。");
        if (!match.matchedSkills().isEmpty()) {
            resumeImprovements.add("已匹配能力要落到经历证据里：" + joinOrDefault(match.matchedSkills(), "暂无显性匹配能力"));
        }
        if (!match.missingSkills().isEmpty()) {
            resumeImprovements.add("缺口能力需要通过课程、作品、项目或实习经历补充体现：" + String.join("、", match.missingSkills()));
        }
        if (!match.risks().isEmpty()) {
            resumeImprovements.add("针对匹配风险补充解释或证据：" + shortText(match.risks().get(0), 80));
        }

        List<String> projectRewriteTips = new ArrayList<>();
        projectRewriteTips.add("为「" + firstOrDefault(resume.projects(), "代表性经历")
                + "」准备 1 个能讲 3 分钟的核心亮点，说明目标、动作、难点和结果。");
        projectRewriteTips.add("结合岗位职责补充经历细节：" + joinOrDefault(job.responsibilities(), "职责、协作对象、交付物和结果"));
        projectRewriteTips.add("补充可验证结果，例如排名、数据变化、用户反馈、交付物数量、效率提升或最终展示效果。");

        List<String> learningPlan = new ArrayList<>();
        for (String skill : match.missingSkills()) {
            learningPlan.add("补强 " + skill + "：先补基础概念，再用一个小案例、练习或经历复盘证明能力。");
        }
        if (learningPlan.isEmpty()) {
            learningPlan.add("继续打磨已有作品或项目，重点准备过程、职责、结果和复盘问题。");
        }

        String aiAdvice = aiClient.complete(
                "career-advice: generate resume improvement advice for a candidate in Chinese. Base advice only on the actual resume projects, required skills, responsibilities, matched and missing skills, and risks. Do not assume any role direction unless it is clearly present in the input. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "resumeSkills=" + resume.skills()
                        + ", resumeProjects=" + resume.projects()
                        + ", requiredSkills=" + job.requiredSkills()
                        + ", responsibilities=" + job.responsibilities()
                        + ", matched=" + match.matchedSkills()
                        + ", missing=" + match.missingSkills()
                        + ", risks=" + match.risks()
        );
        return new SuggestionPack(resumeImprovements, projectRewriteTips, learningPlan, aiAdvice);
    }

    private InterviewPack generateInterview(ResumeInsight resume, JobInsight job, MatchDetail match) {
        List<String> questions = new ArrayList<>();
        addQuestion(questions, "请结合「" + firstOrDefault(resume.projects(), "一段代表性作品、项目或实习经历")
                + "」，说明它为什么能体现你和这个岗位匹配，以及你具体负责了什么？");
        addQuestion(questions, "这个经历里最难的部分是什么？你是如何分析、推进和复盘的？");

        for (String skill : firstItems(job.requiredSkills(), 4)) {
            addQuestion(questions, "岗位要求提到「" + shortText(skill, 30)
                    + "」，请准备一个能证明这项能力的作品、项目或实践案例，并说明你的方法和结果。");
        }
        for (String responsibility : firstItems(job.responsibilities(), 2)) {
            addQuestion(questions, "岗位职责涉及「" + shortText(responsibility, 50)
                    + "」，你过去哪段经历可以对应？请说明目标、动作和产出。");
        }
        for (String missingSkill : firstItems(match.missingSkills(), 3)) {
            addQuestion(questions, "如果面试官追问「" + shortText(missingSkill, 30)
                    + "」这个缺口，你会如何说明当前基础、补强计划和可验证练习？");
        }
        for (String bonusItem : firstItems(job.bonusItems(), 2)) {
            addQuestion(questions, "岗位加分项「" + shortText(bonusItem, 40)
                    + "」可以怎样用已有经历、作品或学习记录补充证明？");
        }
        for (String risk : firstItems(match.risks(), 2)) {
            addQuestion(questions, "针对匹配风险「" + shortText(risk, 60)
                    + "」，面试中你准备怎样主动解释或补充证据？");
        }

        addQuestion(questions, "如果重新做一次，你会如何优化过程或最终产出？");

        List<String> talkingPoints = new ArrayList<>();
        talkingPoints.add("先讲目标和背景，再讲自己的职责、方法和结果。");
        talkingPoints.add("遇到不会的问题，可以先说自己的理解边界，再给出排查思路。");
        talkingPoints.add("把匹配能力串到作品、项目或实习经历里讲，不要只停留在技能列表。");

        String aiAdvice = aiClient.complete(
                "career-advice: generate interview preparation advice for a candidate in Chinese. Base advice on the actual resume skills, projects, requiredSkills, responsibilities, matched and missing skills, and risks. Do not assume any role direction unless it is clearly present in the input. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "resumeSkills=" + resume.skills()
                        + ", resumeProjects=" + resume.projects()
                        + ", resumeStrengths=" + resume.strengths()
                        + ", requiredSkills=" + job.requiredSkills()
                        + ", responsibilities=" + job.responsibilities()
                        + ", matched=" + match.matchedSkills()
                        + ", missing=" + match.missingSkills()
                        + ", risks=" + match.risks()
        );
        return new InterviewPack(questions, talkingPoints, aiAdvice);
    }

    private void addQuestion(List<String> questions, String question) {
        if (!questions.contains(question)) {
            questions.add(question);
        }
    }

    private List<String> firstItems(List<String> values, int limit) {
        List<String> items = new ArrayList<>();
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank() && !items.contains(cleaned)) {
                items.add(cleaned);
            }
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private String firstOrDefault(List<String> values, String fallback) {
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                return shortText(cleaned, 40);
            }
        }
        return fallback;
    }

    private String joinOrDefault(List<String> values, String fallback) {
        List<String> cleaned = firstItems(values, 5);
        return cleaned.isEmpty() ? fallback : String.join("、", cleaned);
    }

    private String shortText(String value, int maxLength) {
        String cleaned = clean(value);
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "...";
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
