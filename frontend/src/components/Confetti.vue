<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(defineProps<{ count?: number; duration?: number }>(), {
  count: 140,
  duration: 2600
})

const canvas = ref<HTMLCanvasElement | null>(null)
let raf = 0

interface Piece {
  x: number; y: number; vx: number; vy: number
  w: number; h: number; rot: number; vr: number
  color: string; shape: 'rect' | 'circle'
}

const colors = ['#38DDF8', '#2DD4BF', '#47D16C', '#F5B942', '#FF6B6B', '#7C9EFF']

function play() {
  const el = canvas.value
  if (!el) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const ctx = el.getContext('2d')
  if (!ctx) return

  const dpr = Math.min(2, window.devicePixelRatio || 1)
  const W = window.innerWidth
  const H = window.innerHeight
  el.width = W * dpr
  el.height = H * dpr
  el.style.width = W + 'px'
  el.style.height = H + 'px'
  ctx.scale(dpr, dpr)

  const pieces: Piece[] = []
  const origin = { x: W / 2, y: H * 0.4 }
  for (let i = 0; i < props.count; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 6 + Math.random() * 11
    pieces.push({
      x: origin.x,
      y: origin.y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed - 5,
      w: 5 + Math.random() * 6,
      h: 7 + Math.random() * 8,
      rot: Math.random() * Math.PI,
      vr: (Math.random() - 0.5) * 0.3,
      color: colors[Math.floor(Math.random() * colors.length)],
      shape: Math.random() < 0.22 ? 'circle' : 'rect'
    })
  }

  const start = performance.now()
  const tick = (now: number) => {
    const progress = Math.min(1, (now - start) / props.duration)
    ctx.clearRect(0, 0, W, H)
    for (const p of pieces) {
      p.vy += 0.22
      p.vx *= 0.99
      p.x += p.vx
      p.y += p.vy
      p.rot += p.vr
      const alpha = Math.max(0, 1 - progress * 1.15)
      ctx.save()
      ctx.globalAlpha = alpha
      ctx.translate(p.x, p.y)
      ctx.rotate(p.rot)
      ctx.fillStyle = p.color
      if (p.shape === 'circle') {
        ctx.beginPath()
        ctx.arc(0, 0, p.w / 2, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.fillRect(-p.w / 2, -p.h / 2, p.w, p.h)
      }
      ctx.restore()
    }
    if (progress < 1) {
      raf = requestAnimationFrame(tick)
    } else {
      ctx.clearRect(0, 0, W, H)
    }
  }
  raf = requestAnimationFrame(tick)
}

onMounted(play)
onBeforeUnmount(() => cancelAnimationFrame(raf))
</script>

<template>
  <canvas ref="canvas" class="confetti" aria-hidden="true" />
</template>

<style scoped>
.confetti {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
}
</style>
