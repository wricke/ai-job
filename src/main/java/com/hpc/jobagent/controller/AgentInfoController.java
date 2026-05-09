package com.hpc.jobagent.controller;

import java.util.Map;

import com.hpc.jobagent.ai.AiClient;
import com.hpc.jobagent.config.AgentProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentInfoController {

    private final AiClient aiClient;
    private final AgentProperties properties;

    public AgentInfoController(AiClient aiClient, AgentProperties properties) {
        this.aiClient = aiClient;
        this.properties = properties;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "provider", aiClient.providerName(),
                "cacheEnabled", properties.getCache().isEnabled(),
                "workflow", new String[]{
                        "ResumeParserAgent",
                        "JobAnalyzerAgent",
                        "MatchScoringAgent",
                        "SuggestionAgent",
                        "InterviewAgent"
                }
        );
    }
}
