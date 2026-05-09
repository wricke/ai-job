package com.hpc.jobagent.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.JobRecommendationItem;
import com.hpc.jobagent.dto.JobRecommendationResponse;
import org.springframework.stereotype.Component;

@Component
public class JobRecommendationAgent {

    private static final int MAX_RESUME_CHARS = 6000;

    private final TextAnalyzer textAnalyzer;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public JobRecommendationAgent(TextAnalyzer textAnalyzer, AiClient aiClient, ObjectMapper objectMapper) {
        this.textAnalyzer = textAnalyzer;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public JobRecommendationResponse recommend(ResumeProfile resume) {
        List<String> skills = textAnalyzer.findSkills(resume.getContent());
        List<String> projects = textAnalyzer.extractProjectSignals(resume.getContent());
        String rawAdvice = aiClient.complete(systemPrompt(), userPrompt(resume, skills, projects));
        try {
            RecommendationPayload payload = objectMapper.readValue(extractJson(rawAdvice), RecommendationPayload.class);
            return normalize(resume, payload, rawAdvice);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return fallback(resume, skills, rawAdvice);
        }
    }

    private String systemPrompt() {
        return """
                你是一个面向中国互联网校招和实习求职的职业规划 Agent。
                你需要只根据用户简历，推荐适合投递的岗位方向，并给出适配度、理由、短板、搜索关键词和准备建议。
                请只返回合法 JSON，不要 Markdown，不要代码块，不要额外解释。
                JSON 结构必须是：
                {
                  "summary": "一句话总结候选人的求职定位",
                  "recommendations": [
                    {
                      "roleTitle": "岗位方向名称",
                      "fitScore": 0,
                      "fitLevel": "高/中/挑战",
                      "reasons": ["适合原因"],
                      "matchedSkills": ["已匹配技能"],
                      "missingSkills": ["需要补强技能"],
                      "searchKeywords": ["招聘平台搜索关键词"],
                      "preparationTips": ["投递或准备建议"]
                    }
                  ],
                  "overallGaps": ["全局短板"],
                  "searchKeywords": ["通用搜索关键词"]
                }
                推荐 4 到 6 个岗位方向，fitScore 必须是 0 到 100 的整数。
                """;
    }

    private String userPrompt(ResumeProfile resume, List<String> skills, List<String> projects) {
        return """
                简历标题：%s
                候选人目标岗位：%s
                已抽取技能：%s
                项目信号：%s

                简历原文：
                %s
                """.formatted(
                nullToDash(resume.getTitle()),
                nullToDash(resume.getTargetRole()),
                skills,
                projects,
                truncate(resume.getContent())
        );
    }

    private JobRecommendationResponse normalize(ResumeProfile resume,
                                                RecommendationPayload payload,
                                                String rawAdvice) {
        List<JobRecommendationItem> items = new ArrayList<>();
        for (JobRecommendationItem item : safeList(payload.recommendations())) {
            if (item.roleTitle() == null || item.roleTitle().isBlank()) {
                continue;
            }
            int score = Math.max(0, Math.min(100, item.fitScore()));
            items.add(new JobRecommendationItem(
                    item.roleTitle().strip(),
                    score,
                    normalizeLevel(item.fitLevel(), score),
                    limitStrings(item.reasons(), 5),
                    limitStrings(item.matchedSkills(), 8),
                    limitStrings(item.missingSkills(), 8),
                    limitStrings(item.searchKeywords(), 8),
                    limitStrings(item.preparationTips(), 5),
                    item.jobId()
            ));
            if (items.size() >= 6) {
                break;
            }
        }
        if (items.isEmpty()) {
            return fallback(resume, textAnalyzer.findSkills(resume.getContent()), rawAdvice);
        }
        return new JobRecommendationResponse(
                resume.getId(),
                resume.getTitle(),
                blankToDefault(payload.summary(), "已根据简历生成岗位推荐。"),
                items,
                limitStrings(payload.overallGaps(), 6),
                limitStrings(payload.searchKeywords(), 10),
                rawAdvice,
                LocalDateTime.now()
        );
    }

    private JobRecommendationResponse fallback(ResumeProfile resume, List<String> skills, String rawAdvice) {
        List<JobRecommendationItem> items = new ArrayList<>();
        items.add(fallbackItem("Java 后端开发实习生", skills, 86,
                List.of("简历技能与后端开发岗位方向接近", "适合继续围绕 Spring Boot、MySQL、接口开发打磨项目表达"),
                List.of("Redis", "MySQL 索引", "项目压测", "接口性能优化")));
        if (skills.contains("AI Agent") || skills.contains("Python")) {
            items.add(fallbackItem("AI 应用开发实习生", skills, 78,
                    List.of("简历中出现 AI 或大模型相关信号", "可以把 Agent 项目作为差异化亮点"),
                    List.of("RAG", "Prompt 设计", "向量数据库", "模型接口调用")));
        }
        items.add(fallbackItem("软件开发实习生", skills, 74,
                List.of("通用软件开发岗位对技术栈包容度更高", "适合用项目完整度和工程能力争取面试"),
                List.of("数据结构", "操作系统", "网络基础", "代码规范")));

        return new JobRecommendationResponse(
                resume.getId(),
                resume.getTitle(),
                "DeepSeek 返回内容已保留，系统同时生成了可展示的岗位方向兜底结果。",
                items,
                List.of("建议补充项目量化结果", "准备数据库、缓存、接口设计和异常处理追问"),
                List.of("Java 后端 实习", "Spring Boot 实习", "软件开发 实习", "AI 应用开发 实习"),
                rawAdvice,
                LocalDateTime.now()
        );
    }

    private JobRecommendationItem fallbackItem(String roleTitle,
                                               List<String> skills,
                                               int fitScore,
                                               List<String> reasons,
                                               List<String> missingSkills) {
        return new JobRecommendationItem(
                roleTitle,
                fitScore,
                normalizeLevel(null, fitScore),
                reasons,
                limitStrings(skills, 8),
                missingSkills,
                List.of(roleTitle, roleTitle.replace("实习生", "实习"), roleTitle.replace("实习生", "校招")),
                List.of("把项目描述整理成背景、方案、结果三段", "准备 2 到 3 个能深入追问的项目技术点"),
                null
        );
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("AI 返回内容为空");
        }
        String value = text.strip();
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI 返回内容不是 JSON 对象");
        }
        return value.substring(start, end + 1);
    }

    private String normalizeLevel(String value, int score) {
        if (value != null && !value.isBlank()) {
            return value.strip();
        }
        if (score >= 85) {
            return "高";
        }
        if (score >= 70) {
            return "中";
        }
        return "挑战";
    }

    private List<String> limitStrings(List<String> values, int limit) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : safeList(values)) {
            if (value != null && !value.isBlank()) {
                result.add(value.strip());
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.strip();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_RESUME_CHARS) {
            return value;
        }
        return value.substring(0, MAX_RESUME_CHARS) + "\n...（简历内容过长，已截断）";
    }

    private record RecommendationPayload(
            String summary,
            List<JobRecommendationItem> recommendations,
            List<String> overallGaps,
            List<String> searchKeywords
    ) {
    }
}
