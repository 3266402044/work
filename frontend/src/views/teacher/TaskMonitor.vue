<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi } from '@/api'
import { onRealtime } from '@/composables/realtime'
import { formatDateTime, formatTime, methodLabel } from '@/utils/format'
import type { TaskVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import Countdown from '@/components/Countdown.vue'
import ProgressRing from '@/components/ProgressRing.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.id)

const task = ref<TaskVO | null>(null)
const summary = ref<any>({})
const students = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const tab = ref<'roster' | 'map'>('roster')
const filter = ref('ALL')
const keyword = ref('')
const feed = ref<any[]>([])

const qrVisible = ref(false)
const qrImage = ref('')
const qrExpires = ref('')
const qrRefreshing = ref(false)
const passcodeVisible = ref(false)
const passcodeValue = ref('')

const manualVisible = ref(false)
const manualForm = reactive({ studentId: 0, studentName: '', status: 'MANUAL', remark: '' })
const manualLoading = ref(false)

let qrTimer: number | null = null
let refreshTimer: number | null = null
let reloadTimer: number | null = null

const load = async (silent = false) => {
  if (!silent) loading.value = true
  error.value = ''
  try {
    const data = await taskApi.records(taskId)
    task.value = data.task
    summary.value = data.summary
    students.value = data.students
    if (!feed.value.length) {
      feed.value = data.students
        .filter((student: any) => student.signTime)
        .sort((a: any, b: any) => String(b.signTime).localeCompare(String(a.signTime)))
        .slice(0, 8)
        .map((student: any) => ({
          name: student.name,
          status: student.status,
          time: student.signTime,
          method: student.method
        }))
    }
  } catch (e: any) {
    if (!silent) error.value = e?.message || '签到数据加载失败'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  return students.value.filter((student) => {
    const matchKeyword =
      !keyword.value ||
      student.name?.includes(keyword.value) ||
      student.studentNo?.includes(keyword.value)
    let matchStatus = true
    if (filter.value === 'SIGNED') matchStatus = ['NORMAL', 'LATE', 'MANUAL', 'LEAVE'].includes(student.status)
    else if (filter.value !== 'ALL') matchStatus = student.status === filter.value
    return matchKeyword && matchStatus
  })
})

const mapPoints = computed(() => {
  if (!task.value || task.value.longitude === undefined || task.value.latitude === undefined) return []
  const radius = task.value.radiusMeters || 150
  const scale = 70 / radius
  const cosLat = Math.cos((task.value.latitude * Math.PI) / 180)
  return students.value
    .filter((student) => student.longitude !== undefined && student.latitude !== undefined && student.distanceMeters !== null)
    .map((student) => {
      const dx = (student.longitude - task.value!.longitude!) * 111320 * cosLat * scale
      const dy = (student.latitude - task.value!.latitude!) * 110540 * scale
      let x = dx
      let y = -dy
      const dist = Math.hypot(x, y)
      if (dist > 92) {
        x = (x / dist) * 92
        y = (y / dist) * 92
      }
      return {
        x: 100 + x,
        y: 100 + y,
        name: student.name,
        inside: (student.distanceMeters ?? 0) <= radius,
        distance: student.distanceMeters
      }
    })
})

const canQr = computed(() => task.value?.signModes.includes('QR'))
const canPasscode = computed(() => task.value?.signModes.includes('PASSCODE'))
const ongoing = computed(() => task.value?.taskStatus === 'ONGOING')

const refreshQr = async () => {
  if (!task.value) return
  qrRefreshing.value = true
  try {
    const data = await taskApi.qrToken(taskId)
    qrExpires.value = data.expiresAt
    const fullUrl = new URL(data.url, window.location.origin).toString()
    qrImage.value = await QRCode.toDataURL(fullUrl, {
      width: 280,
      margin: 1,
      color: { dark: '#0B1020', light: '#FFFFFF' }
    })
  } finally {
    qrRefreshing.value = false
  }
}

const openQr = async () => {
  qrVisible.value = true
  await refreshQr()
  if (qrTimer) window.clearInterval(qrTimer)
  qrTimer = window.setInterval(() => {
    if (qrVisible.value && ongoing.value) refreshQr()
  }, 25000)
}

const closeQr = () => {
  qrVisible.value = false
  if (qrTimer) {
    window.clearInterval(qrTimer)
    qrTimer = null
  }
}

const openPasscode = async () => {
  const data = await taskApi.passcode(taskId)
  passcodeValue.value = data.passcode
  passcodeVisible.value = true
}

const closeTask = async () => {
  try {
    await ElMessageBox.confirm(
      '提前关闭后学生将立即无法签到，且该操作不可撤销。确认关闭本次签到？',
      '提前关闭签到',
      { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await taskApi.close(taskId)
  ElMessage.success('签到已提前关闭')
  load()
}

const exportExcel = async () => {
  await taskApi.export(taskId)
  ElMessage.success('Excel 已开始下载')
}

const openManual = (student: any, mode: 'SIGN' | 'LEAVE') => {
  manualForm.studentId = student.studentId
  manualForm.studentName = student.name
  manualForm.status = mode === 'LEAVE' ? 'LEAVE' : 'MANUAL'
  manualForm.remark = mode === 'LEAVE' ? '教师标记请假' : '教师手动补签'
  manualVisible.value = true
}

const submitManual = async () => {
  manualLoading.value = true
  try {
    await taskApi.manual(taskId, {
      studentId: manualForm.studentId,
      status: manualForm.status,
      remark: manualForm.remark
    })
    ElMessage.success('签到记录已更新')
    manualVisible.value = false
    load(true)
  } finally {
    manualLoading.value = false
  }
}

const statusText = (status: string) => {
  switch (status) {
    case 'NORMAL': return '正常'
    case 'LATE': return '迟到'
    case 'LEAVE': return '请假'
    case 'MANUAL': return '补签'
    case 'ABSENT': return '未签到'
    case 'LEAVE_PENDING_RECORD': return '已批准请假'
    default: return status
  }
}

let offSign: (() => void) | null = null
let offTask: (() => void) | null = null

onMounted(() => {
  load()
  refreshTimer = window.setInterval(() => load(true), 20000)
  offSign = onRealtime('sign:new', (data: any) => {
    if (data?.taskId !== taskId) return
    if (data.studentName) {
      feed.value.unshift({
        name: data.studentName,
        status: data.status,
        time: data.signTime,
        method: data.method
      })
      feed.value = feed.value.slice(0, 10)
    }
    if (reloadTimer) window.clearTimeout(reloadTimer)
    reloadTimer = window.setTimeout(() => load(true), 300)
  })
  offTask = onRealtime('task:changed', (data: any) => {
    if (data?.taskId === taskId) load(true)
  })
})

onBeforeUnmount(() => {
  offSign?.()
  offTask?.()
  if (refreshTimer) window.clearInterval(refreshTimer)
  if (qrTimer) window.clearInterval(qrTimer)
  if (reloadTimer) window.clearTimeout(reloadTimer)
})
</script>

<template>
  <div class="page">
    <PageHeader
      :title="task?.title || '任务实时监控'"
      :subtitle="task ? `${task.courseName} · ${task.className} · ${task.teacherName}` : '加载中…'"
      eyebrow="LIVE MONITOR"
    >
      <template #actions>
        <el-button @click="router.push('/teacher/tasks')">任务列表</el-button>
        <el-button :loading="loading" @click="load()">刷新</el-button>
        <el-button @click="exportExcel"><el-icon><Download /></el-icon> 导出</el-button>
        <el-button v-if="canPasscode" @click="openPasscode"><el-icon><Key /></el-icon> 口令</el-button>
        <el-button v-if="canQr" type="primary" plain :disabled="!ongoing" @click="openQr"><el-icon><Grid /></el-icon> 二维码</el-button>
        <el-button v-if="ongoing" type="danger" plain @click="closeTask">提前关闭</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div class="skeleton" style="height: 150px" />
      <div class="skeleton" style="height: 420px" />
    </div>

    <div v-else-if="error" class="panel">
      <EmptyState title="监控数据加载失败" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load()">重试</el-button></template>
      </EmptyState>
    </div>

    <template v-else-if="task">
      <section class="panel monitor-hero" :class="task.taskStatus.toLowerCase()">
        <div class="hero-left">
          <div class="row wrap" style="gap: 8px">
            <StatusTag :status="task.taskStatus" />
            <span class="chip">{{ task.locationName || '未设置地点' }}</span>
            <span class="chip mono">{{ formatTime(task.startTime) }} - {{ formatTime(task.endTime) }}</span>
            <span v-for="mode in task.signModes" :key="mode" class="chip cyan">{{ mode === 'CLICK' ? '普通' : mode === 'QR' ? '二维码' : mode === 'LOCATION' ? '定位' : '口令' }}</span>
          </div>
          <div class="hero-count" v-if="ongoing">
            <span class="label">距离签到结束</span>
            <Countdown :target="task.endTime" size="lg" @end="load" />
          </div>
          <div class="hero-count" v-else>
            <span class="label">任务状态</span>
            <span class="mono hero-static">{{ task.taskStatusLabel }}</span>
          </div>
          <div class="hero-stats">
            <div><span class="label">已签到</span><strong class="mono">{{ summary.signed || 0 }}</strong></div>
            <div><span class="label">正常</span><strong class="mono green">{{ summary.normal || 0 }}</strong></div>
            <div><span class="label">迟到</span><strong class="mono amber">{{ summary.late || 0 }}</strong></div>
            <div><span class="label">请假</span><strong class="mono cyan">{{ summary.leave || 0 }}</strong></div>
            <div><span class="label">未签到</span><strong class="mono coral">{{ summary.absent || 0 }}</strong></div>
          </div>
        </div>
        <div class="hero-right">
          <ProgressRing :value="summary.attendanceRate || 0" label="实时到课率" :sublabel="`${summary.signed || 0}/${summary.total || 0} 人`" :size="150" />
          <div class="hero-actions">
            <el-button v-if="canQr && ongoing" size="small" @click="openQr">刷新二维码</el-button>
            <el-button v-if="canPasscode" size="small" @click="openPasscode">查看口令</el-button>
          </div>
        </div>
      </section>

      <div class="grid grid-main-side section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="row" style="gap: 10px">
              <div class="panel-title">{{ tab === 'roster' ? '签到名单' : '定位分布' }}</div>
              <el-radio-group v-model="tab" size="small">
                <el-radio-button value="roster">名单</el-radio-button>
                <el-radio-button value="map">定位</el-radio-button>
              </el-radio-group>
            </div>
            <span class="panel-sub">共 {{ students.length }} 人</span>
          </div>

          <template v-if="tab === 'roster'">
            <div class="filter-row">
              <el-radio-group v-model="filter" size="small">
                <el-radio-button value="ALL">全部</el-radio-button>
                <el-radio-button value="SIGNED">已签到</el-radio-button>
                <el-radio-button value="ABSENT">未签到</el-radio-button>
                <el-radio-button value="LATE">迟到</el-radio-button>
                <el-radio-button value="LEAVE">请假</el-radio-button>
              </el-radio-group>
              <el-input v-model="keyword" placeholder="搜索姓名或学号" clearable style="width: 200px">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>

            <el-table :data="filtered" class="hide-mobile roster-table" max-height="480">
              <el-table-column label="学生" min-width="150">
                <template #default="{ row }">
                  <div class="student-cell">
                    <div class="student-avatar">{{ row.name?.slice(0, 1) }}</div>
                    <div>
                      <div class="student-name">{{ row.name }}</div>
                      <div class="student-no mono">{{ row.studentNo }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <StatusTag :status="row.status" :label="statusText(row.status)" />
                </template>
              </el-table-column>
              <el-table-column label="签到时间" width="150">
                <template #default="{ row }"><span class="mono muted">{{ row.signTime ? formatDateTime(row.signTime) : '—' }}</span></template>
              </el-table-column>
              <el-table-column label="方式" width="100">
                <template #default="{ row }"><span class="muted">{{ row.method ? methodLabel(row.method) : '—' }}</span></template>
              </el-table-column>
              <el-table-column label="距离" width="90">
                <template #default="{ row }"><span class="mono muted">{{ row.distanceMeters ? row.distanceMeters + ' m' : '—' }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'ABSENT'" text type="primary" size="small" @click="openManual(row, 'SIGN')">补签</el-button>
                  <el-button v-else text type="primary" size="small" @click="openManual(row, 'SIGN')">修改</el-button>
                  <el-button text type="warning" size="small" @click="openManual(row, 'LEAVE')">请假</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="mobile-roster">
              <div v-for="row in filtered" :key="row.studentId" class="roster-card">
                <div class="row-between">
                  <div class="student-cell">
                    <div class="student-avatar">{{ row.name?.slice(0, 1) }}</div>
                    <div>
                      <div class="student-name">{{ row.name }}</div>
                      <div class="student-no mono">{{ row.studentNo }}</div>
                    </div>
                  </div>
                  <StatusTag :status="row.status" :label="statusText(row.status)" />
                </div>
                <div class="roster-meta">
                  <span v-if="row.signTime" class="mono">{{ formatDateTime(row.signTime) }}</span>
                  <span v-if="row.method">{{ methodLabel(row.method) }}</span>
                  <span v-if="row.distanceMeters" class="mono">{{ row.distanceMeters }}m</span>
                </div>
                <div class="row" style="gap: 6px; margin-top: 8px">
                  <el-button size="small" @click="openManual(row, 'SIGN')">{{ row.status === 'ABSENT' ? '补签' : '修改' }}</el-button>
                  <el-button size="small" type="warning" plain @click="openManual(row, 'LEAVE')">请假</el-button>
                </div>
              </div>
            </div>
          </template>

          <template v-else>
            <div v-if="!mapPoints.length" class="map-empty">
              <EmptyState title="暂无定位数据" description="学生使用定位签到后，这里会展示相对课堂中心的位置散点" icon="Location" />
            </div>
            <div v-else class="map-wrap">
              <svg viewBox="0 0 200 200" class="map">
                <defs>
                  <pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse">
                    <path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(148,163,184,.08)" stroke-width="0.5" />
                  </pattern>
                </defs>
                <rect width="200" height="200" fill="#0A1120" />
                <rect width="200" height="200" fill="url(#grid)" />
                <circle cx="100" cy="100" :r="70" fill="rgba(56,221,248,.04)" stroke="rgba(56,221,248,.4)" stroke-width="0.7" stroke-dasharray="3 3" />
                <circle cx="100" cy="100" r="4" fill="#38DDF8" />
                <g v-for="(point, index) in mapPoints" :key="index">
                  <circle :cx="point.x" :cy="point.y" r="3.2" :fill="point.inside ? '#47D16C' : '#FF6B6B'" opacity="0.9" />
                  <text :x="point.x + 5" :y="point.y + 3" font-size="5" fill="#B7C4DA">{{ point.name }}</text>
                </g>
                <text x="100" y="193" font-size="5.5" fill="#8B9AB5" text-anchor="middle">允许半径 {{ task.radiusMeters }}m · 中心 {{ task.locationName }}</text>
              </svg>
              <div class="map-legend">
                <span><i class="dot" style="color: var(--green)" />范围内</span>
                <span><i class="dot" style="color: var(--coral)" />范围外</span>
                <span class="muted">按经纬度相对课堂中心绘制</span>
              </div>
            </div>
          </template>
        </section>

        <section class="panel feed-panel">
          <div class="panel-head">
            <div class="panel-title">签到动态</div>
            <span class="row" style="gap: 6px"><i class="pulse-dot" /><span class="panel-sub">实时</span></span>
          </div>
          <div v-if="!feed.length" class="muted" style="font-size: 12px">等待学生签到…</div>
          <div v-else class="feed-list">
            <div v-for="(item, index) in feed" :key="index" class="feed-item fade-in">
              <div class="feed-avatar">{{ item.name?.slice(0, 1) }}</div>
              <div class="feed-body">
                <div class="feed-name">{{ item.name }}</div>
                <div class="feed-meta">{{ statusText(item.status) }} · {{ item.method ? methodLabel(item.method) : '签到' }}</div>
              </div>
              <div class="feed-time mono">{{ formatTime(item.time) }}</div>
            </div>
          </div>
          <div class="divider" />
          <div class="feed-note">
            <el-icon><InfoFilled /></el-icon>
            <span>数据同时由 SSE 实时推送与 20 秒轮询兜底，刷新页面不会丢失结果。</span>
          </div>
        </section>
      </div>
    </template>

    <!-- 二维码 -->
    <el-dialog v-model="qrVisible" title="动态签到二维码" width="360px" @close="closeQr">
      <div class="qr-wrap">
        <div v-if="qrImage" class="qr-image"><img :src="qrImage" alt="签到二维码" /></div>
        <div v-else class="skeleton" style="width: 260px; height: 260px" />
        <div class="qr-tip">学生使用手机扫码进入任务详情，令牌 30 秒内有效</div>
        <div class="qr-expire mono" v-if="qrExpires">过期时间 {{ formatDateTime(qrExpires) }}</div>
        <el-button type="primary" plain :loading="qrRefreshing" @click="refreshQr">立即刷新二维码</el-button>
      </div>
    </el-dialog>

    <!-- 口令 -->
    <el-dialog v-model="passcodeVisible" title="签到口令" width="360px">
      <div class="passcode-wrap">
        <div class="muted">请在大屏展示或口述给学生</div>
        <div class="passcode-value mono">{{ passcodeValue }}</div>
        <div class="muted tiny">口令仅保存哈希，重新生成后旧口令立即失效</div>
        <el-button size="small" @click="openPasscode">重新生成</el-button>
      </div>
    </el-dialog>

    <!-- 手动补签 -->
    <el-dialog v-model="manualVisible" :title="`处理记录 · ${manualForm.studentName}`" width="420px">
      <el-form label-position="top">
        <el-form-item label="记录状态">
          <el-select v-model="manualForm.status" style="width: 100%">
            <el-option label="手动补签（MANUAL）" value="MANUAL" />
            <el-option label="正常（NORMAL）" value="NORMAL" />
            <el-option label="迟到（LATE）" value="LATE" />
            <el-option label="请假（LEAVE）" value="LEAVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="manualForm.remark" type="textarea" :rows="3" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualLoading" @click="submitManual">保存记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.monitor-hero { display: flex; justify-content: space-between; gap: 24px; align-items: center; padding: 20px 24px; border-left: 2px solid var(--green); }
.monitor-hero.ended, .monitor-hero.closed { border-left-color: var(--muted); }
.monitor-hero.not_started { border-left-color: var(--cyan); }
.hero-left { flex: 1; min-width: 0; }
.hero-count { display: flex; flex-direction: column; gap: 2px; margin: 14px 0 16px; }
.hero-static { font-size: 28px; font-weight: 600; }
.hero-stats { display: flex; gap: 30px; flex-wrap: wrap; }
.hero-stats div { display: flex; flex-direction: column; gap: 0; }
.hero-stats strong { font-size: 24px; font-weight: 600; }
.hero-right { display: flex; flex-direction: column; align-items: center; gap: 12px; border-left: 1px solid var(--line); padding-left: 24px; }
.hero-actions { display: flex; gap: 6px; }

.filter-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.student-cell { display: flex; align-items: center; gap: 10px; }
.student-avatar {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(56, 221, 248, 0.08);
  color: var(--cyan); font-size: 12px;
  flex-shrink: 0;
}
.student-name { font-size: 12.5px; color: var(--text-2); }
.student-no { font-size: 10.5px; color: var(--muted); }
.mobile-roster { display: none; }

.feed-list { display: flex; flex-direction: column; max-height: 400px; overflow: auto; }
.feed-item { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--line); }
.feed-item:last-child { border-bottom: none; }
.feed-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(45, 212, 191, 0.09); color: var(--teal); font-size: 11px;
  flex-shrink: 0;
}
.feed-body { flex: 1; min-width: 0; }
.feed-name { font-size: 12px; color: var(--text-2); }
.feed-meta { font-size: 10.5px; color: var(--muted); }
.feed-time { font-size: 10.5px; color: var(--muted); }
.feed-note { display: flex; gap: 8px; font-size: 10.5px; color: var(--muted); line-height: 1.7; }

.qr-wrap { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.qr-image { background: #fff; padding: 10px; border-radius: 10px; }
.qr-image img { display: block; width: 260px; height: 260px; }
.qr-tip { font-size: 11.5px; color: var(--muted); text-align: center; }
.qr-expire { font-size: 11px; color: var(--cyan); }
.passcode-wrap { display: flex; flex-direction: column; align-items: center; gap: 12px; font-size: 12px; }
.passcode-value { font-size: 40px; font-weight: 700; letter-spacing: 0.3em; color: var(--cyan); }
.tiny { font-size: 10.5px; }

.map-wrap { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.map { width: 100%; max-width: 520px; border-radius: 10px; border: 1px solid var(--line); }
.map-legend { display: flex; gap: 16px; font-size: 11px; color: var(--text-2); flex-wrap: wrap; }
.map-legend span { display: inline-flex; align-items: center; gap: 6px; }
.map-empty :deep(.empty-state) { padding: 40px 10px; }

@media (max-width: 1100px) {
  .monitor-hero { flex-direction: column; align-items: flex-start; }
  .hero-right { border-left: none; border-top: 1px solid var(--line); padding-left: 0; padding-top: 16px; width: 100%; flex-direction: row; justify-content: space-between; }
}
@media (max-width: 760px) {
  .hero-stats { gap: 18px; }
  .hide-mobile { display: none !important; }
  .mobile-roster { display: flex; flex-direction: column; gap: 10px; }
  .roster-card { border: 1px solid var(--line); border-radius: 8px; padding: 11px 12px; background: var(--panel-3); }
  .roster-meta { display: flex; gap: 10px; flex-wrap: wrap; font-size: 10.5px; color: var(--muted); margin-top: 7px; }
}
</style>
