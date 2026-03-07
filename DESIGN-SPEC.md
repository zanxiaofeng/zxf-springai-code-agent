# CodeInsight Agent - 设计SPEC文档

> 基于 Spring AI + Spring AI Alibaba 的大型Java代码库智能分析Agent

**版本**: v1.0
**日期**: 2026-03-07
**状态**: 设计阶段

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈与版本](#2-技术栈与版本)
3. [系统架构设计](#3-系统架构设计)
4. [Maven模块结构](#4-maven模块结构)
5. [核心领域模型](#5-核心领域模型)
6. [Agent编排设计](#6-agent编排设计)
7. [代码解析与索引管线](#7-代码解析与索引管线)
8. [REST API设计](#8-rest-api设计)
9. [数据库设计](#9-数据库设计)
10. [安全设计](#10-安全设计)
11. [可观测性设计](#11-可观测性设计)
12. [部署方案](#12-部署方案)
13. [测试策略](#13-测试策略)
14. [关键时序图](#14-关键时序图)

---

## 1. 项目概述

### 1.1 产品定位

CodeInsight Agent 是一个面向企业的大型Java代码库智能分析平台，通过多Agent协作架构，为开发团队提供代码审查、智能问答、架构分析、代码生成、依赖漏洞检测和安全审查六大核心能力。

### 1.2 核心场景

| 编号 | 场景 | 描述 |
|------|------|------|
| S1 | **代码审查辅助** | 自动审查代码变更，发现质量问题、反模式、性能隐患 |
| S2 | **代码库问答** | 自然语言提问，Agent理解代码库后回答（模块原理、Bug定位等） |
| S3 | **架构分析与可视化** | 分析模块依赖、调用链路、架构分层，生成架构报告 |
| S4 | **代码生成与重构建议** | 基于现有代码风格和架构，生成新代码或给出重构方案 |
| S5 | **依赖库漏洞分析** | 解析pom.xml依赖树，检测CVE漏洞，给出升级建议 |
| S6 | **代码静态安全审查** | OWASP Top 10检测、硬编码敏感信息扫描、权限缺陷检测，生成修复代码 |

### 1.3 设计约束

- 目标代码库规模：**200万行+，1000+ Java文件**
- 部署形态：**Web应用（REST API + Vue前端）**，支持多用户
- 代码接入：Git仓库克隆 + 压缩包上传

---

## 2. 技术栈与版本

### 2.1 核心框架

| 组件 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| JDK | Azul Zulu / MS OpenJDK | **21** | 虚拟线程、Record、Pattern Matching |
| Spring Boot | spring-boot-starter | **3.5.x** | 基础框架 |
| Spring AI | spring-ai-bom | **1.1.2** | AI模型抽象层（原约束1.0.x，升级至最新稳定版） |
| Spring AI Alibaba | spring-ai-alibaba-starter-dashscope | **1.1.2.1** | 通义千问DashScope集成 |
| Agent Framework | spring-ai-alibaba-agent-framework | **1.1.2.0** | 多Agent编排框架 |
| 构建工具 | Maven | **3.9+** | 多模块管理 |

### 2.2 数据存储

| 组件 | 技术选型 | 版本 | 用途 |
|------|---------|------|------|
| 关系型数据库 | PostgreSQL | **16+** | 项目元数据、用户、会话历史、审计日志 |
| 向量/全文检索 | Elasticsearch | **8.15+** | 代码向量索引 + 全文检索 |
| 缓存/队列 | Redis Stack | **7.4+** | 缓存 + 会话存储 + 任务队列（Stream） |

### 2.3 代码解析

| 组件 | 技术选型 | 版本 | 用途 |
|------|---------|------|------|
| AST解析 | JavaParser | **3.26+** | Java源码AST解析，提取类/方法/注解/调用关系 |
| Maven解析 | Maven Model | **3.9+** | 解析pom.xml依赖树 |
| Git操作 | JGit | **7.0+** | Git仓库克隆与增量同步 |

### 2.4 AI模型策略

| 用途 | 模型 | 说明 |
|------|------|------|
| 复杂推理 | **qwen-max** | 代码审查、安全分析、架构分析等需要深度理解的场景 |
| 轻量任务 | **qwen-turbo** | 代码问答、简单生成等响应速度优先的场景 |
| 文本向量化 | **text-embedding-v3** | 代码分块向量化，写入Elasticsearch |

### 2.5 基础设施

| 组件 | 技术选型 | 用途 |
|------|---------|------|
| 安全 | Spring Security + JWT | 认证授权 |
| API文档 | SpringDoc OpenAPI 2.x | Swagger UI |
| 监控 | Micrometer + Prometheus | 指标采集 |
| 追踪 | Micrometer Tracing | 分布式追踪 |
| 日志 | Logback + JSON Encoder | 结构化日志 |
| 健康检查 | Spring Boot Actuator | 组件健康检测 |
| 容器化 | Docker Compose | 一键部署 |
| E2E测试 | Spring Boot Test + WireMock 3.10+ | 端到端API测试，模拟外部AI/漏洞API |
| 测试容器 | Testcontainers 1.20+ | 真实PG/ES/Redis容器用于集成测试 |
| 覆盖率 | JaCoCo | Maven verify阶段强制≥80%覆盖率 |

---

## 3. 系统架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vue 3 + Element Plus 前端                        │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│   │ 对话界面  │ │ 项目管理  │ │ 分析报告  │ │ 漏洞看板  │ │ 用户管理  │    │
│   └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘    │
└────────┼────────────┼────────────┼────────────┼────────────┼───────────┘
         │   SSE/REST │            │            │            │
─────────┼────────────┼────────────┼────────────┼────────────┼───────────
         ▼            ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3.5 应用层                              │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    API Gateway Layer                              │   │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────┐  ┌─────────────┐  │   │
│  │  │ ChatCtrl   │  │ProjectCtrl │  │ AuthCtrl │  │ ReportCtrl  │  │   │
│  │  │ (SSE流式)  │  │ (REST)     │  │ (REST)   │  │ (REST)      │  │   │
│  │  └────────────┘  └────────────┘  └──────────┘  └─────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │               Orchestrator Agent (调度层)                         │   │
│  │                                                                    │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐  │   │
│  │  │ Review  │ │   QA    │ │  Arch   │ │  Sec    │ │   Dep    │  │   │
│  │  │ Agent   │ │  Agent  │ │  Agent  │ │  Agent  │ │  Agent   │  │   │
│  │  │ 代码审查 │ │ 智能问答 │ │ 架构分析 │ │ 安全审查 │ │ 依赖分析  │  │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬─────┘  │   │
│  │       │           │           │           │           │         │   │
│  │  ┌────┴───────────┴───────────┴───────────┴───────────┴──────┐  │   │
│  │  │                    Tool Layer (工具层)                       │  │   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │  │   │
│  │  │  │CodeSearch│ │ASTAnalyze│ │VulnQuery │ │CodeGenerator │  │  │   │
│  │  │  │代码检索   │ │AST分析   │ │漏洞查询   │ │代码生成       │  │  │   │
│  │  │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │  │   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │  │   │
│  │  │  │DepTree   │ │CallGraph │ │SecScan   │ │MetricCollect │  │  │   │
│  │  │  │依赖树解析 │ │调用图构建 │ │安全扫描   │ │指标收集       │  │  │   │
│  │  │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │  │   │
│  │  └───────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Service Layer (业务层)                          │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐  │   │
│  │  │ProjectSvc  │ │IndexingSvc │ │AnalysisSvc │ │ConverseSvc   │  │   │
│  │  │项目管理     │ │索引管理     │ │分析服务     │ │会话管理       │  │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └──────────────┘  │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐  │   │
│  │  │SecuritySvc │ │ AuditSvc   │ │  UserSvc   │ │AsyncTaskSvc  │  │   │
│  │  │安全服务     │ │审计服务     │ │用户服务     │ │异步任务服务   │  │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └──────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                   Infrastructure Layer (基础设施层)                │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │   │
│  │  │ JPA Repo │ │ES Vector │ │Redis Cache│ │  JGit    │           │   │
│  │  │ Store    │ │ Store    │ │ /Queue   │ │ Client   │           │   │
│  │  └─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘           │   │
│  └────────┼────────────┼────────────┼────────────┼─────────────────┘   │
└───────────┼────────────┼────────────┼────────────┼─────────────────────┘
            ▼            ▼            ▼            ▼
     ┌────────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐
     │ PostgreSQL │ │  Elas-  │ │  Redis  │ │ Git Repo │
     │    16+     │ │tic 8.x │ │  7.4+   │ │ (Remote) │
     └────────────┘ └─────────┘ └─────────┘ └──────────┘
```

### 3.2 分层职责

| 层级 | 职责 | 关键技术 |
|------|------|---------|
| **API层** | HTTP请求路由、SSE流式输出、请求校验、异常处理 | Spring MVC, SSE, SpringDoc |
| **Agent调度层** | 意图识别、Agent路由、多Agent协作编排 | Spring AI Alibaba Agent Framework |
| **工具层** | Agent可调用的原子能力（代码检索、AST分析、漏洞查询等） | Spring AI Function Calling, @Tool |
| **业务层** | 项目管理、索引管理、会话管理、用户管理等业务逻辑 | Spring Service |
| **基础设施层** | 数据访问、向量存储、缓存、外部系统集成 | Spring Data JPA, ES VectorStore, Redis |

---

## 4. Maven模块结构

```
codeinsight-agent/                          (父POM)
├── pom.xml
│
├── codeinsight-common/                     (公共模块)
│   ├── src/main/java/.../common/
│   │   ├── exception/                      # 全局异常定义
│   │   ├── result/                         # 统一响应封装
│   │   ├── constant/                       # 常量定义
│   │   └── util/                           # 工具类
│   └── pom.xml
│
├── codeinsight-model/                      (领域模型模块)
│   ├── src/main/java/.../model/
│   │   ├── entity/                         # JPA实体
│   │   ├── dto/                            # 数据传输对象
│   │   ├── enums/                          # 枚举类型
│   │   └── vo/                             # 视图对象
│   └── pom.xml
│
├── codeinsight-parser/                     (代码解析模块)
│   ├── src/main/java/.../parser/
│   │   ├── ast/                            # AST解析器（JavaParser）
│   │   ├── chunker/                        # 文本智能分块器
│   │   ├── dependency/                     # Maven依赖树解析
│   │   ├── git/                            # JGit操作封装
│   │   └── model/                          # 解析结果模型
│   └── pom.xml
│
├── codeinsight-indexer/                    (索引管理模块)
│   ├── src/main/java/.../indexer/
│   │   ├── pipeline/                       # 索引管线（解析→分块→向量化→存储）
│   │   ├── embedding/                      # Embedding封装
│   │   ├── store/                          # ES VectorStore配置
│   │   └── task/                           # 异步索引任务
│   └── pom.xml
│
├── codeinsight-agent-core/                 (Agent核心模块)
│   ├── src/main/java/.../agent/
│   │   ├── orchestrator/                   # Orchestrator调度器
│   │   ├── specialist/                     # 六大专职Agent
│   │   │   ├── ReviewAgent.java
│   │   │   ├── QAAgent.java
│   │   │   ├── ArchitectureAgent.java
│   │   │   ├── CodeGenAgent.java
│   │   │   ├── DependencyAgent.java
│   │   │   └── SecurityAgent.java
│   │   ├── tool/                           # Agent工具定义（@Tool）
│   │   ├── prompt/                         # Prompt模板管理
│   │   └── memory/                         # 会话记忆配置
│   └── pom.xml
│
├── codeinsight-security/                   (安全模块)
│   ├── src/main/java/.../security/
│   │   ├── config/                         # Spring Security配置
│   │   ├── jwt/                            # JWT工具
│   │   ├── rbac/                           # RBAC权限模型
│   │   └── audit/                          # 审计日志
│   └── pom.xml
│
├── codeinsight-service/                    (业务服务模块)
│   ├── src/main/java/.../service/
│   │   ├── project/                        # 项目管理服务
│   │   ├── conversation/                   # 会话管理服务
│   │   ├── analysis/                       # 分析报告服务
│   │   ├── user/                           # 用户管理服务
│   │   └── task/                           # 异步任务服务（Redis Stream）
│   └── pom.xml
│
├── codeinsight-web/                        (Web启动模块)
│   ├── src/main/java/.../web/
│   │   ├── controller/                     # REST控制器
│   │   ├── sse/                            # SSE流式端点
│   │   ├── config/                         # Web配置（CORS等）
│   │   └── CodeInsightApplication.java     # 启动类
│   ├── src/main/resources/
│   │   ├── application.yml                 # 主配置
│   │   ├── application-dev.yml             # 开发环境
│   │   └── application-prod.yml            # 生产环境
│   └── pom.xml
│
└── deploy/                                 (部署配置)
    ├── docker/
    │   ├── Dockerfile
    │   └── docker-compose.yml
    ├── prometheus/
    │   └── prometheus.yml
    └── elasticsearch/
        └── elasticsearch.yml
```

### 4.1 模块依赖关系

```
codeinsight-web
  ├── codeinsight-agent-core
  │     ├── codeinsight-parser
  │     │     ├── codeinsight-model
  │     │     └── codeinsight-common
  │     ├── codeinsight-indexer
  │     │     ├── codeinsight-parser
  │     │     └── codeinsight-model
  │     └── codeinsight-model
  ├── codeinsight-service
  │     ├── codeinsight-model
  │     └── codeinsight-common
  └── codeinsight-security
        ├── codeinsight-model
        └── codeinsight-common
```

---

## 5. 核心领域模型

### 5.1 JPA实体设计

#### 项目 (Project)

```java
@Entity
@Table(name = "ci_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;           // GIT, ARCHIVE

    private String gitUrl;
    private String gitBranch;
    private String gitCredentialId;

    @Enumerated(EnumType.STRING)
    private IndexStatus indexStatus;         // PENDING, INDEXING, COMPLETED, FAILED

    private Integer totalFiles;
    private Integer totalLines;
    private Integer indexedChunks;

    private LocalDateTime lastSyncAt;
    private LocalDateTime indexStartedAt;
    private LocalDateTime indexCompletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 用户 (User)

```java
@Entity
@Table(name = "ci_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String displayName;
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;                       // ADMIN, PROJECT_LEAD, DEVELOPER, GUEST

    private Boolean enabled;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
```

#### 会话 (Conversation)

```java
@Entity
@Table(name = "ci_conversation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ScenarioType scenarioType;       // REVIEW, QA, ARCHITECTURE, CODEGEN, DEPENDENCY, SECURITY

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### 会话消息 (ConversationMessage)

```java
@Entity
@Table(name = "ci_conversation_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    private MessageRole role;                // USER, ASSISTANT, SYSTEM

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;    // Agent名称、工具调用记录、token消耗等

    private Integer tokenCount;
    private LocalDateTime createdAt;
}
```

#### 分析报告 (AnalysisReport)

```java
@Entity
@Table(name = "ci_analysis_report")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    private ReportType reportType;           // SECURITY_SCAN, DEPENDENCY_AUDIT, ARCHITECTURE, CODE_REVIEW

    @Enumerated(EnumType.STRING)
    private ReportStatus status;             // RUNNING, COMPLETED, FAILED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> resultData;  // 分析结果（JSON结构化数据）

    @Column(columnDefinition = "TEXT")
    private String summary;                  // AI生成的自然语言总结

    private Integer issueCount;
    private Integer criticalCount;
    private Integer highCount;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
```

#### 审计日志 (AuditLog)

```java
@Entity
@Table(name = "ci_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;
    private String username;
    private String action;                   // LOGIN, QUERY, ANALYZE, EXPORT, PROJECT_CREATE ...
    private String resourceType;             // PROJECT, CONVERSATION, REPORT
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
```

#### 异步任务 (AsyncTask)

```java
@Entity
@Table(name = "ci_async_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;               // INDEX_FULL, INDEX_INCREMENTAL, SECURITY_SCAN, DEPENDENCY_SCAN

    private String projectId;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;               // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    private Integer progressPercent;
    private String progressMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> params;      // 任务参数

    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
```

### 5.2 枚举定义

```java
public enum SourceType { GIT, ARCHIVE }
public enum IndexStatus { PENDING, INDEXING, COMPLETED, FAILED }
public enum Role { ADMIN, PROJECT_LEAD, DEVELOPER, GUEST }
public enum ScenarioType { REVIEW, QA, ARCHITECTURE, CODEGEN, DEPENDENCY, SECURITY }
public enum MessageRole { USER, ASSISTANT, SYSTEM }
public enum ReportType { SECURITY_SCAN, DEPENDENCY_AUDIT, ARCHITECTURE, CODE_REVIEW }
public enum ReportStatus { RUNNING, COMPLETED, FAILED }
public enum TaskType { INDEX_FULL, INDEX_INCREMENTAL, SECURITY_SCAN, DEPENDENCY_SCAN }
public enum TaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
public enum Severity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
```

### 5.3 代码解析结果模型（非JPA）

```java
// AST解析结果
@Data
@Builder
public class ParsedClass {
    private String filePath;
    private String packageName;
    private String className;
    private String qualifiedName;
    private ClassType classType;             // CLASS, INTERFACE, ENUM, RECORD, ANNOTATION
    private List<String> annotations;
    private List<String> implementedInterfaces;
    private String superClass;
    private List<ParsedMethod> methods;
    private List<ParsedField> fields;
    private List<String> imports;
}

@Data
@Builder
public class ParsedMethod {
    private String name;
    private String returnType;
    private List<MethodParam> parameters;
    private List<String> annotations;
    private List<String> calledMethods;      // 调用的其他方法（qualifiedName）
    private int startLine;
    private int endLine;
    private String sourceCode;               // 方法源码
    private int complexity;                  // 圈复杂度
}

@Data
@Builder
public class ParsedField {
    private String name;
    private String type;
    private List<String> annotations;
    private boolean isStatic;
    private boolean isFinal;
}

// 依赖分析结果
@Data
@Builder
public class DependencyNode {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private List<DependencyNode> children;
    private List<VulnerabilityInfo> vulnerabilities;
}

@Data
@Builder
public class VulnerabilityInfo {
    private String cveId;
    private Severity severity;
    private double cvssScore;
    private String description;
    private String fixedVersion;             // 修复版本建议
    private String referenceUrl;
}
```

---

## 6. Agent编排设计

### 6.1 Agent架构总览

基于 Spring AI Alibaba Agent Framework，采用 **Orchestrator → Specialist Agent** 模式：

```
                     用户请求
                        │
                        ▼
              ┌──────────────────┐
              │   Orchestrator   │
              │   (LlmRouting    │
              │    Agent)        │
              │                  │
              │  意图识别 + 路由  │
              └────────┬─────────┘
                       │
         ┌─────────────┼─────────────────────────┐
         │             │             │            │
         ▼             ▼             ▼            ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐
   │ Review   │ │   QA     │ │  Arch    │ │  CodeGen  │
   │ Agent    │ │  Agent   │ │  Agent   │ │  Agent    │
   │          │ │          │ │          │ │           │
   │ Tools:   │ │ Tools:   │ │ Tools:   │ │ Tools:    │
   │-CodeSrch │ │-CodeSrch │ │-CallGraph│ │-CodeSrch  │
   │-ASTCheck │ │-ASTQuery │ │-DepGraph │ │-ASTQuery  │
   │-SecScan  │ │-FullText │ │-MetricCo │ │-CodeWrite │
   └──────────┘ └──────────┘ └──────────┘ └───────────┘
         │             │
         ▼             ▼
   ┌──────────┐ ┌──────────┐
   │   Dep    │ │   Sec    │
   │  Agent   │ │  Agent   │
   │          │ │          │
   │ Tools:   │ │ Tools:   │
   │-DepTree  │ │-OWASPChk │
   │-VulnDB   │ │-SecRule  │
   │-VerCheck │ │-HardCode │
   └──────────┘ └──────────┘
```

### 6.2 Orchestrator Agent

```java
@Configuration
public class OrchestratorConfig {

    @Bean
    public LlmRoutingAgent orchestratorAgent(
            ChatModel qwenMaxModel,
            ReviewAgent reviewAgent,
            QAAgent qaAgent,
            ArchitectureAgent archAgent,
            CodeGenAgent codeGenAgent,
            DependencyAgent depAgent,
            SecurityAgent secAgent) {

        return LlmRoutingAgent.builder()
                .name("orchestrator")
                .model(qwenMaxModel)
                .systemPrompt("""
                    你是CodeInsight智能调度器。根据用户意图，将请求路由到最合适的专职Agent。

                    可用Agent及其职责：
                    1. review-agent: 代码审查（代码质量、反模式、性能问题）
                    2. qa-agent: 代码库问答（模块原理、功能解释、Bug定位）
                    3. architecture-agent: 架构分析（模块依赖、调用链、分层分析）
                    4. codegen-agent: 代码生成与重构建议
                    5. dependency-agent: Maven依赖漏洞分析与升级建议
                    6. security-agent: 代码静态安全审查与修复建议

                    如果用户请求涉及多个Agent的职责，按需协调多Agent协作。
                    """)
                .subAgents(List.of(
                        reviewAgent, qaAgent, archAgent,
                        codeGenAgent, depAgent, secAgent))
                .build();
    }
}
```

### 6.3 专职Agent示例（代码审查Agent）

```java
@Component
@Slf4j
public class ReviewAgent {

    private final ChatClient chatClient;

    public ReviewAgent(
            @Qualifier("qwenMaxModel") ChatModel chatModel,
            CodeSearchTool codeSearchTool,
            ASTAnalyzeTool astAnalyzeTool) {

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                    你是一位资深Java代码审查专家。你的职责是：
                    1. 分析代码质量（命名规范、设计模式、SOLID原则）
                    2. 检测性能隐患（N+1查询、内存泄漏、线程安全问题）
                    3. 发现反模式和代码坏味道
                    4. 给出具体的改进建议和修复代码

                    使用提供的工具搜索和分析代码，给出专业的审查报告。
                    """)
                .defaultTools(codeSearchTool, astAnalyzeTool)
                .build();
    }

    public Flux<String> review(String projectId, String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
```

### 6.4 Tool定义示例

```java
@Component
@Slf4j
public class CodeSearchTool {

    private final ElasticsearchVectorStore vectorStore;

    public CodeSearchTool(ElasticsearchVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "在代码库中搜索与查询语义相关的代码片段。返回最相关的代码段及其文件路径、行号等上下文信息。")
    public List<CodeSearchResult> searchCode(
            @ToolParam(description = "搜索查询，描述要查找的代码功能或特征") String query,
            @ToolParam(description = "项目ID") String projectId,
            @ToolParam(description = "返回结果数量，默认5") int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK > 0 ? topK : 5)
                .similarityThreshold(0.6)
                .filterExpression("projectId == '" + projectId + "'")
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(doc -> CodeSearchResult.builder()
                        .filePath(doc.getMetadata().get("filePath").toString())
                        .className(doc.getMetadata().get("className").toString())
                        .methodName(doc.getMetadata().getOrDefault("methodName", "").toString())
                        .startLine(Integer.parseInt(doc.getMetadata().getOrDefault("startLine", "0").toString()))
                        .content(doc.getText())
                        .score(doc.getScore())
                        .build())
                .toList();
    }

    @Tool(description = "按类名或方法名精确搜索代码，支持全文关键词匹配。")
    public List<CodeSearchResult> searchByKeyword(
            @ToolParam(description = "关键词，如类名、方法名、注解名") String keyword,
            @ToolParam(description = "项目ID") String projectId) {
        // 使用 ES 全文检索能力
        // ...
    }
}

@Component
@Slf4j
public class ASTAnalyzeTool {

    private final ASTIndexRepository astIndexRepository;

    public ASTAnalyzeTool(ASTIndexRepository astIndexRepository) {
        this.astIndexRepository = astIndexRepository;
    }

    @Tool(description = "分析指定Java类的结构信息，包括字段、方法、注解、继承关系和调用链。")
    public ClassStructure analyzeClass(
            @ToolParam(description = "完整类名，如com.example.OrderService") String qualifiedClassName,
            @ToolParam(description = "项目ID") String projectId) {
        // 从AST索引中查询类结构信息
        // ...
    }

    @Tool(description = "获取指定方法的调用链，找出哪些方法调用了它，以及它调用了哪些方法。")
    public CallChain getCallChain(
            @ToolParam(description = "完整方法签名") String methodSignature,
            @ToolParam(description = "项目ID") String projectId,
            @ToolParam(description = "调用链深度，默认3") int depth) {
        // ...
    }
}

@Component
@Slf4j
public class VulnerabilityQueryTool {

    @Tool(description = "查询指定Maven依赖的已知CVE漏洞信息，返回漏洞详情和修复版本建议。")
    public List<VulnerabilityInfo> queryVulnerabilities(
            @ToolParam(description = "Maven groupId") String groupId,
            @ToolParam(description = "Maven artifactId") String artifactId,
            @ToolParam(description = "当前版本号") String version) {
        // 调用OSV API查询漏洞
        // GET https://api.osv.dev/v1/query
        // ...
    }
}

@Component
@Slf4j
public class DependencyTreeTool {

    private final MavenDependencyParser mavenParser;

    public DependencyTreeTool(MavenDependencyParser mavenParser) {
        this.mavenParser = mavenParser;
    }

    @Tool(description = "解析项目的Maven依赖树，包括传递依赖，返回完整的依赖层级结构。")
    public DependencyNode parseDependencyTree(
            @ToolParam(description = "项目ID") String projectId) {
        // 解析pom.xml生成完整依赖树
        // ...
    }
}
```

### 6.5 会话记忆配置

```java
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public JdbcChatMemoryRepository jdbcChatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor memoryChatAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
```

### 6.6 多模型配置

```java
@Configuration
public class ModelConfig {

    @Bean("qwenMaxModel")
    public ChatModel qwenMaxModel(DashScopeConnectionProperties connProps) {
        return DashScopeChatModel.builder()
                .dashScopeApi(new DashScopeApi(connProps.getApiKey()))
                .defaultOptions(DashScopeChatOptions.builder()
                        .model("qwen-max")
                        .temperature(0.3)
                        .build())
                .build();
    }

    @Bean("qwenTurboModel")
    public ChatModel qwenTurboModel(DashScopeConnectionProperties connProps) {
        return DashScopeChatModel.builder()
                .dashScopeApi(new DashScopeApi(connProps.getApiKey()))
                .defaultOptions(DashScopeChatOptions.builder()
                        .model("qwen-turbo")
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(DashScopeConnectionProperties connProps) {
        return DashScopeEmbeddingModel.builder()
                .dashScopeApi(new DashScopeApi(connProps.getApiKey()))
                .defaultOptions(DashScopeEmbeddingOptions.builder()
                        .model("text-embedding-v3")
                        .build())
                .build();
    }
}
```

---

## 7. 代码解析与索引管线

### 7.1 索引管线流程

```
  代码接入                解析阶段               索引阶段             就绪
┌──────────┐         ┌──────────────┐        ┌─────────────┐      ┌──────┐
│ Git Clone│         │  Java文件    │        │  文本分块    │      │      │
│    或    │────────▶│  扫描发现    │───────▶│  智能切分    │─────▶│ 可供 │
│ 压缩包   │         │              │        │  (方法级)    │      │ 查询 │
│  解压    │         └──────┬───────┘        └──────┬──────┘      │      │
└──────────┘                │                       │             └──────┘
                            │                       │
                            ▼                       ▼
                    ┌──────────────┐        ┌─────────────┐
                    │  AST解析     │        │  Embedding   │
                    │ (JavaParser) │        │ (text-emb-v3)│
                    │              │        │              │
                    │ 提取:        │        │ 向量化并     │
                    │ - 类结构     │        │ 写入ES      │
                    │ - 方法签名   │        │              │
                    │ - 调用关系   │        └──────────────┘
                    │ - 注解信息   │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ AST索引      │
                    │ 写入ES       │
                    │ (结构化索引)  │
                    └──────────────┘

  并行处理: pom.xml解析
                    ┌──────────────┐        ┌─────────────┐
                    │  Maven POM   │        │  OSV API    │
                    │  依赖树解析   │───────▶│  漏洞查询   │
                    │              │        │             │
                    └──────────────┘        └──────┬──────┘
                                                   │
                                                   ▼
                                           ┌─────────────┐
                                           │  漏洞数据    │
                                           │  写入PG     │
                                           └─────────────┘
```

### 7.2 智能分块策略

```java
@Component
@Slf4j
public class JavaCodeChunker {

    private static final int MAX_CHUNK_TOKENS = 512;
    private static final int OVERLAP_LINES = 3;

    /**
     * 按方法级别切分Java源码，每个方法作为一个独立chunk。
     * 过长方法会按固定行数二次切分，保留上下文overlap。
     */
    public List<CodeChunk> chunkJavaFile(String filePath, String sourceCode, ParsedClass parsedClass) {
        List<CodeChunk> chunks = new ArrayList<>();

        // 1. 类级别chunk（类签名 + 字段声明）
        chunks.add(buildClassLevelChunk(filePath, parsedClass));

        // 2. 方法级别chunk
        for (ParsedMethod method : parsedClass.getMethods()) {
            String methodSource = method.getSourceCode();
            if (estimateTokens(methodSource) <= MAX_CHUNK_TOKENS) {
                chunks.add(buildMethodChunk(filePath, parsedClass, method, methodSource));
            } else {
                // 过长方法：按行数二次切分
                chunks.addAll(splitLongMethod(filePath, parsedClass, method));
            }
        }

        return chunks;
    }

    private CodeChunk buildMethodChunk(String filePath, ParsedClass cls, ParsedMethod method, String source) {
        return CodeChunk.builder()
                .content(source)
                .filePath(filePath)
                .className(cls.getQualifiedName())
                .methodName(method.getName())
                .startLine(method.getStartLine())
                .endLine(method.getEndLine())
                .chunkType(ChunkType.METHOD)
                .metadata(Map.of(
                        "annotations", String.join(",", method.getAnnotations()),
                        "returnType", method.getReturnType(),
                        "complexity", method.getComplexity()
                ))
                .build();
    }
}
```

### 7.3 ES索引结构设计

```json
{
  "code_vectors": {
    "mappings": {
      "properties": {
        "content": { "type": "text", "analyzer": "standard" },
        "embedding": { "type": "dense_vector", "dims": 1024, "similarity": "cosine" },
        "projectId": { "type": "keyword" },
        "filePath": { "type": "keyword" },
        "className": { "type": "keyword" },
        "methodName": { "type": "keyword" },
        "chunkType": { "type": "keyword" },
        "startLine": { "type": "integer" },
        "endLine": { "type": "integer" },
        "annotations": { "type": "keyword" },
        "packageName": { "type": "keyword" },
        "lastModified": { "type": "date" }
      }
    }
  },
  "ast_index": {
    "mappings": {
      "properties": {
        "projectId": { "type": "keyword" },
        "qualifiedName": { "type": "keyword" },
        "className": { "type": "keyword" },
        "classType": { "type": "keyword" },
        "packageName": { "type": "keyword" },
        "superClass": { "type": "keyword" },
        "interfaces": { "type": "keyword" },
        "annotations": { "type": "keyword" },
        "methods": {
          "type": "nested",
          "properties": {
            "name": { "type": "keyword" },
            "signature": { "type": "keyword" },
            "returnType": { "type": "keyword" },
            "annotations": { "type": "keyword" },
            "calledMethods": { "type": "keyword" },
            "complexity": { "type": "integer" }
          }
        },
        "fields": {
          "type": "nested",
          "properties": {
            "name": { "type": "keyword" },
            "type": { "type": "keyword" },
            "annotations": { "type": "keyword" }
          }
        },
        "imports": { "type": "keyword" },
        "filePath": { "type": "keyword" },
        "totalLines": { "type": "integer" }
      }
    }
  }
}
```

### 7.4 异步索引任务（Redis Stream）

```java
@Component
@Slf4j
public class IndexingTaskProducer {

    private final StringRedisTemplate redisTemplate;
    private static final String STREAM_KEY = "codeinsight:task:indexing";

    public IndexingTaskProducer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String submitIndexTask(String projectId, TaskType taskType) {
        String taskId = UUID.randomUUID().toString();
        Map<String, String> message = Map.of(
                "taskId", taskId,
                "projectId", projectId,
                "taskType", taskType.name(),
                "submittedAt", Instant.now().toString()
        );

        redisTemplate.opsForStream().add(
                StreamRecords.string(message).withStreamKey(STREAM_KEY));

        return taskId;
    }
}

@Component
@Slf4j
public class IndexingTaskConsumer implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final IndexingPipeline indexingPipeline;
    private final AsyncTaskService asyncTaskService;

    private static final String STREAM_KEY = "codeinsight:task:indexing";
    private static final String GROUP_NAME = "indexing-group";
    private static final String CONSUMER_NAME = "consumer-1";

    @Override
    public void afterPropertiesSet() {
        // 创建消费者组（如不存在）
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.debug("Consumer group already exists");
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeTasks() {
        List<MapRecord<String, String, String>> records = redisTemplate.opsForStream()
                .read(Consumer.from(GROUP_NAME, CONSUMER_NAME),
                      StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                      StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, String, String> record : records) {
            try {
                processTask(record.getValue());
                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
            } catch (Exception e) {
                log.error("Failed to process indexing task: {}", record.getValue(), e);
            }
        }
    }

    private void processTask(Map<String, String> taskData) {
        String taskId = taskData.get("taskId");
        String projectId = taskData.get("projectId");
        TaskType taskType = TaskType.valueOf(taskData.get("taskType"));

        asyncTaskService.updateStatus(taskId, TaskStatus.RUNNING, 0, "开始索引...");

        try {
            indexingPipeline.execute(projectId, taskType, (percent, msg) ->
                    asyncTaskService.updateStatus(taskId, TaskStatus.RUNNING, percent, msg));
            asyncTaskService.updateStatus(taskId, TaskStatus.COMPLETED, 100, "索引完成");
        } catch (Exception e) {
            asyncTaskService.updateStatus(taskId, TaskStatus.FAILED, -1, e.getMessage());
        }
    }
}
```

---

## 8. REST API设计

### 8.1 API总览

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 认证 | `/api/v1/auth` | 注册、登录、Token刷新 |
| 用户 | `/api/v1/users` | 用户管理（ADMIN） |
| 项目 | `/api/v1/projects` | 项目CRUD、索引触发 |
| 对话 | `/api/v1/conversations` | 会话管理 |
| 聊天 | `/api/v1/chat` | Agent对话（SSE流式） |
| 报告 | `/api/v1/reports` | 分析报告查看 |
| 任务 | `/api/v1/tasks` | 异步任务状态查询 |

### 8.2 核心API详细设计

#### 8.2.1 认证API

```
POST   /api/v1/auth/register          # 用户注册
POST   /api/v1/auth/login             # 用户登录 → 返回JWT Token
POST   /api/v1/auth/refresh           # 刷新Token
```

#### 8.2.2 项目管理API

```
POST   /api/v1/projects               # 创建项目（Git URL或上传压缩包）
GET    /api/v1/projects               # 项目列表（分页）
GET    /api/v1/projects/{id}          # 项目详情
DELETE /api/v1/projects/{id}          # 删除项目
POST   /api/v1/projects/{id}/index    # 触发索引（全量/增量）
GET    /api/v1/projects/{id}/index/status  # 索引状态查询
POST   /api/v1/projects/{id}/sync     # Git同步最新代码
```

#### 8.2.3 聊天API（SSE流式）

```
POST   /api/v1/chat/stream            # SSE流式对话（核心接口）
```

**请求体：**

```json
{
  "projectId": "uuid",
  "conversationId": "uuid | null",
  "message": "OrderService的安全隐患有哪些？",
  "scenario": "SECURITY"
}
```

**SSE事件流格式：**

```
event: metadata
data: {"conversationId":"uuid","agentName":"security-agent","model":"qwen-max"}

event: content
data: {"text":"根据对OrderService的安全分析，"}

event: content
data: {"text":"发现以下安全隐患：\n\n"}

event: tool_call
data: {"tool":"searchCode","args":{"query":"OrderService","projectId":"xxx"}}

event: content
data: {"text":"1. **SQL注入风险**..."}

event: done
data: {"tokenUsage":{"promptTokens":1200,"completionTokens":800},"duration":3500}
```

**Controller实现：**

```java
@RestController
@RequestMapping("/api/v1/chat")
@Slf4j
public class ChatController {

    private final OrchestratorService orchestratorService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestBody @Valid ChatRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        return orchestratorService.chat(request, user.getId())
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.type())
                        .data(event.toJson())
                        .build());
    }
}
```

#### 8.2.4 报告API

```
GET    /api/v1/reports?projectId={id}&type={type}   # 报告列表
GET    /api/v1/reports/{id}                          # 报告详情
POST   /api/v1/reports/generate                      # 触发生成报告（异步）
DELETE /api/v1/reports/{id}                          # 删除报告
```

#### 8.2.5 任务API

```
GET    /api/v1/tasks?projectId={id}        # 任务列表
GET    /api/v1/tasks/{id}                  # 任务详情（含进度）
POST   /api/v1/tasks/{id}/cancel           # 取消任务
```

### 8.3 统一响应格式

```java
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private PageMeta meta;      // 分页信息（仅列表接口）

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> fail(String error) {
        return ApiResponse.<T>builder().success(false).error(error).build();
    }
}

@Data
@Builder
public class PageMeta {
    private long total;
    private int page;
    private int size;
}
```

---

## 9. 数据库设计

### 9.1 ER图

```
┌──────────────┐     1:N     ┌───────────────────┐     1:N     ┌────────────────────────┐
│   ci_user    │────────────▶│  ci_conversation  │────────────▶│ci_conversation_message │
│              │             │                   │             │                        │
│ id (PK)      │             │ id (PK)           │             │ id (PK)                │
│ username     │             │ title             │             │ conversation_id (FK)   │
│ password_hash│             │ project_id (FK)   │             │ role                   │
│ display_name │             │ user_id (FK)      │             │ content                │
│ email        │             │ scenario_type     │             │ metadata (jsonb)       │
│ role         │             │ created_at        │             │ token_count            │
│ enabled      │             │ updated_at        │             │ created_at             │
│ last_login_at│             └───────────────────┘             └────────────────────────┘
│ created_at   │
└──────┬───────┘
       │  1:N
       ▼
┌──────────────┐     1:N     ┌───────────────────┐
│  ci_project  │────────────▶│ci_analysis_report │
│              │             │                   │
│ id (PK)      │             │ id (PK)           │
│ name         │             │ project_id (FK)   │
│ description  │             │ report_type       │
│ source_type  │             │ status            │
│ git_url      │             │ result_data(jsonb)│
│ git_branch   │             │ summary           │
│ index_status │             │ issue_count       │
│ total_files  │             │ critical_count    │
│ total_lines  │             │ high_count        │
│ indexed_chunk│             │ started_at        │
│ owner_id(FK) │             │ completed_at      │
│ last_sync_at │             │ created_at        │
│ created_at   │             └───────────────────┘
│ updated_at   │
└──────┬───────┘
       │  1:N
       ▼
┌──────────────┐             ┌───────────────────┐
│ci_async_task │             │  ci_audit_log     │
│              │             │                   │
│ id (PK)      │             │ id (PK)           │
│ task_type    │             │ user_id           │
│ project_id   │             │ username          │
│ status       │             │ action            │
│ progress_%   │             │ resource_type     │
│ progress_msg │             │ resource_id       │
│ params(jsonb)│             │ details (jsonb)   │
│ error_message│             │ ip_address        │
│ started_at   │             │ user_agent        │
│ completed_at │             │ created_at        │
│ created_at   │             └───────────────────┘
└──────────────┘

                             ┌───────────────────┐
                             │ ci_chat_memory     │  (Spring AI JDBC Memory)
                             │                   │
                             │ conversation_id   │
                             │ content           │
                             │ type              │
                             │ timestamp         │
                             └───────────────────┘
```

### 9.2 DDL（PostgreSQL）

```sql
-- 用户表
CREATE TABLE ci_user (
    id            VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100),
    email         VARCHAR(100),
    role          VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 项目表
CREATE TABLE ci_project (
    id                 VARCHAR(36) PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    source_type        VARCHAR(20)  NOT NULL,
    git_url            VARCHAR(500),
    git_branch         VARCHAR(100) DEFAULT 'main',
    git_credential_id  VARCHAR(36),
    index_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_files        INTEGER      DEFAULT 0,
    total_lines        INTEGER      DEFAULT 0,
    indexed_chunks     INTEGER      DEFAULT 0,
    last_sync_at       TIMESTAMPTZ,
    index_started_at   TIMESTAMPTZ,
    index_completed_at TIMESTAMPTZ,
    owner_id           VARCHAR(36)  REFERENCES ci_user(id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 会话表
CREATE TABLE ci_conversation (
    id            VARCHAR(36) PRIMARY KEY,
    title         VARCHAR(200),
    project_id    VARCHAR(36)  REFERENCES ci_project(id) ON DELETE CASCADE,
    user_id       VARCHAR(36)  REFERENCES ci_user(id),
    scenario_type VARCHAR(20),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 会话消息表
CREATE TABLE ci_conversation_message (
    id              VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES ci_conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    metadata        JSONB,
    token_count     INTEGER     DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conv_msg_conversation ON ci_conversation_message(conversation_id, created_at);

-- 分析报告表
CREATE TABLE ci_analysis_report (
    id             VARCHAR(36) PRIMARY KEY,
    project_id     VARCHAR(36) NOT NULL REFERENCES ci_project(id) ON DELETE CASCADE,
    report_type    VARCHAR(30) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    result_data    JSONB,
    summary        TEXT,
    issue_count    INTEGER     DEFAULT 0,
    critical_count INTEGER     DEFAULT 0,
    high_count     INTEGER     DEFAULT 0,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_report_project ON ci_analysis_report(project_id, report_type);

-- 异步任务表
CREATE TABLE ci_async_task (
    id               VARCHAR(36) PRIMARY KEY,
    task_type        VARCHAR(30) NOT NULL,
    project_id       VARCHAR(36),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress_percent INTEGER     DEFAULT 0,
    progress_message VARCHAR(500),
    params           JSONB,
    error_message    TEXT,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_project_status ON ci_async_task(project_id, status);

-- 审计日志表
CREATE TABLE ci_audit_log (
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36),
    username      VARCHAR(50),
    action        VARCHAR(50)  NOT NULL,
    resource_type VARCHAR(30),
    resource_id   VARCHAR(36),
    details       JSONB,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user    ON ci_audit_log(user_id, created_at);
CREATE INDEX idx_audit_action  ON ci_audit_log(action, created_at);

-- Spring AI JDBC Chat Memory 表（框架自动创建，此处显式定义）
CREATE TABLE ci_chat_memory (
    conversation_id VARCHAR(36)  NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    "timestamp"     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_memory_conv ON ci_chat_memory(conversation_id, "timestamp");
```

---

## 10. 安全设计

### 10.1 JWT认证流程

```
┌────────┐                  ┌──────────────────┐               ┌─────────┐
│  前端   │                  │  Spring Security │               │ 业务API  │
└───┬────┘                  └────────┬─────────┘               └────┬────┘
    │  POST /auth/login              │                              │
    │  {username, password}          │                              │
    │──────────────────────────────▶ │                              │
    │                                │ 验证密码 (BCrypt)             │
    │  200 {accessToken, expiresIn}  │                              │
    │◀────────────────────────────── │                              │
    │                                │                              │
    │  GET /api/v1/projects          │                              │
    │  Authorization: Bearer xxx     │                              │
    │──────────────────────────────▶ │                              │
    │                                │ 验证JWT签名+过期时间          │
    │                                │ 解析userId + role            │
    │                                │──────────────────────────── ▶│
    │                                │                              │ 检查RBAC权限
    │  200 {projects: [...]}         │                              │
    │◀───────────────────────────────┼──────────────────────────────│
```

### 10.2 Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 10.3 RBAC权限矩阵

| 功能 | ADMIN | PROJECT_LEAD | DEVELOPER | GUEST |
|------|:-----:|:------------:|:---------:|:-----:|
| 用户管理 | ✅ | ❌ | ❌ | ❌ |
| 创建项目 | ✅ | ✅ | ❌ | ❌ |
| 删除项目 | ✅ | ✅（本人的） | ❌ | ❌ |
| 触发索引 | ✅ | ✅ | ❌ | ❌ |
| 代码对话 | ✅ | ✅ | ✅ | ✅（只读） |
| 生成报告 | ✅ | ✅ | ✅ | ❌ |
| 查看报告 | ✅ | ✅ | ✅ | ✅ |
| 审计日志 | ✅ | ❌ | ❌ | ❌ |

### 10.4 审计日志切面

```java
@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepo;

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        UserPrincipal user = SecurityContextHolder.getContext()...;
        HttpServletRequest request = ...;

        AuditLog auditLog = AuditLog.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .action(auditable.action())
                .resourceType(auditable.resourceType())
                .resourceId(extractResourceId(joinPoint))
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepo.save(auditLog);
    }
}

// 使用方式
@Auditable(action = "QUERY", resourceType = "PROJECT")
@PostMapping("/stream")
public Flux<ServerSentEvent<String>> streamChat(...) { ... }
```

---

## 11. 可观测性设计

### 11.1 结构化日志

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdc>true</includeMdc>
            <customFields>{"service":"codeinsight-agent"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### 11.2 自定义指标

```java
@Component
@Slf4j
public class AgentMetrics {

    private final MeterRegistry registry;
    private final Counter chatRequestCounter;
    private final Timer modelCallTimer;
    private final AtomicInteger activeIndexingTasks;

    public AgentMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.chatRequestCounter = Counter.builder("codeinsight.chat.requests")
                .description("Total chat requests")
                .tag("scenario", "all")
                .register(registry);

        this.modelCallTimer = Timer.builder("codeinsight.model.call.duration")
                .description("AI model call duration")
                .register(registry);

        this.activeIndexingTasks = registry.gauge("codeinsight.indexing.active",
                new AtomicInteger(0));
    }

    public void recordChatRequest(ScenarioType scenario) {
        Counter.builder("codeinsight.chat.requests")
                .tag("scenario", scenario.name())
                .register(registry).increment();
    }

    public Timer.Sample startModelCall() {
        return Timer.start(registry);
    }

    public void stopModelCall(Timer.Sample sample, String model) {
        sample.stop(Timer.builder("codeinsight.model.call.duration")
                .tag("model", model)
                .register(registry));
    }
}
```

### 11.3 健康检查

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      show-details: always
  health:
    elasticsearch:
      enabled: true
    redis:
      enabled: true
    db:
      enabled: true
```

---

## 12. 部署方案

### 12.1 Docker Compose

```yaml
# deploy/docker/docker-compose.yml
version: '3.9'

services:
  app:
    build:
      context: ../..
      dockerfile: deploy/docker/Dockerfile
    container_name: codeinsight-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/codeinsight
      SPRING_DATASOURCE_USERNAME: codeinsight
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_ELASTICSEARCH_URIS: http://elasticsearch:9200
      SPRING_DATA_REDIS_HOST: redis
      SPRING_AI_DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}
      JAVA_OPTS: "-Xms1g -Xmx4g -XX:+UseZGC"
    depends_on:
      postgres:
        condition: service_healthy
      elasticsearch:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - repo-data:/data/repos
    restart: unless-stopped

  postgres:
    image: postgres:16-alpine
    container_name: codeinsight-postgres
    environment:
      POSTGRES_DB: codeinsight
      POSTGRES_USER: codeinsight
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - pg-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U codeinsight"]
      interval: 10s
      timeout: 5s
      retries: 5

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
    container_name: codeinsight-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms2g -Xmx2g
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5

  redis:
    image: redis:7.4-alpine
    container_name: codeinsight-redis
    command: redis-server --appendonly yes --maxmemory 1gb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  prometheus:
    image: prom/prometheus:latest
    container_name: codeinsight-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ../prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    depends_on:
      - app

volumes:
  pg-data:
  es-data:
  redis-data:
  repo-data:
```

### 12.2 Dockerfile

```dockerfile
# deploy/docker/Dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY codeinsight-web/target/codeinsight-web-*.jar app.jar

RUN addgroup -S app && adduser -S app -G app
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 12.3 核心配置文件

```yaml
# application.yml
spring:
  application:
    name: codeinsight-agent

  datasource:
    url: jdbc:postgresql://localhost:5432/codeinsight
    username: codeinsight
    password: ${DB_PASSWORD:changeme}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        default_schema: public

  elasticsearch:
    uris: http://localhost:9200

  data:
    redis:
      host: localhost
      port: 6379

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-max
          temperature: 0.3

  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

# CodeInsight 自定义配置
codeinsight:
  repo:
    base-path: /data/repos               # Git克隆/解压存储路径
  indexing:
    chunk-max-tokens: 512                 # 分块最大Token数
    chunk-overlap-lines: 3                # 分块重叠行数
    batch-size: 100                       # 批量向量化大小
    parallelism: 4                        # 解析并行度
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000                # 24小时
  vulnerability:
    osv-api-url: https://api.osv.dev/v1   # OSV漏洞数据库API

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      show-details: always
  tracing:
    sampling:
      probability: 1.0
```

---

## 13. 测试策略

### 13.1 测试框架与依赖

| 组件 | 技术选型 | 版本 | 用途 |
|------|---------|------|------|
| 测试框架 | Spring Boot Test + JUnit 5 | 随Spring Boot 3.5 | 集成测试基础框架 |
| API Mock | WireMock | **3.10+** | 模拟DashScope AI API、OSV漏洞API等外部服务 |
| 数据库测试 | Testcontainers | **1.20+** | 启动真实PostgreSQL/ES/Redis容器进行集成测试 |
| 断言库 | AssertJ | 随Spring Boot | 流畅断言 |
| HTTP测试 | WebTestClient | 随Spring Boot | 响应式端点测试（SSE流式） |

**Maven依赖：**

```xml
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- WireMock -->
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock-standalone</artifactId>
        <version>3.10.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot WireMock 集成 -->
    <dependency>
        <groupId>com.maciejwalkowiak.spring</groupId>
        <artifactId>wiremock-spring-boot</artifactId>
        <version>2.1.3</version>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>elasticsearch</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- WebTestClient (响应式测试) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 13.2 测试分层架构

```
┌──────────────────────────────────────────────────────────────┐
│                   E2E API Tests (端到端)                      │
│                                                              │
│  Spring Boot Test + WireMock + Testcontainers                │
│  完整HTTP请求 → Controller → Agent → Tool → 存储             │
│  外部AI API / 漏洞API 由 WireMock 模拟                       │
├──────────────────────────────────────────────────────────────┤
│                  Integration Tests (集成)                     │
│                                                              │
│  @DataJpaTest / @SpringBootTest(部分)                        │
│  Repository ↔ 真实数据库 (Testcontainers)                    │
│  索引管线 ↔ 真实ES (Testcontainers)                          │
├──────────────────────────────────────────────────────────────┤
│                    Unit Tests (单元)                          │
│                                                              │
│  纯JUnit 5 + Mockito                                        │
│  Service / Parser / Chunker / Tool 独立测试                   │
└──────────────────────────────────────────────────────────────┘
```

### 13.3 WireMock 模拟策略

E2E测试中，所有外部HTTP服务均由WireMock模拟，确保测试隔离且可重复：

| 外部服务 | WireMock Stub | 说明 |
|---------|---------------|------|
| DashScope Chat API | `POST /api/v1/services/aigc/text-generation/generation` | 模拟qwen-max/qwen-turbo的Chat Completion响应（含流式SSE） |
| DashScope Embedding API | `POST /api/v1/services/embeddings/text-embedding/text-embedding` | 模拟text-embedding-v3向量化响应 |
| OSV Vulnerability API | `POST /v1/query` | 模拟CVE漏洞查询响应 |
| Git Remote | — | 使用本地bare repo替代远程Git，无需WireMock |

#### WireMock基础配置

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
    @ConfigureWireMock(name = "dashscope", property = "spring.ai.dashscope.base-url"),
    @ConfigureWireMock(name = "osv", property = "codeinsight.vulnerability.osv-api-url")
})
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected WebTestClient webTestClient;

    @InjectWireMock("dashscope")
    protected WireMockServer dashscopeMock;

    @InjectWireMock("osv")
    protected WireMockServer osvMock;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String authToken;

    @BeforeEach
    void setUp() {
        // 注册测试用户并获取JWT Token
        authToken = registerAndLogin("testuser", "password123");
    }

    @AfterEach
    void tearDown() {
        dashscopeMock.resetAll();
        osvMock.resetAll();
    }

    /**
     * 注册DashScope Chat Completion的WireMock Stub（非流式）
     */
    protected void stubDashScopeChat(String expectedContent) {
        dashscopeMock.stubFor(
            post(urlPathEqualTo("/api/v1/services/aigc/text-generation/generation"))
                .willReturn(okJson("""
                    {
                        "output": {
                            "text": "%s",
                            "finish_reason": "stop"
                        },
                        "usage": {
                            "input_tokens": 100,
                            "output_tokens": 50
                        }
                    }
                    """.formatted(expectedContent))));
    }

    /**
     * 注册DashScope Chat Completion的WireMock Stub（SSE流式）
     */
    protected void stubDashScopeChatStream(String... chunks) {
        StringBuilder sseBody = new StringBuilder();
        for (int i = 0; i < chunks.length; i++) {
            boolean isLast = (i == chunks.length - 1);
            sseBody.append("data: {\"output\":{\"text\":\"")
                   .append(chunks[i])
                   .append("\",\"finish_reason\":\"")
                   .append(isLast ? "stop" : "null")
                   .append("\"}}\n\n");
        }

        dashscopeMock.stubFor(
            post(urlPathEqualTo("/api/v1/services/aigc/text-generation/generation"))
                .withHeader("Accept", containing("text/event-stream"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sseBody.toString())));
    }

    /**
     * 注册DashScope Embedding的WireMock Stub
     */
    protected void stubDashScopeEmbedding(int dimension) {
        float[] vector = new float[dimension];
        Arrays.fill(vector, 0.1f);
        String vectorJson = Arrays.toString(vector);

        dashscopeMock.stubFor(
            post(urlPathEqualTo("/api/v1/services/embeddings/text-embedding/text-embedding"))
                .willReturn(okJson("""
                    {
                        "output": {
                            "embeddings": [{"text_index": 0, "embedding": %s}]
                        },
                        "usage": {"total_tokens": 10}
                    }
                    """.formatted(vectorJson))));
    }

    /**
     * 注册OSV漏洞查询的WireMock Stub
     */
    protected void stubOsvVulnerability(String cveId, String severity, String fixedVersion) {
        osvMock.stubFor(
            post(urlPathEqualTo("/v1/query"))
                .willReturn(okJson("""
                    {
                        "vulns": [{
                            "id": "%s",
                            "summary": "Test vulnerability",
                            "database_specific": {"severity": "%s"},
                            "affected": [{
                                "ranges": [{
                                    "events": [{"fixed": "%s"}]
                                }]
                            }]
                        }]
                    }
                    """.formatted(cveId, severity, fixedVersion))));
    }

    /**
     * 注册OSV无漏洞的WireMock Stub
     */
    protected void stubOsvNoVulnerability() {
        osvMock.stubFor(
            post(urlPathEqualTo("/v1/query"))
                .willReturn(okJson("{\"vulns\": []}")));
    }
}
```

### 13.4 E2E测试用例设计

#### 13.4.1 认证API E2E测试

```java
class AuthE2ETest extends BaseE2ETest {

    @Test
    void shouldRegisterAndLogin() {
        // Given
        var registerReq = Map.of(
            "username", "newuser",
            "password", "SecurePass123!",
            "email", "new@example.com");

        // When - 注册
        var registerResp = restTemplate.postForEntity(
            "/api/v1/auth/register", registerReq, ApiResponse.class);

        // Then
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResp.getBody().isSuccess()).isTrue();

        // When - 登录
        var loginReq = Map.of("username", "newuser", "password", "SecurePass123!");
        var loginResp = restTemplate.postForEntity(
            "/api/v1/auth/login", loginReq, ApiResponse.class);

        // Then
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody().getData()).hasFieldOrProperty("accessToken");
    }

    @Test
    void shouldRejectInvalidCredentials() {
        var loginReq = Map.of("username", "nonexistent", "password", "wrong");

        var resp = restTemplate.postForEntity(
            "/api/v1/auth/login", loginReq, ApiResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

#### 13.4.2 项目管理 + 索引 E2E测试

```java
class ProjectE2ETest extends BaseE2ETest {

    @Test
    void shouldCreateProjectAndTriggerIndexing() {
        // Given - 模拟Embedding API
        stubDashScopeEmbedding(1024);

        var headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        // When - 创建项目（使用测试用的本地Git仓库）
        var createReq = Map.of(
            "name", "test-project",
            "sourceType", "GIT",
            "gitUrl", testRepoPath);

        var createResp = restTemplate.exchange(
            "/api/v1/projects", HttpMethod.POST,
            new HttpEntity<>(createReq, headers), ApiResponse.class);

        assertThat(createResp.getBody().isSuccess()).isTrue();
        String projectId = extractId(createResp.getBody().getData());

        // When - 触发索引
        var indexResp = restTemplate.exchange(
            "/api/v1/projects/" + projectId + "/index", HttpMethod.POST,
            new HttpEntity<>(headers), ApiResponse.class);

        assertThat(indexResp.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String taskId = extractId(indexResp.getBody().getData());

        // Then - 轮询任务状态直到完成
        await().atMost(Duration.ofSeconds(30))
               .pollInterval(Duration.ofSeconds(1))
               .untilAsserted(() -> {
                   var taskResp = restTemplate.exchange(
                       "/api/v1/tasks/" + taskId, HttpMethod.GET,
                       new HttpEntity<>(headers), ApiResponse.class);
                   assertThat(extractField(taskResp, "status")).isEqualTo("COMPLETED");
               });

        // 验证Embedding API被调用
        dashscopeMock.verify(postRequestedFor(
            urlPathEqualTo("/api/v1/services/embeddings/text-embedding/text-embedding")));
    }
}
```

#### 13.4.3 代码问答（SSE流式）E2E测试

```java
class ChatE2ETest extends BaseE2ETest {

    @Test
    void shouldStreamChatResponse() {
        // Given - 模拟DashScope流式响应
        stubDashScopeChatStream(
            "OrderService",
            "是一个订单管理服务，",
            "负责处理订单的创建和查询。"
        );
        stubDashScopeEmbedding(1024);

        // 预先创建项目并完成索引
        String projectId = createAndIndexProject();

        // When - 发起SSE流式对话
        var chatRequest = Map.of(
            "projectId", projectId,
            "message", "OrderService是做什么的？",
            "scenario", "QA");

        Flux<ServerSentEvent<String>> eventStream = webTestClient
            .post().uri("/api/v1/chat/stream")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(chatRequest)
            .exchange()
            .expectStatus().isOk()
            .returnResult(ServerSentEvent.class)
            .getResponseBody()
            .cast(ServerSentEvent.class);

        // Then - 验证SSE事件流
        StepVerifier.create(eventStream)
            .expectNextMatches(event -> "metadata".equals(event.event()))
            .thenConsumeWhile(event -> "content".equals(event.event()),
                event -> assertThat(event.data()).isNotNull())
            .expectNextMatches(event -> "done".equals(event.event()))
            .verifyComplete();

        // 验证DashScope API被调用
        dashscopeMock.verify(postRequestedFor(
            urlPathEqualTo("/api/v1/services/aigc/text-generation/generation")));
    }

    @Test
    void shouldMaintainMultiTurnConversation() {
        stubDashScopeChat("OrderService位于com.example.order包下。");
        stubDashScopeEmbedding(1024);
        String projectId = createAndIndexProject();

        var headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 第1轮对话
        var req1 = Map.of("projectId", projectId, "message", "OrderService在哪个包？", "scenario", "QA");
        var resp1 = restTemplate.exchange("/api/v1/chat/stream",
            HttpMethod.POST, new HttpEntity<>(req1, headers), String.class);
        String conversationId = extractConversationId(resp1.getBody());

        // 第2轮对话 - 携带conversationId实现上下文关联
        stubDashScopeChat("它有3个public方法：createOrder、getOrder、cancelOrder。");
        var req2 = Map.of(
            "projectId", projectId,
            "conversationId", conversationId,
            "message", "它有哪些public方法？",
            "scenario", "QA");

        var resp2 = restTemplate.exchange("/api/v1/chat/stream",
            HttpMethod.POST, new HttpEntity<>(req2, headers), String.class);

        assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证DashScope被调用2次（两轮对话）
        dashscopeMock.verify(2, postRequestedFor(
            urlPathEqualTo("/api/v1/services/aigc/text-generation/generation")));
    }
}
```

#### 13.4.4 依赖漏洞分析 E2E测试

```java
class DependencyE2ETest extends BaseE2ETest {

    @Test
    void shouldDetectVulnerableDepAndSuggestUpgrade() {
        // Given - 模拟OSV返回log4j漏洞
        stubOsvVulnerability("CVE-2021-44228", "CRITICAL", "2.17.0");
        stubDashScopeChat("发现1个严重漏洞：log4j-core 2.14.1存在CVE-2021-44228远程代码执行漏洞，建议升级到2.17.0。");
        stubDashScopeEmbedding(1024);

        String projectId = createAndIndexProject();

        var headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        // When - 触发依赖漏洞分析
        var req = Map.of("projectId", projectId, "message", "分析项目依赖漏洞", "scenario", "DEPENDENCY");
        var resp = restTemplate.exchange("/api/v1/chat/stream",
            HttpMethod.POST, new HttpEntity<>(req, headers), String.class);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("CVE-2021-44228");

        // 验证OSV API被调用
        osvMock.verify(postRequestedFor(urlPathEqualTo("/v1/query")));
    }

    @Test
    void shouldReportCleanWhenNoCVE() {
        // Given - OSV返回无漏洞
        stubOsvNoVulnerability();
        stubDashScopeChat("项目依赖安全检查通过，未发现已知CVE漏洞。");
        stubDashScopeEmbedding(1024);

        String projectId = createAndIndexProject();

        var headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        var req = Map.of("projectId", projectId, "message", "检查依赖安全性", "scenario", "DEPENDENCY");
        var resp = restTemplate.exchange("/api/v1/chat/stream",
            HttpMethod.POST, new HttpEntity<>(req, headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

#### 13.4.5 安全审查 E2E测试

```java
class SecurityScanE2ETest extends BaseE2ETest {

    @Test
    void shouldDetectHardcodedSecret() {
        // Given - 模拟AI识别出硬编码密钥
        stubDashScopeChat("""
            发现1个安全问题：
            **[CRITICAL] 硬编码API密钥** - DatabaseConfig.java:15
            `private String apiKey = "sk-proj-xxxxx";`
            修复建议：改为从环境变量读取 `@Value("${api.key}") private String apiKey;`
            """);
        stubDashScopeEmbedding(1024);

        String projectId = createAndIndexProject();

        var headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        // When
        var req = Map.of("projectId", projectId, "message", "扫描项目中的安全隐患", "scenario", "SECURITY");
        var resp = restTemplate.exchange("/api/v1/chat/stream",
            HttpMethod.POST, new HttpEntity<>(req, headers), String.class);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("CRITICAL");
    }
}
```

### 13.5 Testcontainers 基础设施配置

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("codeinsight_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    @ServiceConnection
    public ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.15.0")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    }

    @Bean
    @ServiceConnection
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7.4-alpine")
                .withExposedPorts(6379);
    }
}
```

### 13.6 测试覆盖要求

| 测试层级 | 覆盖目标 | 关键覆盖范围 |
|---------|---------|-------------|
| **E2E API测试** | 所有API端点 | 认证流程、项目CRUD、SSE流式对话、报告生成、任务管理 |
| **WireMock验证** | 所有外部调用 | DashScope Chat/Embedding、OSV漏洞API的请求参数和调用次数 |
| **集成测试** | Repository层 | JPA查询、ES向量存储读写、Redis缓存命中 |
| **单元测试** | 核心逻辑 | AST解析器、代码分块器、JWT工具、权限校验 |
| **总体覆盖率** | **≥ 80%** | 由JaCoCo在Maven verify阶段强制检查 |

### 13.7 测试Profile配置

```yaml
# application-test.yml
spring:
  ai:
    dashscope:
      api-key: test-key              # WireMock不校验，使用占位值
      base-url: http://localhost:${wiremock.server.dashscope.port}

codeinsight:
  vulnerability:
    osv-api-url: http://localhost:${wiremock.server.osv.port}
  repo:
    base-path: ${java.io.tmpdir}/codeinsight-test-repos
  indexing:
    batch-size: 10                   # 测试环境用小批量加速
    parallelism: 2
```

---

## 14. 关键时序图

### 13.1 用户对话流程（SSE流式）

```
  用户(前端)         ChatController      OrchestratorAgent     SpecialistAgent      Tools             ES/PG
    │                    │                    │                    │                 │                 │
    │ POST /chat/stream  │                    │                    │                 │                 │
    │ {message, project} │                    │                    │                 │                 │
    │───────────────────▶│                    │                    │                 │                 │
    │                    │ chat(request)      │                    │                 │                 │
    │                    │───────────────────▶│                    │                 │                 │
    │                    │                    │ 意图识别            │                 │                 │
    │                    │                    │ (qwen-turbo)       │                 │                 │
    │                    │                    │                    │                 │                 │
    │                    │                    │ route to agent     │                 │                 │
    │                    │                    │───────────────────▶│                 │                 │
    │                    │                    │                    │ @Tool调用       │                 │
    │                    │                    │                    │ searchCode()    │                 │
    │                    │                    │                    │────────────────▶│                 │
    │                    │                    │                    │                 │ vector search   │
    │                    │                    │                    │                 │────────────────▶│
    │                    │                    │                    │                 │ search results  │
    │                    │                    │                    │                 │◀────────────────│
    │                    │                    │                    │ code context    │                 │
    │                    │                    │                    │◀────────────────│                 │
    │ SSE: event=content │                    │                    │                 │                 │
    │ data: "根据分析..." │                    │                    │ 流式生成回答      │                 │
    │◀───────────────────┼────────────────────┼────────────────────│ (qwen-max)     │                 │
    │ SSE: event=content │                    │                    │                 │                 │
    │ data: "发现以下..."  │                    │                    │                 │                 │
    │◀───────────────────┼────────────────────┼────────────────────│                 │                 │
    │ ...                │                    │                    │                 │                 │
    │ SSE: event=done    │                    │                    │                 │                 │
    │◀───────────────────│                    │                    │                 │                 │
    │                    │                    │                    │ 保存消息到PG     │                 │
    │                    │                    │                    │─────────────────┼────────────────▶│
```

### 13.2 代码索引流程

```
  用户(前端)        ProjectController    IndexTaskProducer    Redis Stream     IndexTaskConsumer      JavaParser        ES
    │                    │                    │                    │                 │                    │              │
    │ POST /projects     │                    │                    │                 │                    │              │
    │ {gitUrl, name}     │                    │                    │                 │                    │              │
    │───────────────────▶│                    │                    │                 │                    │              │
    │                    │ git clone          │                    │                 │                    │              │
    │                    │──────────(JGit)    │                    │                 │                    │              │
    │                    │                    │                    │                 │                    │              │
    │ POST /projects     │                    │                    │                 │                    │              │
    │   /{id}/index      │                    │                    │                 │                    │              │
    │───────────────────▶│ submitIndexTask()  │                    │                 │                    │              │
    │                    │───────────────────▶│ XADD               │                 │                    │              │
    │                    │                    │───────────────────▶│                 │                    │              │
    │ 202 {taskId}       │                    │                    │                 │                    │              │
    │◀───────────────────│                    │                    │                 │                    │              │
    │                    │                    │                    │ XREADGROUP      │                    │              │
    │                    │                    │                    │◀────────────────│                    │              │
    │                    │                    │                    │                 │                    │              │
    │                    │                    │                    │                 │ 扫描.java文件       │              │
    │                    │                    │                    │                 │ 逐文件AST解析       │              │
    │                    │                    │                    │                 │───────────────────▶│              │
    │                    │                    │                    │                 │ ParsedClass        │              │
    │                    │                    │                    │                 │◀───────────────────│              │
    │                    │                    │                    │                 │                    │              │
    │                    │                    │                    │                 │ 智能分块 + Embedding               │
    │                    │                    │                    │                 │ 批量写入ES (100/batch)             │
    │                    │                    │                    │                 │─────────────────────────────────▶ │
    │                    │                    │                    │                 │                    │              │
    │ GET /tasks/{id}    │                    │                    │                 │ 更新进度到PG        │              │
    │───────────────────▶│                    │                    │                 │ status=COMPLETED   │              │
    │ {progress: 100%}   │                    │                    │                 │                    │              │
    │◀───────────────────│                    │                    │                 │                    │              │
```

### 13.3 依赖漏洞分析流程

```
  DependencyAgent       DependencyTreeTool    VulnQueryTool        OSV API           LLM(qwen-max)
       │                      │                    │                  │                    │
       │ parseDependencyTree()│                    │                  │                    │
       │─────────────────────▶│                    │                  │                    │
       │                      │ 解析pom.xml        │                  │                    │
       │                      │ 构建完整依赖树      │                  │                    │
       │ DependencyNode tree  │                    │                  │                    │
       │◀─────────────────────│                    │                  │                    │
       │                      │                    │                  │                    │
       │ 遍历每个依赖节点      │                    │                  │                    │
       │ queryVulnerabilities()│                   │                  │                    │
       │──────────────────────┼───────────────────▶│                  │                    │
       │                      │                    │ POST /v1/query   │                    │
       │                      │                    │─────────────────▶│                    │
       │                      │                    │ CVE列表           │                    │
       │                      │                    │◀─────────────────│                    │
       │ VulnerabilityInfo[]  │                    │                  │                    │
       │◀─────────────────────┼────────────────────│                  │                    │
       │                      │                    │                  │                    │
       │ 汇总所有漏洞数据      │                    │                  │                    │
       │ 生成分析报告 Prompt   │                    │                  │                    │
       │────────────────────────────────────────────────────────────▶│                    │
       │                      │                    │                  │ 流式输出:           │
       │                      │                    │                  │ - 漏洞分级汇总      │
       │                      │                    │                  │ - 升级建议          │
       │◀───────────────────────────────────────────────────────────│ - 兼容性风险提示    │
       │ Flux<String>         │                    │                  │                    │
```

---

## 附录A：技术选型决策记录

| 决策项 | 选型 | 理由 |
|--------|------|------|
| 向量存储选ES而非Milvus | Elasticsearch | 全文检索+向量检索一体化，避免多套存储运维；六大场景中代码搜索既需语义检索也需关键词检索 |
| AST解析选JavaParser而非Spoon | JavaParser | 更轻量、API简洁、社区活跃度高，满足方法级解析需求 |
| 异步队列选Redis Stream而非RabbitMQ | Redis Stream | 复用已有Redis实例、索引任务模式简单无需复杂路由，降低运维成本 |
| Agent编排选LlmRoutingAgent | Spring AI Alibaba Agent Framework | 原生支持LLM意图路由，与Spring AI深度集成，避免手写路由逻辑 |
| 会话记忆选JDBC而非Redis | JdbcChatMemoryRepository | 会话历史需要持久化和查询回溯，PG的JSONB + 索引比Redis更适合长期存储 |
| E2E测试选WireMock而非MockServer | WireMock + Spring Boot Test | Spring生态集成最成熟，支持SSE流式Stub，wiremock-spring-boot自动注入端口，Testcontainers提供真实存储层 |

## 附录B：性能预估

| 指标 | 预估值 | 说明 |
|------|--------|------|
| 首次全量索引（200万行） | 30-60分钟 | 4并行度，batch=100，含Embedding API调用 |
| 增量索引（单次PR） | 1-5分钟 | 仅变更文件重新解析和向量化 |
| 代码问答响应（首Token） | 1-3秒 | ES向量检索 + qwen-max首Token延迟 |
| 代码问答完整响应 | 5-15秒 | 取决于回答长度和工具调用次数 |
| 向量索引总量 | 10-50万条 | 按方法级分块，平均每文件10-50个chunk |
| 并发用户支持 | 50-100 | 受限于qwen-max API QPS |

## 附录C：分期实施建议

| 阶段 | 范围 | 预估工期 |
|------|------|---------|
| **P0 - 基础骨架** | 项目管理、代码接入、AST解析、ES索引管线、基础问答Agent | 第1-3周 |
| **P1 - 核心Agent** | 代码审查Agent、架构分析Agent、多Agent编排、SSE流式 | 第4-6周 |
| **P2 - 安全与依赖** | 依赖漏洞分析Agent、安全审查Agent、报告系统 | 第7-9周 |
| **P3 - 企业特性** | RBAC权限、审计日志、会话持久化、Vue前端 | 第10-12周 |
| **P4 - 生产就绪** | 可观测性、Docker部署、性能调优、压力测试 | 第13-14周 |
