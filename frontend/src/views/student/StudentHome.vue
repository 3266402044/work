<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { studentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { onRealtime } from '@/composables/realtime'
import { formatDateTime, formatTime, modeLabel, taskStatusClass } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import ProgressRing from '@/components/ProgressRing.vue'
import Countdown from '@/components/Countdown.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const data = ref<any>(null)
const loading = ref(true)
const error = ref('')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const heroTask = computed(() => data.value?.ongoingTask || data.value?.upcomingTask || null)
const ongoing = computed(() => Boolean(data.value?.ongoingTask))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    data.value = await studentApi.dashboard()
  } catch (e: any) {
    error.value = e?.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

const goTask = (id: number) => router.push(`/student/tasks/${id}`)

let offTask: (() => void) | null = null
onMounted(() => {
  load()
  offTask = onRealtime('task:changed', () => load())
})
onBeforeUnmount(() => offTask?.())

const attendance = computed(() => data.value?.attendance7d || {})
</script>

<template>
  <div class="page">
    <PageHeader
      :title="`${greeting}，${auth.user?.realName || '同学'}`"
      :subtitle="`${data?.student?.className || ''} · 今天 ${data?.today || ''}`"
      eyebrow="STUDENT WORKSPACE"
    >
      <template #actions>
        <el-button @click="router.push('/student/records')">我的签到</el-button>
        <el-button type="primary" @click="router.push('/student/calendar')">课程日历</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div class="skeleton" style="height: 260px" />
      <div class="grid grid-2">
        <div class="skeleton" style="height: 220px" />
        <div class="skeleton" style="height: 220px" />
      </div>
    </div>

    <div v-else-if="error" class="panel">
      <EmptyState title="首页加载失败" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重新加载</el-button></template>
      </EmptyState>
    </div>

    <template v-else>
      <!-- 进行中 / 下一场签到 -->
      <section class="hero panel" :class="{ live: ongoing }">
        <div class="hero-bg" />
        <div v-if="heroTask" class="hero-inner">
          <div class="hero-left">
            <div class="row wrap" style="margin-bottom: 12px">
              <StatusTag :status="heroTask.taskStatus" />
              <span class="chip mono">{{ heroTask.className }}</span>
              <span class="chip mono">{{ heroTask.courseName }}</span>
              <span v-if="heroTask.signed" class="chip green"><i class="dot" />已签到</span>
            </div>
            <h2>{{ heroTask.title }}</h2>
            <div class="hero-meta row wrap">
              <span><el-icon><User /></el-icon>{{ heroTask.teacherName }}</span>
              <span v-if="heroTask.locationName"><el-icon><Location /></el-icon>{{ heroTask.locationName }}</span>
              <span><el-icon><Clock /></el-icon>{{ formatTime(heroTask.startTime) }} - {{ formatTime(heroTask.endTime) }}</span>
              <span v-for="mode in heroTask.signModes" :key="mode" class="chip cyan">{{ modeLabel(mode) }}</span>
            </div>

            <div class="hero-count">
              <div class="col" style="gap: 2px">
                <span class="label">{{ ongoing ? '距离签到结束' : '距离签到开始' }}</span>
                <Countdown
                  :target="ongoing ? heroTask.endTime : heroTask.startTime"
                  size="lg"
                  @end="load"
                />
              </div>
              <div class="count-divider" />
              <div class="col" style="gap: 2px">
                <span class="label">迟到截止</span>
                <span class="mono count-sub">{{ heroTask.lateTime ? formatTime(heroTask.lateTime) : '未设置' }}</span>
              </div>
              <div class="count-divider" />
              <div class="col" style="gap: 2px">
                <span class="label">签到方式</span>
                <span class="count-sub">{{ heroTask.signModes.length }} 种可用</span>
              </div>
            </div>

            <div class="hero-actions">
              <el-button
                v-if="heroTask.canSign"
                type="primary"
                size="large"
                class="cta"
                @click="goTask(heroTask.id)"
              >
                立即签到
              </el-button>
              <el-button
                v-else
                size="large"
                :disabled="!heroTask.signed"
                class="cta"
                @click="goTask(heroTask.id)"
              >
                {{ heroTask.signed ? '已签到，查看详情' : heroTask.signHint || '查看任务详情' }}
              </el-button>
              <span v-if="heroTask.signed && heroTask.myRecord" class="signed-note">
                <el-icon><CircleCheckFilled /></el-icon>
                {{ formatDateTime(heroTask.myRecord.signTime) }} 已完成签到
              </span>
              <span v-else-if="!heroTask.canSign" class="signed-note muted">{{ heroTask.signHint }}</span>
            </div>
          </div>

          <div class="hero-right">
            <ProgressRing :value="heroTask.attendanceRate" label="班级到课进度" :sublabel="`${heroTask.signedCount}/${heroTask.totalStudents} 人`" :size="148" />
            <div class="mini-stats">
              <div><span class="dot" style="color: var(--green)" />正常 <strong class="mono">{{ heroTask.normalCount }}</strong></div>
              <div><span class="dot" style="color: var(--amber)" />迟到 <strong class="mono">{{ heroTask.lateCount }}</strong></div>
              <div><span class="dot" style="color: var(--muted)" />未签 <strong class="mono">{{ heroTask.absentCount }}</strong></div>
            </div>
          </div>
        </div>
        <EmptyState
          v-else
          title="当前没有签到任务"
          description="今天的签到任务完成后，可在这里查看最近的课堂记录"
          icon="Coffee"
        />
      </section>

      <div class="grid grid-main-side section-gap">
        <!-- 今日课表 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">今日课表与签到</div>
            <span class="panel-sub">{{ data?.todayTasks?.length || 0 }} 项安排</span>
          </div>
          <div v-if="!data?.todayTasks?.length" class="empty-line">
            <EmptyState title="今天没有课程安排" description="可以在课程日历查看本周计划" icon="Calendar" />
          </div>
          <div v-else class="today-list">
            <button
              v-for="task in data.todayTasks"
              :key="task.id"
              type="button"
              class="today-item"
              @click="goTask(task.id)"
            >
              <div class="today-time mono">{{ formatTime(task.startTime) }}</div>
              <div class="today-line" :style="{ background: task.courseColor || 'var(--cyan)' }" />
              <div class="today-body">
                <div class="row" style="gap: 8px">
                  <span class="today-course">{{ task.courseName }}</span>
                  <StatusTag :status="task.taskStatus" />
                </div>
                <div class="today-title">{{ task.title }}</div>
                <div class="today-meta">
                  <span>{{ task.locationName || '未设置地点' }}</span>
                  <span>·</span>
                  <span>{{ task.signModes.map(modeLabel).join(' / ') }}</span>
                </div>
              </div>
              <div class="today-right">
                <span v-if="task.signed" class="signed-badge"><el-icon><Check /></el-icon>已签</span>
                <span v-else-if="task.canSign" class="chip green">可签到</span>
                <span v-else class="chip">{{ task.taskStatusLabel }}</span>
              </div>
            </button>
          </div>
        </section>

        <!-- 近 7 日出勤 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">近 7 日出勤</div>
            <span class="panel-sub">基于已结束任务</span>
          </div>
          <div class="attendance-wrap">
            <ProgressRing :value="attendance.rate || 0" label="出勤率" :sublabel="`${attendance.present}/${attendance.total} 次`" :size="132" color="var(--teal)" />
            <div class="att-legend">
              <div><span class="dot" style="color: var(--green)" />正常签到 <strong class="mono">{{ attendance.normal || 0 }}</strong></div>
              <div><span class="dot" style="color: var(--amber)" />迟到 <strong class="mono">{{ attendance.late || 0 }}</strong></div>
              <div><span class="dot" style="color: var(--cyan)" />请假 <strong class="mono">{{ attendance.leave || 0 }}</strong></div>
              <div><span class="dot" style="color: var(--coral)" />缺勤 <strong class="mono">{{ attendance.absent || 0 }}</strong></div>
            </div>
          </div>
          <div class="divider" />
          <div class="row-between">
            <div class="col" style="gap: 2px">
              <span class="label">待处理请假</span>
              <strong class="medium-num" :class="{ amber: data?.pendingLeaves }">{{ data?.pendingLeaves || 0 }}</strong>
            </div>
            <el-button text type="primary" @click="router.push('/student/leave')">去申请</el-button>
          </div>
        </section>
      </div>

      <div class="grid grid-2 section-gap">
        <!-- 最近签到 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">最近签到</div>
            <el-button text type="primary" @click="router.push('/student/records')">全部记录</el-button>
          </div>
          <div v-if="!data?.recentRecords?.length">
            <EmptyState title="暂无签到记录" icon="Tickets" />
          </div>
          <div v-else class="timeline">
            <div v-for="record in data.recentRecords" :key="record.id" class="timeline-item">
              <div class="timeline-time">{{ formatDateTime(record.signTime) }}</div>
              <div class="timeline-dot" :style="{ background: record.status === 'LATE' ? 'var(--amber)' : record.status === 'LEAVE' ? 'var(--cyan)' : 'var(--green)' }" />
              <div class="grow">
                <div class="row-between">
                  <span class="record-title">{{ record.taskTitle }}</span>
                  <StatusTag :status="record.status" :label="record.statusLabel" />
                </div>
                <div class="record-meta">
                  {{ record.courseName }} · {{ record.methodLabel }}
                  <template v-if="record.distanceMeters"> · {{ record.distanceMeters }}m</template>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 公告 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">课堂公告</div>
            <span class="panel-sub">来自你的课程</span>
          </div>
          <div v-if="!data?.announcements?.length">
            <EmptyState title="暂无公告" icon="Bell" />
          </div>
          <div v-else class="ann-list">
            <div v-for="item in data.announcements" :key="item.id" class="ann-item">
              <div class="row" style="gap: 8px; margin-bottom: 4px">
                <span class="chip cyan">{{ item.courseName }}</span>
                <span v-if="item.pinned" class="chip amber">置顶</span>
                <span class="muted mono tiny">{{ formatDateTime(item.createTime) }}</span>
              </div>
              <div class="ann-content">{{ item.content }}</div>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.hero { padding: 0; overflow: hidden; position: relative; }
.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 78% 20%, rgba(56, 221, 248, 0.13), transparent 52%),
    radial-gradient(circle at 8% 90%, rgba(45, 212, 191, 0.08), transparent 45%);
  pointer-events: none;
}
.hero.live { border-color: rgba(71, 209, 108, 0.28); }
.hero.live .hero-bg {
  background:
    radial-gradient(circle at 80% 16%, rgba(71, 209, 108, 0.12), transparent 50%),
    radial-gradient(circle at 10% 90%, rgba(56, 221, 248, 0.08), transparent 45%);
}
.hero-inner {
  position: relative;
  display: flex;
  gap: 28px;
  padding: 26px 28px;
  justify-content: space-between;
}
.hero-left { flex: 1; min-width: 0; }
.hero h2 {
  margin: 0 0 12px;
  font-size: clamp(22px, 2.6vw, 30px);
  letter-spacing: -0.02em;
  font-weight: 650;
}
.hero-meta { gap: 16px; color: var(--muted); font-size: 12px; }
.hero-meta span { display: inline-flex; align-items: center; gap: 5px; }
.hero-count { display: flex; align-items: flex-end; gap: 22px; margin: 24px 0 20px; flex-wrap: wrap; }
.count-sub { font-size: 15px; color: var(--text-2); }
.count-divider { width: 1px; height: 34px; background: var(--line); }
.hero-actions { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.cta { min-width: 168px; height: 44px; font-size: 14px; }
.signed-note { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--green); }
.hero-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-width: 210px;
  border-left: 1px solid var(--line);
  padding-left: 28px;
}
.mini-stats { display: flex; gap: 16px; font-size: 11px; color: var(--muted); }
.mini-stats div { display: flex; align-items: center; gap: 5px; }
.mini-stats strong { color: var(--text-2); font-weight: 500; }

.today-list { display: flex; flex-direction: column; }
.today-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 12px 4px;
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--line);
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.today-item:last-child { border-bottom: none; }
.today-item:hover .today-course { color: var(--cyan); }
.today-time { font-size: 13px; color: var(--text-2); width: 46px; flex-shrink: 0; }
.today-line { width: 3px; height: 34px; border-radius: 3px; opacity: 0.85; flex-shrink: 0; }
.today-body { flex: 1; min-width: 0; }
.today-course { font-size: 13px; font-weight: 600; transition: color 0.16s ease; }
.today-title { font-size: 12px; color: var(--text-2); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.today-meta { font-size: 11px; color: var(--muted); display: flex; gap: 5px; margin-top: 3px; }
.today-right { flex-shrink: 0; }
.signed-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--green); }

.attendance-wrap { display: flex; align-items: center; gap: 24px; }
.att-legend { display: flex; flex-direction: column; gap: 9px; font-size: 12px; color: var(--muted); flex: 1; }
.att-legend div { display: flex; align-items: center; gap: 8px; }
.att-legend strong { margin-left: auto; color: var(--text-2); font-weight: 500; }

.record-title { font-size: 13px; color: var(--text-2); }
.record-meta { font-size: 11px; color: var(--muted); margin-top: 3px; }
.ann-list { display: flex; flex-direction: column; gap: 14px; }
.ann-item { padding-bottom: 12px; border-bottom: 1px solid var(--line); }
.ann-item:last-child { border-bottom: none; padding-bottom: 0; }
.ann-content { font-size: 12.5px; color: var(--text-2); line-height: 1.75; }
.tiny { font-size: 10.5px; }
.empty-line { padding: 10px 0; }

@media (max-width: 980px) {
  .hero-inner { flex-direction: column; }
  .hero-right { border-left: none; border-top: 1px solid var(--line); padding: 20px 0 0; flex-direction: row; justify-content: space-between; }
}
@media (max-width: 760px) {
  .hero-inner { padding: 20px; }
  .hero-count { gap: 14px; }
  .count-divider { display: none; }
  .hero-right { flex-direction: column; align-items: stretch; }
  .mini-stats { justify-content: space-between; }
  .attendance-wrap { flex-direction: column; align-items: flex-start; }
}
</style>
