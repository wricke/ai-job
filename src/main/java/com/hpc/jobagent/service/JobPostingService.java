package com.hpc.jobagent.service;

import java.time.LocalDateTime;
import java.util.List;

import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.CreateJobRequest;
import com.hpc.jobagent.dto.JobRecommendationItem;
import com.hpc.jobagent.dto.JobResponse;
import com.hpc.jobagent.mapper.JobPostingMapper;
import com.hpc.jobagent.support.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JobPostingService {

    private final JobPostingMapper jobMapper;
    private final CurrentUserService currentUserService;

    public JobPostingService(JobPostingMapper jobMapper, CurrentUserService currentUserService) {
        this.jobMapper = jobMapper;
        this.currentUserService = currentUserService;
    }

    public JobResponse create(CreateJobRequest request) {
        Long userId = currentUserService.userId();
        LocalDateTime now = LocalDateTime.now();
        JobPosting job = new JobPosting();
        job.setUserId(userId);
        job.setCompany(request.company());
        job.setTitle(required(request.title(), "岗位名称"));
        job.setSource(request.source());
        job.setDescription(required(request.description(), "岗位要求内容"));
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insert(job);
        return JobResponse.from(job);
    }

    public JobResponse saveRecommendation(ResumeProfile resume, JobRecommendationItem recommendation) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = resume.getUserId();
        String title = required(recommendation.roleTitle(), "岗位名称");
        String source = "AI 推荐 / 简历 " + resume.getId();
        String description = recommendationDescription(recommendation);
        JobPosting job = jobMapper.findByTitleAndSource(userId, title, source);
        if (job == null) {
            job = new JobPosting();
            job.setUserId(userId);
            job.setCreatedAt(now);
        }
        job.setCompany("AI 推荐");
        job.setTitle(title);
        job.setSource(source);
        job.setDescription(description);
        job.setUpdatedAt(now);
        if (job.getId() == null) {
            jobMapper.insert(job);
        } else {
            jobMapper.update(job);
        }
        return JobResponse.from(job);
    }

    public JobResponse get(Long id) {
        return JobResponse.from(getEntity(id));
    }

    public List<JobResponse> list() {
        return jobMapper.findAllByUserId(currentUserService.userId()).stream().map(JobResponse::from).toList();
    }

    public JobPosting getEntity(Long id) {
        return getEntityForUser(id, currentUserService.userId());
    }

    public JobPosting getEntityForUser(Long id, Long userId) {
        JobPosting job = jobMapper.findByIdAndUserId(id, userId);
        if (job == null) {
            throw new NotFoundException("岗位要求不存在：" + id);
        }
        return job;
    }

    public void delete(Long id) {
        getEntity(id);
        jobMapper.deleteByIdAndUserId(id, currentUserService.userId());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.strip();
    }

    private String recommendationDescription(JobRecommendationItem item) {
        return """
                推荐岗位方向：%s
                适配度：%d 分（%s）

                推荐理由：
                %s

                岗位常见要求：
                %s

                当前简历已匹配能力：
                %s

                需要补强的能力：
                %s

                招聘平台搜索关键词：
                %s

                投递准备建议：
                %s
                """.formatted(
                item.roleTitle(),
                item.fitScore(),
                blankToDefault(item.fitLevel(), "待评估"),
                bulletList(item.reasons()),
                bulletList(requirements(item)),
                bulletList(item.matchedSkills()),
                bulletList(item.missingSkills()),
                bulletList(item.searchKeywords()),
                bulletList(item.preparationTips())
        ).strip();
    }

    private List<String> requirements(JobRecommendationItem item) {
        List<String> skills = item.matchedSkills() == null ? List.of() : item.matchedSkills();
        List<String> gaps = item.missingSkills() == null ? List.of() : item.missingSkills();
        return java.util.stream.Stream.concat(skills.stream(), gaps.stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .map(value -> "熟悉或了解 " + value.strip())
                .toList();
    }

    private String bulletList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "- 暂无";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> "- " + value.strip())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("- 暂无");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
