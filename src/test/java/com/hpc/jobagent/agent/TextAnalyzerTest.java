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
                .contains("视觉设计", "交互设计", "SketchUp", "3ds Max", "Maya", "PS", "PR", "AE", "VR")
                .doesNotContain("AI Agent", "Java", "Spring Boot", "MySQL");
    }

    @Test
    void extractsNewDomainSignalsWithoutAddingAliasEntries() {
        String resume = """
                目标岗位：供应链运营实习。
                熟悉采购协调、库存周转分析、供应商沟通，使用 Excel 和 Power BI 做每周补货报表。
                项目经历：校园物资采购优化项目，负责需求汇总、供应商比价和交付进度跟踪。
                """;

        assertThat(analyzer.findSkills(resume))
                .contains("供应链运营", "采购协调", "库存周转分析", "供应商沟通", "Excel", "Power BI");
    }
}
