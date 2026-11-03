import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api'

export type ThemeMode = 'dark' | 'light'

export const useUiStore = defineStore('ui', () => {
  const unread = ref(0)
  const sidebarCollapsed = ref(localStorage.getItem('cp_sidebar') !== '0')
  const theme = ref<ThemeMode>((localStorage.getItem('cp_theme') as ThemeMode) || 'dark')

  async function refreshUnread() {
    try {
      const data = await notificationApi.unreadCount()
      unread.value = data.count
    } catch {
      unread.value = 0
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('cp_sidebar', sidebarCollapsed.value ? '1' : '0')
  }

  function applyTheme() {
    const dark = theme.value === 'dark'
    document.documentElement.classList.toggle('dark', dark)
    document.documentElement.setAttribute('data-theme', theme.value)
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', dark ? '#0B1020' : '#F3F6FB')
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('cp_theme', theme.value)
    applyTheme()
  }

  return { unread, sidebarCollapsed, theme, refreshUnread, toggleSidebar, applyTheme, toggleTheme }
})
