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
        if (!requiredSkills.isEmpty() && matched.isEmpty()) {
            risks.add("简历与岗位要求的显性关键词重合较少，建议优先调整目标方向或补充更相关的作品/项目经历");
        }
        if (resume.projects().size() < 2) {
            risks.add("项目、作品或实践经历数量偏少，建议补充一个更贴近目标岗位的代表性案例");
        }
        if (requiredSkills.isEmpty()) {
            risks.add("岗位要求中可识别关键词较少，建议补充更明确的 JD 或目标岗位描述以提升匹配准确度");
        }
        if (risks.isEmpty()) {
            risks.add("主要风险较低，重点准备项目细节追问即可");
        }

        String reason = "匹配关键词 " + matched.size() + "/" + Math.max(requiredSkills.size(), 1)
                + "，项目表达带来 " + projectBonus + " 分加成。";
        return new MatchDetail(score, matched, missing, risks, reason);
    }
}
