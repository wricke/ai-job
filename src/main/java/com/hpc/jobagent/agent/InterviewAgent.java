package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import org.springframework.stereotype.Component;

@Component
public class InterviewAgent {

    private final AiClient aiClient;

    public InterviewAgent(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public InterviewPack generate(ResumeInsight resume, JobInsight job, MatchDetail match) {
        List<String> questions = new ArrayList<>();
        questions.add("请介绍一段最能体现你岗位匹配度的作品、项目或实习经历，你具体负责了什么？");
        questions.add("这个经历里最难的部分是什么？你是如何分析、推进和复盘的？");
        questions.add("如果重新做一次，你会如何优化过程或最终产出？");
        if (containsAny(job.requiredSkills(), "平面设计", "视觉设计", "UI 设计", "交互设计", "Photoshop")) {
            questions.add("请说明一个设计作品的目标用户、视觉思路、版式选择和最终效果。");
        }
        if (containsAny(job.requiredSkills(), "三维建模", "SketchUp", "3ds Max", "Maya", "VR 交互")) {
            questions.add("请介绍一个三维或 VR 项目的建模流程、空间表现和交互设计思路。");
        }
        if (containsAny(job.requiredSkills(), "Premiere", "After Effects", "视频剪辑")) {
            questions.add("请介绍一个视频或后期作品的剪辑节奏、视觉包装和交付标准。");
        }
        if (containsAny(job.requiredSkills(), "Java", "Spring Boot", "MyBatis")) {
            questions.add("请介绍一个你最熟悉的软件项目，你负责了哪些模块？");
            questions.add("Spring Boot 接口从请求进来到返回响应，中间大概经历了哪些过程？");
        }
        if (job.requiredSkills().contains("MySQL")) {
            questions.add("MySQL 索引为什么能提升查询性能？什么情况下索引会失效？");
        }
        if (job.requiredSkills().contains("Redis")) {
            questions.add("Redis 缓存和数据库数据不一致时，你会怎么处理？");
        }
        if (job.requiredSkills().contains("RabbitMQ")) {
            questions.add("为什么要使用消息队列？如何保证消息不丢失或不重复消费？");
        }
        if (job.requiredSkills().contains("AI Agent")) {
            questions.add("你的 Agent 项目里，为什么要拆成多个步骤？每一步的输入输出是什么？");
        }

        List<String> talkingPoints = new ArrayList<>();
        talkingPoints.add("先讲目标和背景，再讲自己的职责、方法和结果。");
        talkingPoints.add("遇到不会的问题，可以先说自己的理解边界，再给出排查思路。");
        talkingPoints.add("把匹配能力串到作品、项目或实习经历里讲，不要只停留在技能列表。");

        String aiAdvice = aiClient.complete(
                "interview: generate interview preparation advice for a candidate in Chinese. Base questions on the actual requiredSkills and risks. Do not assume backend/software direction unless requiredSkills clearly contains backend technologies. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "requiredSkills=" + job.requiredSkills() + ", risks=" + match.risks()
        );
        return new InterviewPack(questions, talkingPoints, aiAdvice);
    }

    private boolean containsAny(List<String> values, String... targets) {
        for (String target : targets) {
            if (values.contains(target)) {
                return true;
            }
        }
        return false;
    }
}
