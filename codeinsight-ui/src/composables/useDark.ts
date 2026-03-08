import { ref, watchEffect } from 'vue'

export function useDark() {
  const isDark = ref(localStorage.getItem('theme') === 'dark')

  watchEffect(() => {
    document.documentElement.classList.toggle('dark', isDark.value)
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  })

  return isDark
}

export function useToggle(isDark: ReturnType<typeof ref<boolean>>) {
  return () => {
    isDark.value = !isDark.value
  }
}
