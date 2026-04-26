<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const isRegister = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  displayName: '',
  email: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    if (isRegister.value) {
      const res = await auth.register(form)
      if (res.success) {
        ElMessage.success('注册成功，请登录')
        isRegister.value = false
      } else {
        ElMessage.error(res.error ?? '注册失败')
      }
    } else {
      const res = await auth.login({ username: form.username, password: form.password })
      if (res.success) {
        ElMessage.success('登录成功')
        const raw = (route.query.redirect as string) || '/projects'
        const redirect = raw.startsWith('/') && !raw.startsWith('//') ? raw : '/projects'
        router.push(redirect)
      } else {
        ElMessage.error(res.error ?? '登录失败')
      }
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  isRegister.value = !isRegister.value
  formRef.value?.clearValidate()
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
    <el-form-item label="用户名" prop="username">
      <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" />
    </el-form-item>

    <el-form-item label="密码" prop="password">
      <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="'Lock'" />
    </el-form-item>

    <template v-if="isRegister">
      <el-form-item label="显示名称">
        <el-input v-model="form.displayName" placeholder="可选" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="form.email" placeholder="可选" />
      </el-form-item>
    </template>

    <el-form-item>
      <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
        {{ isRegister ? '注 册' : '登 录' }}
      </el-button>
    </el-form-item>

    <div class="toggle-mode">
      <el-link type="primary" @click="toggleMode">
        {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
      </el-link>
    </div>
  </el-form>
</template>

<style scoped>
.toggle-mode {
  text-align: center;
}
</style>
