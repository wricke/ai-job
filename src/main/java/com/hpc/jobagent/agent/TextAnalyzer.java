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
        SKILL_ALIASES.put("Go", List.of("golang", " go ", "go语言", "go 编程"));
        SKILL_ALIASES.put("AI Agent", List.of("agent", "langchain", "大模型", "llm", "ai"));
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
        return pickSentences(text, List.of("项目", "系统", "平台", "负责", "实现", "优化", "缓存", "秒杀"), 6);
    }

    public List<String> extractResponsibilities(String text) {
        return pickSentences(text, List.of("岗位职责", "职责", "负责", "参与", "开发", "优化", "设计"), 6);
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
