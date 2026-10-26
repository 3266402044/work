<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { statisticsApi, teacherApi } from '@/api'
import { formatDateTime, formatTime } from '@/utils/format'
import type { ClassGroup, Course } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const courses = ref<Course[]>([])
const classes = ref<ClassGroup[]>([])
const data = ref<any>(null)
const loading = ref(false)
const coursesLoading = ref(true)
const error = ref('')

const filters = reactive({
  courseId: undefined as number | undefined,
  classId: undefined as number | undefined,
  range: [dayjs().subtract(30, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')] as string[]
})

const load = async () => {
  if (!filters.courseId) {
    data.value = null
    return
  }
  loading.value = true
  error.value = ''
  try {
    data.value = await statisticsApi.course({
      courseId: filters.courseId,
      classId: filters.classId,
      from: filters.range?.[0],
      to: filters.range?.[1]
    })
  } catch (e: any) {
    error.value = e?.message || '统计加载失败'
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  if (!filters.courseId) {
    classes.value = []
    return
  }
  classes.value = await teacherApi.classes({ courseId: filters.courseId })
  filters.classId = undefined
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
      data: trend.map((item: any) => item.label?.slice(5)),
      axisLine: { lineStyle: { color: 'rgba(148,163,184,.2)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8B9AB5', fontSize: 10 }
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      splitLine: { lineStyle: { color: 'rgba(148,163,184,.08)' } },
      axisLabel: { color: '#8B9AB5', fontSize: 10, formatter: '{value}%' }
    },
    series: [{
      type: 'bar',
      data: trend.map((item: any) => item.rate),
      barWidth: 14,
      itemStyle: {
        borderRadius: [3, 3, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#38DDF8' },
            { offset: 1, color: 'rgba(56,221,248,.25)' }
          ]
        }
      }
    }]
  }
})

watch(
  () => filters.courseId,
  async () => {
    await loadClasses()
    load()
  }
)
watch(() => filters.classId, load)
watch(() => filters.range, load)

onMounted(async () => {
  coursesLoading.value = true
  try {
    courses.value = await teacherApi.courses()
    if (courses.value.length) {
      filters.courseId = courses.value[0].id
    }
  } finally {
    coursesLoading.value = false
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="课程统计" subtitle="按课程、班级和日期范围查看到课率、迟到率与缺勤排名" eyebrow="COURSE ANALYTICS">
      <template #actions>
        <el-button :loading="loading" :disabled="!filters.courseId" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel filters">
      <div class="filter-row">
        <el-select v-model="filters.courseId" placeholder="选择课程" style="width: 190px" :loading="coursesLoading">
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-select v-model="filters.classId" placeholder="全部班级" clearable style="width: 160px">
          <el-option v-for="group in classes" :key="group.id" :label="group.name" :value="group.id" />
        </el-select>
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
        />
      </div>
    </section>

    <div v-if="!filters.courseId" class="panel section-gap">
      <EmptyState title="请选择课程" description="选择课程后即可查看到课趋势、任务明细与学生排名" icon="TrendCharts" />
    </div>

    <div v-else-if="loading" class="col section-gap" style="gap: 16px">
      <div class="grid grid-4">
        <div v-for="i in 4" :key="i" class="skeleton" style="height: 108px" />
      </div>
      <div class="skeleton" style="height: 280px" />
    </div>

    <div v-else-if="error" class="panel section-gap">
      <EmptyState title="统计加载失败" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重试</el-button></template>
      </EmptyState>
    </div>

    <template v-else-if="data">
      <div class="grid grid-4 section-gap">
        <StatCard label="任务总数" :value="data.summary.taskCount" suffix="个" icon="Tickets" accent="var(--cyan)" hint="所选日期范围内" />
        <StatCard label="到课率" :value="data.summary.attendanceRate" suffix="%" icon="CircleCheck" accent="var(--green)" :decimals="1" hint="签到人次 / 应到人次" />
        <StatCard label="迟到率" :value="data.summary.lateRate" suffix="%" icon="Clock" accent="var(--amber)" :decimals="1" hint="迟到记录占签到比例" />
        <StatCard label="缺勤人次" :value="data.summary.absentCount" suffix="人次" icon="UserFilled" accent="var(--coral)" :hint="`${data.summary.warningCount} 名学生需关注`" />
      </div>

      <div class="grid grid-main-side section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">任务到课率趋势</div>
            <span class="panel-sub">{{ data.course.name }}</span>
          </div>
          <ChartBox :option="trendOption" :empty="!data.trend.length" height="280px" />
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">出勤异常提醒</div>
            <span class="panel-sub">{{ data.warnings.length }} 人</span>
          </div>
          <div v-if="!data.warnings.length" class="muted" style="font-size: 12px">暂无需关注的学生</div>
          <div v-else class="warning-list">
            <div v-for="item in data.warnings.slice(0, 6)" :key="item.studentId" class="warning-item">
              <div class="w-avatar">{{ item.name?.slice(0, 1) }}</div>
              <div class="w-body">
                <div class="w-name">{{ item.name }} <span class="muted mono">{{ item.studentNo }}</span></div>
                <div class="w-reasons">
                  <span v-for="reason in item.reasons" :key="reason">{{ reason }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section class="panel section-gap">
        <div class="panel-head">
          <div class="panel-title">任务明细</div>
          <span class="panel-sub">{{ data.taskStats.length }} 个任务</span>
        </div>
        <el-table :data="data.taskStats" class="hide-mobile" max-height="380">
          <el-table-column label="任务" min-width="200">
            <template #default="{ row }"><span class="task-title">{{ row.title }}</span></template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.startTime) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column label="应到" width="70" prop="expected" />
          <el-table-column label="签到" width="70" prop="signed" />
          <el-table-column label="迟到" width="70" prop="late" />
          <el-table-column label="缺勤" width="70" prop="absent" />
          <el-table-column label="到课率" width="130">
            <template #default="{ row }">
              <div class="row" style="gap: 8px">
                <div class="bar-track" style="flex: 1"><div class="bar-fill" :style="{ width: row.rate + '%' }" /></div>
                <span class="mono">{{ row.rate }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="mobile-task-list">
          <div v-for="row in data.taskStats" :key="row.taskId" class="mobile-task">
            <div class="row-between">
              <span class="task-title">{{ row.title }}</span>
              <span class="mono cyan">{{ row.rate }}%</span>
            </div>
            <div class="muted mono" style="font-size: 11px; margin-top: 3px">{{ formatDateTime(row.startTime) }}</div>
            <div class="row" style="gap: 10px; margin-top: 6px; font-size: 11px; color: var(--muted)">
              <span>应到 {{ row.expected }}</span><span>签到 {{ row.signed }}</span><span>迟到 {{ row.late }}</span><span>缺勤 {{ row.absent }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel section-gap">
        <div class="panel-head">
          <div class="panel-title">学生出勤明细</div>
          <span class="panel-sub">按到课率升序排列，便于关注低出勤学生</span>
        </div>
        <el-table :data="data.studentStats" class="hide-mobile" max-height="460">
          <el-table-column label="学生" min-width="160">
            <template #default="{ row }">
              <div class="student-cell">
                <div class="s-avatar">{{ row.name?.slice(0, 1) }}</div>
                <div>
                  <div class="s-name">{{ row.name }}</div>
                  <div class="s-no mono">{{ row.studentNo }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="班级" min-width="150" prop="className" />
          <el-table-column label="正常" width="70" prop="normal" />
          <el-table-column label="迟到" width="70" prop="late" />
          <el-table-column label="请假" width="70" prop="leave" />
          <el-table-column label="缺勤" width="70">
            <template #default="{ row }"><span :class="{ coral: row.absent }">{{ row.absent }}</span></template>
          </el-table-column>
          <el-table-column label="到课率" width="150">
            <template #default="{ row }">
              <div class="row" style="gap: 8px">
                <div class="bar-track" style="flex: 1">
                  <div class="bar-fill" :style="{ width: row.rate + '%', background: row.rate < 60 ? 'var(--coral)' : row.rate < 80 ? 'var(--amber)' : undefined }" />
                </div>
                <span class="mono">{{ row.rate }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="迟到率" width="90">
            <template #default="{ row }"><span class="mono muted">{{ row.lateRate }}%</span></template>
          </el-table-column>
        </el-table>
        <div class="mobile-task-list">
          <div v-for="row in data.studentStats" :key="row.studentId" class="mobile-task">
            <div class="row-between">
              <span class="task-title">{{ row.name }} <span class="muted mono">{{ row.studentNo }}</span></span>
              <span class="mono" :class="row.rate < 60 ? 'coral' : row.rate < 80 ? 'amber' : 'green'">{{ row.rate }}%</span>
            </div>
            <div class="row" style="gap: 10px; margin-top: 6px; font-size: 11px; color: var(--muted)">
              <span>正常 {{ row.normal }}</span><span>迟到 {{ row.late }}</span><span>请假 {{ row.leave }}</span><span>缺勤 {{ row.absent }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.filters { padding: 16px 20px; }
.filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.task-title { font-size: 12.5px; color: var(--text-2); }
.student-cell { display: flex; align-items: center; gap: 9px; }
.s-avatar { width: 28px; height: 28px; border-radius: 8px; display: flex; align-items: center; justify-content: center; background: rgba(56,221,248,.08); color: var(--cyan); font-size: 11px; }
.s-name { font-size: 12px; color: var(--text-2); }
.s-no { font-size: 10px; color: var(--muted); }
.warning-list { display: flex; flex-direction: column; gap: 8px; }
.warning-item { display: flex; gap: 10px; align-items: flex-start; padding: 9px; border-radius: 7px; background: rgba(255,107,107,.035); border: 1px solid rgba(255,107,107,.16); }
.w-avatar { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: rgba(255,107,107,.12); color: var(--coral); font-size: 11px; flex-shrink: 0; }
.w-body { flex: 1; min-width: 0; }
.w-name { font-size: 12px; color: var(--text-2); }
.w-reasons { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 3px; }
.w-reasons span { font-size: 10px; color: var(--coral); }
.mobile-task-list { display: none; }
@media (max-width: 760px) {
  .hide-mobile { display: none !important; }
  .mobile-task-list { display: flex; flex-direction: column; gap: 8px; }
  .mobile-task { border: 1px solid var(--line); border-radius: 8px; padding: 10px 12px; background: var(--panel-3); }
}
</style>
