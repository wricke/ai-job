package com.hpc.jobagent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.junit.jupiter.api.Test;

class AgentWorkflowServiceTest {

    @Test
    void workflowRunsThreeCoarseGrainedAgents() {
        AiClient aiClient = new StubAiClient();
        TextAnalyzer textAnalyzer = new TextAnalyzer();
        AgentWorkflowService service = new AgentWorkflowService(
                new ProfileAnalysisAgent(textAnalyzer, aiClient),
                new MatchEvaluationAgent(),
                new CareerAdviceAgent(aiClient)
        );

        AgentReport report = service.run(resume(), job());

        assertThat(report.trace())
                .extracting(AgentStepTrace::agentName)
                .containsExactly("ProfileAnalysisAgent", "MatchEvaluationAgent", "CareerAdviceAgent");
        assertThat(report.resumeInsight().skills()).contains("供应链运营", "采购协调");
        assertThat(report.jobInsight().requiredSkills()).contains("供应链运营", "供应商沟通");
        assertThat(report.suggestions().resumeImprovements()).isNotEmpty();
        assertThat(report.interviewQuestions().questions()).isNotEmpty();
    }

    private ResumeProfile resume() {
        ResumeProfile resume = new ResumeProfile();
        resume.setTitle("供应链运营方向简历");
        resume.setTargetRole("供应链运营实习");
        resume.setContent("""
                目标岗位：供应链运营实习。
                熟悉采购协调、库存周转分析，使用 Excel 做每周补货报表。
                项目经历：校园物资采购优化项目，负责需求汇总、供应商比价和交付进度跟踪。
                """);
        return resume;
    }

    private JobPosting job() {
        JobPosting job = new JobPosting();
        job.setTitle("供应链运营实习");
        job.setDescription("""
                岗位职责：负责补货计划、供应商沟通和库存周转分析。
                要求熟悉供应链运营、供应商沟通、数据报表。
                """);
        return job;
    }

    private static class StubAiClient implements AiClient {

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return "AI 输出";
        }

        @Override
        public String providerName() {
            return "stub";
        }
    }
}
