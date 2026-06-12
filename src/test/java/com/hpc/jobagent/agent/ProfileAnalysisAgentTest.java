package com.hpc.jobagent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.junit.jupiter.api.Test;

class ProfileAnalysisAgentTest {

    private final ProfileAnalysisAgent agent = new ProfileAnalysisAgent(new TextAnalyzer(), new StubAiClient());

    @Test
    void resumeStrengthsAreBuiltFromExtractedSignals() {
        ProfileAnalysis analysis = agent.analyze(resume(), job());

        assertThat(analysis.resumeInsight().skills())
                .contains("供应链运营", "采购协调", "库存周转分析", "供应商沟通");
        assertThat(String.join("\n", analysis.resumeInsight().strengths()))
                .contains("供应链运营", "采购协调")
                .doesNotContain("Java Web 后端", "视觉传达", "三维建模");
    }

    private ResumeProfile resume() {
        ResumeProfile resume = new ResumeProfile();
        resume.setTargetRole("供应链运营实习");
        resume.setContent("""
                目标岗位：供应链运营实习。
                熟悉采购协调、库存周转分析、供应商沟通，使用 Excel 和 Power BI 做每周补货报表。
                项目经历：校园物资采购优化项目，负责需求汇总、供应商比价和交付进度跟踪。
                """);
        return resume;
    }

    private JobPosting job() {
        JobPosting job = new JobPosting();
        job.setTitle("供应链运营实习");
        job.setDescription("岗位职责：负责补货计划、供应商沟通和库存周转分析。");
        return job;
    }

    private static class StubAiClient implements AiClient {

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            return "AI 摘要";
        }

        @Override
        public String providerName() {
            return "stub";
        }
    }
}
