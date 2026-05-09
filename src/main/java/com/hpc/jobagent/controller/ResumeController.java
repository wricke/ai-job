package com.hpc.jobagent.controller;

import java.util.List;

import com.hpc.jobagent.dto.CreateResumeRequest;
import com.hpc.jobagent.dto.JobRecommendationResponse;
import com.hpc.jobagent.dto.ResumeResponse;
import com.hpc.jobagent.service.JobRecommendationService;
import com.hpc.jobagent.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final JobRecommendationService jobRecommendationService;

    public ResumeController(ResumeService resumeService, JobRecommendationService jobRecommendationService) {
        this.resumeService = resumeService;
        this.jobRecommendationService = jobRecommendationService;
    }

    @GetMapping
    public List<ResumeResponse> list() {
        return resumeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse create(@Valid @RequestBody CreateResumeRequest request) {
        return resumeService.create(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse upload(@RequestPart(required = false) String title,
                                 @RequestPart(required = false) String ownerName,
                                 @RequestPart(required = false) String targetRole,
                                 @RequestPart MultipartFile file) {
        return resumeService.upload(title, ownerName, targetRole, file);
    }

    @GetMapping("/{id}")
    public ResumeResponse get(@PathVariable Long id) {
        return resumeService.get(id);
    }

    @PostMapping("/{id}/job-recommendations")
    public JobRecommendationResponse recommendJobs(@PathVariable Long id) {
        return jobRecommendationService.recommend(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        resumeService.delete(id);
    }
}
