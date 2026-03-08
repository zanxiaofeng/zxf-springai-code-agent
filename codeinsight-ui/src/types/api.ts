/* ==================== Generic ==================== */

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  meta?: PageMeta
}

export interface PageMeta {
  total: number
  page: number
  size: number
}

/* ==================== Auth ==================== */

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName?: string
  email?: string
}

export interface LoginResponse {
  accessToken: string
  expiresIn: number
  username: string
  role: string
}

/* ==================== Project ==================== */

export type SourceType = 'GIT' | 'ARCHIVE'
export type IndexStatus = 'PENDING' | 'INDEXING' | 'COMPLETED' | 'FAILED'

export interface ProjectCreateRequest {
  name: string
  description?: string
  sourceType: SourceType
  gitUrl?: string
  gitBranch?: string
}

export interface ProjectResponse {
  id: string
  name: string
  description: string
  sourceType: SourceType
  gitUrl: string
  gitBranch: string
  indexStatus: IndexStatus
  totalFiles: number
  totalLines: number
  indexedChunks: number
  lastSyncAt: string
  createdAt: string
  updatedAt: string
}

/* ==================== Chat ==================== */

export type ScenarioType = 'QA' | 'REVIEW' | 'ARCHITECTURE' | 'CODEGEN' | 'DEPENDENCY' | 'SECURITY'

export interface ChatRequest {
  projectId: string
  conversationId?: string
  message: string
  scenario: ScenarioType
}

export interface ChatEvent {
  type: 'metadata' | 'content' | 'done' | 'error'
  data: Record<string, unknown>
}

/* ==================== Conversation ==================== */

export interface ConversationResponse {
  id: string
  title: string
  projectId: string
  scenarioType: ScenarioType
  createdAt: string
  updatedAt: string
}

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'

export interface MessageResponse {
  id: string
  role: MessageRole
  content: string
  tokenCount: number
  createdAt: string
}

/* ==================== Task ==================== */

export type TaskType = 'INDEX_FULL' | 'INDEX_INCREMENTAL' | 'SECURITY_SCAN' | 'DEPENDENCY_SCAN'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export interface TaskResponse {
  id: string
  taskType: TaskType
  projectId: string
  status: TaskStatus
  progressPercent: number
  progressMessage: string
  errorMessage: string
  startedAt: string
  completedAt: string
  createdAt: string
}

/* ==================== Report ==================== */

export type ReportType = 'SECURITY_SCAN' | 'DEPENDENCY_AUDIT' | 'ARCHITECTURE' | 'CODE_REVIEW'
export type ReportStatus = 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface ReportResponse {
  id: string
  projectId: string
  reportType: ReportType
  status: ReportStatus
  resultData: Record<string, unknown>
  summary: string
  issueCount: number
  criticalCount: number
  highCount: number
  startedAt: string
  completedAt: string
  createdAt: string
}
