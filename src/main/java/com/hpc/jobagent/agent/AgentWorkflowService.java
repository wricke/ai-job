package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.springframework.stereotype.Service;

@Service
public class AgentWorkflowService {

    private final ProfileAnalysisAgent profileAnalysisAgent;
    private final MatchEvaluationAgent matchEvaluationAgent;
    private final CareerAdviceAgent careerAdviceAgent;

    public AgentWorkflowService(ProfileAnalysisAgent profileAnalysisAgent,
                                MatchEvaluationAgent matchEvaluationAgent,
                                CareerAdviceAgent careerAdviceAgent) {
        this.profileAnalysisAgent = profileAnalysisAgent;
        this.matchEvaluationAgent = matchEvaluationAgent;
        this.careerAdviceAgent = careerAdviceAgent;
    }

    public AgentReport run(ResumeProfile resume, JobPosting job) {
        List<AgentStepTrace> trace = new ArrayList<>();
        ProfileAnalysis profile = timed(trace, "ProfileAnalysisAgent",
                () -> profileAnalysisAgent.analyze(resume, job));
        MatchDetail matchDetail = timed(trace, "MatchEvaluationAgent",
                () -> matchEvaluationAgent.evaluate(profile.resumeInsight(), profile.jobInsight()));
        CareerAdvice advice = timed(trace, "CareerAdviceAgent",
                () -> careerAdviceAgent.generate(profile.resumeInsight(), profile.jobInsight(), matchDetail));

        String summary = "匹配度 " + matchDetail.score() + " 分。"
                + "优势：" + String.join("；", profile.resumeInsight().strengths())
                + "。主要建议：" + advice.suggestions().resumeImprovements().get(0);
        return new AgentReport(
                summary,
                profile.resumeInsight(),
                profile.jobInsight(),
                matchDetail,
                advice.suggestions(),
                advice.interview(),
                trace
        );
    }

    private <T> T timed(List<AgentStepTrace> trace, String agentName, Supplier<T> supplier) {
        long start = System.nanoTime();
        try {
            T result = supplier.get();
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            trace.add(new AgentStepTrace(agentName, "SUCCESS", durationMs, "step completed"));
            return result;
        } catch (RuntimeException ex) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            trace.add(new AgentStepTrace(agentName, "FAILED", durationMs, ex.getMessage()));
            throw ex;
        }
    }
}
