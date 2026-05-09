package com.hpc.jobagent.ai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.hpc.jobagent.config.AgentProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ChatCompletionAiClient implements AiClient {

    private final AgentProperties properties;
    private final RestClient restClient;

    public ChatCompletionAiClient(AgentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getAi().getBaseUrl()).build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String apiKey = properties.getAi().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("需要配置 DEEPSEEK_API_KEY 环境变量");
        }
        Map<String, Object> body = Map.of(
                "model", properties.getAi().getModel(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("choices") || response.path("choices").isEmpty()) {
            throw new IllegalStateException("LLM response has no choices");
        }
        return response.path("choices").get(0).path("message").path("content").asText();
    }

    @Override
    public String providerName() {
        return "deepseek:" + properties.getAi().getModel();
    }
}
