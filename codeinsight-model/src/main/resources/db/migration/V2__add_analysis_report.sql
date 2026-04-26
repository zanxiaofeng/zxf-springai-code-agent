-- 分析报告表
CREATE TABLE ci_analysis_report (
    id              VARCHAR(36)  PRIMARY KEY,
    project_id      VARCHAR(36)  NOT NULL REFERENCES ci_project(id) ON DELETE CASCADE,
    report_type     VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    result_data     JSONB,
    summary         TEXT,
    issue_count     INTEGER      DEFAULT 0,
    critical_count  INTEGER      DEFAULT 0,
    high_count      INTEGER      DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_project ON ci_analysis_report(project_id, created_at);
