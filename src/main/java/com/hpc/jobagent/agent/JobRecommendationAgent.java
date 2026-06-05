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
                你是一个面向中国校招和实习求职的职业规划助手。
                你需要只根据用户简历中的真实专业、目标岗位、工具、作品、项目和经历，推荐适合投递的岗位方向，并给出适配度、理由、短板、搜索关键词和准备建议。
                不要默认推荐后端、软件开发或 AI 方向；只有简历明确体现相关技术经历时才推荐。
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
        String text = (resume.getTargetRole() + "\n" + resume.getContent()).toLowerCase();
        if (isDesignProfile(skills, text)) {
            items.add(fallbackItem("视觉设计实习生", skills, 84,
                    List.of("简历体现平面设计、视觉表达或 Adobe 工具基础", "适合用作品集展示版式、色彩和项目产出"),
                    List.of("作品集完整度", "品牌视觉规范", "商业项目复盘")));
            items.add(fallbackItem("交互/UI 设计实习生", skills, 76,
                    List.of("简历出现交互设计、界面设计或数字媒体相关经历", "可以用 App、小程序或交互作品证明设计思路"),
                    List.of("用户调研", "原型设计", "可用性测试")));
        }
        if (isThreeDProfile(skills)) {
            items.add(fallbackItem("三维/空间设计实习生", skills, 82,
                    List.of("简历体现 SketchUp、3ds Max、Maya 或三维建模能力", "适合用三维作品展示建模、材质、空间表现和最终效果"),
                    List.of("渲染表现", "作品集排版", "项目落地说明")));
            items.add(fallbackItem("VR 交互设计实习生", skills, 78,
                    List.of("简历出现 VR 交互或数字媒体项目经历", "适合强调沉浸式体验、交互流程和场景设计"),
                    List.of("交互原型", "用户体验说明", "项目演示视频")));
        }
        if (isMediaProfile(skills)) {
            items.add(fallbackItem("新媒体视觉/视频剪辑实习生", skills, 76,
                    List.of("简历体现视频剪辑、后期或内容制作能力", "适合用短视频、宣传物料和视觉包装作品证明执行力"),
                    List.of("镜头节奏", "数据复盘", "内容策划")));
        }
        if (containsAny(skills, "Python", "数据分析")) {
            items.add(fallbackItem("数据分析实习生", skills, 74,
                    List.of("简历体现数据处理或分析工具基础", "适合用课程项目、报表或可视化作品证明分析能力"),
                    List.of("Excel/SQL 分析案例", "数据可视化", "业务指标理解")));
        }
        if (isBackendProfile(skills)) {
            items.add(fallbackItem("Java 后端开发实习生", skills, 82,
                    List.of("简历技能与后端开发岗位方向接近", "适合继续围绕 Spring Boot、MySQL、接口开发打磨项目表达"),
                    List.of("Redis", "MySQL 索引", "接口性能优化")));
        }
        if (skills.contains("AI Agent")) {
            items.add(fallbackItem("AI 应用开发实习生", skills, 78,
                    List.of("简历中出现 AI Agent 或大模型相关信号", "可以把相关项目作为差异化亮点"),
                    List.of("RAG", "Prompt 设计", "向量数据库", "模型接口调用")));
        }
        if (items.isEmpty()) {
            items.add(fallbackItem("综合类实习岗位", skills, 68,
                    List.of("简历中已有可迁移的学习、项目或实践经历", "建议结合目标岗位进一步补充关键词和成果说明"),
                    List.of("目标岗位关键词", "代表性项目", "量化结果")));
        }

        return new JobRecommendationResponse(
                resume.getId(),
                resume.getTitle(),
                "DeepSeek 返回内容已保留，系统同时生成了可展示的岗位方向兜底结果。",
                items,
                fallbackGaps(skills, text),
                fallbackKeywords(items),
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

    private boolean isDesignProfile(List<String> skills, String text) {
        return containsAny(skills, "平面设计", "视觉设计", "UI 设计", "交互设计", "Photoshop", "Illustrator")
                || text.contains("平面设计")
                || text.contains("视觉")
                || text.contains("数字媒体")
                || text.contains("交互设计")
                || text.contains("ui")
                || text.contains("作品集")
                || text.contains("海报")
                || text.contains("版式");
    }

    private boolean isThreeDProfile(List<String> skills) {
        return containsAny(skills, "三维建模", "SketchUp", "3ds Max", "Maya", "Mars", "VR 交互");
    }

    private boolean isMediaProfile(List<String> skills) {
        return containsAny(skills, "Premiere", "After Effects", "视频剪辑");
    }

    private boolean isBackendProfile(List<String> skills) {
        return containsAny(skills, "Java", "Spring Boot", "MyBatis", "MySQL", "Redis", "RabbitMQ");
    }

    private boolean containsAny(List<String> values, String... targets) {
        for (String target : targets) {
            if (values.contains(target)) {
                return true;
            }
        }
        return false;
    }

    private List<String> fallbackGaps(List<String> skills, String text) {
        if (isDesignProfile(skills, text) || isThreeDProfile(skills) || isMediaProfile(skills)) {
            return List.of("建议补充作品集链接或作品截图", "为每个作品补充目标、职责、工具、过程和结果", "准备 2 到 3 个可深入讲解的代表性作品");
        }
        if (isBackendProfile(skills)) {
            return List.of("建议补充项目量化结果", "准备数据库、接口设计和异常处理追问");
        }
        return List.of("建议明确目标岗位方向", "补充与目标岗位相关的关键词、项目和量化成果");
    }

    private List<String> fallbackKeywords(List<JobRecommendationItem> items) {
        Set<String> keywords = new LinkedHashSet<>();
        for (JobRecommendationItem item : items) {
            keywords.add(item.roleTitle());
            keywords.add(item.roleTitle().replace("实习生", "实习"));
        }
        return new ArrayList<>(keywords);
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
