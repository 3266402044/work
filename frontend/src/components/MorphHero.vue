<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

interface FeatureItem {
  label: string
  note?: string
  bg?: string
}

interface Feature {
  key: string
  label: string
  eyebrow: string
  title: string
  desc: string
  accent: string
  width: number
  kind: 'discover' | 'journeys' | 'list'
  items: FeatureItem[]
}

const features: Feature[] = [
  {
    key: 'sign',
    label: '签到',
    eyebrow: 'SIGN-IN / 实时签到',
    title: '实时到课，一眼看清',
    desc: '四种签到方式，迟到截止自动判定。',
    accent: 'var(--cyan)',
    width: 470,
    kind: 'discover',
    items: [
      { label: '普通签到', bg: 'linear-gradient(150deg, #0a3d4a, #0e6b7a)' },
      { label: '二维码', bg: 'linear-gradient(150deg, #0b2f4a, #1760a8)' },
      { label: '定位签到', bg: 'linear-gradient(150deg, #123a2a, #1f7a52)' },
      { label: '口令签到', bg: 'linear-gradient(150deg, #4a3a0b, #a87a17)' }
    ]
  },
  {
    key: 'roll',
    label: '点名',
    eyebrow: 'ROLL CALL / 随机点名',
    title: '随机点名，绝不重复',
    desc: '后端无放回抽取并保存结果。',
    accent: 'var(--amber)',
    width: 420,
    kind: 'journeys',
    items: [
      { label: '公平抽取', note: '从未被抽到的优先' },
      { label: '投屏大屏', note: '2.6 秒滚动定格' }
    ]
  },
  {
    key: 'interact',
    label: '互动',
    eyebrow: 'INTERACTION / 课堂互动',
    title: '抢答与小测',
    desc: '服务端排名防作弊。',
    accent: 'var(--teal)',
    width: 330,
    kind: 'list',
    items: [
      { label: '课堂抢答', note: '服务端排名' },
      { label: '即时小测', note: '单选 / 多选 / 判断' },
      { label: '课堂公告', note: '置顶与失效' }
    ]
  },
  {
    key: 'stats',
    label: '统计',
    eyebrow: 'ANALYTICS / 学情统计',
    title: '学情与预警',
    desc: '班级维度一键导出。',
    accent: 'var(--green)',
    width: 340,
    kind: 'list',
    items: [
      { label: '到课率', note: '按班级' },
      { label: '迟到榜', note: '周维度' },
      { label: '异常预警', note: '连续缺勤' }
    ]
  }
]

const heroRef = ref<HTMLElement | null>(null)
const navRefs = ref<(HTMLElement | null)[]>([])
const active = ref<number | null>(null)
const open = ref(false)
const direction = ref<'left' | 'right'>('right')
const panelLeft = ref(0)
const panelTop = ref(56)
const panelWidth = ref(360)
const mediaIndex = ref(0)
let closeTimer: number | null = null

const feature = computed(() => features[active.value ?? 0])
const mediaBg = computed(() => feature.value.items[mediaIndex.value]?.bg || '')

const setNavRef = (el: any, i: number) => {
  navRefs.value[i] = (el as HTMLElement) ?? null
}

function measure() {
  const hero = heroRef.value
  const nav = navRefs.value[active.value ?? 0]
  if (!hero || !nav) return
  const hr = hero.getBoundingClientRect()
  const nr = nav.getBoundingClientRect()
  const width = Math.min(feature.value.width, Math.max(260, hr.width - 8))
  let left = nr.left - hr.left
  left = Math.max(0, Math.min(left, hr.width - width))
  panelLeft.value = left
  panelWidth.value = width
  panelTop.value = nr.bottom - hr.top + 12
}

async function hover(i: number) {
  if (closeTimer) {
    window.clearTimeout(closeTimer)
    closeTimer = null
  }
  if (active.value !== i) {
    direction.value = i > (active.value ?? 0) ? 'right' : 'left'
    mediaIndex.value = 0
  }
  const wasOpen = open.value
  active.value = i
  await nextTick()
  measure()
  if (!wasOpen) open.value = true
}

function scheduleClose() {
  if (closeTimer) window.clearTimeout(closeTimer)
  closeTimer = window.setTimeout(() => {
    open.value = false
    active.value = null
    closeTimer = null
  }, 240)
}

function cancelClose() {
  if (closeTimer) {
    window.clearTimeout(closeTimer)
    closeTimer = null
  }
}

function onResize() {
  if (open.value) measure()
}

onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (closeTimer) window.clearTimeout(closeTimer)
})
</script>

<template>
  <div class="morph-hero" ref="heroRef" @mouseenter="cancelClose" @mouseleave="scheduleClose">
    <header class="hero-bar">
      <div class="hero-brand">
        <span class="brand-mark">CP</span>
        <span class="brand-name">ClassPulse</span>
      </div>

      <nav class="hero-nav" aria-label="功能预览">
        <button
          v-for="(f, i) in features"
          :key="f.key"
          :ref="(el) => setNavRef(el, i)"
          type="button"
          class="nav-item"
          :class="{ active: active === i }"
          @mouseenter="hover(i)"
          @click.prevent="hover(i)"
          @focus="hover(i)"
        >
          {{ f.label }}
          <span class="nav-underline" />
        </button>
      </nav>

      <span class="hero-pill">进入系统</span>
    </header>

    <transition name="panel">
      <div
        v-if="open"
        class="morph-panel"
        :style="{ left: panelLeft + 'px', top: panelTop + 'px', width: panelWidth + 'px', '--accent': feature.accent }"
        @mouseenter="cancelClose"
      >
        <transition :name="`swap-${direction}`" mode="out-in">
          <div :key="feature.key" class="panel-inner" :class="'kind-' + feature.kind">
            <!-- Discover：图片预览 + 并列选项 -->
            <template v-if="feature.kind === 'discover'">
              <div class="disc-media">
                <transition name="fade">
                  <div :key="mediaIndex" class="disc-layer" :style="{ background: mediaBg }" />
                </transition>
                <span class="disc-media-label">{{ feature.items[mediaIndex]?.label }}</span>
              </div>
              <div class="disc-body">
                <div class="panel-eyebrow">{{ feature.eyebrow }}</div>
                <div class="disc-title">{{ feature.title }}</div>
                <div class="disc-desc">{{ feature.desc }}</div>
                <div class="disc-chips">
                  <button
                    v-for="(it, k) in feature.items"
                    :key="it.label"
                    type="button"
                    class="disc-chip"
                    :class="{ on: mediaIndex === k }"
                    @mouseenter="mediaIndex = k"
                    @click.prevent="mediaIndex = k"
                  >{{ it.label }}</button>
                </div>
              </div>
            </template>

            <!-- Journeys：两张会放大的卡片 -->
            <template v-else-if="feature.kind === 'journeys'">
              <div class="panel-eyebrow">{{ feature.eyebrow }}</div>
              <div class="jr-title">{{ feature.title }}</div>
              <div class="jr-cards">
                <div v-for="it in feature.items" :key="it.label" class="jr-card">
                  <div class="jr-glow" />
                  <div class="jr-name">{{ it.label }}</div>
                  <div class="jr-note">{{ it.note }}</div>
                </div>
              </div>
            </template>

            <!-- Regions：短列表 -->
            <template v-else>
              <div class="panel-eyebrow">{{ feature.eyebrow }}</div>
              <div class="li-title">{{ feature.title }}</div>
              <div class="li-list">
                <div v-for="it in feature.items" :key="it.label" class="li-row">
                  <span class="li-dot" />
                  <span class="li-name">{{ it.label }}</span>
                  <span class="li-note">{{ it.note }}</span>
                </div>
              </div>
            </template>
          </div>
        </transition>
      </div>
    </transition>

    <div class="hero-stage" :class="{ dim: open }">
      <div class="stage-badge">CLASS OPERATIONS CONSOLE</div>
      <h1>课堂的每一次<br />到课与回应，<br /><span class="accent">都清晰可见。</span></h1>
      <p>签到任务、实时到课、随机点名、课堂抢答与学情统计，统一在一套指挥台中完成。</p>
      <div class="stage-metrics">
        <div><strong class="mono">30s</strong><span>二维码有效期</span></div>
        <div><strong class="mono">150m</strong><span>定位默认半径</span></div>
        <div><strong class="mono">0</strong><span>重复签到容忍</span></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.morph-hero {
  position: relative;
  width: 100%;
  max-width: 680px;
  user-select: none;
}

/* ===== 顶部导航栏 ===== */
.hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}
.hero-brand { display: flex; align-items: center; gap: 9px; }
.brand-mark {
  width: 30px; height: 30px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--mono); font-weight: 700; font-size: 12px;
  color: var(--cyan);
  background: linear-gradient(145deg, rgba(56, 221, 248, 0.2), rgba(45, 212, 191, 0.08));
  border: 1px solid rgba(56, 221, 248, 0.35);
}
.brand-name { font-size: 14px; font-weight: 650; letter-spacing: -0.01em; }

.hero-nav { display: flex; gap: 2px; }
.nav-item {
  position: relative;
  height: 30px;
  padding: 0 11px;
  border: none;
  background: transparent;
  color: var(--text-2);
  font-size: 12.5px;
  cursor: pointer;
  border-radius: 7px;
  transition: color 0.2s ease, background 0.2s ease;
}
.nav-item:hover { color: var(--text); background: rgba(148, 163, 184, 0.07); }
.nav-item.active { color: var(--cyan); }
.nav-underline {
  position: absolute;
  left: 11px;
  right: 11px;
  bottom: 3px;
  height: 1.5px;
  border-radius: 2px;
  background: var(--cyan);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.nav-item.active .nav-underline { transform: scaleX(1); }

.hero-pill {
  font-size: 11.5px;
  color: var(--cyan);
  border: 1px solid rgba(56, 221, 248, 0.4);
  background: rgba(56, 221, 248, 0.06);
  border-radius: 999px;
  padding: 6px 14px;
  white-space: nowrap;
}

/* ===== 会滑动 / 缩放的磨砂面板 ===== */
.morph-panel {
  position: absolute;
  z-index: 40;
  border-radius: 14px;
  padding: 14px;
  background: color-mix(in srgb, var(--panel-2) 80%, transparent);
  border: 1px solid var(--line-strong);
  box-shadow: 0 26px 64px rgba(0, 0, 0, 0.42);
  backdrop-filter: blur(22px) saturate(140%);
  -webkit-backdrop-filter: blur(22px) saturate(140%);
  transition:
    left 0.44s cubic-bezier(0.22, 1, 0.36, 1),
    width 0.44s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.2s ease;
}

.panel-enter-active { transition: opacity 0.22s ease, transform 0.34s cubic-bezier(0.22, 1, 0.36, 1); }
.panel-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-6px) scale(0.985); }

.panel-inner { display: block; }

.panel-eyebrow {
  display: flex; align-items: center; gap: 7px;
  font-size: 9.5px; letter-spacing: 0.16em; color: var(--accent);
}
.panel-eyebrow::before {
  content: ""; width: 5px; height: 5px; border-radius: 50%; background: var(--accent);
}

/* Discover */
.panel-inner.kind-discover { display: grid; grid-template-columns: 158px 1fr; gap: 14px; }
.disc-media {
  position: relative;
  height: 148px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.disc-layer { position: absolute; inset: 0; }
.disc-media-label {
  position: absolute; left: 10px; bottom: 9px;
  font-size: 10px; color: rgba(255, 255, 255, 0.82);
  letter-spacing: 0.06em;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.34s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.disc-body { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.disc-title { font-size: 17px; font-weight: 650; letter-spacing: -0.01em; }
.disc-desc { font-size: 11.5px; color: var(--muted); line-height: 1.7; }
.disc-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
.disc-chip {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}
.disc-chip:hover { color: var(--text); border-color: var(--line-strong); }
.disc-chip.on {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 46%, transparent);
  background: color-mix(in srgb, var(--accent) 10%, transparent);
}

/* Journeys */
.jr-title { font-size: 16px; font-weight: 650; margin: 6px 0 2px; }
.jr-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 8px; }
.jr-card {
  position: relative;
  height: 104px;
  border-radius: 10px;
  overflow: hidden;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 1px;
  border: 1px solid var(--line);
  background: linear-gradient(150deg, color-mix(in srgb, var(--accent) 16%, transparent), var(--panel-3));
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}
.jr-card:hover { transform: scale(1.045); }
.jr-glow {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 72% 18%, color-mix(in srgb, var(--accent) 32%, transparent), transparent 62%);
  pointer-events: none;
}
.jr-name { position: relative; font-size: 12.5px; font-weight: 600; }
.jr-note { position: relative; font-size: 10px; color: var(--muted); }

/* Regions / list */
.li-title { font-size: 16px; font-weight: 650; margin: 6px 0 2px; }
.li-list { display: flex; flex-direction: column; margin-top: 4px; }
.li-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 2px;
  border-bottom: 1px solid var(--line);
}
.li-row:last-child { border-bottom: none; }
.li-dot { width: 5px; height: 5px; border-radius: 50%; background: var(--accent); }
.li-name { font-size: 12px; color: var(--text-2); }
.li-note { margin-left: auto; font-size: 10.5px; color: var(--muted); }

/* 内容按移动方向带模糊交叉淡入 */
.swap-right-enter-active, .swap-right-leave-active,
.swap-left-enter-active, .swap-left-leave-active {
  transition: opacity 0.22s ease, transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), filter 0.3s ease;
}
.swap-right-enter-from { opacity: 0; transform: translateX(-18px); filter: blur(5px); }
.swap-right-leave-to { opacity: 0; transform: translateX(18px); filter: blur(5px); }
.swap-left-enter-from { opacity: 0; transform: translateX(18px); filter: blur(5px); }
.swap-left-leave-to { opacity: 0; transform: translateX(-18px); filter: blur(5px); }

/* ===== 底部标题区 ===== */
.hero-stage { margin-top: 26px; transition: opacity 0.3s ease, filter 0.3s ease, transform 0.3s ease; }
.hero-stage.dim { opacity: 0.28; filter: blur(1.5px); transform: scale(0.99); }
.stage-badge {
  display: inline-block; font-size: 10px; letter-spacing: 0.22em; color: var(--cyan);
  border: 1px solid rgba(56, 221, 248, 0.3); padding: 5px 10px; border-radius: 6px; margin-bottom: 20px;
}
.hero-stage h1 {
  margin: 0; font-size: clamp(28px, 3.4vw, 48px); line-height: 1.16;
  letter-spacing: -0.03em; font-weight: 650;
}
.hero-stage h1 .accent { color: var(--cyan); }
.hero-stage p { color: var(--muted); font-size: 13px; line-height: 1.9; max-width: 430px; margin: 16px 0 26px; }
.stage-metrics { display: flex; gap: 32px; }
.stage-metrics div { display: flex; flex-direction: column; gap: 4px; }
.stage-metrics strong { font-size: 21px; color: var(--text); letter-spacing: -0.02em; }
.stage-metrics span { font-size: 11px; color: var(--muted); }

@media (prefers-reduced-motion: reduce) {
  .morph-panel, .panel-inner, .jr-card, .nav-underline { transition: none !important; }
}

@media (max-width: 760px) {
  .hero-nav { display: none; }
  .hero-pill { margin-left: auto; }
  .hero-stage { margin-top: 20px; }
}
</style>
