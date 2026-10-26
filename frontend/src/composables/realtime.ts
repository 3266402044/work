import { ElMessage, ElNotification } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeStore } from '@/stores/realtime'
import { useUiStore } from '@/stores/ui'
import { studentApi } from '@/api'

type Handler = (data: any) => void

const handlers = new Map<string, Set<Handler>>()
let source: EventSource | null = null

export function onRealtime(event: string, handler: Handler) {
  if (!handlers.has(event)) handlers.set(event, new Set())
  handlers.get(event)!.add(handler)
  return () => handlers.get(event)?.delete(handler)
}

function emit(event: string, data: any) {
  handlers.get(event)?.forEach((handler) => {
    try {
      handler(data)
    } catch (error) {
      console.warn('realtime handler error', event, error)
    }
  })
}

export function connectRealtime() {
  const auth = useAuthStore()
  if (!auth.token || source) return
  source = new EventSource(`/api/stream?token=${encodeURIComponent(auth.token)}`)
  const events = [
    'notification',
    'rollcall:drawn',
    'rollcall:stage',
    'rollcall:update',
    'rollcall:session',
    'rollcall:result',
    'quiz:new',
    'quiz:update',
    'race:started',
    'race:update',
    'task:changed',
    'sign:new',
    'leave:result',
    'announcement:new'
  ]
  events.forEach((event) => {
    source!.addEventListener(event, (e) => {
      let data: any = null
      try {
        data = JSON.parse((e as MessageEvent).data)
      } catch {
        data = (e as MessageEvent).data
      }
      handleGlobal(event, data)
      emit(event, data)
    })
  })
  source.onerror = () => {
    // EventSource 会自动重连；后端恢复后无需手工刷新
  }
}

export function disconnectRealtime() {
  if (source) {
    source.close()
    source = null
  }
}

function handleGlobal(event: string, data: any) {
  const auth = useAuthStore()
  const rt = useRealtimeStore()
  const ui = useUiStore()

  if (event === 'notification') {
    ui.unread += 1
    ElNotification({
      title: data?.title || '新消息',
      message: data?.content || '',
      type: 'info',
      duration: 4500
    })
  }
  if (event === 'task:changed') {
    rt.bumpTask()
  }
  if (event === 'leave:result') {
    ElMessage[data?.approved ? 'success' : 'warning'](data?.approved ? '你的请假申请已通过' : '你的请假申请未通过')
  }
  if (auth.role === 'STUDENT') {
    if (event === 'rollcall:drawn') {
      rt.setRollCall({
        sessionId: data.sessionId,
        recordId: data.recordId,
        courseName: data.courseName,
        teacherName: data.teacherName,
        deadlineSeconds: data.deadlineSeconds,
        drawnAt: data.drawnAt,
        studentName: data.studentName
      })
    }
    if (event === 'quiz:new') {
      rt.setQuiz({ quizId: data.quizId, title: data.title })
    }
    if (event === 'race:started' && data?.courseId) {
      rt.setRace({
        raceId: data.raceId,
        title: data.title,
        courseName: data.courseName,
        endTime: data.endTime
      })
    }
  }
}

export async function restoreStudentPrompts() {
  const auth = useAuthStore()
  if (auth.role !== 'STUDENT') return
  const rt = useRealtimeStore()
  try {
    const data = await studentApi.activeInteractions()
    if (data.rollCalls?.length) {
      const first = data.rollCalls[0]
      rt.setRollCall({
        sessionId: first.sessionId,
        recordId: first.recordId,
        courseName: first.courseName,
        teacherName: first.teacherName,
        drawnAt: new Date().toISOString(),
        deadlineSeconds: first.remainingSeconds
      })
    }
    const quiz = (data.quizzes || [])[0]
    if (quiz) rt.setQuiz({ quizId: quiz.id, title: quiz.title })
    const race = (data.races || [])[0]
    if (race) {
      rt.setRace({
        raceId: race.raceId,
        title: race.title,
        courseName: race.courseName,
        endTime: race.endTime
      })
    }
  } catch {
    // 忽略恢复失败
  }
}
