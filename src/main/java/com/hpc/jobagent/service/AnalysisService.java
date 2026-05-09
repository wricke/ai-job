package com.hpc.jobagent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import com.hpc.jobagent.agent.AgentReport;
import com.hpc.jobagent.agent.AgentWorkflowService;
import com.hpc.jobagent.config.AgentProperties;
import com.hpc.jobagent.domain.AnalysisCache;
import com.hpc.jobagent.domain.AnalysisStatus;
import com.hpc.jobagent.domain.AnalysisTask;
import com.hpc.jobagent.domain.JobPosting;
import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.AnalysisResponse;
import com.hpc.jobagent.dto.CreateAnalysisRequest;
import com.hpc.jobagent.mapper.AnalysisTaskMapper;
import com.hpc.jobagent.support.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    private final AnalysisTaskMapper analysisMapper;
    private final ResumeService resumeService;
    private final JobPostingService jobPostingService;
    private final AgentWorkflowService agentWorkflowService;
    private final AnalysisCacheService cacheService;
    private final HashService hashService;
    private final JsonService jsonService;
    private final AgentProperties properties;
    private final Executor analysisExecutor;
    private final CurrentUserService currentUserService;

    public AnalysisService(AnalysisTaskMapper analysisMapper,
                           ResumeService resumeService,
                           JobPostingService jobPostingService,
                           AgentWorkflowService agentWorkflowService,
                           AnalysisCacheService cacheService,
                           HashService hashService,
                           JsonService jsonService,
                           AgentProperties properties,
                           @Qualifier("analysisExecutor") Executor analysisExecutor,
                           CurrentUserService currentUserService) {
        this.analysisMapper = analysisMapper;
        this.resumeService = resumeService;
        this.jobPostingService = jobPostingService;
        this.agentWorkflowService = agentWorkflowService;
        this.cacheService = cacheService;
        this.hashService = hashService;
        this.jsonService = jsonService;
        this.properties = properties;
        this.analysisExecutor = analysisExecutor;
        this.currentUserService = currentUserService;
    }

    public AnalysisResponse createAndRun(CreateAnalysisRequest request) {
        Long userId = currentUserService.userId();
        ResumeProfile resume = resumeService.getEntity(request.resumeId());
        JobPosting job = jobPostingService.getEntity(request.jobId());
        LocalDateTime now = LocalDateTime.now();
        AnalysisTask task = new AnalysisTask();
        task.setUserId(userId);
        task.setResumeId(resume.getId());
        task.setJobId(job.getId());
        task.setStatus(AnalysisStatus.PENDING);
        task.setCacheKey(cacheKey(resume, job));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        analysisMapper.insert(task);
        analysisExecutor.execute(() -> runInternal(task.getId()));
        return AnalysisResponse.from(task);
    }

    public AnalysisResponse get(Long id) {
        return AnalysisResponse.from(getEntity(id));
    }

    public List<AnalysisResponse> search(Long resumeId, Long jobId, AnalysisStatus status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return analysisMapper.search(resumeId, jobId, status, safeLimit, currentUserService.userId()).stream()
                .map(AnalysisResponse::from)
                .toList();
    }

    public void delete(Long id) {
        getEntity(id);
        analysisMapper.deleteByIdAndUserId(id, currentUserService.userId());
    }

    public AnalysisTask getEntity(Long id) {
        AnalysisTask task = analysisMapper.findByIdAndUserId(id, currentUserService.userId());
        if (task == null) {
            throw new NotFoundException("分析任务不存在：" + id);
        }
        return task;
    }

    public void run(Long id) {
        getEntity(id);
        runInternal(id);
    }

    private void runInternal(Long id) {
        AnalysisTask task = analysisMapper.findById(id);
        if (task == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        analysisMapper.updateStatus(id, AnalysisStatus.RUNNING, null, now);
        try {
            if (properties.getCache().isEnabled()) {
                AnalysisCache cache = cacheService.find(task.getCacheKey());
                if (cache != null) {
                    completeFromCache(task, cache);
                    return;
                }
            }

            ResumeProfile resume = resumeService.getEntityForUser(task.getResumeId(), task.getUserId());
            JobPosting job = jobPostingService.getEntityForUser(task.getJobId(), task.getUserId());
            AgentReport report = agentWorkflowService.run(resume, job);
            completeFromReport(task, report);
        } catch (Exception ex) {
            analysisMapper.updateStatus(id, AnalysisStatus.FAILED, rootMessage(ex), LocalDateTime.now());
        }
    }

    private void completeFromReport(AnalysisTask task, AgentReport report) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(AnalysisStatus.COMPLETED);
        task.setMatchScore(report.matchDetail().score());
        task.setSummary(report.summary());
        task.setResumeInsight(jsonService.toJson(report.resumeInsight()));
        task.setJobInsight(jsonService.toJson(report.jobInsight()));
        task.setMatchDetail(jsonService.toJson(report.matchDetail()));
        task.setSuggestions(jsonService.toJson(report.suggestions()));
        task.setInterviewQuestions(jsonService.toJson(report.interviewQuestions()));
        task.setAgentTrace(jsonService.toJson(report.trace()));
        task.setUpdatedAt(now);
        task.setCompletedAt(now);
        analysisMapper.markCompleted(task);
        saveCache(task);
    }

    private void completeFromCache(AnalysisTask task, AnalysisCache cache) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(AnalysisStatus.COMPLETED);
        task.setMatchScore(cache.getMatchScore());
        task.setSummary(cache.getSummary() + "（命中缓存）");
        task.setResumeInsight(cache.getResumeInsight());
        task.setJobInsight(cache.getJobInsight());
        task.setMatchDetail(cache.getMatchDetail());
        task.setSuggestions(cache.getSuggestions());
        task.setInterviewQuestions(cache.getInterviewQuestions());
        task.setAgentTrace(cache.getAgentTrace());
        task.setUpdatedAt(now);
        task.setCompletedAt(now);
        analysisMapper.markCompleted(task);
    }

    private void saveCache(AnalysisTask task) {
        if (!properties.getCache().isEnabled()) {
            return;
        }
        AnalysisCache cache = new AnalysisCache();
        cache.setCacheKey(task.getCacheKey());
        cache.setMatchScore(task.getMatchScore());
        cache.setSummary(task.getSummary());
        cache.setResumeInsight(task.getResumeInsight());
        cache.setJobInsight(task.getJobInsight());
        cache.setMatchDetail(task.getMatchDetail());
        cache.setSuggestions(task.getSuggestions());
        cache.setInterviewQuestions(task.getInterviewQuestions());
        cache.setAgentTrace(task.getAgentTrace());
        cacheService.save(cache);
    }

    private String cacheKey(ResumeProfile resume, JobPosting job) {
        return hashService.sha256(resume.getUserId() + "\n---RESUME---\n"
                + resume.getContent() + "\n---JOB---\n" + job.getDescription());
    }

    private String rootMessage(Exception ex) {
        Throwable cursor = ex;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getClass().getSimpleName() + ": " + cursor.getMessage();
    }
}
