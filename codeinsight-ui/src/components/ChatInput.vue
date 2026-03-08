<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  send: [message: string]
}>()

const message = ref('')

function handleSend() {
  const text = message.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  message.value = ''
}

function handleKeydown(e: KeyboardEvent | Event) {
  if (e instanceof KeyboardEvent && e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input">
    <el-input
      v-model="message"
      type="textarea"
      :rows="3"
      placeholder="输入消息... (Ctrl+Enter 发送)"
      :disabled="disabled"
      resize="none"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      :disabled="!message.trim() || disabled"
      :loading="disabled"
      @click="handleSend"
      class="send-btn"
    >
      <el-icon v-if="!disabled"><Promotion /></el-icon>
      {{ disabled ? '发送中...' : '发送' }}
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.chat-input :deep(.el-textarea__inner) {
  font-size: 14px;
}

.send-btn {
  height: 40px;
  min-width: 80px;
}
</style>
