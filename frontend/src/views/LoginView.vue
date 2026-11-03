<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const remember = ref(localStorage.getItem('cp_remember') !== '0')

const form = reactive({
  username: localStorage.getItem('cp_username') || '',
  password: ''
})

const demoAccounts = [
  { label: '教师', username: 'teacher01', password: 'Teacher@123', name: '王启明', note: '2 门课程 · 签到 / 点名 / 互动' },
  { label: '学生', username: 'student01', password: 'Student@123', name: '张明', note: '查看任务并完成签到' },
  { label: '管理员', username: 'admin', password: 'Admin@123', name: '系统管理员', note: '运营总览与基础数据' }
]

const fill = (account: (typeof demoAccounts)[number]) => {
  form.username = account.username
  form.password = account.password
}

const submit = async () => {
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login(form.username.trim(), form.password, remember.value)
    localStorage.setItem('cp_remember', remember.value ? '1' : '0')
    if (remember.value) {
      localStorage.setItem('cp_username', form.username)
    } else {
      localStorage.removeItem('cp_username')
    }
    ElMessage.success(`欢迎回来，${user.realName}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect || auth.home)
  } finally {
    loading.value = false
  }
}

const quickLogin = (account: (typeof demoAccounts)[number]) => {
  form.username = account.username
  form.password = account.password
  submit()
}

const usernameRef = ref<HTMLInputElement | null>(null)
const featureHints: Record<string, string> = {
  dash: '登录后可查看今日课程与安排',
  sign: '登录后完成二维码 / 定位 / 口令签到',
  roll: '登录后参与随机点名',
  chat: '登录后参与课堂抢答与小测',
  stats: '登录后查看学情与出勤统计'
}
const hintFeature = (key: string) => ElMessage.info(featureHints[key] || '登录后可用')
const focusUsername = () => usernameRef.value?.focus()
const hintBell = () => ElMessage.info('课堂消息将在登录后实时推送')
const fillAdmin = () => {
  fill(demoAccounts[2])
  ElMessage.info('已填入管理员演示账号')
}
const clearRemembered = () => {
  localStorage.removeItem('cp_username')
  ElMessage.success('已清除记住的账号')
}
</script>

<template>
  <div class="stage">
    <div class="bg" />
    <div class="vignette" />

    <!-- 左侧玻璃导航 -->
    <aside class="sidebar">
      <span class="pip" />
      <div class="logo">
        <svg viewBox="0 0 40 40" fill="none">
          <rect x="1" y="1" width="38" height="38" rx="12" stroke="rgba(255,255,255,.85)" stroke-width="1.6" />
          <path d="M7 22c4-5 8-5 12 0s8 5 12 0" stroke="#fff" stroke-width="1.8" stroke-linecap="round" />
          <path d="M7 16c4-5 8-5 12 0s8 5 12 0" stroke="rgba(255,255,255,.55)" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </div>
      <nav class="nav">
        <a class="nav-link on" href="#" aria-current="page" aria-label="仪表盘" @click.prevent="hintFeature('dash')">
          <svg viewBox="0 0 24 24"><rect x="3.5" y="3.5" width="7" height="7" rx="2" /><rect x="13.5" y="3.5" width="7" height="7" rx="2" /><rect x="3.5" y="13.5" width="7" height="7" rx="2" /><rect x="13.5" y="13.5" width="7" height="7" rx="2" /></svg>
        </a>
        <a class="nav-link" href="#" aria-label="签到" @click.prevent="hintFeature('sign')">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8.5" /><path d="M8 12.4l2.7 2.7L16.4 9.3" /></svg>
        </a>
        <a class="nav-link" href="#" aria-label="点名" @click.prevent="hintFeature('roll')">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3.2" /><circle cx="12" cy="12" r="8.5" /><path d="M12 1.5v3M12 19.5v3M1.5 12h3M19.5 12h3" /></svg>
        </a>
        <a class="nav-link" href="#" aria-label="互动" @click.prevent="hintFeature('chat')">
          <svg viewBox="0 0 24 24"><path d="M20 12a7.5 7.5 0 0 1-10.9 6.7L4 20l1.3-4.1A7.5 7.5 0 1 1 20 12z" /></svg>
        </a>
        <a class="nav-link" href="#" aria-label="统计" @click.prevent="hintFeature('stats')">
          <svg viewBox="0 0 24 24"><path d="M5 20V10M12 20V4M19 20v-7" /></svg>
        </a>
      </nav>
      <a class="logout" href="#" aria-label="退出" @click.prevent="clearRemembered">
        <svg viewBox="0 0 24 24"><path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" /><path d="M10 16l4-4-4-4" /><path d="M14 12H3.5" /></svg>
      </a>
    </aside>

    <!-- 顶部 -->
    <header class="header">
      <div class="hello">
        <div class="hello-label">欢迎回来</div>
        <div class="hello-name">ClassPulse 课堂指挥台</div>
      </div>
      <div class="tools">
        <button class="tool" type="button" aria-label="新建任务" @click="fillAdmin">
          <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
        </button>
        <button class="tool" type="button" aria-label="搜索" @click="focusUsername">
          <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4 4" /></svg>
        </button>
        <button class="tool" type="button" aria-label="消息" @click="hintBell">
          <svg viewBox="0 0 24 24"><path d="M18 8.5a6 6 0 1 0-12 0c0 5-2 6.5-2 6.5h16s-2-1.5-2-6.5z" /><path d="M10 19a2 2 0 0 0 4 0" /></svg>
        </button>
        <div class="avatar">CP</div>
      </div>
    </header>

    <!-- 主视觉 -->
    <section class="hero">
      <span class="chip">课堂签到</span>
      <h1>
        <span class="ln"><span>实时签到</span></span>
        <span class="ln"><span>让到课一目了然</span></span>
      </h1>
      <p class="blurb">
        四种签到方式覆盖课堂场景，迟到截止自动判定。<br />
        并发重复提交只保留一条记录，数据实时推送到教师大屏。
      </p>
    </section>

    <!-- 底部趋势 -->
    <section class="forecast">
      <div class="temps">
        <span>92<em>%</em></span>
        <span>88<em>%</em></span>
        <span>95<em>%</em></span>
        <span>86<em>%</em></span>
        <span>97<em>%</em></span>
        <span>90<em>%</em></span>
      </div>

      <svg class="wave" viewBox="0 0 835 230" preserveAspectRatio="none">
        <defs>
          <linearGradient id="wg" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stop-color="#fff" stop-opacity=".05" />
            <stop offset=".5" stop-color="#fff" stop-opacity=".82" />
            <stop offset="1" stop-color="#fff" stop-opacity=".2" />
          </linearGradient>
          <linearGradient id="wf" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#fff" stop-opacity=".26" />
            <stop offset="1" stop-color="#fff" stop-opacity="0" />
          </linearGradient>
          <clipPath id="wclipr"><rect x="0" y="0" width="835" height="230" /></clipPath>
          <path
            id="wpath"
            pathLength="1"
            d="M0,150 C80,128 140,88 212,108 C284,128 344,162 414,122 C484,82 544,68 614,100 C684,132 748,150 835,110"
          />
          <path
            id="wfillpath"
            d="M0,150 C80,128 140,88 212,108 C284,128 344,162 414,122 C484,82 544,68 614,100 C684,132 748,150 835,110 L835,230 L0,230 Z"
          />
        </defs>

        <g clip-path="url(#wclipr)">
          <use href="#wfillpath" fill="url(#wf)" />
        </g>

        <use href="#wpath" class="wline" stroke="url(#wg)" stroke-width="6.2" opacity=".17" />
        <use href="#wpath" class="wline" stroke="url(#wg)" stroke-width="4.6" opacity=".26" />
        <use href="#wpath" class="wline" stroke="url(#wg)" stroke-width="3.4" />
      </svg>

      <div class="days">
        <span>周日</span>
        <span>周一</span>
        <span>周二</span>
        <span>周三</span>
        <span class="on">周四</span>
        <span>周五</span>
        <span>周六</span>
      </div>
    </section>

    <!-- 右侧玻璃栏 -->
    <aside class="rail">
      <div class="card login-card">
        <div class="card-head">
          <span class="pin">
            <svg viewBox="0 0 24 24"><path d="M12 21s7-5.6 7-11a7 7 0 1 0-14 0c0 5.4 7 11 7 11z" /><circle cx="12" cy="10" r="2.6" /></svg>
          </span>
          ClassPulse
        </div>

        <div class="login-title">登录指挥台</div>
        <div class="login-sub">使用账号或下方演示身份进入</div>

        <label class="field">
          <span>账号</span>
          <input ref="usernameRef" v-model="form.username" type="text" autocomplete="username" placeholder="学号 / 工号 / 管理员账号" />
        </label>
        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </label>

        <div class="login-row">
          <label class="check">
            <input v-model="remember" type="checkbox" />
            <i />
            记住账号
          </label>
          <span class="ghost">演示环境</span>
        </div>

        <button class="submit" type="button" :disabled="loading" @click="submit">
          {{ loading ? '登录中…' : '进入系统' }}
        </button>
      </div>

      <div
        v-for="account in demoAccounts"
        :key="account.username"
        class="card row"
        role="button"
        tabindex="0"
        @click="quickLogin(account)"
        @keyup.enter="quickLogin(account)"
      >
        <div class="row-left">
          <div class="row-dot" />
          <div>
            <div class="row-name">{{ account.name }} · {{ account.label }}</div>
            <div class="row-note">{{ account.note }}</div>
          </div>
        </div>
        <div class="row-right">
          <span class="row-user">{{ account.username }}</span>
          <svg viewBox="0 0 24 24"><path d="M9 6l6 6-6 6" /></svg>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Inter+Tight:wght@500&display=swap');

.stage {
  position: fixed;
  inset: 0;
  overflow: hidden;
  background: #04121b;
  color: #fff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  font-feature-settings: 'kern' 1;

  --u: min(100vw / 1357, 100dvh / 871);
  --ink: #ffffff;
  --e-out: cubic-bezier(0.16, 1, 0.3, 1);
  --e-soft: cubic-bezier(0.22, 0.61, 0.36, 1);
  --e-pen: cubic-bezier(0.37, 0.01, 0.2, 1);
}

@supports not (height: 100dvh) {
  .stage { --u: min(100vw / 1357, 100vh / 871); }
}

/* ===== 背景 + 暗角 ===== */
.bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 80% at 72% 6%, rgba(64, 150, 140, 0.28), transparent 58%),
    radial-gradient(90% 70% at 18% 92%, rgba(8, 34, 44, 0.82), transparent 68%),
    linear-gradient(160deg, rgba(6, 22, 30, 0.62) 0%, rgba(4, 18, 27, 0.88) 100%),
    url('/login-bg.jpg') center 25% / cover no-repeat;
  animation: bgZoom 1.8s var(--e-out) both;
}
.bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(55% 40% at 62% 18%, rgba(150, 220, 210, 0.10), transparent 70%),
    radial-gradient(45% 35% at 30% 55%, rgba(40, 110, 110, 0.14), transparent 70%);
  filter: blur(28px);
  animation: drift 18s ease-in-out infinite alternate;
}
@keyframes bgZoom { from { transform: scale(1.08); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes drift { from { transform: translate3d(-12px, 0, 0); } to { transform: translate3d(14px, -10px, 0); } }

.vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(105deg, rgba(4, 16, 24, 0.34) 0%, rgba(4, 16, 24, 0.20) 40%, rgba(4, 16, 24, 0.06) 78%, transparent 100%),
    linear-gradient(180deg, rgba(4, 16, 24, 0.12) 0%, transparent 22%),
    linear-gradient(0deg, rgba(4, 16, 24, 0.07), rgba(4, 16, 24, 0.07));
}

/* ===== 侧边栏 ===== */
.sidebar {
  position: absolute;
  left: calc(16 * var(--u));
  top: calc(14 * var(--u));
  bottom: calc(7 * var(--u));
  width: calc(72 * var(--u));
  border-radius: calc(26 * var(--u));
  background: linear-gradient(180deg, rgba(255,255,255,.125) 0%, rgba(255,255,255,.135) 13%, rgba(255,255,255,.098) 34%, rgba(255,255,255,.092) 100%);
  backdrop-filter: blur(calc(18 * var(--u))) saturate(115%);
  -webkit-backdrop-filter: blur(calc(18 * var(--u))) saturate(115%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: calc(22 * var(--u));
  padding-bottom: calc(52 * var(--u));
  animation: slideL 0.92s var(--e-out) 0.05s both;
}
.pip {
  position: absolute;
  left: calc(-2 * var(--u));
  top: calc(131 * var(--u));
  width: calc(5 * var(--u));
  height: calc(29 * var(--u));
  border-radius: calc(3 * var(--u));
  background: #fff;
  box-shadow: 0 0 calc(10 * var(--u)) rgba(255, 255, 255, 0.55);
  transform-origin: center top;
  animation: growY 0.5s var(--e-out) 0.68s both;
}
.logo { width: calc(40 * var(--u)); height: calc(40 * var(--u)); animation: popIn 0.7s var(--e-soft) 0.26s both; }
.logo svg { width: 100%; height: 100%; }
.nav {
  margin-top: calc(57.5 * var(--u));
  display: flex;
  flex-direction: column;
  gap: calc(43 * var(--u));
}
.nav-link {
  width: calc(23 * var(--u));
  height: calc(23 * var(--u));
  opacity: 0.62;
  color: #fff;
  transition: opacity 0.2s ease, transform 0.2s var(--e-out);
  animation: riseIn 0.6s var(--e-soft) both;
}
.nav-link:nth-child(1) { animation-delay: 0.36s; }
.nav-link:nth-child(2) { animation-delay: 0.41s; }
.nav-link:nth-child(3) { animation-delay: 0.46s; }
.nav-link:nth-child(4) { animation-delay: 0.51s; }
.nav-link:nth-child(5) { animation-delay: 0.56s; }
.nav-link:hover { opacity: 1; transform: translateY(calc(-1 * var(--u))); }
.nav-link.on { opacity: 1; }
.nav-link svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.logout {
  margin-top: auto;
  width: calc(21 * var(--u));
  height: calc(21 * var(--u));
  opacity: 0.6;
  color: #fff;
  transition: opacity 0.2s ease;
  animation: riseIn 0.6s var(--e-soft) 0.7s both;
}
.logout:hover { opacity: 1; }
.logout svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }

/* ===== 顶部 ===== */
.header {
  position: absolute;
  top: calc(22 * var(--u));
  left: calc(126 * var(--u));
  right: calc(37 * var(--u));
  height: calc(52 * var(--u));
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: riseIn 0.7s var(--e-soft) 0.14s both;
}
.hello-label { font-size: calc(16 * var(--u)); font-weight: 400; color: rgba(255, 255, 255, 0.93); }
.hello-name { font-size: calc(19.5 * var(--u)); font-weight: 700; letter-spacing: calc(-0.35 * var(--u)); margin-top: calc(5 * var(--u)); }
.tools { display: flex; align-items: center; gap: calc(16 * var(--u)); }
.tool {
  position: relative;
  width: calc(52 * var(--u));
  height: calc(52 * var(--u));
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(calc(16 * var(--u))) saturate(115%);
  -webkit-backdrop-filter: blur(calc(16 * var(--u))) saturate(115%);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: background 0.2s ease;
  animation: popIn 0.58s var(--e-soft) both;
}
.tool:nth-child(1) { animation-delay: 0.30s; }
.tool:nth-child(2) { animation-delay: 0.365s; }
.tool:nth-child(3) { animation-delay: 0.43s; }
.tool:hover { background: rgba(255, 255, 255, 0.24); }
.tool svg { width: calc(20 * var(--u)); height: calc(20 * var(--u)); fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.avatar {
  width: calc(52 * var(--u));
  height: calc(52 * var(--u));
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(120, 220, 210, 0.6), rgba(40, 90, 110, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: calc(15 * var(--u));
  font-weight: 600;
  letter-spacing: 0.02em;
  animation: popIn 0.58s var(--e-soft) 0.495s both;
}

/* ===== 主视觉 ===== */
.hero {
  position: absolute;
  left: calc(126 * var(--u));
  top: calc(136 * var(--u));
  max-width: calc(560 * var(--u));
}
.chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  height: calc(36 * var(--u));
  padding: 0 calc(15 * var(--u));
  border-radius: calc(18 * var(--u));
  background: rgba(255, 255, 255, 0.175);
  backdrop-filter: blur(calc(16 * var(--u))) saturate(115%);
  -webkit-backdrop-filter: blur(calc(16 * var(--u))) saturate(115%);
  font-size: calc(13 * var(--u));
  overflow: hidden;
  animation: wipeRight 0.8s var(--e-out) 0.44s both;
}
.hero h1 {
  margin: calc(22 * var(--u)) 0 0;
  font-family: 'Inter Tight', 'Inter', sans-serif;
  font-weight: 500;
  font-size: calc(63 * var(--u));
  line-height: calc(78 * var(--u));
  letter-spacing: calc(0.25 * var(--u));
}
.hero h1 .ln { display: block; overflow: hidden; }
.hero h1 .ln > span { display: block; transform: translateY(115%); animation: lineUp 1.05s var(--e-out) both; }
.hero h1 .ln:nth-child(1) > span { animation-delay: 0.56s; }
.hero h1 .ln:nth-child(2) > span { animation-delay: 0.67s; }
.blurb {
  width: calc(480 * var(--u));
  margin: calc(24 * var(--u)) 0 0;
  font-size: calc(15.2 * var(--u));
  line-height: calc(24 * var(--u));
  font-weight: 500;
  letter-spacing: calc(-0.3 * var(--u));
  color: rgba(255, 255, 255, 0.95);
  animation: fadeInSoft 0.9s var(--e-soft) 0.9s both;
}

/* ===== 底部趋势 ===== */
.forecast {
  position: absolute;
  left: calc(126 * var(--u));
  right: calc(396 * var(--u));
  bottom: calc(99 * var(--u));
}
.temps { display: flex; justify-content: space-between; }
.temps span {
  font-size: calc(37 * var(--u));
  font-weight: 400;
  letter-spacing: calc(-0.7 * var(--u));
  animation: riseIn 0.7s var(--e-soft) both;
}
.temps span em { font-style: normal; font-size: calc(18 * var(--u)); opacity: 0.7; }
.temps span:nth-child(1) { animation-delay: 1.26s; }
.temps span:nth-child(2) { animation-delay: 1.33s; }
.temps span:nth-child(3) { animation-delay: 1.40s; }
.temps span:nth-child(4) { animation-delay: 1.47s; }
.temps span:nth-child(5) { animation-delay: 1.54s; }
.temps span:nth-child(6) { animation-delay: 1.61s; }

.wave {
  width: 100%;
  height: calc(230 * var(--u));
  margin-top: calc(43 * var(--u));
  display: block;
  overflow: visible;
}
.wline { fill: none; stroke-linecap: round; stroke-dasharray: 1; stroke-dashoffset: 1; animation: drawLine 1.6s var(--e-pen) 1.5s both; }
#wclipr rect {
  transform-box: fill-box;
  transform-origin: left center;
  transform: scaleX(0);
  animation: wipeX 1.42s var(--e-pen) 1.72s both;
}

.days {
  display: flex;
  justify-content: space-between;
  margin-top: calc(-31 * var(--u));
  font-size: calc(18 * var(--u));
  font-weight: 500;
  color: rgba(255, 255, 255, 0.88);
}
.days span { animation: riseIn 0.6s var(--e-soft) both; }
.days span:nth-child(1) { animation-delay: 2.10s; }
.days span:nth-child(2) { animation-delay: 2.155s; }
.days span:nth-child(3) { animation-delay: 2.21s; }
.days span:nth-child(4) { animation-delay: 2.265s; }
.days span:nth-child(5) { animation-delay: 2.32s; }
.days span:nth-child(6) { animation-delay: 2.375s; }
.days span:nth-child(7) { animation-delay: 2.43s; }
.days .on { color: #fff; font-weight: 600; }

/* ===== 右侧玻璃栏 ===== */
.rail {
  position: absolute;
  right: calc(38 * var(--u));
  top: calc(134 * var(--u));
  bottom: calc(95 * var(--u));
  width: calc(310 * var(--u));
  display: flex;
  flex-direction: column;
  gap: calc(20 * var(--u));
}
.card {
  position: relative;
  overflow: hidden;
  border-radius: calc(24 * var(--u));
  background: linear-gradient(180deg, rgba(255,255,255,.20) 0%, rgba(255,255,255,.258) 24%, rgba(255,255,255,.252) 78%, rgba(255,255,255,.232) 100%);
  backdrop-filter: blur(calc(26 * var(--u))) saturate(118%);
  -webkit-backdrop-filter: blur(calc(26 * var(--u))) saturate(118%);
  animation: slideR 0.95s var(--e-out) both;
}
.card:nth-child(1) { animation-delay: 0.80s; }
.card:nth-child(2) { animation-delay: 0.92s; }
.card:nth-child(3) { animation-delay: 1.03s; }
.card:nth-child(4) { animation-delay: 1.14s; }

.card::after, .chip::after, .tool::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 38%;
  height: 100%;
  background: linear-gradient(100deg, transparent 0%, rgba(255, 255, 255, 0.17) 50%, transparent 100%);
  transform: translate3d(-150%, 0, 0) skewX(-18deg);
  animation: sheen 1.15s var(--e-soft) 2.55s 1 both;
  pointer-events: none;
}

.login-card { padding: calc(20 * var(--u)) calc(20 * var(--u)) calc(18 * var(--u)); }
.card-head {
  display: flex;
  align-items: center;
  gap: calc(8 * var(--u));
  font-size: calc(12.5 * var(--u));
  color: rgba(255, 255, 255, 0.9);
}
.pin { width: calc(16 * var(--u)); height: calc(16 * var(--u)); opacity: 0.85; }
.pin svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.login-title { font-size: calc(22 * var(--u)); font-weight: 600; letter-spacing: calc(-0.3 * var(--u)); margin-top: calc(14 * var(--u)); }
.login-sub { font-size: calc(11.5 * var(--u)); color: rgba(255, 255, 255, 0.62); margin-top: calc(4 * var(--u)); }

.field { display: block; margin-top: calc(12 * var(--u)); }
.field > span { display: block; font-size: calc(11 * var(--u)); color: rgba(255, 255, 255, 0.7); margin-bottom: calc(5 * var(--u)); }
.field input {
  width: 100%;
  height: calc(38 * var(--u));
  border-radius: calc(10 * var(--u));
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.10);
  color: #fff;
  padding: 0 calc(12 * var(--u));
  font-size: calc(12.5 * var(--u));
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.field input::placeholder { color: rgba(255, 255, 255, 0.42); }
.field input:focus { border-color: rgba(255, 255, 255, 0.5); background: rgba(255, 255, 255, 0.14); }

.login-row { display: flex; align-items: center; justify-content: space-between; margin-top: calc(12 * var(--u)); }
.check { display: inline-flex; align-items: center; gap: calc(7 * var(--u)); font-size: calc(11.5 * var(--u)); color: rgba(255, 255, 255, 0.8); cursor: pointer; }
.check input { display: none; }
.check i {
  width: calc(14 * var(--u));
  height: calc(14 * var(--u));
  border-radius: calc(4 * var(--u));
  border: 1px solid rgba(255, 255, 255, 0.35);
  transition: background 0.16s ease, border-color 0.16s ease;
}
.check input:checked + i { background: #fff; border-color: #fff; }
.ghost { font-size: calc(10.5 * var(--u)); color: rgba(255, 255, 255, 0.5); }

.submit {
  width: 100%;
  height: calc(40 * var(--u));
  margin-top: calc(14 * var(--u));
  border: none;
  border-radius: calc(12 * var(--u));
  background: #fff;
  color: #04121b;
  font-family: inherit;
  font-size: calc(13 * var(--u));
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s var(--e-out), box-shadow 0.2s ease, opacity 0.2s ease;
}
.submit:hover { transform: translateY(calc(-1 * var(--u))); box-shadow: 0 10px 26px rgba(255, 255, 255, 0.22); }
.submit:disabled { opacity: 0.6; cursor: default; transform: none; box-shadow: none; }

.card.row {
  height: calc(96 * var(--u));
  padding: calc(14 * var(--u)) calc(16 * var(--u));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: calc(10 * var(--u));
  cursor: pointer;
  transition: background 0.2s ease;
}
.card.row:hover { background: linear-gradient(180deg, rgba(255,255,255,.26) 0%, rgba(255,255,255,.3) 60%, rgba(255,255,255,.27) 100%); }
.card.row:focus-visible { outline: calc(2 * var(--u)) solid rgba(255, 255, 255, 0.8); outline-offset: calc(2 * var(--u)); }
.row-left { display: flex; align-items: center; gap: calc(10 * var(--u)); min-width: 0; }
.row-dot { width: calc(8 * var(--u)); height: calc(8 * var(--u)); border-radius: 50%; background: #fff; opacity: 0.85; flex-shrink: 0; }
.row-name { font-size: calc(12.5 * var(--u)); font-weight: 600; }
.row-note { font-size: calc(10.5 * var(--u)); color: rgba(255, 255, 255, 0.62); margin-top: calc(2 * var(--u)); }
.row-right { display: flex; align-items: center; gap: calc(6 * var(--u)); flex-shrink: 0; }
.row-user { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: calc(10.5 * var(--u)); color: rgba(255, 255, 255, 0.7); }
.row-right svg { width: calc(15 * var(--u)); height: calc(15 * var(--u)); fill: none; stroke: rgba(255, 255, 255, 0.8); stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }

/* ===== 动画 ===== */
@keyframes riseIn { from { opacity: 0; transform: translateY(calc(14 * var(--u))); } to { opacity: 1; transform: none; } }
@keyframes slideL { from { opacity: 0; transform: translateX(calc(-26 * var(--u))); } to { opacity: 1; transform: none; } }
@keyframes slideR { from { opacity: 0; transform: translateX(calc(30 * var(--u))) scale(0.985); } to { opacity: 1; transform: none; } }
@keyframes popIn { from { opacity: 0; transform: scale(0.7); } to { opacity: 1; transform: scale(1); } }
@keyframes growY { from { opacity: 0; transform: scaleY(0); } to { opacity: 1; transform: scaleY(1); } }
@keyframes lineUp { to { transform: translateY(0); } }
@keyframes wipeRight { from { opacity: 0; clip-path: inset(0 100% 0 0); } to { opacity: 1; clip-path: inset(0 0 0 0); } }
@keyframes drawLine { to { stroke-dashoffset: 0; } }
@keyframes wipeX { to { transform: scaleX(1); } }
@keyframes fadeInSoft { from { opacity: 0; } to { opacity: 1; } }
@keyframes sheen {
  from { transform: translate3d(-150%, 0, 0) skewX(-18deg); }
  to { transform: translate3d(260%, 0, 0) skewX(-18deg); }
}

@media (prefers-reduced-motion: reduce) {
  .bg, .bg::before, .sidebar, .pip, .logo, .nav-link, .logout, .header, .tool, .avatar,
  .chip, .hero h1 .ln > span, .blurb, .temps span, .days span, .card, .wline {
    animation: none !important;
    transition: none !important;
  }
  .card::after, .chip::after, .tool::after { display: none; }
  .hero h1 .ln > span { transform: none; }
  .wline { stroke-dashoffset: 0; }
  #wclipr rect { transform: scaleX(1); }
  .chip { clip-path: none; }
}

/* ===== 移动端 ===== */
@media (max-width: 860px) {
  .stage {
    position: relative;
    min-height: 100dvh;
    overflow-y: auto;
    --u: min(calc(100vw / 430), 1.22px);
  }
  .sidebar { display: none; }
  .header { position: relative; top: auto; left: auto; right: auto; height: auto; padding: calc(16 * var(--u)); }
  .hero { position: relative; top: auto; left: auto; max-width: none; padding: 0 calc(16 * var(--u)); }
  .hero h1 { font-size: calc(46 * var(--u)); line-height: calc(56 * var(--u)); }
  .blurb { width: auto; }
  .forecast { position: relative; left: auto; right: auto; bottom: auto; padding: calc(16 * var(--u)); }
  .rail { position: relative; right: auto; top: auto; bottom: auto; width: auto; padding: 0 calc(16 * var(--u)) calc(24 * var(--u)); }
}
</style>
