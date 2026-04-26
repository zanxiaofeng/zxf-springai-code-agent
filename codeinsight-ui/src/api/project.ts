import { get, post, del } from './request'
import request from './request'
import type { ProjectCreateRequest, ProjectResponse, TaskResponse } from '@/types/api'

export const projectApi = {
  list: (page = 0, size = 20) =>
    get<ProjectResponse[]>('/projects', { page, size }),

  getById: (id: string) =>
    get<ProjectResponse>(`/projects/${id}`),

  create: (data: ProjectCreateRequest) =>
    post<ProjectResponse>('/projects', data),

  remove: (id: string) =>
    del<void>(`/projects/${id}`),

  triggerIndex: (id: string) =>
    post<TaskResponse>(`/projects/${id}/index`),

  uploadArchive: (id: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request.post<{ success: boolean; data?: ProjectResponse; error?: string }>(
      `/projects/${id}/archive`, form, { timeout: 300000 },
    ).then(r => r.data)
  },
}
