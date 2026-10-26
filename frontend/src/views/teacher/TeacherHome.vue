<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherApi } from '@/api'
import { onRealtime } from '@/composables/realtime'
import { formatDate, formatDateTime, formatTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const data = ref<any>(null)
const leaves = ref<any[]>([])
const loading = ref(true)
const leavesLoading = ref(true)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    data.value = await teacherApi.dashboard()
  } catch (e: any) {
    error.value = e?.message || '工作台加载失败'
  } finally {
    loading.value = false
  }
}

const loadLeaves = async () => {
  leavesLoading.value = true
  try {
    leaves.value = await teacherApi.leaves({ status: 'PENDING' })
  } finally {
    leavesLoading.value = false
  }
}

const refreshAll = () => {
  load()
  loadLeaves()
}

const review = async (id: number, approved: boolean) => {
  let remark = ''
  if (!approved) {
    try {
      const result = await ElMessageBox.prompt('请输入驳回原因（可选）', '驳回请假', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：证明材料不完整'
      })
      remark = result.value || ''
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm('确认通过该请假申请？系统将自动登记请假记录。', '通过请假', {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  await teacherApi.reviewLeave(id, approved, remark)
  ElMessage.success(approved ? '已通过请假申请' : '已驳回请假申请')
  refreshAll()
}

const trendOption = computed(() => {
  const trend = data.value?.trend || []
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1A2236',
      borderColor: 'rgba(148,163,184,.2)',
      textStyle: { color: '#E7EDF7', fontSize: 11 },
      formatter: (params: any[]) => {
        const item = trend[params[0]?.dataIndex ?? 0]
        return `${item?.label || ''}<br/>到课率 ${item?.rate ?? 0}%（${item?.signed ?? 0}/${item?.total ?? 0}）`
      }
    },
    grid: { left: 40, right: 16, top: 20, bottom: 34 },
    xAxis: {
      type: 'category',
      data: trend.map((item: any) => (item.label || '').slice(5, 10)),
      axisLine: { lineStyle: { color: 'rgba(148,163,184,.2)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8B9AB5', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      splitLine: { lineStyle: { color: 'rgba(148,163,184,.08)' } },
      axisLabel: { color: '#8B9AB5', fontSize: 10, formatter: '{value}%' }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: trend.map((item: any) => item.rate),
        lineStyle: { color: '#38DDF8', width: 2 },
        itemStyle: { color: '#38DDF8', borderColor: '#0B1020', borderWidth: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(56,221,248,.22)' },
              { offset: 1, color: 'rgba(56,221,248,0)' }
            ]
          }
        }
      }
    ]
  }
})

let offTask: (() => void) | null = null
let offSign: (() => void) | null = null
onMounted(() => {
  refreshAll()
  offTask = onRealtime('task:changed', load)
  offSign = onRealtime('sign:new', load)
})
</script>

<template>
  <div class="page">
    <PageHeader
      title="教师工作台"
      :subtitle="`今天是 ${formatDate(data?.date || new Date().toISOString())}，掌握每一堂课的到课与互动情况`"
      eyebrow="TEACHER CONSOLE"
    >
      <template #actions>
        <el-button @click="router.push('/teacher/rollcall')"><el-icon><Aim /></el-icon> 随机点名</el-button>
        <el-button type="primary" @click="router.push('/teacher/tasks/create')"><el-icon><Plus /></el-icon> 创建签到</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div class="grid grid-4">
        <div v-for="i in 4" :key="i" class="skeleton" style="height: 112px" />
      </div>
      <div class="skeleton" style="height: 300px" />
    </div>

    <div v-else-if="error" class="panel">
      <EmptyState title="工作台加载失败" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重试</el-button></template>
      </EmptyState>
    </div>

    <template v-else-if="data">
      <div class="grid grid-4">
        <StatCard label="今日课程" :value="data.metrics.todayCourses" suffix="门" icon="Reading" accent="var(--cyan)" hint="按授课关系统计" />
        <StatCard label="今日任务" :value="data.metrics.todayTasks" suffix="个" icon="Tickets" accent="var(--teal)" :hint="`进行中 ${data.metrics.ongoingTasks} 个`" />
        <StatCard label="待审批请假" :value="data.metrics.pendingLeaves" suffix="条" icon="Document" accent="var(--amber)" hint="需要及时处理" />
        <StatCard label="总体到课率" :value="data.metrics.attendanceRate" suffix="%" icon="TrendCharts" accent="var(--green)" :decimals="1" :hint="`${data.metrics.totalSigned}/${data.metrics.totalStudents} 人次`" />
      </div>

      <div class="grid grid-main-side section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">最近 7 次课程到课率</div>
            <span class="panel-sub">按任务创建时间排序</span>
          </div>
          <ChartBox :option="trendOption" :loading="false" :empty="!data.trend.length" height="268px" />
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">进行中的签到</div>
            <el-button text type="primary" @click="router.push('/teacher/tasks')">全部任务</el-button>
          </div>
          <div v-if="!data.ongoingTasks.length" class="empty-small">
            <EmptyState title="暂无进行中的签到" description="创建任务后将在这里实时显示到课进度" icon="Tickets" />
          </div>
          <div v-else class="ongoing-list">
            <button
              v-for="task in data.ongoingTasks"
              :key="task.id"
              type="button"
              class="ongoing-item"
              @click="router.push(`/teacher/tasks/${task.id}/monitor`)"
            >
              <div class="row-between">
                <div class="row" style="gap: 8px">
                  <i class="pulse-dot" />
                  <span class="ongoing-course">{{ task.courseName }}</span>
                </div>
                <StatusTag status="ONGOING" />
              </div>
              <div class="ongoing-title">{{ task.title }}</div>
              <div class="row-between" style="margin-top: 10px">
                <span class="muted mono">{{ task.signedCount }} / {{ task.totalStudents }} 人</span>
                <span class="mono cyan">{{ task.attendanceRate }}%</span>
              </div>
              <div class="bar-track" style="margin-top: 6px">
                <div class="bar-fill" :style="{ width: task.attendanceRate + '%' }" />
              </div>
              <div class="ongoing-foot">
                <span><el-icon><Clock /></el-icon> {{ formatTime(task.startTime) }} - {{ formatTime(task.endTime) }}</span>
                <span class="muted">{{ task.locationName || '未设置地点' }}</span>
              </div>
            </button>
          </div>
        </section>
      </div>

      <div class="grid grid-2 section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">今日课程</div>
            <span class="panel-sub">{{ data.courses.length }} 门</span>
          </div>
          <div v-if="!data.courses.length">
            <EmptyState title="暂无授课课程" description="请联系管理员配置课程与授课关系" icon="Reading" />
          </div>
          <div v-else class="course-list">
            <div v-for="course in data.courses" :key="course.id" class="course-item">
              <span class="course-color" :style="{ background: course.color || 'var(--cyan)' }" />
              <div class="course-body">
                <div class="course-name">{{ course.name }}</div>
                <div class="course-meta">
                  {{ course.classroom || '教室未设置' }} · {{ course.studentCount }} 名学生
                </div>
              </div>
              <div class="course-right">
                <span class="chip" :class="{ cyan: course.taskCountToday }">今日 {{ course.taskCountToday }} 个任务</span>
                <el-button text type="primary" size="small" @click="router.push({ path: '/teacher/tasks/create', query: { courseId: course.id } })">
                  发起签到
                </el-button>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">待审批请假</div>
            <span class="panel-sub">{{ leaves.length }} 条</span>
          </div>
          <div v-if="leavesLoading" class="col" style="gap: 10px">
            <div v-for="i in 3" :key="i" class="skeleton" style="height: 62px" />
          </div>
          <div v-else-if="!leaves.length">
            <EmptyState title="没有待审批的请假" description="新的申请会实时出现在这里" icon="CircleCheck" />
          </div>
          <div v-else class="leave-list">
            <div v-for="leave in leaves.slice(0, 4)" :key="leave.id" class="leave-item">
              <div class="row-between">
                <div class="row" style="gap: 8px">
                  <span class="leave-name">{{ leave.studentName }}</span>
                  <span class="muted mono">{{ leave.studentNo }}</span>
                </div>
                <StatusTag status="PENDING" />
              </div>
              <div class="leave-task">{{ leave.taskTitle }}</div>
              <div class="leave-reason">{{ leave.reason }}</div>
              <div class="row-between" style="margin-top: 8px">
                <span class="muted mono">{{ formatDateTime(leave.createTime) }}</span>
                <div class="row" style="gap: 6px">
                  <el-button size="small" @click="review(leave.id, false)">驳回</el-button>
                  <el-button size="small" type="primary" @click="review(leave.id, true)">通过</el-button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section class="panel section-gap">
        <div class="panel-head">
          <div class="panel-title">出勤异常提醒</div>
          <span class="panel-sub">连续未签到或迟到率偏高的学生 · 仅提示不处罚</span>
        </div>
        <div v-if="!data.warnings.length">
          <EmptyState title="暂无需关注的学生" description="系统会持续监测连续缺勤与迟到率" icon="CircleCheck" />
        </div>
        <div v-else class="warning-list">
          <div v-for="item in data.warnings" :key="item.studentId" class="warning-item">
            <div class="warning-avatar">{{ item.name?.slice(0, 1) }}</div>
            <div class="warning-body">
              <div class="warning-name">{{ item.name }} <span class="muted mono">{{ item.studentNo }}</span></div>
              <div class="warning-class">{{ item.className }}</div>
            </div>
            <div class="warning-reasons">
              <span v-for="reason in item.reasons" :key="reason" class="chip coral">{{ reason }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.ongoing-list { display: flex; flex-direction: column; gap: 10px; }
.ongoing-item {
  width: 100%;
  text-align: left;
  padding: 13px 14px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--panel-3);
  color: inherit;
  cursor: pointer;
  transition: border-color 0.16s ease;
}
.ongoing-item:hover { border-color: rgba(71, 209, 108, 0.4); }
.ongoing-course { font-size: 12px; color: var(--green); }
.ongoing-title { font-size: 13.5px; color: var(--text); margin-top: 5px; }
.ongoing-foot { display: flex; justify-content: space-between; gap: 10px; font-size: 10.5px; color: var(--muted); margin-top: 9px; }
.ongoing-foot span { display: inline-flex; align-items: center; gap: 4px; }
.empty-small :deep(.empty-state) { padding: 20px 10px; }

.course-list { display: flex; flex-direction: column; }
.course-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.course-item:last-child { border-bottom: none; }
.course-color { width: 3px; height: 30px; border-radius: 3px; flex-shrink: 0; }
.course-body { flex: 1; min-width: 0; }
.course-name { font-size: 13px; color: var(--text-2); }
.course-meta { font-size: 11px; color: var(--muted); margin-top: 2px; }
.course-right { display: flex; flex-direction: column; align-items: flex-end; gap: 3px; }

.leave-list { display: flex; flex-direction: column; gap: 10px; }
.leave-item { border: 1px solid var(--line); border-radius: 8px; padding: 11px 12px; background: var(--panel-3); }
.leave-name { font-size: 13px; color: var(--text); }
.leave-task { font-size: 11.5px; color: var(--text-2); margin-top: 4px; }
.leave-reason { font-size: 11.5px; color: var(--muted); margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.warning-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.warning-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 8px; border: 1px solid rgba(255, 107, 107, 0.2); background: rgba(255, 107, 107, 0.035); }
.warning-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 107, 107, 0.12); color: var(--coral); font-size: 14px; font-weight: 600;
}
.warning-body { flex: 1; min-width: 0; }
.warning-name { font-size: 12.5px; color: var(--text); }
.warning-class { font-size: 10.5px; color: var(--muted); }
.warning-reasons { display: flex; flex-wrap: wrap; gap: 6px; justify-content: flex-end; }

@media (max-width: 900px) {
  .warning-list { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .course-right { flex-direction: row; align-items: center; }
}
</style>
