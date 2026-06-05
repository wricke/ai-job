package com.hpc.jobagent.controller;

import java.util.List;

import com.hpc.jobagent.domain.AnalysisStatus;
import com.hpc.jobagent.dto.AnalysisResponse;
import com.hpc.jobagent.dto.CreateAnalysisRequest;
import com.hpc.jobagent.service.AnalysisReportPdfService;
import com.hpc.jobagent.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisReportPdfService analysisReportPdfService;

    public AnalysisController(AnalysisService analysisService, AnalysisReportPdfService analysisReportPdfService) {
        this.analysisService = analysisService;
        this.analysisReportPdfService = analysisReportPdfService;
    }

    @GetMapping
    public List<AnalysisResponse> search(@RequestParam(required = false) Long resumeId,
                                         @RequestParam(required = false) Long jobId,
                                         @RequestParam(required = false) AnalysisStatus status,
                                         @RequestParam(defaultValue = "30") int limit) {
        return analysisService.search(resumeId, jobId, status, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(@Valid @RequestBody CreateAnalysisRequest request) {
        return analysisService.createAndRun(request);
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(@PathVariable Long id) {
        return analysisService.get(id);
    }

    @GetMapping(value = "/{id}/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> reportPdf(@PathVariable Long id) {
        byte[] content = analysisReportPdfService.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ai-job-analysis-" + id + ".pdf\"")
                .body(content);
    }

    @PostMapping("/{id}/run")
    public AnalysisResponse run(@PathVariable Long id) {
        analysisService.run(id);
        return analysisService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        analysisService.delete(id);
    }
}
