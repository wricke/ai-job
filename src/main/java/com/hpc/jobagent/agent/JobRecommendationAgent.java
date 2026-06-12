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
            return fallback(resume, skills, projects, rawAdvice);
        }
    }

    private String systemPrompt() {
        return """
                你是一个面向中国校招和实习求职的职业规划助手。
                你需要只根据用户简历中的真实专业、目标岗位、工具、作品、项目和经历，推荐适合投递的岗位方向，并给出适配度、理由、短板、搜索关键词和准备建议。
                不要默认推荐任何固定岗位方向；只有简历明确体现相关经历、技能或目标时才推荐。
                请只返回合法 JSON，不要 Markdown，不要代码块，不要额外解释。JSON 字符串内容也不要包含 **、###、``` 等 Markdown 标记。
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
            return fallback(
                    resume,
                    textAnalyzer.findSkills(resume.getContent()),
                    textAnalyzer.extractProjectSignals(resume.getContent()),
                    rawAdvice
            );
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

    private JobRecommendationResponse fallback(ResumeProfile resume,
                                               List<String> skills,
                                               List<String> projects,
                                               String rawAdvice) {
        List<JobRecommendationItem> items = new ArrayList<>();
        int score = 82;
        for (String direction : fallbackDirections(resume, skills)) {
            items.add(fallbackItem(direction, skills, projects, score));
            score = Math.max(68, score - 5);
            if (items.size() >= 6) {
                break;
            }
        }

        return new JobRecommendationResponse(
                resume.getId(),
                resume.getTitle(),
                "AI 返回内容已保留，系统已根据目标岗位、技能和经历信号生成兜底推荐。",
                items,
                fallbackGaps(skills, projects, items),
                fallbackKeywords(items),
                rawAdvice,
                LocalDateTime.now()
        );
    }

    private JobRecommendationItem fallbackItem(String roleTitle,
                                               List<String> skills,
                                               List<String> projects,
                                               int fitScore) {
        List<String> matchedSkills = limitStrings(skills, 8);
        List<String> reasons = new ArrayList<>();
        reasons.add("简历或目标岗位中出现「" + roleTitle + "」相关信号，可作为投递方向继续验证。");
        if (!matchedSkills.isEmpty()) {
            reasons.add("已提取到可复用能力关键词：" + String.join("、", limitStrings(matchedSkills, 5)));
        }
        if (!projects.isEmpty()) {
            reasons.add("可结合经历「" + shortText(projects.get(0), 50) + "」说明职责、过程和结果。");
        }

        return new JobRecommendationItem(
                roleTitle,
                fitScore,
                normalizeLevel(null, fitScore),
                limitStrings(reasons, 5),
                matchedSkills,
                fallbackMissingSkills(roleTitle, matchedSkills, projects),
                fallbackSearchKeywords(roleTitle, matchedSkills),
                fallbackPreparationTips(roleTitle, matchedSkills, projects),
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

    private List<String> fallbackDirections(ResumeProfile resume, List<String> skills) {
        Set<String> directions = new LinkedHashSet<>();
        addDirection(directions, resume.getTargetRole());
        addDirection(directions, resume.getTitle());
        for (String skill : safeList(skills)) {
            addDirection(directions, skill);
            if (directions.size() >= 6) {
                break;
            }
        }
        if (directions.isEmpty()) {
            directions.add("目标岗位方向");
        }
        return new ArrayList<>(directions);
    }

    private void addDirection(Set<String> directions, String value) {
        String direction = cleanDirection(value);
        if (!direction.isBlank()) {
            directions.add(direction + "方向");
        }
    }

    private String cleanDirection(String value) {
        String direction = value == null ? "" : value.strip()
                .replaceAll("(简历|求职|个人|方向)+$", "")
                .replaceAll("(实习生?|校招|岗位|职位|工作)$", "")
                .replaceAll("[：:，,。；;]+$", "")
                .strip();
        if (direction.length() > 24) {
            direction = direction.substring(0, 24).strip();
        }
        return direction;
    }

    private List<String> fallbackMissingSkills(String roleTitle, List<String> matchedSkills, List<String> projects) {
        List<String> gaps = new ArrayList<>();
        gaps.add("补充「" + roleTitle + "」相关 JD 中反复出现但简历未覆盖的要求");
        if (!matchedSkills.isEmpty()) {
            gaps.add("为「" + matchedSkills.get(0) + "」补充可验证结果或交付物");
        }
        if (!projects.isEmpty()) {
            gaps.add("完善「" + shortText(projects.get(0), 36) + "」的量化结果和复盘结论");
        }
        return limitStrings(gaps, 6);
    }

    private List<String> fallbackPreparationTips(String roleTitle, List<String> matchedSkills, List<String> projects) {
        List<String> tips = new ArrayList<>();
        tips.add("围绕「" + roleTitle + "」准备 2 到 3 个能深入追问的经历案例");
        if (!projects.isEmpty()) {
            tips.add("把「" + shortText(projects.get(0), 36) + "」整理成背景、职责、动作、结果四段");
        }
        if (!matchedSkills.isEmpty()) {
            tips.add("把「" + String.join("、", limitStrings(matchedSkills, 3)) + "」分别对应到具体经历证据");
        }
        return limitStrings(tips, 6);
    }

    private List<String> fallbackGaps(List<String> skills, List<String> projects, List<JobRecommendationItem> items) {
        List<String> gaps = new ArrayList<>();
        String direction = items.isEmpty() ? "目标岗位方向" : items.get(0).roleTitle();
        gaps.add("对照「" + direction + "」补充 JD 中出现频率最高的要求");
        if (skills == null || skills.isEmpty()) {
            gaps.add("在简历中明确写出工具、方法或专业能力关键词");
        } else {
            gaps.add("把「" + String.join("、", limitStrings(skills, 3)) + "」写进具体经历的动作和结果里");
        }
        if (projects == null || projects.isEmpty()) {
            gaps.add("补充一段能支撑「" + direction + "」的代表性经历");
        } else {
            gaps.add("为「" + shortText(projects.get(0), 36) + "」补充量化结果、交付物或复盘结论");
        }
        return limitStrings(gaps, 6);
    }

    private List<String> fallbackKeywords(List<JobRecommendationItem> items) {
        Set<String> keywords = new LinkedHashSet<>();
        for (JobRecommendationItem item : items) {
            keywords.addAll(item.searchKeywords());
        }
        return new ArrayList<>(keywords);
    }

    private List<String> fallbackSearchKeywords(String roleTitle, List<String> matchedSkills) {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.add(roleTitle);
        String baseDirection = roleTitle.replace("方向", "").strip();
        if (!baseDirection.isBlank()) {
            keywords.add(baseDirection);
        }
        for (String skill : matchedSkills) {
            keywords.add(skill);
            if (keywords.size() >= 8) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private String shortText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
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
