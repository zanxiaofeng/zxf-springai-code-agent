import type { ChatRequest, ChatEvent } from '@/types/api'

export async function streamChat(
  req: ChatRequest,
  onEvent: (event: ChatEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/v1/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(req),
    signal,
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `HTTP ${response.status}`)
  }

  const reader = response.body?.getReader()
  if (!reader) throw new Error('No response body')

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    let currentEventType = ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEventType = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        const raw = line.slice(5).trim()
        if (!raw) continue
        try {
          const parsed = JSON.parse(raw)
          onEvent({
            type: (currentEventType || parsed.type || 'content') as ChatEvent['type'],
            data: typeof parsed === 'string' ? { text: parsed } : parsed,
          })
        } catch {
          onEvent({ type: 'content', data: { text: raw } })
        }
      } else if (line.trim() === '') {
        currentEventType = ''
      }
    }
  }
}
