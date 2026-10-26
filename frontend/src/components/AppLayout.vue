<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessageBox } from 'element-plus'
import { roleLabel, useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { avatarColor } from '@/utils/format'

const auth = useAuthStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()

const isMobile = ref(window.innerWidth <= 900)
const mobileOpen = ref(false)
const search = ref('')
const now = ref(dayjs())

const onResize = () => {
  isMobile.value = window.innerWidth <= 900
  if (!isMobile.value) mobileOpen.value = false
}

let clock: number | null = null
onMounted(() => {
  window.addEventListener('resize', onResize)
  clock = window.setInterval(() => {
    now.value = dayjs()
  }, 1000)
  ui.refreshUnread()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (clock) window.clearInterval(clock)
})

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  }
)

const collapsed = computed(() => ui.sidebarCollapsed && !isMobile.value)

const navItems = computed(() => {
  if (auth.role === 'TEACHER') {
    return [
      { path: '/teacher/home', title: '教师工作台', icon: 'Odometer' },
      { path: '/teacher/tasks/create', title: '创建签到', icon: 'Plus' },
      { path: '/teacher/tasks', title: '签到任务', icon: 'Tickets' },
      { path: '/teacher/stats', title: '课程统计', icon: 'TrendCharts' },
      { path: '/teacher/rollcall', title: '随机点名', icon: 'Aim' },
      { path: '/teacher/interaction', title: '互动控制台', icon: 'ChatDotRound' }
    ]
  }
  if (auth.role === 'ADMIN') {
    return [
      { path: '/admin/overview', title: '运营总览', icon: 'DataAnalysis' },
      { path: '/admin/data', title: '基础数据', icon: 'Files' },
      { path: '/admin/logs', title: '系统日志', icon: 'Monitor' },
      { path: '/admin/settings', title: '系统设置', icon: 'Setting' }
    ]
  }
  return [
    { path: '/student/home', title: '学生首页', icon: 'HomeFilled' },
    { path: '/student/records', title: '我的签到', icon: 'Tickets' },
    { path: '/student/leave', title: '请假申请', icon: 'Document' },
    { path: '/student/calendar', title: '课程日历', icon: 'Calendar' },
    { path: '/student/participation', title: '学习参与度', icon: 'DataAnalysis' }
  ]
})

const isActive = (path: string) => {
  if (path === '/teacher/tasks') return route.path === '/teacher/tasks' || route.path.includes('/monitor')
  if (path === '/student/home') return route.path.startsWith('/student')
  return route.path === path
}

const weekText = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const dateText = computed(() => now.value.format('YYYY年MM月DD日') + ' ' + weekText[now.value.day()])
const timeText = computed(() => now.value.format('HH:mm:ss'))
const initial = computed(() => auth.user?.realName?.slice(0, 1) || 'C')

const doSearch = () => {
  const keyword = search.value.trim()
  if (!keyword) return
  if (auth.role === 'STUDENT') {
    router.push({ path: '/student/records', query: { keyword } })
  } else if (auth.role === 'ADMIN') {
    router.push({ path: '/admin/data', query: { keyword } })
  } else {
    router.push({ path: '/teacher/tasks', query: { keyword } })
  }
}

const onCommand = async (command: string) => {
  if (command === 'profile') router.push('/profile')
  if (command === 'notifications') router.push('/notifications')
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      auth.logout()
      router.push('/login')
    } catch {
      // cancelled
    }
  }
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar" :class="{ collapsed, open: mobileOpen }">
      <div class="brand">
        <div class="brand-mark">CP</div>
        <div v-show="!collapsed" class="brand-text">
          <div class="brand-name">ClassPulse</div>
          <div class="brand-desc">课堂指挥台</div>
        </div>
      </div>

      <nav class="nav">
        <div v-show="!collapsed" class="nav-section">工作区</div>
        <el-tooltip
          v-for="item in navItems"
          :key="item.path"
          :content="item.title"
          placement="right"
          :disabled="!collapsed"
        >
          <router-link :to="item.path" class="nav-item" :class="{ active: isActive(item.path) }">
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-text">{{ item.title }}</span>
            <span v-if="isActive(item.path)" class="active-bar" />
          </router-link>
        </el-tooltip>

        <div class="nav-divider" />
        <el-tooltip content="个人中心" placement="right" :disabled="!collapsed">
          <router-link to="/profile" class="nav-item" :class="{ active: route.path === '/profile' }">
            <el-icon class="nav-icon"><User /></el-icon>
            <span v-show="!collapsed" class="nav-text">个人中心</span>
          </router-link>
        </el-tooltip>
        <el-tooltip content="消息中心" placement="right" :disabled="!collapsed">
          <router-link to="/notifications" class="nav-item" :class="{ active: route.path === '/notifications' }">
            <el-icon class="nav-icon"><Message /></el-icon>
            <span v-show="!collapsed" class="nav-text">消息中心</span>
            <span v-if="ui.unread" class="nav-badge">{{ ui.unread }}</span>
          </router-link>
        </el-tooltip>
      </nav>

      <div v-show="!collapsed" class="side-foot">
        <div class="live"><i class="pulse-dot" /> 实时通道已连接</div>
        <div class="version">ClassPulse v1.0 · 2026</div>
      </div>
    </aside>

    <div v-if="mobileOpen" class="mask" @click="mobileOpen = false" />

    <div class="main" :class="{ collapsed }">
      <header class="topbar">
        <div class="tb-left">
          <button class="icon-btn" type="button" @click="isMobile ? (mobileOpen = !mobileOpen) : ui.toggleSidebar()">
            <el-icon><component :is="isMobile ? (mobileOpen ? 'Close' : 'Expand') : (collapsed ? 'Expand' : 'Fold')" /></el-icon>
          </button>
          <div class="date-block hide-mobile">
            <div class="date">{{ dateText }}</div>
            <div class="clock mono">{{ timeText }}</div>
          </div>
        </div>

        <div class="tb-right">
          <el-input
            v-model="search"
            class="search hide-mobile"
            placeholder="搜索任务、学生或记录"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <button class="icon-btn" type="button" @click="router.push('/notifications')">
            <el-badge :value="ui.unread" :hidden="!ui.unread" :max="99">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </button>

          <el-dropdown trigger="click" @command="onCommand">
            <div class="avatar-wrap">
              <div class="avatar" :style="{ color: avatarColor(auth.user?.realName) }">
                <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" :alt="initial" />
                <span v-else>{{ initial }}</span>
              </div>
              <div class="user hide-mobile">
                <div class="name">{{ auth.user?.realName }}</div>
                <div class="role">{{ roleLabel(auth.role) }}</div>
              </div>
              <el-icon class="hide-mobile caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="notifications"><el-icon><Message /></el-icon>消息中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell { min-height: 100vh; }

.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 248px;
  background: linear-gradient(180deg, #0E1524, #0B1120);
  border-right: 1px solid var(--line);
  z-index: 60;
  display: flex;
  flex-direction: column;
  transition: width 0.22s ease, transform 0.22s ease;
}
.sidebar.collapsed { width: 72px; }

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}
.brand-mark {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--mono);
  font-weight: 700;
  font-size: 13px;
  color: var(--cyan);
  background: linear-gradient(145deg, rgba(56, 221, 248, 0.2), rgba(45, 212, 191, 0.08));
  border: 1px solid rgba(56, 221, 248, 0.35);
}
.brand-name { font-size: 15px; font-weight: 650; letter-spacing: -0.01em; line-height: 1.2; }
.brand-desc { font-size: 10px; color: var(--muted); letter-spacing: 0.14em; }

.nav { padding: 12px 10px; display: flex; flex-direction: column; gap: 4px; flex: 1; overflow-y: auto; }
.nav-section {
  font-size: 10px;
  color: var(--muted);
  letter-spacing: 0.14em;
  padding: 6px 12px 8px;
  opacity: 0.7;
}
.nav-item {
  position: relative;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 12px;
  color: var(--text-2);
  transition: background 0.16s ease, color 0.16s ease;
  white-space: nowrap;
  overflow: hidden;
}
.nav-item:hover { background: rgba(148, 163, 184, 0.07); color: var(--text); }
.nav-item.active { background: rgba(56, 221, 248, 0.09); color: var(--cyan); }
.nav-icon { font-size: 17px; flex-shrink: 0; }
.nav-text { font-size: 13px; }
.active-bar {
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 2px;
  border-radius: 2px;
  background: var(--cyan);
}
.nav-badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 107, 107, 0.16);
  color: var(--coral);
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.nav-divider { height: 1px; background: var(--line); margin: 10px 8px; }
.side-foot { padding: 14px 18px 18px; border-top: 1px solid var(--line); }
.live { display: flex; align-items: center; gap: 8px; font-size: 11px; color: var(--muted); }
.version { font-size: 10px; color: var(--muted); opacity: 0.55; margin-top: 6px; }

.main { margin-left: 248px; transition: margin-left 0.22s ease; min-height: 100vh; }
.main.collapsed { margin-left: 72px; }

.topbar {
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(11, 16, 32, 0.82);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}
.tb-left, .tb-right { display: flex; align-items: center; gap: 14px; }
.date-block { border-left: 1px solid var(--line); padding-left: 16px; }
.date { font-size: 12px; color: var(--text-2); line-height: 1.3; }
.clock { font-size: 13px; color: var(--cyan); letter-spacing: 0.06em; }

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  transition: background 0.16s ease, color 0.16s ease;
}
.icon-btn:hover { background: rgba(148, 163, 184, 0.09); color: var(--text); }

.search { width: 260px; }
.search :deep(.el-input__wrapper) { border-radius: 8px; height: 34px; }

.avatar-wrap { display: flex; align-items: center; gap: 9px; cursor: pointer; padding: 4px 6px; border-radius: 8px; }
.avatar-wrap:hover { background: rgba(148, 163, 184, 0.08); }
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: rgba(56, 221, 248, 0.09);
  border: 1px solid rgba(56, 221, 248, 0.24);
  overflow: hidden;
  flex-shrink: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.user { line-height: 1.25; }
.user .name { font-size: 12.5px; color: var(--text); }
.user .role { font-size: 10.5px; color: var(--muted); }
.caret { font-size: 11px; color: var(--muted); }

.content { min-height: calc(100vh - 64px); }

.mask {
  position: fixed;
  inset: 0;
  background: rgba(3, 7, 15, 0.6);
  z-index: 55;
  backdrop-filter: blur(2px);
}

@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); width: 248px; }
  .sidebar.open { transform: translateX(0); }
  .sidebar.collapsed { width: 248px; }
  .main, .main.collapsed { margin-left: 0; }
  .topbar { padding: 0 14px; }
  .tb-right { gap: 8px; }
}
</style>
