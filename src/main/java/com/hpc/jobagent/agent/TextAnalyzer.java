package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class TextAnalyzer {

    private static final Map<String, List<String>> SKILL_ALIASES = new LinkedHashMap<>();

    static {
        SKILL_ALIASES.put("Java", List.of("java"));
        SKILL_ALIASES.put("Spring Boot", List.of("spring boot", "springboot"));
        SKILL_ALIASES.put("Spring MVC", List.of("spring mvc", "springmvc"));
        SKILL_ALIASES.put("MyBatis", List.of("mybatis"));
        SKILL_ALIASES.put("MySQL", List.of("mysql", "sql", "数据库"));
        SKILL_ALIASES.put("Redis", List.of("redis", "缓存"));
        SKILL_ALIASES.put("RabbitMQ", List.of("rabbitmq", "mq", "消息队列"));
        SKILL_ALIASES.put("Kafka", List.of("kafka"));
        SKILL_ALIASES.put("Docker", List.of("docker", "容器"));
        SKILL_ALIASES.put("Nacos", List.of("nacos", "注册发现"));
        SKILL_ALIASES.put("Linux", List.of("linux"));
        SKILL_ALIASES.put("Git", List.of("git"));
        SKILL_ALIASES.put("RESTful API", List.of("restful", "rest api", "接口"));
        SKILL_ALIASES.put("JWT", List.of("jwt", "token"));
        SKILL_ALIASES.put("微服务", List.of("微服务", "microservice"));
        SKILL_ALIASES.put("高并发", List.of("高并发", "并发", "秒杀"));
        SKILL_ALIASES.put("Python", List.of("python"));
        SKILL_ALIASES.put("数据分析", List.of("数据分析", "数据可视化", "可视化", "tableau", "power bi", "excel"));
        SKILL_ALIASES.put("Go", List.of("golang", " go ", "go语言", "go 编程"));
        SKILL_ALIASES.put("AI Agent", List.of("ai agent", "智能体", "langchain", "大模型", "llm", "rag"));
        SKILL_ALIASES.put("平面设计", List.of("平面设计", "版式设计", "海报", "画册", "视觉传达"));
        SKILL_ALIASES.put("视觉设计", List.of("视觉设计", "视觉/", "品牌视觉", "视觉表现", "主视觉", "vi"));
        SKILL_ALIASES.put("UI 设计", List.of("ui", "界面设计", "app 界面", "小程序界面"));
        SKILL_ALIASES.put("交互设计", List.of("交互设计", "用户体验", "ux", "原型设计", "交互"));
        SKILL_ALIASES.put("三维建模", List.of("三维建模", "3d建模", "3d 建模", "建模"));
        SKILL_ALIASES.put("SketchUp", List.of("sketchup", "草图大师"));
        SKILL_ALIASES.put("3ds Max", List.of("3ds max", "3dmax"));
        SKILL_ALIASES.put("Maya", List.of("maya"));
        SKILL_ALIASES.put("Mars", List.of("mars"));
        SKILL_ALIASES.put("VR 交互", List.of("vr", "vr交互", "虚拟现实"));
        SKILL_ALIASES.put("Photoshop", List.of("photoshop", " ps ", "ps/", "ps、", "ps，", "ps。", "ps；"));
        SKILL_ALIASES.put("Illustrator", List.of("illustrator", "adobe illustrator", "矢量绘图", "矢量设计"));
        SKILL_ALIASES.put("Premiere", List.of("premiere", " pr ", "pr/", "pr、", "pr，", "pr。", "pr；"));
        SKILL_ALIASES.put("After Effects", List.of("after effects", " ae ", "ae/", "ae、", "ae，", "ae。", "ae；"));
        SKILL_ALIASES.put("视频剪辑", List.of("视频剪辑", "剪辑", "后期", "短视频"));
        SKILL_ALIASES.put("作品集", List.of("作品集", "作品展示"));
    }

    public List<String> findSkills(String text) {
        String normalized = normalize(text);
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : SKILL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalized.contains(alias.toLowerCase(Locale.ROOT))) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return new ArrayList<>(result);
    }

    public List<String> extractProjectSignals(String text) {
        return pickSentences(text, List.of("项目", "作品", "系统", "平台", "负责", "实现", "优化", "设计", "建模", "剪辑", "交互", "调研", "方案", "获奖"), 6);
    }

    public List<String> extractResponsibilities(String text) {
        return pickSentences(text, List.of("岗位职责", "职责", "负责", "参与", "开发", "优化", "设计", "制作", "策划", "运营", "调研"), 6);
    }

    public List<String> extractBonusItems(String text) {
        return pickSentences(text, List.of("优先", "加分", "熟悉", "了解", "经验"), 5);
    }

    private List<String> pickSentences(String text, List<String> keywords, int limit) {
        List<String> sentences = splitSentences(text);
        List<String> picked = new ArrayList<>();
        for (String sentence : sentences) {
            String normalized = normalize(sentence);
            for (String keyword : keywords) {
                if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    picked.add(trim(sentence, 120));
                    break;
                }
            }
            if (picked.size() >= limit) {
                break;
            }
        }
        return picked;
    }

    private List<String> splitSentences(String text) {
        String[] parts = text.replace("\r", "\n").split("[\\n。；;]");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String value = part.strip();
            if (!value.isBlank()) {
                sentences.add(value);
            }
        }
        return sentences;
    }

    private String normalize(String text) {
        return (" " + (text == null ? "" : text) + " ").toLowerCase(Locale.ROOT);
    }

    private String trim(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
