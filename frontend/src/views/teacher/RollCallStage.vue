<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { rollCallApi, teacherApi } from '@/api'
import { onRealtime } from '@/composables/realtime'
import { formatTime, performanceLabel, ruleLabel, scopeLabel } from '@/utils/format'
import type { ClassGroup, Course, RollCallVO, Student, TaskVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import Countdown from '@/components/Countdown.vue'
import Particles from '@/components/Particles.vue'

const router = useRouter()
const courses = ref<Course[]>([])
const classes = ref<ClassGroup[]>([])
const tasks = ref<TaskVO[]>([])
const roster = ref<Student[]>([])
const session = ref<RollCallVO | null>(null)
const loading = ref(false)
const phase = ref<'idle' | 'rolling' | 'result'>('idle')
const rollingNames = ref<string[]>([])
const customVisible = ref(false)
const customChecked = ref<number[]>([])
const marking = ref<number | null>(null)

const form = reactive({
  courseId: undefined as number | undefined,
  classId: undefined as number | undefined,
  scope: 'ALL',
  drawCount: 2,
  antiRepeat: 'DAYS7',
  excludeLeave: true,
  responseSeconds: 30,
  taskId: undefined as number | undefined
})

const performanceOptions = [
  { value: 'EXCELLENT', label: '优秀' },
  { value: 'ACTIVE', label: '积极' },
  { value: 'NORMAL', label: '一般' },
  { value: 'FOLLOW_UP', label: '待跟进' }
]

const scopeOptions = [
  { value: 'ALL', label: '全班学生' },
  { value: 'SIGNED', label: '已签到' },
  { value: 'UNSIGNED', label: '未签到' },
  { value: 'CUSTOM', label: '自定义' }
]

const ruleOptions = [
  { value: 'NONE', label: '不防重复' },
  { value: 'SESSION', label: '本次会话内不重复' },
  { value: 'DAYS7', label: '最近 7 天不重复' },
  { value: 'LAST_N', label: '最近 5 次不重复' }
]

let rollTimer: number | null = null
let offUpdate: (() => void) | null = null
let offSession: (() => void) | null = null

const candidatesLabel = computed(() => {
  if (form.scope === 'CUSTOM') return `${customChecked.value.length} 人（自定义）`
  return `${roster.value.length} 人`
})

const loadCourses = async () => {
  courses.value = await teacherApi.courses()
  if (courses.value.length) {
    form.courseId = courses.value[0].id
  }
}

const loadClasses = async () => {
  if (!form.courseId) return
  classes.value = await teacherApi.classes({ courseId: form.courseId })
  form.classId = classes.value[0]?.id
  const taskPage = await teacherApi.tasks({ courseId: form.courseId, page: 1, size: 50 })
  tasks.value = taskPage.list
  form.taskId = tasks.value[0]?.id
}

const loadRoster = async (resetCustom = false) => {
  roster.value = []
  if (form.classId) {
    const data = await teacherApi.roster(form.classId)
    roster.value = data.students || []
  } else {
    for (const group of classes.value) {
      const data = await teacherApi.roster(group.id)
      roster.value = [...roster.value, ...(data.students || [])]
    }
  }
  const ids = roster.value.map((student) => student.id)
  if (resetCustom || !customChecked.value.length) {
    customChecked.value = ids
  } else {
    customChecked.value = customChecked.value.filter((id) => ids.includes(id))
  }
}

watch(
  () => form.courseId,
  async () => {
    await loadClasses()
    await loadRoster(true)
    session.value = null
    phase.value = 'idle'
  }
)

watch(
  () => form.classId,
  async () => {
    await loadRoster(true)
  }
)

const startRoll = () => {
  if (!roster.value.length) {
    rollingNames.value = ['准备中']
    return
  }
  const count = Math.max(1, form.drawCount)
  const update = () => {
    const names: string[] = []
    for (let i = 0; i < count; i++) {
      const student = roster.value[Math.floor(Math.random() * roster.value.length)]
      names.push(student?.name || '—')
    }
    rollingNames.value = names
  }
  update()
  rollTimer = window.setInterval(update, 80)
}

const stopRoll = () => {
  if (rollTimer) {
    window.clearInterval(rollTimer)
    rollTimer = null
  }
}

const wait = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms))

const start = async () => {
  if (!form.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (['SIGNED', 'UNSIGNED'].includes(form.scope) && !form.taskId) {
    ElMessage.warning('按签到状态抽取需要选择签到任务')
    return
  }
  if (form.scope === 'CUSTOM' && !customChecked.value.length) {
    ElMessage.warning('请先勾选参与抽取的学生')
    return
  }
  await loadRoster()
  if (form.scope === 'CUSTOM' && !customChecked.value.length) {
    ElMessage.warning('请先勾选参与抽取的学生')
    return
  }
  loading.value = true
  phase.value = 'rolling'
  startRoll()
  const begin = Date.now()
  try {
    const result = await rollCallApi.create({
      courseId: form.courseId,
      classId: form.classId,
      candidateScope: form.scope,
      drawCount: form.drawCount,
      antiRepeatRule: form.antiRepeat,
      excludeLeave: form.excludeLeave,
      responseDeadlineSeconds: form.responseSeconds,
      taskId: form.taskId,
      customStudentIds: form.scope === 'CUSTOM' ? customChecked.value : undefined
    })
    session.value = result
    const elapsed = Date.now() - begin
    if (elapsed < 2600) await wait(2600 - elapsed)
    stopRoll()
    phase.value = 'result'
  } catch {
    stopRoll()
    phase.value = 'idle'
  } finally {
    loading.value = false
  }
}

const drawAgain = async () => {
  if (!session.value) return
  loading.value = true
  phase.value = 'rolling'
  startRoll()
  const begin = Date.now()
  try {
    const result = await rollCallApi.drawAgain(session.value.id, form.drawCount)
    session.value = result
    const elapsed = Date.now() - begin
    if (elapsed < 2000) await wait(2000 - elapsed)
    stopRoll()
    phase.value = 'result'
  } catch {
    stopRoll()
    phase.value = 'result'
  } finally {
    loading.value = false
  }
}

const refreshSession = async () => {
  if (!session.value) return
  try {
    session.value = await rollCallApi.detail(session.value.id)
    if (phase.value !== 'rolling') phase.value = 'result'
  } catch {
    // ignore
  }
}

const mark = async (record: any, result: string, performance?: string) => {
  if (!session.value) return
  marking.value = record.recordId
  try {
    await rollCallApi.markResult(session.value.id, record.recordId, {
      result,
      performance: performance ?? record.performance ?? null
    })
    await refreshSession()
  } finally {
    marking.value = null
  }
}

const changePerformance = async (record: any, value: string) => {
  await mark(record, record.result === 'PENDING' ? 'RESPONDED' : record.result, value)
}

const finishSession = async () => {
  if (!session.value) return
  try {
    await ElMessageBox.confirm('结束后本次点名不可继续抽取，但记录会完整保留。确认结束？', '结束点名会话', {
      confirmButtonText: '结束会话',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await rollCallApi.finish(session.value.id)
  ElMessage.success('点名会话已结束')
  await refreshSession()
}

const syncAttendance = async () => {
  if (!session.value) return
  try {
    await ElMessageBox.confirm(
      '该操作会向未响应学生发送出勤异常提醒并写入审计日志，不会修改签到记录。确认同步？',
      '同步异常出勤提醒',
      { confirmButtonText: '确认同步', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const result = await rollCallApi.syncAttendance(session.value.id)
  ElMessage.success(result.message)
}

const resetSession = () => {
  session.value = null
  phase.value = 'idle'
  stopRoll()
}

const openCustom = async () => {
  await loadRoster()
  customVisible.value = true
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.()
  } else {
    document.exitFullscreen?.()
  }
}

const statusOf = (result: string) => result

const deadlineOf = (record: any) => {
  if (!session.value) return undefined
  const drawn = new Date(record.drawnAt).getTime()
  return new Date(drawn + session.value.responseDeadlineSeconds * 1000).toISOString()
}

onMounted(async () => {
  await loadCourses()
  await loadClasses()
  await loadRoster(true)
  offUpdate = onRealtime('rollcall:update', (data: any) => {
    if (data?.sessionId === session.value?.id) refreshSession()
  })
  offSession = onRealtime('rollcall:session', (data: any) => {
    if (data?.sessionId === session.value?.id) refreshSession()
  })
})

onBeforeUnmount(() => {
  stopRoll()
  offUpdate?.()
  offSession?.()
})
</script>

<template>
  <div class="page">
    <PageHeader title="随机点名大屏" subtitle="后端无放回抽取并保存结果，动画仅用于呈现；同一会话绝不重复" eyebrow="ROLL CALL STAGE">
      <template #actions>
        <el-button @click="router.push('/teacher/interaction')">互动记录</el-button>
        <el-button @click="toggleFullscreen"><el-icon><FullScreen /></el-icon> 投屏全屏</el-button>
      </template>
    </PageHeader>

    <!-- 配置区 -->
    <section v-if="!session" class="panel config-panel">
      <div class="config-grid">
        <div class="config-item">
          <span class="label">课程</span>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%">
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </div>
        <div class="config-item">
          <span class="label">班级</span>
          <el-select v-model="form.classId" placeholder="全部班级" clearable style="width: 100%">
            <el-option v-for="group in classes" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </div>
        <div class="config-item">
          <span class="label">候选范围</span>
          <el-select v-model="form.scope" style="width: 100%">
            <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div v-if="['SIGNED', 'UNSIGNED'].includes(form.scope)" class="config-item">
          <span class="label">关联签到任务</span>
          <el-select v-model="form.taskId" placeholder="选择任务" style="width: 100%">
            <el-option v-for="task in tasks" :key="task.id" :label="task.title" :value="task.id" />
          </el-select>
        </div>
        <div class="config-item">
          <span class="label">抽取人数</span>
          <el-input-number v-model="form.drawCount" :min="1" :max="10" style="width: 100%" />
        </div>
        <div class="config-item">
          <span class="label">防重复规则</span>
          <el-select v-model="form.antiRepeat" style="width: 100%">
            <el-option v-for="item in ruleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="config-item">
          <span class="label">响应时长（秒）</span>
          <el-input-number v-model="form.responseSeconds" :min="10" :max="120" style="width: 100%" />
        </div>
        <div class="config-item switch-item">
          <span class="label">排除请假学生</span>
          <el-switch v-model="form.excludeLeave" />
        </div>
      </div>

      <div class="config-foot">
        <div class="row" style="gap: 12px">
          <span class="chip cyan"><i class="dot" />候选 {{ candidatesLabel }}</span>
          <span class="chip">{{ ruleLabel(form.antiRepeat) }}</span>
          <el-button v-if="form.scope === 'CUSTOM'" text type="primary" @click="openCustom">选择学生</el-button>
        </div>
        <el-button type="primary" size="large" class="start-btn" :loading="loading" @click="start">
          开始抽取
        </el-button>
      </div>
    </section>

    <!-- 抽取舞台 -->
    <section v-if="session || phase === 'rolling'" class="stage">
      <Particles :count="22" color="var(--cyan)" />
      <div class="stage-top">
        <div class="row" style="gap: 8px; flex-wrap: wrap">
          <span class="chip cyan">{{ session?.courseName || courses.find((c) => c.id === form.courseId)?.name }}</span>
          <span class="chip">{{ session?.className || classes.find((c) => c.id === form.classId)?.name || '全部班级' }}</span>
          <span class="chip">{{ scopeLabel(session?.candidateScope || form.scope) }}</span>
          <span class="chip mono" v-if="session">#{{ session.id }}</span>
        </div>
        <div class="row" style="gap: 10px">
          <StatusTag v-if="session" :status="session.status" />
          <span v-if="session" class="muted mono" style="font-size: 11px">进行中 {{ session.summary.pending }} 人待响应</span>
        </div>
      </div>

      <div class="stage-body">
        <template v-if="phase === 'rolling'">
          <div class="rolling-label">正在从花名册中抽取</div>
          <div class="roll-names">
            <span v-for="(name, index) in rollingNames" :key="index" class="stage-roll">{{ name }}</span>
          </div>
          <div class="rolling-hint">结果由服务端确定，动画结束后定格</div>
        </template>

        <template v-else-if="phase === 'idle'">
          <div class="idle-icon"><el-icon><Aim /></el-icon></div>
          <div class="idle-title">准备开始</div>
          <div class="idle-sub">候选池 {{ candidatesLabel }} · {{ ruleLabel(form.antiRepeat) }}</div>
          <el-button type="primary" size="large" class="idle-btn" :loading="loading" @click="start">开始抽取</el-button>
        </template>

        <template v-else-if="session">
          <div class="result-grid" :class="`count-${Math.min(session.records.length, 3)}`">
            <div
              v-for="(record, index) in session.records"
              :key="record.recordId"
              class="draw-card"
              :style="{ animationDelay: `${index * 80}ms` }"
            >
              <div class="draw-avatar">{{ record.avatarText }}</div>
              <div class="draw-name">{{ record.name }}</div>
              <div class="draw-no mono">{{ record.studentNo }}</div>
              <div class="draw-status">
                <StatusTag :status="record.result" :label="record.resultLabel" />
              </div>
              <div v-if="record.result === 'PENDING'" class="draw-timer">
                <Countdown :target="deadlineOf(record)" size="md" />
                <span class="timer-label">等待学生确认</span>
              </div>
              <div v-else-if="record.performance" class="draw-perf">{{ performanceLabel(record.performance) }}</div>
              <div class="draw-actions">
                <el-button
                  v-if="record.result === 'PENDING'"
                  size="small"
                  type="primary"
                  plain
                  :loading="marking === record.recordId"
                  @click="mark(record, 'RESPONDED')"
                >标记已响应</el-button>
                <el-button v-if="record.result === 'PENDING'" size="small" @click="mark(record, 'NO_RESPONSE')">未响应</el-button>
                <el-button v-if="record.result === 'PENDING'" size="small" text @click="mark(record, 'SKIPPED')">跳过</el-button>
                <el-button v-if="record.result !== 'PENDING' && record.result !== 'CANCELED'" size="small" text type="danger" @click="mark(record, 'CANCELED')">撤销</el-button>
              </div>
              <el-select
                :model-value="record.performance"
                size="small"
                placeholder="课堂表现"
                class="perf-select"
                @change="(value: string) => changePerformance(record, value)"
              >
                <el-option v-for="item in performanceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </div>
          </div>
        </template>
      </div>

      <div v-if="session" class="stage-foot">
        <div class="stage-stats">
          <div><span class="label">已抽</span><strong class="mono">{{ session.summary.total }}</strong></div>
          <div><span class="label">已响应</span><strong class="mono green">{{ session.summary.responded }}</strong></div>
          <div><span class="label">未响应</span><strong class="mono coral">{{ session.summary.noResponse }}</strong></div>
          <div><span class="label">待响应</span><strong class="mono amber">{{ session.summary.pending }}</strong></div>
          <div><span class="label">剩余候选</span><strong class="mono">{{ session.summary.remainingCandidates }}</strong></div>
        </div>
        <div class="stage-actions">
          <el-button @click="resetSession">返回配置</el-button>
          <el-button @click="syncAttendance">同步异常出勤</el-button>
          <el-button type="danger" plain :disabled="session.status !== 'RUNNING'" @click="finishSession">结束会话</el-button>
          <el-button type="primary" :loading="loading" :disabled="session.status !== 'RUNNING'" @click="drawAgain">
            再抽一次
          </el-button>
        </div>
      </div>
    </section>

    <!-- 自定义学生 -->
    <el-dialog v-model="customVisible" title="选择参与抽取的学生" width="520px">
      <div class="custom-head">
        <span class="muted">已选 {{ customChecked.length }} / {{ roster.length }} 人</span>
        <div class="row" style="gap: 6px">
          <el-button size="small" @click="customChecked = roster.map((s) => s.id)">全选</el-button>
          <el-button size="small" @click="customChecked = []">清空</el-button>
        </div>
      </div>
      <el-checkbox-group v-model="customChecked" class="custom-list">
        <el-checkbox v-for="student in roster" :key="student.id" :value="student.id" class="custom-item">
          <span>{{ student.name }}</span>
          <span class="muted mono">{{ student.studentNo }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="customVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.config-panel { padding: 20px; }
.config-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.config-item { display: flex; flex-direction: column; gap: 6px; }
.switch-item { justify-content: flex-end; }
.switch-item .label { margin-bottom: 6px; }
.config-foot { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--line); }
.start-btn { min-width: 180px; height: 46px; font-size: 15px; }

.stage {
  margin-top: 16px;
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(56, 221, 248, 0.18);
  background:
    radial-gradient(circle at 18% 12%, rgba(56, 221, 248, 0.1), transparent 55%),
    radial-gradient(circle at 80% 84%, rgba(45, 212, 191, 0.08), transparent 52%),
    var(--stage-bg);
  min-height: 500px;
  display: flex;
  flex-direction: column;
}
.stage::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(56, 221, 248, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 221, 248, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(circle at 50% 45%, #000, transparent 82%);
  pointer-events: none;
}
.stage-top { position: relative; display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 16px 20px; border-bottom: 1px solid rgba(148, 163, 184, 0.1); }
.stage-body { position: relative; flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px 24px; min-height: 330px; }
.rolling-label { font-size: 12px; letter-spacing: 0.16em; color: var(--cyan); margin-bottom: 26px; }
.roll-names { display: flex; gap: 28px; flex-wrap: wrap; justify-content: center; }
.stage-roll { animation: flicker 0.16s ease infinite alternate; text-shadow: 0 0 24px rgba(56, 221, 248, 0.4); }
@keyframes flicker { from { opacity: 0.55; filter: blur(1px); } to { opacity: 1; filter: none; } }
.rolling-hint { margin-top: 28px; font-size: 11px; color: var(--muted); }
.idle-icon { font-size: 48px; color: rgba(56, 221, 248, 0.7); }
.idle-title { font-family: var(--mono); font-size: clamp(30px, 5vw, 56px); font-weight: 700; margin-top: 12px; letter-spacing: 0.04em; }
.idle-sub { font-size: 12px; color: var(--muted); margin: 8px 0 24px; }
.idle-btn { min-width: 180px; height: 46px; }

.result-grid { display: grid; gap: 16px; width: 100%; justify-content: center; }
.result-grid.count-1 { grid-template-columns: minmax(0, 360px); }
.result-grid.count-2 { grid-template-columns: repeat(2, minmax(0, 320px)); }
.result-grid.count-3 { grid-template-columns: repeat(3, minmax(0, 300px)); }
.draw-card {
  background: var(--glass-bg);
  border: 1px solid rgba(56, 221, 248, 0.22);
  border-radius: 12px;
  padding: 22px 16px 16px;
  text-align: center;
  backdrop-filter: blur(8px);
  animation: drawIn 0.5s cubic-bezier(0.2, 0.9, 0.3, 1) both;
}
@keyframes drawIn {
  from { opacity: 0; transform: scale(0.94) translateY(10px); }
  to { opacity: 1; transform: none; }
}
.draw-avatar {
  width: 64px; height: 64px; border-radius: 50%;
  margin: 0 auto 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 650;
  color: var(--cyan);
  background: rgba(56, 221, 248, 0.09);
  border: 1px solid rgba(56, 221, 248, 0.3);
}
.draw-name { font-family: var(--mono); font-size: clamp(24px, 3vw, 38px); font-weight: 700; letter-spacing: 0.02em; line-height: 1.2; }
.draw-no { font-size: 12px; color: var(--muted); margin-top: 4px; }
.draw-status { margin-top: 10px; }
.draw-timer { margin-top: 10px; display: flex; flex-direction: column; align-items: center; gap: 2px; }
.timer-label { font-size: 10.5px; color: var(--muted); }
.draw-perf { margin-top: 10px; font-size: 12px; color: var(--teal); }
.draw-actions { display: flex; justify-content: center; gap: 4px; flex-wrap: wrap; margin-top: 12px; }
.perf-select { margin-top: 10px; width: 130px; }
.perf-select :deep(.el-select__wrapper) { min-height: 28px; }

.stage-foot {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  flex-wrap: wrap;
}
.stage-stats { display: flex; gap: 24px; flex-wrap: wrap; }
.stage-stats div { display: flex; align-items: baseline; gap: 6px; }
.stage-stats strong { font-size: 18px; }
.stage-actions { display: flex; gap: 8px; flex-wrap: wrap; }

.custom-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.custom-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 12px; max-height: 360px; overflow: auto; }
.custom-item { margin-right: 0; }
.custom-item :deep(.el-checkbox__label) { display: flex; gap: 8px; align-items: center; font-size: 12px; }

@media (max-width: 1100px) {
  .config-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .result-grid.count-3 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .config-grid { grid-template-columns: 1fr; }
  .config-foot { flex-direction: column; align-items: stretch; }
  .start-btn { width: 100%; }
  .result-grid.count-2, .result-grid.count-3, .result-grid.count-1 { grid-template-columns: 1fr; }
  .roll-names { flex-direction: column; gap: 10px; align-items: center; }
  .stage { min-height: 560px; }
  .stage-foot { flex-direction: column; align-items: stretch; }
  .custom-list { grid-template-columns: 1fr; }
}
</style>
