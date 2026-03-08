<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import { taskApi } from '@/api/task'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ProjectResponse, TaskResponse } from '@/types/api'
import { statusTagType } from '@/utils/status'

const route = useRoute()
const router = useRouter()
const projectId = route.params.id as string

const project = ref<ProjectResponse | null>(null)
const tasks = ref<TaskResponse[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadProject() {
  loading.value = true
  try {
    const res = await projectApi.getById(projectId)
    if (res.success) project.value = res.data ?? null
  } finally {
    loading.value = false
  }
}

async function loadTasks() {
  const res = await taskApi.list(projectId)
  if (res.success) tasks.value = res.data ?? []
}

async function handleIndex() {
  await ElMessageBox.confirm('确定要触发索引任务吗？', '触发索引', { type: 'info' })
  const res = await projectApi.triggerIndex(projectId)
  if (res.success) {
    ElMessage.success('索引任务已创建')
    await Promise.all([loadProject(), loadTasks()])
  }
}

function goChat() {
  router.push(`/chat/${projectId}`)
}


onMounted(async () => {
  await Promise.all([loadProject(), loadTasks()])
  pollTimer = setInterval(() => {
    if (tasks.value.some((t) => t.status === 'RUNNING' || t.status === 'PENDING')) {
      loadProject()
      loadTasks()
    }
  }, 5000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="project-detail" v-loading="loading">
    <div class="page-header">
      <el-page-header @back="router.push('/projects')">
        <template #content>
          <span>{{ project?.name }}</span>
        </template>
      </el-page-header>
    </div>

    <template v-if="project">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>项目信息</span>
                <div>
                  <el-button type="warning" @click="handleIndex">触发索引</el-button>
                  <el-button type="primary" @click="goChat">进入对话</el-button>
                </div>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="项目名称">{{ project.name }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">
                <el-tag size="small">{{ project.sourceType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="索引状态">
                <el-tag :type="statusTagType(project.indexStatus)" size="small">{{ project.indexStatus }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="文件数">{{ project.totalFiles ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="总行数">{{ project.totalLines ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="索引块数">{{ project.indexedChunks ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="Git URL" :span="2">{{ project.gitUrl || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分支">{{ project.gitBranch || '-' }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ project.description || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后同步">
                {{ project.lastSyncAt?.replace('T', ' ').slice(0, 19) || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ project.createdAt?.replace('T', ' ').slice(0, 19) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card>
            <template #header>任务列表</template>
            <div v-if="tasks.length === 0" class="empty-text">暂无任务</div>
            <div v-for="task in tasks" :key="task.id" class="task-item">
              <div class="task-row">
                <el-tag :type="statusTagType(task.status)" size="small">{{ task.status }}</el-tag>
                <span class="task-type">{{ task.taskType }}</span>
              </div>
              <div v-if="task.progressMessage" class="task-progress">
                {{ task.progressMessage }}
                <el-progress
                  v-if="task.progressPercent > 0"
                  :percentage="task.progressPercent"
                  :stroke-width="4"
                  style="margin-top: 4px"
                />
              </div>
              <div v-if="task.errorMessage" class="task-error">{{ task.errorMessage }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped>
.project-detail {
  padding: 8px;
}

.page-header {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.task-item:last-child {
  border-bottom: none;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-type {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.task-progress {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.task-error {
  font-size: 12px;
  color: var(--el-color-danger);
  margin-top: 4px;
}

.empty-text {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px 0;
}
</style>
