import { get, post, del } from './request'
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
}
