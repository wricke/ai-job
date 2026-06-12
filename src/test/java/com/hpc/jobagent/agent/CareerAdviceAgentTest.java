package com.hpc.jobagent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.hpc.jobagent.ai.AiClient;
import org.junit.jupiter.api.Test;

class CareerAdviceAgentTest {

    private final CareerAdviceAgent agent = new CareerAdviceAgent(new StubAiClient());

    @Test
    void adviceIsBuiltFromActualSignalsWithoutRoleSpecificDefaults() {
        CareerAdvice advice = agent.generate(
                new ResumeInsight(
                        List.of("采购协调", "库存周转分析"),
                        List.of("校园物资采购优化项目"),
                        List.of("目标岗位方向较明确：供应链运营实习"),
                        "简历摘要"),
                new JobInsight(
                        List.of("供应链运营", "供应商沟通"),
                        List.of("负责补货计划、供应商跟进和库存周转分析"),
                        List.of("Power BI"),
                        "岗位摘要"),
                new MatchDetail(
                        78,
                        List.of("库存周转分析"),
                        List.of("供应商沟通"),
                        List.of("供应商沟通在简历中体现较少"),
                        "匹配说明")
        );

        String output = String.join("\n",
                String.join("\n", advice.suggestions().resumeImprovements()),
                String.join("\n", advice.suggestions().projectRewriteTips()),
                String.join("\n", advice.suggestions().learningPlan()),
                String.join("\n", advice.interview().questions())
        );

        assertThat(output)
                .contains("供应链运营", "供应商沟通", "校园物资采购优化项目")
                .doesNotContain("设计/媒体", "视觉/交互", "后端", "Spring Boot", "Redis");
    }

    @Test
    void javaOnlySkillDoesNotInventSpringBootQuestions() {
        CareerAdvice advice = agent.generate(
                new ResumeInsight(
                        List.of("Java"),
                        List.of("课程管理系统"),
                        List.of("有课程项目实践"),
                        "候选人有 Java 基础"),
                new JobInsight(
                        List.of("Java"),
                        List.of("参与业务功能开发"),
                        List.of(),
                        "岗位要求 Java 基础"),
                new MatchDetail(
                        80,
                        List.of("Java"),
                        List.of(),
                        List.of(),
                        "Java 技能匹配")
        );

        String questions = String.join("\n", advice.interview().questions());

        assertThat(questions)
                .contains("Java")
                .doesNotContain("Spring Boot", "MySQL", "Redis", "接口从请求进来到返回响应");
    }

    private static class StubAiClient implements AiClient {

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return "AI 建议";
        }

        @Override
        public String providerName() {
            return "stub";
        }
    }
}
