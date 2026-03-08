<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '@/api/project'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ProjectResponse, ProjectCreateRequest } from '@/types/api'
import { statusTagType } from '@/utils/status'

const router = useRouter()
const projects = ref<ProjectResponse[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = ref(20)
const loading = ref(false)
const dialogVisible = ref(false)
const createLoading = ref(false)

const createForm = ref<ProjectCreateRequest>({
  name: '',
  description: '',
  sourceType: 'GIT',
  gitUrl: '',
  gitBranch: 'main',
})

async function loadProjects() {
  loading.value = true
  try {
    const res = await projectApi.list(page.value, pageSize.value)
    if (res.success) {
      projects.value = res.data ?? []
      total.value = res.meta?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  createLoading.value = true
  try {
    const res = await projectApi.create(createForm.value)
    if (res.success) {
      ElMessage.success('项目创建成功')
      dialogVisible.value = false
      resetForm()
      await loadProjects()
    } else {
      ElMessage.error(res.error ?? '创建失败')
    }
  } finally {
    createLoading.value = false
  }
}

function resetForm() {
  createForm.value = { name: '', description: '', sourceType: 'GIT', gitUrl: '', gitBranch: 'main' }
}

async function handleDelete(row: ProjectResponse) {
  await ElMessageBox.confirm(`确定要删除项目「${row.name}」吗？`, '确认删除', { type: 'warning' })
  const res = await projectApi.remove(row.id)
  if (res.success) {
    ElMessage.success('删除成功')
    await loadProjects()
  }
}

async function handleIndex(row: ProjectResponse) {
  await ElMessageBox.confirm(`确定要触发「${row.name}」的索引任务吗？`, '触发索引', { type: 'info' })
  const res = await projectApi.triggerIndex(row.id)
  if (res.success) {
    ElMessage.success('索引任务已创建')
    await loadProjects()
  }
}

function goDetail(row: ProjectResponse) {
  router.push(`/projects/${row.id}`)
}

function goChat(row: ProjectResponse) {
  router.push(`/chat/${row.id}`)
}


function handlePageChange(p: number) {
  page.value = p - 1
  loadProjects()
}

onMounted(loadProjects)
</script>

<template>
  <div class="project-list">
    <div class="page-header">
      <h2>项目管理</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        创建项目
      </el-button>
    </div>

    <el-table :data="projects" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="name" label="项目名称" min-width="160">
        <template #default="{ row }">
          <el-link type="primary" @click="goDetail(row)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="sourceType" label="来源" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.sourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="indexStatus" label="索引状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.indexStatus)" size="small">{{ row.indexStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalFiles" label="文件数" width="90" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ row.createdAt?.replace('T', ' ').slice(0, 19) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goChat(row)">对话</el-button>
          <el-button size="small" type="warning" @click="handleIndex(row)">索引</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page + 1"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" title="创建项目" width="520" @close="resetForm">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="createForm.name" placeholder="输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="项目描述（可选）" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-radio-group v-model="createForm.sourceType">
            <el-radio value="GIT">Git 仓库</el-radio>
            <el-radio value="ARCHIVE">归档文件</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="createForm.sourceType === 'GIT'">
          <el-form-item label="Git URL">
            <el-input v-model="createForm.gitUrl" placeholder="https://github.com/user/repo.git" />
          </el-form-item>
          <el-form-item label="分支">
            <el-input v-model="createForm.gitBranch" placeholder="main" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.project-list {
  padding: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
