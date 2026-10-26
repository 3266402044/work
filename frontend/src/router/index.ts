import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { roleHome, useAuthStore } from '@/stores/auth'
import AppLayout from '@/components/AppLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/student/home' },
      {
        path: 'student/home',
        component: () => import('@/views/student/StudentHome.vue'),
        meta: { roles: ['STUDENT'], title: '学生首页' }
      },
      {
        path: 'student/tasks/:id',
        component: () => import('@/views/student/StudentTaskDetail.vue'),
        meta: { roles: ['STUDENT'], title: '签到任务' }
      },
      {
        path: 'student/records',
        component: () => import('@/views/student/StudentRecords.vue'),
        meta: { roles: ['STUDENT'], title: '我的签到' }
      },
      {
        path: 'student/leave',
        component: () => import('@/views/student/StudentLeave.vue'),
        meta: { roles: ['STUDENT'], title: '请假申请' }
      },
      {
        path: 'student/calendar',
        component: () => import('@/views/student/StudentCalendar.vue'),
        meta: { roles: ['STUDENT'], title: '课程日历' }
      },
      {
        path: 'student/participation',
        component: () => import('@/views/student/StudentParticipation.vue'),
        meta: { roles: ['STUDENT'], title: '学习参与度' }
      },
      {
        path: 'teacher/home',
        component: () => import('@/views/teacher/TeacherHome.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '教师工作台' }
      },
      {
        path: 'teacher/tasks/create',
        component: () => import('@/views/teacher/TaskCreate.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '创建签到' }
      },
      {
        path: 'teacher/tasks',
        component: () => import('@/views/teacher/TaskHistory.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '签到任务' }
      },
      {
        path: 'teacher/tasks/:id/monitor',
        component: () => import('@/views/teacher/TaskMonitor.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '实时监控' }
      },
      {
        path: 'teacher/stats',
        component: () => import('@/views/teacher/CourseStats.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '课程统计' }
      },
      {
        path: 'teacher/rollcall',
        component: () => import('@/views/teacher/RollCallStage.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '随机点名' }
      },
      {
        path: 'teacher/interaction',
        component: () => import('@/views/teacher/InteractionConsole.vue'),
        meta: { roles: ['TEACHER', 'ADMIN'], title: '互动控制台' }
      },
      {
        path: 'admin/overview',
        component: () => import('@/views/admin/AdminOverview.vue'),
        meta: { roles: ['ADMIN'], title: '运营总览' }
      },
      {
        path: 'admin/data',
        component: () => import('@/views/admin/AdminData.vue'),
        meta: { roles: ['ADMIN'], title: '基础数据' }
      },
      {
        path: 'admin/logs',
        component: () => import('@/views/admin/AdminLogs.vue'),
        meta: { roles: ['ADMIN'], title: '系统日志' }
      },
      {
        path: 'admin/settings',
        component: () => import('@/views/admin/AdminSettings.vue'),
        meta: { roles: ['ADMIN'], title: '系统设置' }
      },
      {
        path: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'notifications',
        component: () => import('@/views/NotificationsView.vue'),
        meta: { title: '消息中心' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.token && to.path === '/login') return auth.home
    return true
  }
  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  if (to.path === '/') {
    return roleHome(auth.role)
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && auth.role && !roles.includes(auth.role)) {
    ElMessage.warning('当前账号无权访问该页面')
    return roleHome(auth.role)
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · ClassPulse` : 'ClassPulse · 课堂签到与互动系统'
})

export default router
