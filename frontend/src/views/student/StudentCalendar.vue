<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { studentApi } from '@/api'
import { formatTime, weekStartOf } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const weekStart = ref(weekStartOf(dayjs()))
const events = ref<any[]>([])
const loading = ref(true)
const error = ref('')

const days = computed(() =>
  Array.from({ length: 7 }, (_, index) => {
    const date = weekStart.value.add(index, 'day')
    return {
      date,
      key: date.format('YYYY-MM-DD'),
      label: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][index],
      day: date.format('DD'),
      isToday: date.isSame(dayjs(), 'day'),
      events: events.value.filter((event) => dayjs(event.start).isSame(date, 'day'))
    }
  })
)

const rangeText = computed(
  () => `${weekStart.value.format('YYYY年MM月DD日')} - ${weekStart.value.add(6, 'day').format('MM月DD日')}`
)

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    events.value = await studentApi.calendar({ weekStart: weekStart.value.format('YYYY-MM-DD') })
  } catch (e: any) {
    error.value = e?.message || '日历加载失败'
  } finally {
    loading.value = false
  }
}

const shift = (offset: number) => {
  weekStart.value = weekStart.value.add(offset, 'week')
}

const goToday = () => {
  weekStart.value = weekStartOf(dayjs())
}

watch(weekStart, load)
onMounted(load)

const typeMeta = (type: string) => {
  switch (type) {
    case 'TASK': return { label: '签到', color: 'var(--cyan)', icon: 'Tickets' }
    case 'QUIZ': return { label: '小测', color: 'var(--amber)', icon: 'EditPen' }
    case 'ANNOUNCEMENT': return { label: '公告', color: 'var(--teal)', icon: 'Bell' }
    default: return { label: '事件', color: 'var(--muted)', icon: 'Calendar' }
  }
}

const openEvent = (event: any) => {
  if (event.type === 'TASK') router.push(event.link)
  else if (event.type === 'QUIZ') router.push('/student/home')
}
</script>

<template>
  <div class="page">
    <PageHeader title="课程日历" subtitle="按周查看课程签到、课堂小测与公告安排" eyebrow="WEEKLY CALENDAR">
      <template #actions>
        <el-button-group>
          <el-button @click="shift(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
          <el-button @click="goToday">本周</el-button>
          <el-button @click="shift(1)"><el-icon><ArrowRight /></el-icon></el-button>
        </el-button-group>
        <span class="range mono">{{ rangeText }}</span>
      </template>
    </PageHeader>

    <section class="panel calendar-panel">
      <div v-if="loading" class="grid days">
        <div v-for="i in 7" :key="i" class="skeleton" style="height: 240px" />
      </div>
      <EmptyState v-else-if="error" title="日历加载失败" :description="error" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重试</el-button></template>
      </EmptyState>
      <div v-else class="days">
        <div v-for="day in days" :key="day.key" class="day-col" :class="{ today: day.isToday }">
          <div class="day-head">
            <span class="day-label">{{ day.label }}</span>
            <span class="day-num mono" :class="{ active: day.isToday }">{{ day.day }}</span>
          </div>
          <div class="day-body">
            <button
              v-for="event in day.events"
              :key="event.id"
              type="button"
              class="event"
              :style="{ '--ev-color': typeMeta(event.type).color }"
              @click="openEvent(event)"
            >
              <div class="event-time mono">{{ formatTime(event.start) }}</div>
              <div class="event-title">{{ event.title }}</div>
              <div class="event-meta">
                <span class="chip" :style="{ color: typeMeta(event.type).color }">{{ typeMeta(event.type).label }}</span>
                <span class="muted">{{ event.courseName }}</span>
              </div>
            </button>
            <div v-if="!day.events.length" class="no-event">—</div>
          </div>
        </div>
      </div>
    </section>

    <div class="legend">
      <span><i class="dot" style="color: var(--cyan)" />签到任务</span>
      <span><i class="dot" style="color: var(--amber)" />课堂小测</span>
      <span><i class="dot" style="color: var(--teal)" />课堂公告</span>
      <span class="muted">点击日历项可进入对应详情</span>
    </div>
  </div>
</template>

<style scoped>
.range { font-size: 12px; color: var(--text-2); }
.calendar-panel { padding: 16px; overflow-x: auto; }
.days { display: grid; grid-template-columns: repeat(7, minmax(140px, 1fr)); gap: 10px; min-width: 980px; }
.day-col {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(16, 23, 40, 0.6);
  min-height: 260px;
  overflow: hidden;
}
.day-col.today { border-color: rgba(56, 221, 248, 0.4); background: rgba(56, 221, 248, 0.04); }
.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
}
.day-label { font-size: 11px; color: var(--muted); }
.day-num { font-size: 15px; color: var(--text-2); }
.day-num.active { color: var(--cyan); font-weight: 600; }
.day-body { padding: 8px; display: flex; flex-direction: column; gap: 8px; }
.event {
  text-align: left;
  border: 1px solid var(--line);
  border-left: 2px solid var(--ev-color);
  border-radius: 6px;
  background: var(--panel-3);
  padding: 8px 9px;
  color: inherit;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease;
}
.event:hover { background: rgba(56, 221, 248, 0.06); border-color: var(--line-strong); border-left-color: var(--ev-color); }
.event-time { font-size: 10.5px; color: var(--muted); }
.event-title { font-size: 12px; color: var(--text-2); line-height: 1.45; margin: 3px 0 5px; }
.event-meta { display: flex; flex-direction: column; gap: 3px; font-size: 10.5px; }
.event-meta .chip { height: 18px; padding: 0 6px; font-size: 10px; align-self: flex-start; }
.no-event { color: var(--muted); font-size: 11px; text-align: center; padding: 20px 0; opacity: 0.5; }
.legend { display: flex; gap: 18px; flex-wrap: wrap; margin-top: 14px; font-size: 11px; color: var(--text-2); }
.legend span { display: inline-flex; align-items: center; gap: 6px; }

@media (max-width: 760px) {
  .calendar-panel { overflow: visible; }
  .days { grid-template-columns: 1fr; min-width: 0; }
  .day-col { min-height: auto; }
  .day-body { flex-direction: row; flex-wrap: wrap; }
  .event { flex: 1 1 160px; }
}
</style>
