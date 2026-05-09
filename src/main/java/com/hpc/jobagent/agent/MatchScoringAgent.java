package com.hpc.jobagent.agent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class MatchScoringAgent {

    public MatchDetail score(ResumeInsight resume, JobInsight job) {
        Set<String> resumeSkills = new LinkedHashSet<>(resume.skills());
        Set<String> requiredSkills = new LinkedHashSet<>(job.requiredSkills());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : requiredSkills) {
            if (resumeSkills.contains(skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int baseScore = requiredSkills.isEmpty() ? 65 : 35 + matched.size() * 55 / Math.max(requiredSkills.size(), 1);
        int projectBonus = Math.min(10, resume.projects().size() * 2);
        int score = Math.max(0, Math.min(100, baseScore + projectBonus));

        List<String> risks = new ArrayList<>();
        if (!missing.isEmpty()) {
            risks.add("JD 中出现但简历未明显体现：" + String.join("、", missing));
        }
        if (resume.projects().size() < 2) {
            risks.add("项目数量偏少，建议补充一个更贴近目标岗位的 AI Agent 或业务系统项目");
        }
        if (!resumeSkills.contains("MySQL")) {
            risks.add("数据库能力是后端实习常见考点，简历需要体现表设计、索引或事务");
        }
        if (risks.isEmpty()) {
            risks.add("主要风险较低，重点准备项目细节追问即可");
        }

        String reason = "匹配技能 " + matched.size() + "/" + Math.max(requiredSkills.size(), 1)
                + "，项目表达带来 " + projectBonus + " 分加成。";
        return new MatchDetail(score, matched, missing, risks, reason);
    }
}
