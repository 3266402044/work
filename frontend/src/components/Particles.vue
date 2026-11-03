<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ count?: number; color?: string }>(), {
  count: 24,
  color: 'var(--cyan)'
})

// 挂载时生成一次随机粒子参数；纯 CSS 动画，零持续 JS 开销
const particles = computed(() =>
  Array.from({ length: props.count }, () => ({
    left: Math.random() * 100,
    size: 2 + Math.random() * 4,
    duration: 9 + Math.random() * 12,
    delay: -Math.random() * 20,
    drift: (Math.random() - 0.5) * 70,
    peak: 0.3 + Math.random() * 0.5
  }))
)
</script>

<template>
  <div class="particles" aria-hidden="true">
    <span
      v-for="(p, i) in particles"
      :key="i"
      :style="{
        left: p.left + '%',
        width: p.size + 'px',
        height: p.size + 'px',
        background: color,
        boxShadow: `0 0 ${Math.round(p.size * 2)}px ${color}`,
        animationDuration: p.duration + 's',
        animationDelay: p.delay + 's',
        '--drift': p.drift + 'px',
        '--po': p.peak
      }"
    />
  </div>
</template>

<style scoped>
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
.particles span {
  position: absolute;
  bottom: -8vh;
  border-radius: 50%;
  animation: particle-rise linear infinite;
}
@keyframes particle-rise {
  0% { transform: translate3d(0, 0, 0) scale(1); opacity: 0; }
  8% { opacity: var(--po, 0.55); }
  60% { opacity: var(--po, 0.55); }
  100% { transform: translate3d(var(--drift, 0px), -112vh, 0) scale(0.3); opacity: 0; }
}
@media (prefers-reduced-motion: reduce) {
  .particles span { animation: none; opacity: 0; }
}
</style>
