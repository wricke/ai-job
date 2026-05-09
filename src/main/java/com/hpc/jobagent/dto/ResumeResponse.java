package com.hpc.jobagent.dto;

import java.time.LocalDateTime;

import com.hpc.jobagent.domain.ResumeProfile;

public record ResumeResponse(
        Long id,
        String title,
        String ownerName,
        String targetRole,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResumeResponse from(ResumeProfile resume) {
        return new ResumeResponse(
                resume.getId(),
                resume.getTitle(),
                resume.getOwnerName(),
                resume.getTargetRole(),
                resume.getContent(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
