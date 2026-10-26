<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { clockText } from '@/utils/format'

const props = defineProps<{ target?: string; initialSeconds?: number; size?: 'sm' | 'md' | 'lg' }>()
const emit = defineEmits<{ (e: 'end'): void }>()

const remaining = ref(0)
let timer: number | null = null
let ended = false

const compute = () => {
  if (props.target) {
    const target = new Date(props.target).getTime()
    remaining.value = Math.max(0, Math.floor((target - Date.now()) / 1000))
  } else {
    remaining.value = Math.max(0, props.initialSeconds || 0)
  }
  if (remaining.value <= 0 && !ended) {
    ended = true
    emit('end')
  }
}

const start = () => {
  if (timer) window.clearInterval(timer)
  compute()
  timer = window.setInterval(() => {
    if (props.target) {
      compute()
    } else if (remaining.value > 0) {
      remaining.value -= 1
      if (remaining.value === 0 && !ended) {
        ended = true
        emit('end')
      }
    }
  }, 1000)
}

watch(() => props.target, () => {
  ended = false
  start()
})

watch(() => props.initialSeconds, () => {
  ended = false
  start()
})

onMounted(start)
onBeforeUnmount(() => timer && window.clearInterval(timer))

const urgency = computed(() => {
  if (remaining.value <= 10) return 'danger'
  if (remaining.value <= 60) return 'warn'
  return 'normal'
})

const text = computed(() => clockText(remaining.value))
</script>

<template>
  <span class="countdown mono" :class="[size || 'md', urgency]">{{ text }}</span>
</template>

<style scoped>
.countdown { font-variant-numeric: tabular-nums; letter-spacing: -0.01em; }
.sm { font-size: 14px; }
.md { font-size: 20px; }
.lg { font-size: 40px; font-weight: 650; letter-spacing: -0.03em; }
.normal { color: var(--text); }
.warn { color: var(--amber); }
.danger { color: var(--coral); }
</style>
