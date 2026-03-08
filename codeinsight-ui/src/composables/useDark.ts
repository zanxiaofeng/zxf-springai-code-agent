import { ref, watchEffect } from 'vue'

export function useDark() {
  const isDark = ref(localStorage.getItem('theme') === 'dark')

  watchEffect(() => {
    document.documentElement.classList.toggle('dark', isDark.value)
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  })

  const toggle = () => { isDark.value = !isDark.value }

  return { isDark, toggle }
}
