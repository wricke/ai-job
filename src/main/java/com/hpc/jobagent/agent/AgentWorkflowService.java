package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import org.springframework.stereotype.Service;

@Service
public class AgentWorkflowService {

    private final ResumeParserAgent resumeParserAgent;
    private final JobAnalyzerAgent jobAnalyzerAgent;
    private final MatchScoringAgent matchScoringAgent;
    private final SuggestionAgent suggestionAgent;
    private final InterviewAgent interviewAgent;

    public AgentWorkflowService(ResumeParserAgent resumeParserAgent,
                                JobAnalyzerAgent jobAnalyzerAgent,
                                MatchScoringAgent matchScoringAgent,
                                SuggestionAgent suggestionAgent,
                                InterviewAgent interviewAgent) {
        this.resumeParserAgent = resumeParserAgent;
        this.jobAnalyzerAgent = jobAnalyzerAgent;
        this.matchScoringAgent = matchScoringAgent;
        this.suggestionAgent = suggestionAgent;
        this.interviewAgent = interviewAgent;
    }

    public AgentReport run(ResumeProfile resume, JobPosting job) {
        List<AgentStepTrace> trace = new ArrayList<>();
        ResumeInsight resumeInsight = timed(trace, "ResumeParserAgent",
                () -> resumeParserAgent.analyze(resume));
        JobInsight jobInsight = timed(trace, "JobAnalyzerAgent",
                () -> jobAnalyzerAgent.analyze(job));
        MatchDetail matchDetail = timed(trace, "MatchScoringAgent",
                () -> matchScoringAgent.score(resumeInsight, jobInsight));
        SuggestionPack suggestions = timed(trace, "SuggestionAgent",
                () -> suggestionAgent.generate(resumeInsight, jobInsight, matchDetail));
        InterviewPack interview = timed(trace, "InterviewAgent",
                () -> interviewAgent.generate(resumeInsight, jobInsight, matchDetail));

        String summary = "匹配度 " + matchDetail.score() + " 分。"
                + "优势：" + String.join("；", resumeInsight.strengths())
                + "。主要建议：" + suggestions.resumeImprovements().get(0);
        return new AgentReport(summary, resumeInsight, jobInsight, matchDetail, suggestions, interview, trace);
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
