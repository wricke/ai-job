package com.hpc.jobagent.ai;

public interface AiClient {

    String complete(String systemPrompt, String userPrompt);

    String providerName();
}
