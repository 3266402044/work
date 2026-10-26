<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { studentApi } from '@/api'
import { formatDateTime, recordStatusClass } from '@/utils/format'
import type { Course, SignRecordVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const list = ref<SignRecordVO[]>([])
const courses = ref<Course[]>([])
const total = ref(0)
const loading = ref(true)
const page = ref(1)
const size = ref(12)
const filters = reactive({
  status: 'ALL',
  courseId: undefined as number | undefined,
  range: [] as string[],
  keyword: typeof route.query.keyword === 'string' ? route.query.keyword : ''
})

const filtered = computed(() => {
  const key = filters.keyword.trim().toLowerCase()
  if (!key) return list.value
  return list.value.filter(
    (item) =>
      item.taskTitle?.toLowerCase().includes(key) ||
      item.courseName?.toLowerCase().includes(key) ||
      item.remark?.toLowerCase().includes(key)
  )
})

const load = async () => {
  loading.value = true
  try {
    const data = await studentApi.records({
      status: filters.status,
      courseId: filters.courseId,
      from: filters.range?.[0],
      to: filters.range?.[1],
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
  filters.status = 'ALL'
  filters.courseId = undefined
  filters.range = []
  filters.keyword = ''
  page.value = 1
  load()
}

onMounted(async () => {
  load()
  try {
    courses.value = await studentApi.courses()
  } catch {
    courses.value = []
  }
})
</script>

<template>
  <div class="page">
    <PageHeader title="我的签到" subtitle="按状态、课程与日期检索历史签到记录" eyebrow="ATTENDANCE LOG">
      <template #actions>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <section class="panel filters">
      <el-radio-group v-model="filters.status" size="small" @change="search">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="NORMAL">正常</el-radio-button>
        <el-radio-button value="LATE">迟到</el-radio-button>
        <el-radio-button value="LEAVE">请假</el-radio-button>
        <el-radio-button value="MANUAL">补签</el-radio-button>
      </el-radio-group>

      <div class="filter-row">
        <el-select v-model="filters.courseId" placeholder="全部课程" clearable style="width: 170px" @change="search">
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="search"
        />
        <el-input v-model="filters.keyword" placeholder="搜索任务或课程" clearable style="width: 200px" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button plain @click="reset">重置</el-button>
        <span class="muted total">共 {{ total }} 条记录</span>
      </div>
    </section>

    <section class="panel section-gap">
      <div v-if="loading" class="col" style="gap: 10px">
        <div v-for="i in 6" :key="i" class="skeleton" style="height: 52px" />
      </div>
      <EmptyState v-else-if="!filtered.length" title="暂无签到记录" description="完成一次签到后即可在此查看" icon="Tickets" />

      <template v-else>
        <el-table :data="filtered" class="hide-mobile" row-key="id">
          <el-table-column label="签到任务" min-width="220">
            <template #default="{ row }">
              <div class="task-cell">
                <span class="task-title">{{ row.taskTitle }}</span>
                <span class="task-sub">{{ row.courseName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="签到时间" width="170">
            <template #default="{ row }"><span class="mono">{{ formatDateTime(row.signTime) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusTag :status="row.status" :label="row.statusLabel" /></template>
          </el-table-column>
          <el-table-column label="方式" width="110">
            <template #default="{ row }"><span class="muted">{{ row.methodLabel }}</span></template>
          </el-table-column>
          <el-table-column label="距离" width="100">
            <template #default="{ row }">
              <span class="mono muted">{{ row.distanceMeters ? row.distanceMeters + ' m' : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="{ row }"><span class="muted">{{ row.remark || '—' }}</span></template>
          </el-table-column>
        </el-table>

        <div class="mobile-cards">
          <div v-for="row in filtered" :key="row.id" class="mobile-card" :class="recordStatusClass(row.status)">
            <div class="row-between">
              <span class="task-title">{{ row.taskTitle }}</span>
              <StatusTag :status="row.status" :label="row.statusLabel" />
            </div>
            <div class="task-sub">{{ row.courseName }}</div>
            <div class="mobile-meta">
              <span class="mono">{{ formatDateTime(row.signTime) }}</span>
              <span>{{ row.methodLabel }}</span>
              <span v-if="row.distanceMeters" class="mono">{{ row.distanceMeters }} m</span>
            </div>
            <div v-if="row.remark" class="mobile-remark">{{ row.remark }}</div>
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
.filters { display: flex; flex-direction: column; gap: 14px; }
.filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.total { margin-left: auto; font-size: 12px; }
.task-cell { display: flex; flex-direction: column; gap: 2px; }
.task-title { font-size: 13px; color: var(--text); }
.task-sub { font-size: 11px; color: var(--muted); }
.mobile-cards { display: none; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }

@media (max-width: 760px) {
  .hide-mobile { display: none !important; }
  .mobile-cards { display: flex; flex-direction: column; gap: 10px; }
  .mobile-card {
    border: 1px solid var(--line);
    border-left-width: 2px;
    border-radius: 8px;
    padding: 12px 14px;
    background: var(--panel-3);
  }
  .mobile-card.status-normal { border-left-color: var(--teal); }
  .mobile-card.status-warning { border-left-color: var(--amber); }
  .mobile-card.status-closed { border-left-color: var(--coral); }
  .mobile-card.status-not-started { border-left-color: var(--cyan); }
  .mobile-meta { display: flex; gap: 10px; flex-wrap: wrap; font-size: 11px; color: var(--muted); margin-top: 8px; }
  .mobile-remark { font-size: 11.5px; color: var(--text-2); margin-top: 6px; }
  .total { margin-left: 0; }
  .pager { justify-content: center; }
}
</style>
