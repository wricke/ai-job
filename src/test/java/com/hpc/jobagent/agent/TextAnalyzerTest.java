package com.hpc.jobagent.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextAnalyzerTest {

    private final TextAnalyzer analyzer = new TextAnalyzer();

    @Test
    void extractsDesignSkillsWithoutFalseAiAgent() {
        String resume = """
                数字媒体技术专业，主攻视觉/交互设计，掌握 SketchUp、Mars、3ds Max、Maya、PS、PR、AE。
                项目经历包括 VR 非遗油纸伞古镇和平面设计作品。
                """;

        assertThat(analyzer.findSkills(resume))
                .contains("视觉设计", "交互设计", "SketchUp", "3ds Max", "Maya", "Photoshop", "Premiere", "After Effects", "VR 交互")
                .doesNotContain("AI Agent", "Java", "Spring Boot", "MySQL");
    }
}
