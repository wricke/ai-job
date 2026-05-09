package com.hpc.jobagent.agent;

public record AgentStepTrace(
        String agentName,
        String status,
        long durationMs,
        String detail
) {
}
