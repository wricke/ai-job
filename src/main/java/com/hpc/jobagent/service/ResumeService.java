package com.hpc.jobagent.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hpc.jobagent.domain.ResumeProfile;
import com.hpc.jobagent.dto.CreateResumeRequest;
import com.hpc.jobagent.dto.ResumeResponse;
import com.hpc.jobagent.mapper.ResumeProfileMapper;
import com.hpc.jobagent.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    private final ResumeProfileMapper resumeMapper;
    private final ResumeFileExtractor resumeFileExtractor;
    private final CurrentUserService currentUserService;

    public ResumeService(ResumeProfileMapper resumeMapper,
                         ResumeFileExtractor resumeFileExtractor,
                         CurrentUserService currentUserService) {
        this.resumeMapper = resumeMapper;
        this.resumeFileExtractor = resumeFileExtractor;
        this.currentUserService = currentUserService;
    }

    public ResumeResponse create(CreateResumeRequest request) {
        Long userId = currentUserService.userId();
        LocalDateTime now = LocalDateTime.now();
        ResumeProfile resume = new ResumeProfile();
        resume.setUserId(userId);
        resume.setTitle(required(request.title(), "标题"));
        resume.setOwnerName(request.ownerName());
        resume.setTargetRole(request.targetRole());
        resume.setContent(required(request.content(), "简历内容"));
        resume.setCreatedAt(now);
        resume.setUpdatedAt(now);
        resumeMapper.insert(resume);
        return ResumeResponse.from(resume);
    }

    public ResumeResponse upload(String title, String ownerName, String targetRole, MultipartFile file) {
        Long userId = currentUserService.userId();
        String content = resumeFileExtractor.extract(file);
        LocalDateTime now = LocalDateTime.now();
        ResumeProfile existing = resumeMapper.findLatestByContent(userId, content);
        if (existing != null) {
            existing.setTitle(defaultTitle(title, file));
            existing.setOwnerName(blankToNull(ownerName));
            existing.setTargetRole(blankToNull(targetRole));
            existing.setUpdatedAt(now);
            resumeMapper.update(existing);
            return ResumeResponse.from(existing);
        }

        ResumeProfile resume = new ResumeProfile();
        resume.setUserId(userId);
        resume.setTitle(defaultTitle(title, file));
        resume.setOwnerName(blankToNull(ownerName));
        resume.setTargetRole(blankToNull(targetRole));
        resume.setContent(content);
        resume.setCreatedAt(now);
        resume.setUpdatedAt(now);
        resumeMapper.insert(resume);
        return ResumeResponse.from(resume);
    }

    public ResumeResponse get(Long id) {
        return ResumeResponse.from(getEntity(id));
    }

    public List<ResumeResponse> list() {
        Map<String, ResumeProfile> unique = new LinkedHashMap<>();
        for (ResumeProfile resume : resumeMapper.findAllByUserId(currentUserService.userId())) {
            unique.putIfAbsent(dedupeKey(resume), resume);
        }
        return unique.values().stream().map(ResumeResponse::from).toList();
    }

    public ResumeProfile getEntity(Long id) {
        return getEntityForUser(id, currentUserService.userId());
    }

    public ResumeProfile getEntityForUser(Long id, Long userId) {
        ResumeProfile resume = resumeMapper.findByIdAndUserId(id, userId);
        if (resume == null) {
            throw new NotFoundException("简历不存在：" + id);
        }
        return resume;
    }

    public void delete(Long id) {
        getEntity(id);
        resumeMapper.deleteByIdAndUserId(id, currentUserService.userId());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.strip();
    }

    private String defaultTitle(String title, MultipartFile file) {
        if (title != null && !title.isBlank()) {
            return title.strip();
        }
        String filename = file == null ? null : file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return "上传简历";
        }
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(0, index) : filename;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String dedupeKey(ResumeProfile resume) {
        return normalize(resume.getContent());
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().replace("\r\n", "\n");
    }
}
