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
        <div class="brand-text">
          <div class="brand-name">ClassPulse</div>
          <div class="brand-desc">课堂指挥台</div>
        </div>
      </div>

      <nav class="nav">
        <div class="nav-section">工作区</div>
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :title="collapsed ? item.title : undefined"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-text">{{ item.title }}</span>
        </router-link>

        <div class="nav-divider" />
        <router-link
          to="/profile"
          class="nav-item"
          :class="{ active: route.path === '/profile' }"
          :title="collapsed ? '个人中心' : undefined"
        >
          <el-icon class="nav-icon"><User /></el-icon>
          <span class="nav-text">个人中心</span>
        </router-link>
        <router-link
          to="/notifications"
          class="nav-item"
          :class="{ active: route.path === '/notifications' }"
          :title="collapsed ? '消息中心' : undefined"
        >
          <el-icon class="nav-icon"><Message /></el-icon>
          <span class="nav-text">消息中心</span>
          <span v-if="ui.unread" class="nav-badge">{{ ui.unread }}</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <div class="live">
          <i class="pulse-dot" />
          <span class="side-foot-text">实时通道已连接</span>
        </div>
        <button
          class="rail-btn"
          type="button"
          :title="ui.theme === 'dark' ? '切换到浅色主题' : '切换到深色主题'"
          @click="ui.toggleTheme()"
        >
          <el-icon><component :is="ui.theme === 'dark' ? 'Sunny' : 'Moon'" /></el-icon>
        </button>
      </div>
    </aside>

    <div v-if="mobileOpen" class="mask" @click="mobileOpen = false" />

    <div class="main" :class="{ collapsed }">
      <header class="topbar">
        <div class="tb-left">
          <button class="icon-btn" type="button" @click="isMobile ? (mobileOpen = !mobileOpen) : ui.toggleSidebar()">
            <el-icon><component :is="isMobile ? (mobileOpen ? 'Close' : 'Expand') : (collapsed ? 'Expand' : 'Fold')" /></el-icon>
          </button>
          <div class="hello hide-mobile">
            <div class="hello-label">欢迎回来</div>
            <div class="hello-name">{{ auth.user?.realName }} · {{ roleLabel(auth.role) }}</div>
          </div>
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
              <el-icon class="caret hide-mobile"><ArrowDown /></el-icon>
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
        <router-view v-slot="{ Component, route: viewRoute }">
          <transition name="route-fade" mode="out-in">
            <component :is="Component" :key="viewRoute.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell { min-height: 100vh; }

/* ===== 玻璃图标侧栏 ===== */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 84px;
  background: linear-gradient(180deg, rgba(255,255,255,.10) 0%, rgba(255,255,255,.13) 30%, rgba(255,255,255,.07) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.11);
  backdrop-filter: blur(22px) saturate(130%);
  -webkit-backdrop-filter: blur(22px) saturate(130%);
  z-index: 60;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}
.sidebar:not(.collapsed) { width: 248px; }
.sidebar.collapsed:hover { width: 248px; }

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}
.brand-mark {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--mono);
  font-weight: 700;
  font-size: 13px;
  color: #fff;
  background: linear-gradient(145deg, rgba(120, 220, 210, 0.5), rgba(30, 70, 90, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.28);
}
.brand-text { overflow: hidden; white-space: nowrap; }
.brand-name { font-size: 15px; font-weight: 650; letter-spacing: -0.01em; line-height: 1.2; }
.brand-desc { font-size: 10px; color: rgba(255, 255, 255, 0.55); letter-spacing: 0.14em; }

.nav { padding: 14px 10px; display: flex; flex-direction: column; gap: 4px; flex: 1; overflow-y: auto; overflow-x: hidden; }
.nav-section {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.14em;
  padding: 4px 12px 8px;
  white-space: nowrap;
  overflow: hidden;
}
.nav-item {
  position: relative;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 12px;
  color: rgba(255, 255, 255, 0.72);
  transition: background 0.16s ease, color 0.16s ease;
  white-space: nowrap;
  flex-shrink: 0;
}
.nav-item:hover { background: rgba(255, 255, 255, 0.09); color: #fff; }
.nav-item.active { background: rgba(255, 255, 255, 0.12); color: #fff; }
.nav-item.active::after {
  content: "";
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  border-radius: 3px;
  background: #fff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.6);
}
.nav-icon { font-size: 19px; flex-shrink: 0; width: 22px; text-align: center; }
.nav-text {
  opacity: 0;
  max-width: 0;
  overflow: hidden;
  font-size: 13px;
  transition: opacity 0.18s ease, max-width 0.24s ease;
}
.sidebar:not(.collapsed) .nav-text,
.sidebar.collapsed:hover .nav-text { opacity: 1; max-width: 140px; }
.sidebar:not(.collapsed) .brand-text,
.sidebar.collapsed:hover .brand-text { opacity: 1; }
.brand-text { opacity: 0; transition: opacity 0.18s ease; }
.sidebar:not(.collapsed) .nav-section,
.sidebar.collapsed:hover .nav-section { max-width: 140px; }
.nav-section { max-width: 0; }

.nav-badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 107, 107, 0.85);
  color: #fff;
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.nav-divider { height: 1px; background: rgba(255, 255, 255, 0.08); margin: 10px 8px; }

.side-foot {
  padding: 12px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.live { display: flex; align-items: center; gap: 8px; font-size: 11px; color: rgba(255, 255, 255, 0.6); flex: 1; min-width: 0; }
.side-foot-text { white-space: nowrap; overflow: hidden; opacity: 0; max-width: 0; transition: opacity 0.18s ease, max-width 0.24s ease; }
.sidebar:not(.collapsed) .side-foot-text,
.sidebar.collapsed:hover .side-foot-text { opacity: 1; max-width: 140px; }
.rail-btn {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  transition: background 0.16s ease;
}
.rail-btn:hover { background: rgba(255, 255, 255, 0.16); }

.main { margin-left: 84px; transition: margin-left 0.28s cubic-bezier(0.22, 1, 0.36, 1); min-height: 100vh; }
.main:not(.collapsed) { margin-left: 248px; }

/* ===== 玻璃顶栏 ===== */
.topbar {
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(10, 22, 32, 0.6);
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}
.tb-left, .tb-right { display: flex; align-items: center; gap: 14px; }
.hello { border-left: 1px solid rgba(255, 255, 255, 0.12); padding-left: 16px; line-height: 1.3; }
.hello-label { font-size: 11px; color: rgba(255, 255, 255, 0.55); }
.hello-name { font-size: 13.5px; font-weight: 600; }
.date-block { border-left: 1px solid rgba(255, 255, 255, 0.12); padding-left: 16px; }
.date { font-size: 12px; color: var(--text-2); line-height: 1.3; }
.clock { font-size: 13px; color: var(--cyan); letter-spacing: 0.06em; }

.icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid transparent;
  background: transparent;
  color: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  transition: background 0.16s ease, color 0.16s ease;
}
.icon-btn:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }

.search { width: 260px; }
.search :deep(.el-input__wrapper) { border-radius: 10px; height: 34px; background: rgba(255,255,255,.08) !important; }

.avatar-wrap { display: flex; align-items: center; gap: 7px; cursor: pointer; padding: 3px 6px; border-radius: 10px; }
.avatar-wrap:hover { background: rgba(255, 255, 255, 0.08); }
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(145deg, rgba(120, 220, 210, 0.5), rgba(30, 70, 90, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.28);
  overflow: hidden;
  flex-shrink: 0;
}
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.caret { font-size: 11px; color: rgba(255, 255, 255, 0.5); }

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
  .sidebar.collapsed:hover { width: 248px; }
  .sidebar .nav-text, .sidebar .brand-text, .sidebar .side-foot-text, .sidebar .nav-section { opacity: 1; max-width: 140px; }
  .main, .main:not(.collapsed) { margin-left: 0; }
  .topbar { padding: 0 14px; }
  .tb-right { gap: 8px; }
}
</style>
