package com.hpc.jobagent.agent;

import java.util.List;

public record InterviewPack(
        List<String> questions,
        List<String> talkingPoints,
        String aiAdvice
) {
}
