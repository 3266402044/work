export interface LoginResponse {
  token: string
  expireMinutes: number
  user: import('@/stores/auth').CurrentUser
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface TaskVO {
  id: number
  title: string
  teacherId: number
  teacherName?: string
  courseId?: number
  courseName?: string
  courseColor?: string
  classId?: number
  className?: string
  startTime: string
  endTime: string
  lateTime?: string
  closedAt?: string
  createTime: string
  taskStatus: 'NOT_STARTED' | 'ONGOING' | 'CLOSED' | 'ENDED'
  taskStatusLabel: string
  signModes: string[]
  locationName?: string
  longitude?: number
  latitude?: number
  radiusMeters?: number
  signed?: boolean
  canSign?: boolean
  signHint?: string
  myRecord?: SignRecord
  totalStudents: number
  signedCount: number
  normalCount: number
  lateCount: number
  leaveCount: number
  absentCount: number
  attendanceRate: number
  secondsToStart: number
  secondsToEnd: number
  passcode?: string
}

export interface SignRecord {
  id: number
  taskId: number
  studentId: number
  signTime: string
  status: string
  method: string
  longitude?: number
  latitude?: number
  distanceMeters?: number
  remark?: string
  createTime: string
}

export interface SignRecordVO extends SignRecord {
  taskTitle: string
  courseId?: number
  courseName?: string
  studentNo?: string
  studentName?: string
  className?: string
  statusLabel: string
  methodLabel: string
}

export interface Course {
  id: number
  courseNo: string
  name: string
  teacherId: number
  semester?: string
  credit?: number
  coverColor?: string
  classroom?: string
  longitude?: number
  latitude?: number
  defaultRadius?: number
}

export interface ClassGroup {
  id: number
  name: string
  grade?: string
  major?: string
  advisorId?: number
}

export interface Student {
  id: number
  studentNo: string
  name: string
  classId?: number
  userId?: number
}

export interface NotificationItem {
  id: number
  title: string
  content: string
  type?: string
  readFlag: number
  relatedId?: number
  createTime: string
}

export interface RollCallRecord {
  recordId: number
  studentId: number
  studentNo: string
  name: string
  className?: string
  avatarText: string
  drawOrder: number
  result: string
  resultLabel: string
  performance?: string
  drawnAt: string
  respondedAt?: string
  remark?: string
  deadline: string
}

export interface RollCallVO {
  id: number
  courseId: number
  courseName: string
  classId?: number
  className?: string
  teacherName: string
  candidateScope: string
  drawCount: number
  antiRepeatRule?: string
  excludeLeave?: number
  responseDeadlineSeconds: number
  status: string
  startedAt: string
  endedAt?: string
  records: RollCallRecord[]
  summary: Record<string, number>
}

export interface QuizVO {
  id: number
  courseId: number
  courseName: string
  teacherName: string
  title: string
  type: 'SINGLE' | 'MULTIPLE' | 'TRUE_FALSE'
  options: string[]
  startTime: string
  endTime: string
  anonymous: boolean
  status: 'NOT_STARTED' | 'RUNNING' | 'ENDED'
  explanation?: string
  correctAnswer?: number[]
  answered?: boolean
  myAnswer?: number[]
  correct?: boolean
  answerCount: number
  totalStudents: number
  distribution?: Array<{ option: string; count: number }>
  correctRate?: number
  showResult: boolean
  secondsLeft: number
}
