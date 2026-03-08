<script setup lang="ts">
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { projectApi } from '@/api/project'
import { conversationApi } from '@/api/conversation'
import { streamChat } from '@/api/chat'
import { ElMessage } from 'element-plus'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInput from '@/components/ChatInput.vue'
import type {
  ProjectResponse,
  ConversationResponse,
  MessageResponse,
  ScenarioType,
  ChatEvent,
} from '@/types/api'

const route = useRoute()

const projects = ref<ProjectResponse[]>([])
const conversations = ref<ConversationResponse[]>([])
const messages = ref<MessageResponse[]>([])

const selectedProjectId = ref((route.params.projectId as string) || '')
const selectedConversationId = ref('')
const selectedScenario = ref<ScenarioType>('QA')
const sending = ref(false)
const streamingContent = ref('')
const messagesContainer = ref<HTMLElement>()
let abortController: AbortController | null = null

const scenarioOptions: { value: ScenarioType; label: string }[] = [
  { value: 'QA', label: 'Q&A 问答' },
  { value: 'REVIEW', label: '代码审查' },
  { value: 'ARCHITECTURE', label: '架构分析' },
  { value: 'CODEGEN', label: '代码生成' },
  { value: 'DEPENDENCY', label: '依赖分析' },
  { value: 'SECURITY', label: '安全扫描' },
]

async function loadProjects() {
  const res = await projectApi.list(0, 100)
  if (res.success) projects.value = res.data ?? []
}

async function loadConversations() {
  if (!selectedProjectId.value) {
    conversations.value = []
    return
  }
  const res = await conversationApi.list(selectedProjectId.value)
  if (res.success) conversations.value = res.data ?? []
}

async function loadMessages(convId: string) {
  const res = await conversationApi.getMessages(convId)
  if (res.success) {
    messages.value = res.data ?? []
    await scrollToBottom()
  }
}

function selectConversation(conv: ConversationResponse) {
  selectedConversationId.value = conv.id
  selectedScenario.value = conv.scenarioType
  loadMessages(conv.id)
}

function newConversation() {
  selectedConversationId.value = ''
  messages.value = []
  streamingContent.value = ''
}

async function handleSend(text: string) {
  if (!selectedProjectId.value) {
    ElMessage.warning('请先选择一个项目')
    return
  }

  // Add user message to UI immediately
  messages.value.push({
    id: `temp-${Date.now()}`,
    role: 'USER',
    content: text,
    tokenCount: 0,
    createdAt: new Date().toISOString(),
  })
  await scrollToBottom()

  sending.value = true
  streamingContent.value = ''
  abortController = new AbortController()

  try {
    await streamChat(
      {
        projectId: selectedProjectId.value,
        conversationId: selectedConversationId.value || undefined,
        message: text,
        scenario: selectedScenario.value,
      },
      (event: ChatEvent) => handleStreamEvent(event),
      abortController.signal,
    )

    // Finalize streaming message
    if (streamingContent.value) {
      messages.value.push({
        id: `ai-${Date.now()}`,
        role: 'ASSISTANT',
        content: streamingContent.value,
        tokenCount: 0,
        createdAt: new Date().toISOString(),
      })
      streamingContent.value = ''
    }

    // Reload conversations to get updated list
    await loadConversations()
  } catch (err) {
    if ((err as Error).name !== 'AbortError') {
      ElMessage.error('发送失败: ' + (err as Error).message)
    }
  } finally {
    sending.value = false
    abortController = null
  }
}

function handleStreamEvent(event: ChatEvent) {
  switch (event.type) {
    case 'metadata':
      if (event.data.conversationId) {
        selectedConversationId.value = event.data.conversationId as string
      }
      break
    case 'content':
      streamingContent.value += (event.data.text as string) || ''
      scrollToBottom()
      break
    case 'error':
      ElMessage.error((event.data.message as string) || '处理出错')
      break
    case 'done':
      break
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

async function deleteConversation(conv: ConversationResponse) {
  const res = await conversationApi.remove(conv.id)
  if (res.success) {
    ElMessage.success('已删除')
    if (selectedConversationId.value === conv.id) {
      newConversation()
    }
    await loadConversations()
  }
}

watch(() => selectedProjectId.value, () => {
  newConversation()
  loadConversations()
})

onMounted(async () => {
  await loadProjects()
  if (selectedProjectId.value) await loadConversations()
})
</script>

<template>
  <div class="chat-view">
    <!-- Sidebar -->
    <div class="chat-sidebar">
      <div class="sidebar-section">
        <label class="section-label">选择项目</label>
        <el-select v-model="selectedProjectId" placeholder="选择项目" filterable style="width: 100%">
          <el-option
            v-for="p in projects"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
      </div>

      <div class="sidebar-section">
        <label class="section-label">场景</label>
        <el-select v-model="selectedScenario" style="width: 100%">
          <el-option
            v-for="s in scenarioOptions"
            :key="s.value"
            :label="s.label"
            :value="s.value"
          />
        </el-select>
      </div>

      <div class="sidebar-section conversation-list">
        <div class="section-header">
          <label class="section-label">会话历史</label>
          <el-button size="small" text type="primary" @click="newConversation">
            <el-icon><Plus /></el-icon> 新建
          </el-button>
        </div>

        <div v-if="conversations.length === 0" class="empty-hint">暂无会话</div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conv.id === selectedConversationId }"
          @click="selectConversation(conv)"
        >
          <div class="conv-title">{{ conv.title || '新会话' }}</div>
          <div class="conv-meta">
            <el-tag size="small" type="info">{{ conv.scenarioType }}</el-tag>
            <el-icon class="conv-delete" @click.stop="deleteConversation(conv)"><Delete /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Chat Area -->
    <div class="chat-main">
      <div ref="messagesContainer" class="messages-container">
        <div v-if="messages.length === 0 && !streamingContent" class="empty-chat">
          <el-icon :size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
          <p>选择项目和场景，开始对话</p>
        </div>

        <ChatMessage
          v-for="msg in messages"
          :key="msg.id"
          :role="msg.role"
          :content="msg.content"
          :created-at="msg.createdAt"
        />

        <!-- Streaming AI response -->
        <ChatMessage
          v-if="streamingContent"
          role="ASSISTANT"
          :content="streamingContent"
        />

        <!-- Typing indicator -->
        <div v-if="sending && !streamingContent" class="typing-indicator">
          <span></span><span></span><span></span>
        </div>
      </div>

      <ChatInput :disabled="sending" @send="handleSend" />
    </div>
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  height: calc(100vh - var(--ci-header-height) - 40px);
  gap: 0;
}

.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 16px;
}

.section-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  text-transform: uppercase;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.conv-item:hover {
  background: var(--el-fill-color-light);
}

.conv-item.active {
  background: var(--el-color-primary-light-9);
}

.conv-title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.conv-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-delete {
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  font-size: 14px;
}

.conv-delete:hover {
  color: var(--el-color-danger);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  padding: 20px 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--el-text-color-placeholder);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  max-width: 80px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}
</style>
