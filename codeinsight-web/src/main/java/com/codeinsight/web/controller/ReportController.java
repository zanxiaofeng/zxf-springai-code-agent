package com.codeinsight.web.controller;

import com.codeinsight.common.exception.ResourceNotFoundException;
import com.codeinsight.common.result.ApiResponse;
import com.codeinsight.model.dto.ReportResponse;
import com.codeinsight.model.entity.AnalysisReport;
import com.codeinsight.model.enums.ReportType;
import com.codeinsight.model.repository.AnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AnalysisReportRepository reportRepository;

    @GetMapping
    public ApiResponse<List<ReportResponse>> listReports(
            @RequestParam String projectId,
            @RequestParam(required = false) ReportType type) {

        List<AnalysisReport> reports = type != null
                ? reportRepository.findByProjectIdAndReportTypeOrderByCreatedAtDesc(projectId, type)
                : reportRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        List<ReportResponse> responses = reports.stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.ok(responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReportResponse> getReport(@PathVariable String id) {
        AnalysisReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));
        return ApiResponse.ok(toResponse(report));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReport(@PathVariable String id) {
        if (!reportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Report", id);
        }
        reportRepository.deleteById(id);
        return ApiResponse.ok(null);
    }

    private ReportResponse toResponse(AnalysisReport report) {
        return new ReportResponse(
                report.getId(),
                report.getProject().getId(),
                report.getReportType(),
                report.getStatus(),
                report.getResultData(),
                report.getSummary(),
                report.getIssueCount(),
                report.getCriticalCount(),
                report.getHighCount(),
                report.getStartedAt(),
                report.getCompletedAt(),
                report.getCreatedAt()
        );
    }
}
