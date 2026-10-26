<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, teacherApi } from '@/api'
import { formatDateTime, formatTime } from '@/utils/format'
import type { ClassGroup, Course, TaskVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const route = useRoute()
const courses = ref<Course[]>([])
const classes = ref<ClassGroup[]>([])
const list = ref<TaskVO[]>([])
const total = ref(0)
const loading = ref(true)
const page = ref(1)
const size = ref(10)

const filters = reactive({
  courseId: undefined as number | undefined,
  classId: undefined as number | undefined,
  date: '' as string,
  scope: 'ALL',
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : ''
})

const load = async () => {
  loading.value = true
  try {
    const data = await teacherApi.tasks({
      courseId: filters.courseId,
      classId: filters.classId,
      date: filters.date || undefined,
      scope: filters.scope,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: size.value
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const reset = () => {
  filters.courseId = undefined
  filters.classId = undefined
  filters.date = ''
  filters.scope = 'ALL'
  filters.keyword = ''
  search()
}

const closeTask = async (task: TaskVO) => {
  try {
    await ElMessageBox.confirm(
      `关闭后「${task.title}」将立即停止签到，操作不可撤销。确认关闭？`,
      '提前关闭签到',
      { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await taskApi.close(task.id)
  ElMessage.success('已提前关闭')
  load()
}

const exportTask = async (task: TaskVO) => {
  await taskApi.export(task.id)
  ElMessage.success('导出已开始')
}

onMounted(async () => {
  load()
  try {
    courses.value = await teacherApi.courses()
    classes.value = await teacherApi.classes()
  } catch {
    courses.value = []
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="签到任务" subtitle="按课程、班级、日期和状态检索历史任务，可复制或提前关闭" eyebrow="TASK HISTORY">
      <template #actions>
        <el-button type="primary" @click="router.push('/teacher/tasks/create')"><el-icon><Plus /></el-icon> 创建签到</el-button>
      </template>
    </PageHeader>

    <section class="panel filters">
      <div class="filter-row">
        <el-select v-model="filters.courseId" placeholder="全部课程" clearable style="width: 170px" @change="search">
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-select v-model="filters.classId" placeholder="全部班级" clearable style="width: 160px" @change="search">
          <el-option v-for="group in classes" :key="group.id" :label="group.name" :value="group.id" />
        </el-select>
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 160px" @change="search" />
        <el-radio-group v-model="filters.scope" size="small" @change="search">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="ONGOING">进行中</el-radio-button>
          <el-radio-button value="NOT_STARTED">未开始</el-radio-button>
          <el-radio-button value="ENDED">已结束</el-radio-button>
          <el-radio-button value="CLOSED">已关闭</el-radio-button>
        </el-radio-group>
        <el-input v-model="filters.keyword" placeholder="搜索任务标题" clearable style="width: 190px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button plain @click="reset">重置</el-button>
        <span class="muted total">共 {{ total }} 个任务</span>
      </div>
    </section>

    <section class="panel section-gap">
      <div v-if="loading" class="col" style="gap: 10px">
        <div v-for="i in 6" :key="i" class="skeleton" style="height: 54px" />
      </div>
      <EmptyState v-else-if="!list.length" title="暂无签到任务" description="创建一个签到任务，开始第一堂课" icon="Tickets">
        <template #action>
          <el-button type="primary" plain @click="router.push('/teacher/tasks/create')">创建签到</el-button>
        </template>
      </EmptyState>

      <template v-else>
        <el-table :data="list" class="hide-mobile" row-key="id">
          <el-table-column label="任务" min-width="220">
            <template #default="{ row }">
              <div class="task-cell">
                <span class="task-title">{{ row.title }}</span>
                <span class="task-sub">{{ row.courseName }} · {{ row.className }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="签到时间" width="180">
            <template #default="{ row }">
              <div class="col" style="gap: 0">
                <span class="mono" style="font-size: 12px">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</span>
                <span class="muted mono" style="font-size: 10px">{{ formatDateTime(row.startTime).slice(0, 10) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><StatusTag :status="row.taskStatus" /></template>
          </el-table-column>
          <el-table-column label="到课" width="150">
            <template #default="{ row }">
              <div class="col" style="gap: 4px">
                <span class="mono" style="font-size: 11px">{{ row.signedCount }}/{{ row.totalStudents }} · {{ row.attendanceRate }}%</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: row.attendanceRate + '%' }" /></div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="router.push(`/teacher/tasks/${row.id}/monitor`)">监控</el-button>
              <el-button text size="small" @click="router.push({ path: '/teacher/tasks/create', query: { copy: row.id } })">复制</el-button>
              <el-button text size="small" @click="exportTask(row)">导出</el-button>
              <el-button
                v-if="row.taskStatus === 'ONGOING' || row.taskStatus === 'NOT_STARTED'"
                text
                type="danger"
                size="small"
                @click="closeTask(row)"
              >关闭</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mobile-list">
          <div v-for="row in list" :key="row.id" class="task-card">
            <div class="row-between">
              <span class="task-title">{{ row.title }}</span>
              <StatusTag :status="row.taskStatus" />
            </div>
            <div class="task-sub">{{ row.courseName }} · {{ row.className }}</div>
            <div class="task-meta mono">{{ formatDateTime(row.startTime) }} - {{ formatTime(row.endTime) }}</div>
            <div class="row-between" style="margin-top: 8px">
              <span class="mono muted" style="font-size: 11px">{{ row.signedCount }}/{{ row.totalStudents }} 人 · {{ row.attendanceRate }}%</span>
              <div class="row" style="gap: 4px">
                <el-button size="small" type="primary" plain @click="router.push(`/teacher/tasks/${row.id}/monitor`)">监控</el-button>
                <el-button size="small" @click="router.push({ path: '/teacher/tasks/create', query: { copy: row.id } })">复制</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="load"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.filters { padding: 16px 20px; }
.filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.total { margin-left: auto; font-size: 12px; }
.task-cell { display: flex; flex-direction: column; gap: 2px; }
.task-title { font-size: 13px; color: var(--text); }
.task-sub { font-size: 11px; color: var(--muted); }
.task-meta { font-size: 11px; color: var(--muted); margin-top: 3px; }
.mobile-list { display: none; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }

@media (max-width: 760px) {
  .hide-mobile { display: none !important; }
  .mobile-list { display: flex; flex-direction: column; gap: 10px; }
  .task-card { border: 1px solid var(--line); border-radius: 8px; padding: 12px 14px; background: var(--panel-3); }
  .total { margin-left: 0; }
  .pager { justify-content: center; }
}
</style>
