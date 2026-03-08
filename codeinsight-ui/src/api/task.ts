import { get, post } from './request'
import type { TaskResponse } from '@/types/api'

export const taskApi = {
  getById: (id: string) =>
    get<TaskResponse>(`/tasks/${id}`),

  list: (projectId: string) =>
    get<TaskResponse[]>('/tasks', { projectId }),

  cancel: (id: string) =>
    post<TaskResponse>(`/tasks/${id}/cancel`),
}
