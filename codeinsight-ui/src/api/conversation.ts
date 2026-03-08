import { get, del } from './request'
import type { ConversationResponse, MessageResponse } from '@/types/api'

export const conversationApi = {
  list: (projectId: string) =>
    get<ConversationResponse[]>('/conversations', { projectId }),

  getMessages: (id: string) =>
    get<MessageResponse[]>(`/conversations/${id}/messages`),

  remove: (id: string) =>
    del<void>(`/conversations/${id}`),
}
