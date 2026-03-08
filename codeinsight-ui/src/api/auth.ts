import { post } from './request'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/api'

export const authApi = {
  login: (data: LoginRequest) => post<LoginResponse>('/auth/login', data),
  register: (data: RegisterRequest) => post<string>('/auth/register', data),
  refresh: () => post<LoginResponse>('/auth/refresh'),
}
