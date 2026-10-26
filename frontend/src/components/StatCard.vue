<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

const props = defineProps<{
  value: number
  label: string
  accent?: string
  icon?: string
  suffix?: string
  hint?: string
  trend?: string
  trendType?: 'up' | 'down' | 'flat'
  decimals?: number
}>()

const display = ref(0)

const animate = () => {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  // 后台标签页不会触发 requestAnimationFrame，直接落地数值避免出现 0
  if (reduce || document.hidden) {
    display.value = props.value
    return
  }
  const from = display.value
  const to = props.value
  const start = performance.now()
  const duration = 350
  const tick = (now: number) => {
    const progress = Math.min(1, (now - start) / duration)
    const eased = 1 - Math.pow(1 - progress, 3)
    display.value = from + (to - from) * eased
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

onMounted(animate)
watch(() => props.value, animate)

const shown = () => {
  const decimals = props.decimals ?? 0
  return Number(display.value).toFixed(decimals)
}
</script>

<template>
  <div class="stat-card panel">
    <div class="top">
      <span class="label">{{ label }}</span>
      <span v-if="icon" class="icon" :style="{ color: accent || 'var(--cyan)' }">
        <el-icon><component :is="icon" /></el-icon>
      </span>
    </div>
    <div class="value-row">
      <span class="big-num" :style="{ color: accent || 'var(--text)' }">{{ shown() }}</span>
      <span v-if="suffix" class="suffix">{{ suffix }}</span>
    </div>
    <div class="bottom">
      <span v-if="trend" class="trend" :class="trendType || 'flat'">{{ trend }}</span>
      <span class="hint">{{ hint }}</span>
    </div>
    <div class="glow" :style="{ background: accent || 'var(--cyan)' }" />
  </div>
</template>

<style scoped>
.stat-card {
  padding: 16px 18px;
  overflow: hidden;
  min-height: 112px;
}
.top { display: flex; align-items: center; justify-content: space-between; }
.icon { font-size: 18px; opacity: 0.85; }
.value-row { display: flex; align-items: baseline; gap: 6px; margin-top: 8px; }
.suffix { color: var(--muted); font-size: 12px; }
.bottom { display: flex; align-items: center; gap: 8px; margin-top: 8px; font-size: 12px; color: var(--muted); }
.trend { font-family: var(--mono); }
.trend.up { color: var(--green); }
.trend.down { color: var(--coral); }
.trend.flat { color: var(--muted); }
.hint { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.glow {
  position: absolute;
  left: 18px;
  bottom: 0;
  width: 38px;
  height: 2px;
  opacity: 0.75;
  border-radius: 2px;
}
</style>
