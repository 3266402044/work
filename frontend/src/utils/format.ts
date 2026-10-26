import dayjs from 'dayjs'

export const formatDateTime = (value?: string | null) =>
  value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '—'

export const formatTime = (value?: string | null) =>
  value ? dayjs(value).format('HH:mm') : '—'

export const formatDate = (value?: string | null) =>
  value ? dayjs(value).format('YYYY-MM-DD') : '—'

export const formatShort = (value?: string | null) =>
  value ? dayjs(value).format('MM-DD HH:mm') : '—'

export const clockText = (seconds: number) => {
  const safe = Math.max(0, Math.floor(seconds))
  const h = Math.floor(safe / 3600)
  const m = Math.floor((safe % 3600) / 60)
  const s = safe % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export const secondsBetween = (value?: string | null) => {
  if (!value) return 0
  return Math.max(0, Math.floor((dayjs(value).valueOf() - Date.now()) / 1000))
}

export const taskStatusClass = (status?: string) => {
  switch (status) {
    case 'ONGOING': return 'status-ongoing'
    case 'NOT_STARTED': return 'status-not-started'
    case 'CLOSED': return 'status-closed'
    default: return 'status-ended'
  }
}

export const recordStatusClass = (status?: string) => {
  switch (status) {
    case 'NORMAL': return 'status-normal'
    case 'LATE': return 'status-warning'
    case 'LEAVE': return 'status-not-started'
    case 'ABSENT': return 'status-closed'
    default: return 'status-ended'
  }
}

export const methodLabel = (method?: string) => {
  switch (method) {
    case 'CLICK': return '普通签到'
    case 'QR': return '二维码'
    case 'LOCATION': return '定位签到'
    case 'PASSCODE': return '口令签到'
    case 'MANUAL': return '教师补签'
    default: return method || '—'
  }
}

export const modeLabel = (mode?: string) => {
  switch (mode) {
    case 'CLICK': return '普通签到'
    case 'QR': return '动态二维码'
    case 'LOCATION': return '定位签到'
    case 'PASSCODE': return '口令签到'
    default: return mode || '—'
  }
}

export const performanceLabel = (value?: string) => {
  switch (value) {
    case 'EXCELLENT': return '优秀'
    case 'ACTIVE': return '积极'
    case 'NORMAL': return '一般'
    case 'FOLLOW_UP': return '待跟进'
    default: return '—'
  }
}

export const scopeLabel = (value?: string) => {
  switch (value) {
    case 'ALL': return '全班学生'
    case 'SIGNED': return '已签到学生'
    case 'UNSIGNED': return '未签到学生'
    case 'CUSTOM': return '自定义名单'
    default: return value || '—'
  }
}

export const ruleLabel = (value?: string) => {
  switch (value) {
    case 'NONE': return '不防重复'
    case 'SESSION': return '本次会话内不重复'
    case 'DAYS7': return '最近 7 天不重复'
    case 'LAST_N': return '最近 N 次不重复'
    default: return value || '—'
  }
}

export const weekStartOf = (date = dayjs()) => {
  const day = date.day() === 0 ? 7 : date.day()
  return date.subtract(day - 1, 'day').startOf('day')
}

export const avatarColor = (name?: string) => {
  const palette = ['#38DDF8', '#2DD4BF', '#47D16C', '#F5B942', '#FF6B6B', '#8B9AF5', '#F58AC0']
  if (!name) return palette[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) % 997
  return palette[hash % palette.length]
}
