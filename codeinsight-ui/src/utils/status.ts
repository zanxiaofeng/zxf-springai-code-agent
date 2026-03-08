type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

const STATUS_TAG_MAP: Record<string, TagType> = {
  COMPLETED: 'success',
  RUNNING: 'warning',
  INDEXING: 'warning',
  FAILED: 'danger',
  CANCELLED: 'info',
}

export function statusTagType(status: string): TagType {
  return STATUS_TAG_MAP[status] ?? 'info'
}
