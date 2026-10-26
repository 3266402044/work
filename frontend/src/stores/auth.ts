import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authApi } from '@/api'

export interface CurrentUser {
  id: number
  username: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  realName: string
  phone?: string
  avatarUrl?: string
  enabled: boolean
  relatedId?: number
  relatedNo?: string
  classId?: number
  className?: string
  department?: string
  lastLoginTime?: string
  createTime?: string
}

export const roleHome = (role?: string) => {
  if (role === 'TEACHER') return '/teacher/home'
  if (role === 'ADMIN') return '/admin/overview'
  return '/student/home'
}

export const roleLabel = (role?: string) =>
  role === 'TEACHER' ? '教师' : role === 'ADMIN' ? '管理员' : '学生'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('cp_token') || '')
  const user = ref<CurrentUser | null>(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const role = computed(() => user.value?.role)
  const home = computed(() => roleHome(role.value))

  function readUser(): CurrentUser | null {
    try {
      const raw = localStorage.getItem('cp_user')
      return raw ? (JSON.parse(raw) as CurrentUser) : null
    } catch {
      return null
    }
  }

  function setSession(newToken: string, newUser: CurrentUser) {
    token.value = newToken
    user.value = newUser
    localStorage.setItem('cp_token', newToken)
    localStorage.setItem('cp_user', JSON.stringify(newUser))
  }

  async function login(username: string, password: string, remember = true) {
    const data = await authApi.login({ username, password })
    if (!remember) {
      // 仍然保存 token 以便刷新页面保持会话，仅移除“记住账号”标记
      localStorage.removeItem('cp_remember')
    }
    setSession(data.token, data.user)
    return data.user
  }

  async function fetchMe() {
    const me = await authApi.me()
    user.value = me
    localStorage.setItem('cp_user', JSON.stringify(me))
    return me
  }

  function updateUser(partial: Partial<CurrentUser>) {
    if (!user.value) return
    user.value = { ...user.value, ...partial }
    localStorage.setItem('cp_user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('cp_token')
    localStorage.removeItem('cp_user')
  }

  return { token, user, isLoggedIn, role, home, login, fetchMe, updateUser, logout }
})
