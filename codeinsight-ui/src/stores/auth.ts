import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { LoginRequest, RegisterRequest } from '@/types/api'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') ?? '')
  const username = ref(localStorage.getItem('username') ?? '')
  const role = ref(localStorage.getItem('role') ?? '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(form: LoginRequest) {
    const res = await authApi.login(form)
    if (res.success && res.data) {
      token.value = res.data.accessToken
      username.value = res.data.username
      role.value = res.data.role
      localStorage.setItem('token', res.data.accessToken)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('role', res.data.role)
    }
    return res
  }

  async function register(form: RegisterRequest) {
    return authApi.register(form)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    router.push('/login')
  }

  return { token, username, role, isLoggedIn, login, register, logout }
})
