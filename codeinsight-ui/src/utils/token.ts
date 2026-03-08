export function isTokenExpired(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length !== 3 || !parts[1]) return true
    const payload = JSON.parse(atob(parts[1]))
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}
