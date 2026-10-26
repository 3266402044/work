<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    label?: string
    sublabel?: string
    size?: number
    stroke?: number
    color?: string
    track?: string
  }>(),
  { size: 132, stroke: 9, color: 'var(--cyan)', track: 'rgba(148,163,184,.14)' }
)

const radius = 54
const circumference = 2 * Math.PI * radius
const offset = computed(() => circumference * (1 - Math.min(100, Math.max(0, props.value)) / 100))
</script>

<template>
  <div class="ring-wrap" :style="{ width: size + 'px', height: size + 'px' }">
    <svg viewBox="0 0 120 120">
      <circle cx="60" cy="60" :r="radius" fill="none" :stroke="track" :stroke-width="stroke" />
      <circle
        cx="60"
        cy="60"
        :r="radius"
        fill="none"
        :stroke="color"
        :stroke-width="stroke"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
        transform="rotate(-90 60 60)"
        class="progress-ring"
      />
    </svg>
    <div class="center">
      <strong class="mono">{{ Math.round(value) }}<span class="pct">%</span></strong>
      <span v-if="label" class="label">{{ label }}</span>
      <span v-if="sublabel" class="sub">{{ sublabel }}</span>
    </div>
  </div>
</template>

<style scoped>
.ring-wrap { position: relative; display: inline-flex; align-items: center; justify-content: center; }
svg { width: 100%; height: 100%; }
.progress-ring { transition: stroke-dashoffset 0.7s cubic-bezier(0.3, 0.8, 0.3, 1); }
.center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
strong { font-size: 24px; font-weight: 650; letter-spacing: -0.02em; }
.pct { font-size: 12px; color: var(--muted); margin-left: 1px; }
.label { font-size: 11px; color: var(--muted); }
.sub { font-size: 10px; color: var(--muted); opacity: 0.8; }
</style>
