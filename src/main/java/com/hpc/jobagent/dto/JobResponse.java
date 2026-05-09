package com.hpc.jobagent.dto;

import java.time.LocalDateTime;

import com.hpc.jobagent.domain.JobPosting;

public record JobResponse(
        Long id,
        String company,
        String title,
        String source,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static JobResponse from(JobPosting job) {
        return new JobResponse(
                job.getId(),
                job.getCompany(),
                job.getTitle(),
                job.getSource(),
                job.getDescription(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
