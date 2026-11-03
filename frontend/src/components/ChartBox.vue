<script setup lang="ts">
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import EmptyState from './EmptyState.vue'

const props = defineProps<{
  option: Record<string, any>
  loading?: boolean
  empty?: boolean
  error?: string
  height?: string
}>()

const el = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
let observer: ResizeObserver | null = null

const init = () => {
  if (!el.value) return
  chart = echarts.init(el.value, undefined, { renderer: 'canvas' })
  chart.setOption({
    animation: true,
    animationDuration: 600,
    animationEasing: 'cubicOut',
    ...(props.option || {})
  })
}

const resize = () => chart?.resize()

onMounted(() => {
  init()
  if (el.value) {
    observer = new ResizeObserver(resize)
    observer.observe(el.value)
  }
})

watch(
  () => props.option,
  (option) => {
    chart?.setOption(option || {}, true)
  },
  { deep: true }
)

watch(
  () => [props.loading, props.empty, props.error],
  async () => {
    await nextTick()
    resize()
  }
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="chart-wrap" :style="{ height: height || '260px' }">
    <div v-if="loading" class="skeleton chart-skeleton" />
    <EmptyState
      v-else-if="error"
      title="图表加载失败"
      :description="error"
      icon="WarningFilled"
    />
    <EmptyState v-else-if="empty" title="暂无数据" description="数据积累后将在此展示趋势" icon="DataLine" />
    <div v-show="!loading && !empty && !error" ref="el" class="chart-box" :style="{ height: height || '260px' }" />
  </div>
</template>

<style scoped>
.chart-wrap { width: 100%; position: relative; }
.chart-skeleton { width: 100%; height: 100%; }
.chart-box { width: 100%; }
</style>
