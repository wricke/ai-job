package com.hpc.jobagent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.JobRecommendationResponse;
import org.junit.jupiter.api.Test;

class JobRecommendationAgentTest {

    private final JobRecommendationAgent agent = new JobRecommendationAgent(
            new TextAnalyzer(),
            new InvalidJsonAiClient(),
            new ObjectMapper()
    );

    @Test
    void fallbackUsesResumeSignalsInsteadOfFixedInternTitles() {
        ResumeProfile resume = new ResumeProfile();
        resume.setId(9L);
        resume.setTitle("供应链运营方向简历");
        resume.setTargetRole("供应链运营实习");
        resume.setContent("""
                目标岗位：供应链运营实习。
                熟悉采购协调、库存周转分析、供应商沟通，使用 Excel 和 Power BI 做每周补货报表。
                项目经历：校园物资采购优化项目，负责需求汇总、供应商比价和交付进度跟踪。
                """);

        JobRecommendationResponse response = agent.recommend(resume);

        assertThat(response.recommendations())
                .isNotEmpty()
                .anySatisfy(item -> assertThat(item.roleTitle()).contains("供应链运营"));
        assertThat(response.recommendations())
                .allSatisfy(item -> {
                    assertThat(item.roleTitle()).doesNotEndWith("实习生");
                    assertThat(String.join("\n", item.missingSkills())).contains("供应链运营");
                    assertThat(String.join("\n", item.preparationTips())).contains("供应链运营");
                });
        assertThat(String.join("\n", response.overallGaps()))
                .contains("采购协调", "校园物资采购优化项目")
                .doesNotContain("目标岗位高频关键词", "代表性经历证据", "量化结果或交付物说明");
    }

    private static class InvalidJsonAiClient implements AiClient {

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return "not json";
        }

        @Override
        public String providerName() {
            return "stub";
        }
    }
}
