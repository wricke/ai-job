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
        questions.add("请介绍一个你最熟悉的后端项目，你负责了哪些模块？");
        questions.add("MySQL 索引为什么能提升查询性能？什么情况下索引会失效？");
        questions.add("Redis 缓存和数据库数据不一致时，你会怎么处理？");
        questions.add("Spring Boot 接口从请求进来到返回响应，中间大概经历了哪些过程？");
        questions.add("如果多个用户同时提交同一个操作，如何避免重复写入或数据冲突？");
        if (job.requiredSkills().contains("RabbitMQ")) {
            questions.add("为什么要使用消息队列？如何保证消息不丢失或不重复消费？");
        }
        if (job.requiredSkills().contains("AI Agent")) {
            questions.add("你的 Agent 项目里，为什么要拆成多个步骤？每一步的输入输出是什么？");
        }

        List<String> talkingPoints = new ArrayList<>();
        talkingPoints.add("先讲业务问题，再讲技术方案，最后讲结果和反思。");
        talkingPoints.add("遇到不会的问题，可以先说自己的理解边界，再给出排查思路。");
        talkingPoints.add("把匹配技能串到项目里讲，不要只停留在技能列表。");

        String aiAdvice = aiClient.complete(
                "interview: generate interview preparation advice for a backend internship candidate in Chinese. Use plain text only. Do not use Markdown markers such as **, ###, bullet headings, or code fences.",
                "requiredSkills=" + job.requiredSkills() + ", risks=" + match.risks()
        );
        return new InterviewPack(questions, talkingPoints, aiAdvice);
    }
}
