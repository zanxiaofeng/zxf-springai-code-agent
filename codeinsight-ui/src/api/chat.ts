import type { ChatRequest, ChatEvent } from '@/types/api'

const DEFAULT_TIMEOUT_MS = 120_000

export async function streamChat(
  req: ChatRequest,
  onEvent: (event: ChatEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = localStorage.getItem('token')

  // Create internal abort controller for timeout if no external signal
  const internalController = signal ? undefined : new AbortController()
  const effectiveSignal = signal ?? internalController?.signal
  const timeoutId = internalController
    ? setTimeout(() => internalController.abort(), DEFAULT_TIMEOUT_MS)
    : undefined

  try {
    const response = await fetch('/api/v1/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(req),
      signal: effectiveSignal,
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
          parseAndEmit(line.slice(5).trim(), currentEventType, onEvent)
        } else if (line.trim() === '') {
          currentEventType = ''
        }
      }
    }

    // Process remaining buffer after stream ends
    if (buffer.trim()) {
      for (const line of buffer.split('\n')) {
        if (line.startsWith('data:')) {
          parseAndEmit(line.slice(5).trim(), '', onEvent)
        }
      }
    }
  } finally {
    if (timeoutId !== undefined) clearTimeout(timeoutId)
  }
}

function parseAndEmit(raw: string, eventType: string, onEvent: (event: ChatEvent) => void) {
  if (!raw) return
  try {
    const parsed = JSON.parse(raw)
    onEvent({
      type: (eventType || parsed.type || 'content') as ChatEvent['type'],
      data: typeof parsed === 'string' ? { text: parsed } : parsed,
    })
  } catch {
    onEvent({ type: 'content', data: { text: raw } })
  }
}
