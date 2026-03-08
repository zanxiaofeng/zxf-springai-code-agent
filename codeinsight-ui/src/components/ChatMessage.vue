<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import type { MessageRole } from '@/types/api'

const props = defineProps<{
  role: MessageRole
  content: string
  createdAt?: string
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const renderedContent = computed(() =>
  props.role === 'ASSISTANT' ? md.render(props.content) : props.content,
)

const isUser = computed(() => props.role === 'USER')
</script>

<template>
  <div class="chat-message" :class="{ 'is-user': isUser }">
    <div class="message-avatar">
      <el-avatar :size="32" :style="{ background: isUser ? '#409EFF' : '#67C23A' }">
        {{ isUser ? 'U' : 'AI' }}
      </el-avatar>
    </div>
    <div class="message-body">
      <div v-if="isUser" class="message-text user-text">{{ content }}</div>
      <div v-else class="message-text ai-text chat-content" v-html="renderedContent" />
      <div v-if="createdAt" class="message-time">
        {{ createdAt.replace('T', ' ').slice(0, 19) }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 85%;
}

.chat-message.is-user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.user-text {
  background: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 4px;
  white-space: pre-wrap;
}

.ai-text {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-top-left-radius: 4px;
}

.message-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.is-user .message-time {
  text-align: right;
}
</style>
