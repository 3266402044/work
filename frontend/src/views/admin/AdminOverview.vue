<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import ChartBox from '@/components/ChartBox.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const data = ref<any>(null)
const loading = ref(true)
const resetting = ref(false)

const load = async () => {
  loading.value = true
  try {
    data.value = await adminApi.overview()
  } finally {
    loading.value = false
  }
}

const trendOption = computed(() => {
  const trend = data.value?.trend || []
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1A2236',
      borderColor: 'rgba(148,163,184,.2)',
      textStyle: { color: '#E7EDF7', fontSize: 11 }
    },
    legend: { show: true, textStyle: { color: '#8B9AB5', fontSize: 10 }, itemWidth: 10, itemHeight: 6, top: 0 },
    grid: { left: 40, right: 40, top: 34, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trend.map((item: any) => item.label),
      axisLine: { lineStyle: { color: 'rgba(148,163,184,.2)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8B9AB5', fontSize: 10 }
    },
    yAxis: [
      {
        type: 'value', min: 0, max: 100,
        splitLine: { lineStyle: { color: 'rgba(148,163,184,.08)' } },
        axisLabel: { color: '#8B9AB5', fontSize: 10, formatter: '{value}%' }
      },
      {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { color: '#8B9AB5', fontSize: 10 }
      }
    ],
    series: [
      {
        name: '出勤率', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        data: trend.map((item: any) => item.rate),
        lineStyle: { color: '#38DDF8', width: 2 },
        itemStyle: { color: '#38DDF8' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(56,221,248,.2)' },
              { offset: 1, color: 'rgba(56,221,248,0)' }
            ]
          }
        }
      },
      {
        name: '签到人次', type: 'bar', yAxisIndex: 1, barWidth: 8,
        data: trend.map((item: any) => item.signed),
        itemStyle: { color: 'rgba(45,212,191,.45)', borderRadius: [2, 2, 0, 0] }
      }
    ]
  }
})

const resetDemo = async () => {
  try {
    await ElMessageBox.confirm(
      '重置会清空当前所有业务数据并重新生成演示数据（账号、课程、任务、签到与互动记录）。确认继续？',
      '重置演示数据',
      { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'error' }
    )
  } catch {
    return
  }
  resetting.value = true
  try {
    await adminApi.resetDemo()
    ElMessage.success('演示数据已重置')
    await load()
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="运营总览" subtitle="全校签到、出勤趋势与班级排行，异常情况一目了然" eyebrow="ADMIN OVERVIEW">
      <template #actions>
        <el-button @click="router.push('/admin/data')">基础数据</el-button>
        <el-button @click="adminApi.exportInteractions()">导出请假记录</el-button>
        <el-button type="danger" plain :loading="resetting" @click="resetDemo">重置演示数据</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div class="grid grid-4">
        <div v-for="i in 4" :key="i" class="skeleton" style="height: 112px" />
      </div>
      <div class="skeleton" style="height: 320px" />
    </div>

    <template v-else-if="data">
      <div class="grid grid-4">
        <StatCard label="今日活跃课程" :value="data.metrics.activeCourses" suffix="门" icon="Reading" accent="var(--cyan)" hint="今日有签到任务的课程" />
        <StatCard label="今日任务" :value="data.metrics.todayTasks" suffix="个" icon="Tickets" accent="var(--teal)" :hint="`进行中 ${data.metrics.ongoingTasks} 个`" />
        <StatCard label="今日签到人次" :value="data.metrics.todaySigns" suffix="人次" icon="CircleCheck" accent="var(--green)" hint="服务端实时统计" />
        <StatCard label="全校出勤率" :value="data.metrics.attendanceRate" suffix="%" icon="TrendCharts" accent="var(--amber)" :decimals="1" hint="今日签到 / 应到" />
      </div>

      <div class="grid grid-main-side section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">近 14 天出勤趋势</div>
            <span class="panel-sub">出勤率与签到人次</span>
          </div>
          <ChartBox :option="trendOption" :empty="!data.trend.length" height="300px" />
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">班级到课排行</div>
            <span class="panel-sub">近 7 天</span>
          </div>
          <div v-if="!data.classRanking.length">
            <EmptyState title="暂无班级数据" icon="DataAnalysis" />
          </div>
          <div v-else class="rank-list">
            <div v-for="(item, index) in data.classRanking" :key="item.classId" class="rank-item">
              <div class="rank-head">
                <span class="rank-index mono" :class="{ top: index < 3 }">{{ index + 1 }}</span>
                <span class="rank-name">{{ item.className }}</span>
                <span class="rank-rate mono">{{ item.rate }}%</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: item.rate + '%', background: item.rate < 70 ? 'var(--coral)' : index < 3 ? 'var(--green)' : undefined }" />
              </div>
              <div class="rank-meta">{{ item.signed }} / {{ item.expected }} 人次</div>
            </div>
          </div>
        </section>
      </div>

      <div class="grid grid-2 section-gap">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">异常与提醒</div>
            <span class="panel-sub">{{ data.anomalies.length }} 条</span>
          </div>
          <div v-if="!data.anomalies.length">
            <EmptyState title="暂无异常" description="系统运行正常" icon="CircleCheck" />
          </div>
          <div v-else class="anomaly-list">
            <div v-for="(item, index) in data.anomalies" :key="index" class="anomaly-item" :class="item.level.toLowerCase()">
              <span class="anomaly-level">
                {{ item.level === 'WARN' ? '预警' : item.level === 'OK' ? '进行中' : '提示' }}
              </span>
              <div class="anomaly-body">
                <div class="anomaly-title">{{ item.title }}</div>
                <div class="anomaly-detail">{{ item.detail }}</div>
              </div>
              <el-button v-if="item.type === 'ONGOING'" text type="primary" size="small" @click="router.push('/teacher/tasks')">查看</el-button>
              <el-button v-else-if="item.type === 'LEAVE'" text type="primary" size="small" @click="router.push('/teacher/home')">处理</el-button>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">系统概况</div>
            <span class="panel-sub">数据实时来自后端</span>
          </div>
          <div class="system-grid">
            <div class="system-item">
              <span class="label">学生总数</span>
              <strong class="mono">{{ data.metrics.totalStudents }}</strong>
            </div>
            <div class="system-item">
              <span class="label">教师总数</span>
              <strong class="mono">{{ data.metrics.totalTeachers }}</strong>
            </div>
            <div class="system-item">
              <span class="label">课程总数</span>
              <strong class="mono">{{ data.metrics.totalCourses }}</strong>
            </div>
            <div class="system-item">
              <span class="label">进行中任务</span>
              <strong class="mono green">{{ data.metrics.ongoingTasks }}</strong>
            </div>
          </div>
          <div class="divider" />
          <div class="quick-actions">
            <div class="label" style="margin-bottom: 8px">快捷操作</div>
            <div class="row wrap" style="gap: 8px">
              <el-button size="small" @click="router.push('/admin/data')">维护基础数据</el-button>
              <el-button size="small" @click="router.push('/admin/logs')">审计日志</el-button>
              <el-button size="small" @click="router.push('/admin/settings')">签到与点名规则</el-button>
              <el-button size="small" @click="router.push('/teacher/tasks/create')">创建签到任务</el-button>
            </div>
          </div>
          <div class="divider" />
          <div class="row-between" style="font-size: 11px; color: var(--muted)">
            <span class="row" style="gap: 6px"><i class="pulse-dot" /> 服务运行中</span>
            <span class="mono">ClassPulse v1.0</span>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.rank-list { display: flex; flex-direction: column; gap: 13px; }
.rank-head { display: flex; align-items: center; gap: 9px; margin-bottom: 5px; }
.rank-index { width: 18px; height: 18px; border-radius: 5px; display: flex; align-items: center; justify-content: center; font-size: 10px; background: rgba(148,163,184,.08); color: var(--muted); }
.rank-index.top { background: rgba(245,185,66,.12); color: var(--amber); }
.rank-name { flex: 1; font-size: 12px; color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-rate { font-size: 12px; color: var(--text); }
.rank-meta { font-size: 10px; color: var(--muted); margin-top: 3px; text-align: right; }

.anomaly-list { display: flex; flex-direction: column; gap: 8px; }
.anomaly-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--line); background: var(--panel-3); }
.anomaly-item.warn { border-color: rgba(245,185,66,.25); background: rgba(245,185,66,.04); }
.anomaly-item.ok { border-color: rgba(71,209,108,.25); background: rgba(71,209,108,.04); }
.anomaly-level { font-size: 10px; padding: 1px 7px; border-radius: 4px; color: var(--amber); background: rgba(245,185,66,.1); flex-shrink: 0; }
.anomaly-item.ok .anomaly-level { color: var(--green); background: rgba(71,209,108,.1); }
.anomaly-item.info .anomaly-level { color: var(--cyan); background: rgba(56,221,248,.1); }
.anomaly-body { flex: 1; min-width: 0; }
.anomaly-title { font-size: 12px; color: var(--text-2); }
.anomaly-detail { font-size: 10.5px; color: var(--muted); margin-top: 2px; }

.system-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.system-item { padding: 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--panel-3); display: flex; flex-direction: column; gap: 4px; }
.system-item strong { font-size: 22px; font-weight: 600; }
</style>
