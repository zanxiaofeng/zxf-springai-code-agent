package com.codeinsight.model.repository;

import com.codeinsight.model.entity.AnalysisReport;
import com.codeinsight.model.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, String> {

    List<AnalysisReport> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<AnalysisReport> findByProjectIdAndReportTypeOrderByCreatedAtDesc(String projectId, ReportType reportType);
}
