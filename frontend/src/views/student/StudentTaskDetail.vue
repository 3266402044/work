<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentApi, taskApi, uploadApi } from '@/api'
import { onRealtime } from '@/composables/realtime'
import { formatDateTime, formatTime, modeLabel } from '@/utils/format'
import type { TaskVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import Countdown from '@/components/Countdown.vue'
import EmptyState from '@/components/EmptyState.vue'
import ProgressRing from '@/components/ProgressRing.vue'
import Confetti from '@/components/Confetti.vue'
import { playSuccessChime } from '@/utils/sound'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.id)
const task = ref<TaskVO | null>(null)
const loading = ref(true)
const error = ref('')
const signLoading = ref(false)
const locating = ref(false)
const activeMode = ref('')
const passcode = ref('')
const location = reactive<{ lon?: number; lat?: number; accuracy?: number }>({})
const successRecord = ref<any>(null)
const celebrate = ref(false)
const leaveVisible = ref(false)
const leaveForm = reactive({ reason: '', proofUrl: '' })
const uploading = ref(false)

const qrToken = computed(() => (typeof route.query.qr === 'string' ? route.query.qr : ''))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    task.value = await taskApi.detail(taskId)
    if (!activeMode.value && task.value.signModes.length) {
      activeMode.value = task.value.signed ? '' : task.value.signModes[0]
    }
    if (task.value.myRecord) successRecord.value = task.value.myRecord
  } catch (e: any) {
    error.value = e?.message || '任务加载失败'
  } finally {
    loading.value = false
  }
}

const doSign = async (payload: Record<string, any>) => {
  if (!task.value) return
  signLoading.value = true
  try {
    const result = await studentApi.sign(taskId, payload)
    task.value = result.task
    successRecord.value = result.record
    ElMessage.success('签到成功')
    celebrate.value = true
    playSuccessChime()
    window.setTimeout(() => (celebrate.value = false), 2800)
  } finally {
    signLoading.value = false
  }
}

const signClick = () => doSign({ method: 'CLICK' })

const signPasscode = () => {
  if (!passcode.value) {
    ElMessage.warning('请输入教师公布的 6 位口令')
    return
  }
  doSign({ method: 'PASSCODE', passcode: passcode.value })
}

const signQr = () => {
  if (!qrToken.value) {
    ElMessage.warning('请扫描教师展示的动态二维码后进入本页')
    return
  }
  doSign({ method: 'QR', qrToken: qrToken.value })
}

const locate = () => {
  if (!navigator.geolocation) {
    ElMessage.error('当前浏览器不支持定位，请更换浏览器或使用普通签到')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (position) => {
      location.lon = Number(position.coords.longitude.toFixed(7))
      location.lat = Number(position.coords.latitude.toFixed(7))
      location.accuracy = Math.round(position.coords.accuracy)
      locating.value = false
      ElMessage.success('定位成功，可提交定位签到')
    },
    (err) => {
      locating.value = false
      ElMessage.error(err.code === 1 ? '定位权限被拒绝，请在浏览器设置中允许定位' : '定位失败，请到开阔处重试')
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const signLocation = () => {
  if (location.lon === undefined || location.lat === undefined) {
    ElMessage.warning('请先获取当前位置')
    return
  }
  doSign({ method: 'LOCATION', longitude: location.lon, latitude: location.lat })
}

const openLeave = () => {
  leaveForm.reason = ''
  leaveForm.proofUrl = ''
  leaveVisible.value = true
}

const uploadProof = async (options: any) => {
  uploading.value = true
  try {
    const result = await uploadApi.upload(options.file)
    leaveForm.proofUrl = result.url
    ElMessage.success('凭证已上传')
  } finally {
    uploading.value = false
  }
}

const submitLeave = async () => {
  if (!leaveForm.reason.trim()) {
    ElMessage.warning('请填写请假原因')
    return
  }
  await studentApi.applyLeave(taskId, { reason: leaveForm.reason, proofUrl: leaveForm.proofUrl })
  ElMessage.success('请假申请已提交，等待教师审批')
  leaveVisible.value = false
  load()
}

const statusTarget = computed(() => {
  if (!task.value) return undefined
  if (task.value.taskStatus === 'ONGOING') return task.value.endTime
  if (task.value.taskStatus === 'NOT_STARTED') return task.value.startTime
  return undefined
})

const modeIcon = (mode: string) => {
  switch (mode) {
    case 'CLICK': return 'Pointer'
    case 'LOCATION': return 'Location'
    case 'PASSCODE': return 'Key'
    case 'QR': return 'Grid'
    default: return 'Check'
  }
}

let off: (() => void) | null = null
onMounted(() => {
  load()
  off = onRealtime('task:changed', (data: any) => {
    if (data?.taskId === taskId) load()
  })
})
onBeforeUnmount(() => off?.())
</script>

<template>
  <div class="page">
    <PageHeader
      :title="task?.title || '签到任务'"
      :subtitle="task ? `${task.courseName} · ${task.teacherName} · ${task.className}` : '加载中…'"
      eyebrow="SIGN TASK"
    >
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div class="skeleton" style="height: 120px" />
      <div class="skeleton" style="height: 320px" />
    </div>

    <div v-else-if="error" class="panel">
      <EmptyState title="无法加载签到任务" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重试</el-button></template>
      </EmptyState>
    </div>

    <template v-else-if="task">
      <!-- 状态带 -->
      <section class="status-band panel" :class="task.taskStatus.toLowerCase()">
        <div class="band-left">
          <StatusTag :status="task.taskStatus" />
          <div class="band-time" v-if="statusTarget">
            <span class="label">{{ task.taskStatus === 'ONGOING' ? '距离结束' : '距离开始' }}</span>
            <Countdown :target="statusTarget" size="lg" @end="load" />
          </div>
          <div class="band-time" v-else>
            <span class="label">任务状态</span>
            <span class="band-static">{{ task.taskStatusLabel }}</span>
          </div>
        </div>
        <div class="band-right">
          <div class="band-stat">
            <span class="label">班级到课</span>
            <strong class="mono">{{ task.signedCount }}<span class="muted">/{{ task.totalStudents }}</span></strong>
          </div>
          <div class="band-stat">
            <span class="label">出勤率</span>
            <strong class="mono cyan">{{ task.attendanceRate }}%</strong>
          </div>
          <div class="band-stat">
            <span class="label">签到方式</span>
            <strong class="mono">{{ task.signModes.length }}</strong>
          </div>
        </div>
      </section>

      <div class="grid grid-main-side section-gap">
        <!-- 签到操作区 -->
        <section class="panel sign-panel">
          <div class="panel-head">
            <div class="panel-title">{{ successRecord ? '签到结果' : '完成签到' }}</div>
            <span v-if="qrToken" class="chip cyan"><i class="dot" />已识别二维码凭证</span>
          </div>

          <!-- 已签到 -->
          <div v-if="successRecord" class="success-block success-pulse">
            <div class="success-icon"><el-icon><CircleCheckFilled /></el-icon></div>
            <div class="success-title">你已成功签到</div>
            <div class="success-sub">签到时间以服务端时间为准，记录不可重复提交</div>
            <div class="success-grid">
              <div><span>签到时间</span><strong class="mono">{{ formatDateTime(successRecord.signTime) }}</strong></div>
              <div><span>签到状态</span><StatusTag :status="successRecord.status" /></div>
              <div><span>签到方式</span><strong>{{ successRecord.method === 'CLICK' ? '普通签到' : successRecord.method === 'LOCATION' ? '定位签到' : successRecord.method === 'PASSCODE' ? '口令签到' : successRecord.method === 'QR' ? '二维码' : '教师补签' }}</strong></div>
              <div v-if="successRecord.distanceMeters"><span>签到距离</span><strong class="mono">{{ successRecord.distanceMeters }} m</strong></div>
            </div>
          </div>

          <!-- 不可签到 -->
          <div v-else-if="!task.canSign" class="unavailable">
            <el-icon class="un-icon"><Lock /></el-icon>
            <div class="un-title">{{ task.signHint || '当前不可签到' }}</div>
            <div class="un-sub">签到状态由服务端统一判定，请以任务状态为准</div>
          </div>

          <!-- 可签到 -->
          <template v-else>
            <div class="mode-tabs">
              <button
                v-for="mode in task.signModes"
                :key="mode"
                type="button"
                class="mode-tab"
                :class="{ active: activeMode === mode }"
                @click="activeMode = mode"
              >
                <el-icon><component :is="modeIcon(mode)" /></el-icon>
                {{ modeLabel(mode) }}
              </button>
            </div>

            <div class="mode-body">
              <template v-if="activeMode === 'CLICK'">
                <div class="mode-desc">
                  <div class="mode-title">普通签到</div>
                  <p>确认你已到达课堂，点击下方按钮即可完成签到。系统会根据迟到截止时间自动判定正常或迟到。</p>
                </div>
                <el-button type="primary" size="large" class="sign-btn" :loading="signLoading" @click="signClick">
                  立即签到
                </el-button>
              </template>

              <template v-else-if="activeMode === 'LOCATION'">
                <div class="mode-desc">
                  <div class="mode-title">定位签到</div>
                  <p>
                    签到地点：{{ task.locationName || '未命名地点' }}，允许半径 {{ task.radiusMeters }} 米。
                    浏览器需要授权定位，后端使用 Haversine 公式计算距离。
                  </p>
                </div>
                <div class="loc-box">
                  <div class="loc-row">
                    <span class="label">当前位置</span>
                    <span class="mono">{{ location.lon !== undefined ? `${location.lon}, ${location.lat} (±${location.accuracy}m)` : '尚未获取' }}</span>
                  </div>
                  <div class="loc-row">
                    <span class="label">签到中心</span>
                    <span class="mono">{{ task.longitude }}, {{ task.latitude }}</span>
                  </div>
                </div>
                <div class="row wrap">
                  <el-button :loading="locating" @click="locate">
                    <el-icon><Aim /></el-icon> 获取当前位置
                  </el-button>
                  <el-button type="primary" size="large" :loading="signLoading" :disabled="location.lon === undefined" @click="signLocation">
                    提交定位签到
                  </el-button>
                </div>
              </template>

              <template v-else-if="activeMode === 'PASSCODE'">
                <div class="mode-desc">
                  <div class="mode-title">口令签到</div>
                  <p>输入教师在大屏上公布的 6 位数字口令。口令仅以哈希形式保存，不会明文回传。</p>
                </div>
                <el-input
                  v-model="passcode"
                  class="passcode-input"
                  maxlength="6"
                  placeholder="请输入 6 位口令"
                  @keyup.enter="signPasscode"
                >
                  <template #prefix><el-icon><Key /></el-icon></template>
                </el-input>
                <el-button type="primary" size="large" class="sign-btn" :loading="signLoading" @click="signPasscode">
                  验证并签到
                </el-button>
              </template>

              <template v-else-if="activeMode === 'QR'">
                <div class="mode-desc">
                  <div class="mode-title">动态二维码</div>
                  <p>扫描教师大屏上展示的二维码即可进入本页。二维码令牌有效期 30 秒，过期后需教师刷新。</p>
                </div>
                <div class="qr-state" :class="{ ok: qrToken }">
                  <el-icon class="qr-icon"><component :is="qrToken ? 'CircleCheckFilled' : 'Grid'" /></el-icon>
                  <div>
                    <div class="qr-title">{{ qrToken ? '二维码凭证已识别' : '等待扫码' }}</div>
                    <div class="qr-sub">{{ qrToken ? '令牌有效期内可直接完成签到' : '请使用手机扫描教师展示的二维码' }}</div>
                  </div>
                </div>
                <el-button type="primary" size="large" class="sign-btn" :loading="signLoading" :disabled="!qrToken" @click="signQr">
                  完成二维码签到
                </el-button>
              </template>
            </div>
          </template>
        </section>

        <!-- 规则与操作 -->
        <div class="col" style="gap: 16px">
          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">签到规则</div>
            </div>
            <div class="rule-list">
              <div><span>开始时间</span><strong class="mono">{{ formatDateTime(task.startTime) }}</strong></div>
              <div><span>结束时间</span><strong class="mono">{{ formatDateTime(task.endTime) }}</strong></div>
              <div><span>迟到截止</span><strong class="mono">{{ task.lateTime ? formatDateTime(task.lateTime) : '未设置' }}</strong></div>
              <div><span>签到地点</span><strong>{{ task.locationName || '未设置' }}</strong></div>
              <div v-if="task.radiusMeters"><span>允许半径</span><strong class="mono">{{ task.radiusMeters }} m</strong></div>
              <div><span>签到方式</span><strong>{{ task.signModes.map(modeLabel).join(' / ') }}</strong></div>
              <div><span>面向班级</span><strong>{{ task.className }}</strong></div>
            </div>
            <div class="divider" />
            <div class="progress-line">
              <div class="row-between" style="margin-bottom: 6px">
                <span class="label">班级签到进度</span>
                <span class="mono">{{ task.signedCount }} / {{ task.totalStudents }}</span>
              </div>
              <div class="bar-track"><div class="bar-fill" :style="{ width: task.attendanceRate + '%' }" /></div>
              <div class="row wrap" style="margin-top: 12px; gap: 12px">
                <span class="chip green"><i class="dot" />正常 {{ task.normalCount }}</span>
                <span class="chip amber"><i class="dot" />迟到 {{ task.lateCount }}</span>
                <span class="chip"><i class="dot" />未签 {{ task.absentCount }}</span>
              </div>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">无法到场？</div>
            </div>
            <p class="leave-desc">可在任务结束 24 小时内提交请假申请，教师审批通过后将自动登记为请假记录。</p>
            <el-button plain :disabled="Boolean(successRecord) || task.taskStatus === 'ENDED'" @click="openLeave">
              <el-icon><Document /></el-icon> 提交请假申请
            </el-button>
          </section>
        </div>
      </div>
    </template>

    <el-dialog v-model="leaveVisible" title="提交请假申请" width="440px">
      <el-form label-position="top">
        <el-form-item label="请假原因" required>
          <el-input v-model="leaveForm.reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请说明请假原因" />
        </el-form-item>
        <el-form-item label="证明材料（可选）">
          <el-upload :show-file-list="false" :http-request="uploadProof" accept="image/*,.pdf">
            <el-button :loading="uploading"><el-icon><Upload /></el-icon> 上传凭证</el-button>
          </el-upload>
          <span v-if="leaveForm.proofUrl" class="proof-ok"><el-icon><CircleCheck /></el-icon> 已上传</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaveVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLeave">提交申请</el-button>
      </template>
    </el-dialog>

    <Confetti v-if="celebrate" />
  </div>
</template>

<style scoped>
.status-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px;
  border-color: var(--line);
}
.status-band.ongoing { border-left: 2px solid var(--green); }
.status-band.not_started { border-left: 2px solid var(--cyan); }
.status-band.closed { border-left: 2px solid var(--coral); }
.band-left, .band-right { display: flex; align-items: center; gap: 28px; flex-wrap: wrap; }
.band-time { display: flex; flex-direction: column; gap: 0; }
.band-static { font-size: 24px; font-weight: 600; }
.band-stat { display: flex; flex-direction: column; gap: 2px; align-items: flex-end; }
.band-stat strong { font-size: 22px; font-weight: 600; }
.band-stat .muted { font-size: 12px; }

.sign-panel { min-height: 340px; }
.mode-tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
.mode-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-2);
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease, color 0.16s ease;
}
.mode-tab:hover { border-color: var(--line-strong); }
.mode-tab.active { border-color: rgba(56, 221, 248, 0.55); background: rgba(56, 221, 248, 0.08); color: var(--cyan); }
.mode-body { display: flex; flex-direction: column; gap: 16px; }
.mode-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.mode-desc p { margin: 0; color: var(--muted); font-size: 12.5px; line-height: 1.8; max-width: 620px; }
.sign-btn { width: 100%; max-width: 320px; height: 46px; font-size: 15px; }
.passcode-input { max-width: 320px; }
.passcode-input :deep(input) { font-family: var(--mono); font-size: 20px; letter-spacing: 0.35em; text-align: center; }
.loc-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--panel-3);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
  max-width: 520px;
}
.loc-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 12px; }
.qr-state {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 10px;
  border: 1px dashed var(--line-strong);
  background: rgba(148, 163, 184, 0.03);
  max-width: 520px;
}
.qr-state.ok { border-color: rgba(71, 209, 108, 0.4); background: rgba(71, 209, 108, 0.05); }
.qr-icon { font-size: 28px; color: var(--muted); }
.qr-state.ok .qr-icon { color: var(--green); }
.qr-title { font-size: 14px; font-weight: 600; }
.qr-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; }

.success-block { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 18px 0 4px; }
.success-icon { font-size: 64px; color: var(--green); line-height: 1; }
.success-title { font-size: 22px; font-weight: 650; margin-top: 12px; }
.success-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }
.success-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  max-width: 480px;
  margin-top: 22px;
  text-align: left;
}
.success-grid > div {
  background: var(--panel-3);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.success-grid span { font-size: 11px; color: var(--muted); }
.success-grid strong { font-size: 12.5px; font-weight: 500; }

.unavailable { text-align: center; padding: 38px 0; }
.un-icon { font-size: 36px; color: var(--muted); }
.un-title { font-size: 17px; font-weight: 600; margin-top: 10px; }
.un-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }

.rule-list { display: flex; flex-direction: column; gap: 11px; }
.rule-list div { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.rule-list span { font-size: 12px; color: var(--muted); }
.rule-list strong { font-size: 12.5px; font-weight: 500; color: var(--text-2); text-align: right; }
.leave-desc { font-size: 12px; color: var(--muted); line-height: 1.8; margin: 0 0 14px; }
.proof-ok { display: inline-flex; align-items: center; gap: 5px; color: var(--green); font-size: 12px; margin-left: 10px; }

@media (max-width: 760px) {
  .status-band { flex-direction: column; align-items: flex-start; }
  .band-right { width: 100%; justify-content: space-between; }
  .success-grid { grid-template-columns: 1fr; }
  .sign-btn { max-width: 100%; }
}
</style>
