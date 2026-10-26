import { download, request } from './http'
import type {
  ClassGroup,
  Course,
  LoginResponse,
  NotificationItem,
  PageResult,
  QuizVO,
  RollCallVO,
  SignRecordVO,
  TaskVO
} from '@/types'
import type { CurrentUser } from '@/stores/auth'

type Dict = Record<string, any>

export const authApi = {
  login: (data: { username: string; password: string }) =>
    request<LoginResponse>({ url: '/auth/login', method: 'POST', data }),
  me: () => request<CurrentUser>({ url: '/auth/me' }),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request<void>({ url: '/auth/change-password', method: 'POST', data }),
  updateProfile: (data: { phone?: string; avatarUrl?: string }) =>
    request<CurrentUser>({ url: '/profile', method: 'PUT', data })
}

export const studentApi = {
  dashboard: () => request<Dict>({ url: '/student/dashboard' }),
  tasks: (params: Dict) => request<PageResult<TaskVO>>({ url: '/student/tasks', params }),
  sign: (taskId: number, data: Dict) =>
    request<{ record: any; task: TaskVO; message: string }>({
      url: `/tasks/${taskId}/sign`,
      method: 'POST',
      data
    }),
  records: (params: Dict) => request<PageResult<SignRecordVO>>({ url: '/student/records', params }),
  leaves: () => request<Dict[]>({ url: '/student/leaves' }),
  applyLeave: (taskId: number, data: Dict) =>
    request<void>({ url: `/student/tasks/${taskId}/leave-requests`, method: 'POST', data }),
  participation: () => request<Dict>({ url: '/student/participation' }),
  calendar: (params: Dict) => request<Dict[]>({ url: '/student/calendar', params }),
  courses: () => request<Course[]>({ url: '/student/courses' }),
  announcements: () => request<Dict[]>({ url: '/student/announcements' }),
  activeInteractions: () => request<Dict>({ url: '/student/interactions/active' })
}

export const taskApi = {
  detail: (id: number) => request<TaskVO>({ url: `/tasks/${id}` }),
  create: (data: Dict) => request<TaskVO>({ url: '/tasks', method: 'POST', data }),
  update: (id: number, data: Dict) => request<TaskVO>({ url: `/tasks/${id}`, method: 'PUT', data }),
  close: (id: number) => request<TaskVO>({ url: `/tasks/${id}/close`, method: 'POST' }),
  qrToken: (id: number) =>
    request<{ token: string; expiresAt: string; taskId: number; url: string }>({
      url: `/tasks/${id}/qr-token`,
      method: 'POST'
    }),
  passcode: (id: number) => request<{ passcode: string }>({ url: `/tasks/${id}/passcode`, method: 'POST' }),
  records: (id: number) => request<Dict>({ url: `/tasks/${id}/records` }),
  manual: (id: number, data: Dict) => request<void>({ url: `/tasks/${id}/records/manual`, method: 'POST', data }),
  export: (id: number) => download(`/tasks/${id}/export`, `签到明细-${id}.xlsx`)
}

export const teacherApi = {
  dashboard: () => request<Dict>({ url: '/teacher/dashboard' }),
  tasks: (params: Dict) => request<PageResult<TaskVO>>({ url: '/teacher/tasks', params }),
  leaves: (params: Dict) => request<Dict[]>({ url: '/teacher/leaves', params }),
  reviewLeave: (id: number, approved: boolean, remark?: string) =>
    request<void>({ url: `/teacher/leaves/${id}/review`, method: 'POST', params: { approved, remark } }),
  courses: () => request<Course[]>({ url: '/teacher/courses' }),
  classes: (params?: Dict) => request<ClassGroup[]>({ url: '/teacher/classes', params }),
  roster: (classId: number) => request<Dict>({ url: `/teacher/classes/${classId}/roster` }),
  warnings: () => request<Dict[]>({ url: '/teacher/warnings' })
}

export const rollCallApi = {
  create: (data: Dict) => request<RollCallVO>({ url: '/roll-calls', method: 'POST', data }),
  detail: (id: number) => request<RollCallVO>({ url: `/roll-calls/${id}` }),
  active: () => request<Dict[]>({ url: '/roll-calls/active' }),
  respond: (id: number) => request<Dict>({ url: `/roll-calls/${id}/respond`, method: 'POST' }),
  markResult: (id: number, recordId: number, data: Dict) =>
    request<void>({ url: `/roll-calls/${id}/records/${recordId}/result`, method: 'POST', data }),
  drawAgain: (id: number, count?: number) =>
    request<RollCallVO>({ url: `/roll-calls/${id}/draw-again`, method: 'POST', params: { count } }),
  finish: (id: number) => request<void>({ url: `/roll-calls/${id}/finish`, method: 'POST' }),
  syncAttendance: (id: number) =>
    request<{ count: number; message: string }>({ url: `/roll-calls/${id}/sync-attendance`, method: 'POST' }),
  history: (params: Dict) => request<Dict[]>({ url: '/roll-calls/history', params }),
  exportHistory: (params: Dict) => {
    const query = new URLSearchParams()
    if (params.courseId) query.set('courseId', String(params.courseId))
    if (params.from) query.set('from', String(params.from))
    if (params.to) query.set('to', String(params.to))
    return download(`/roll-calls/export?${query.toString()}`, '随机点名记录.xlsx')
  }
}

export const quizApi = {
  create: (data: Dict) => request<QuizVO>({ url: '/quizzes', method: 'POST', data }),
  list: (params?: Dict) => request<QuizVO[]>({ url: '/quizzes', params }),
  detail: (id: number) => request<QuizVO>({ url: `/quizzes/${id}` }),
  submit: (id: number, answers: number[]) =>
    request<Dict>({ url: `/quizzes/${id}/answers`, method: 'POST', data: { answers } }),
  results: (id: number) => request<Dict>({ url: `/quizzes/${id}/results` }),
  history: (params: Dict) => request<Dict[]>({ url: '/quizzes/history', params })
}

export const raceApi = {
  create: (data: Dict) => request<Dict>({ url: '/races', method: 'POST', data }),
  active: () => request<Dict[]>({ url: '/races/active' }),
  detail: (id: number) => request<Dict>({ url: `/races/${id}` }),
  buzz: (id: number) => request<Dict>({ url: `/races/${id}/buzz`, method: 'POST' }),
  history: (params: Dict) => request<Dict[]>({ url: '/races/history', params })
}

export const announcementApi = {
  create: (data: Dict) => request<Dict>({ url: '/announcements', method: 'POST', data }),
  list: (courseId: number) => request<Dict[]>({ url: '/announcements', params: { courseId } }),
  remove: (id: number) => request<void>({ url: `/announcements/${id}`, method: 'DELETE' })
}

export const statisticsApi = {
  course: (params: Dict) => request<Dict>({ url: '/statistics/course', params })
}

export const notificationApi = {
  list: () => request<NotificationItem[]>({ url: '/notifications' }),
  unreadCount: () => request<{ count: number }>({ url: '/notifications/unread-count' }),
  page: (params: Dict) => request<PageResult<NotificationItem>>({ url: '/notifications/page', params }),
  read: (id: number) => request<void>({ url: `/notifications/${id}/read`, method: 'POST' }),
  readAll: () => request<void>({ url: '/notifications/read-all', method: 'POST' })
}

export const adminApi = {
  overview: () => request<Dict>({ url: '/admin/overview' }),
  students: (params: Dict) => request<PageResult<Dict>>({ url: '/admin/students', params }),
  createStudent: (data: Dict) => request<void>({ url: '/admin/students', method: 'POST', data }),
  updateStudent: (data: Dict) => request<void>({ url: '/admin/students', method: 'PUT', data }),
  deleteStudent: (id: number) => request<void>({ url: `/admin/students/${id}`, method: 'DELETE' }),
  importStudents: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<Dict>({ url: '/admin/students/import', method: 'POST', data: form })
  },
  exportStudents: () => download('/admin/students/export', '学生名单.xlsx'),
  teachers: (params: Dict) => request<PageResult<Dict>>({ url: '/admin/teachers', params }),
  createTeacher: (data: Dict) => request<void>({ url: '/admin/teachers', method: 'POST', data }),
  updateTeacher: (data: Dict) => request<void>({ url: '/admin/teachers', method: 'PUT', data }),
  deleteTeacher: (id: number) => request<void>({ url: `/admin/teachers/${id}`, method: 'DELETE' }),
  classes: () => request<Dict[]>({ url: '/admin/classes' }),
  createClass: (data: Dict) => request<void>({ url: '/admin/classes', method: 'POST', data }),
  updateClass: (data: Dict) => request<void>({ url: '/admin/classes', method: 'PUT', data }),
  deleteClass: (id: number) => request<void>({ url: `/admin/classes/${id}`, method: 'DELETE' }),
  courses: () => request<Dict[]>({ url: '/admin/courses' }),
  createCourse: (data: Dict) => request<void>({ url: '/admin/courses', method: 'POST', data }),
  updateCourse: (data: Dict) => request<void>({ url: '/admin/courses', method: 'PUT', data }),
  deleteCourse: (id: number) => request<void>({ url: `/admin/courses/${id}`, method: 'DELETE' }),
  users: (params: Dict) => request<PageResult<Dict>>({ url: '/admin/users', params }),
  createUser: (data: Dict) => request<void>({ url: '/admin/users', method: 'POST', data }),
  updateUser: (data: Dict) => request<void>({ url: '/admin/users', method: 'PUT', data }),
  resetPassword: (id: number) => request<void>({ url: `/admin/users/${id}/reset-password`, method: 'POST' }),
  logs: (params: Dict) => request<PageResult<Dict>>({ url: '/admin/logs', params }),
  settings: () => request<Array<{ key: string; value: string; description: string }>>({ url: '/admin/settings' }),
  saveSettings: (data: Record<string, string>) => request<void>({ url: '/admin/settings', method: 'PUT', data }),
  exportInteractions: () => download('/admin/interactions/export', '请假记录.xlsx'),
  resetDemo: () => request<void>({ url: '/admin/demo/reset', method: 'POST' })
}

export const uploadApi = {
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<{ url: string; name: string }>({ url: '/upload', method: 'POST', data: form })
  }
}
