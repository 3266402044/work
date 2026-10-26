import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api'

export const useUiStore = defineStore('ui', () => {
  const unread = ref(0)
  const sidebarCollapsed = ref(localStorage.getItem('cp_sidebar') === '1')

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

  return { unread, sidebarCollapsed, refreshUnread, toggleSidebar }
})
