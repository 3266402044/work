<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(localStorage.getItem('cp_remember') !== '0')

const form = reactive({
  username: localStorage.getItem('cp_username') || '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '学生', username: 'student01', password: 'Student@123', color: 'var(--teal)' },
  { label: '教师', username: 'teacher01', password: 'Teacher@123', color: 'var(--cyan)' },
  { label: '管理员', username: 'admin', password: 'Admin@123', color: 'var(--amber)' }
]

const fill = (account: (typeof demoAccounts)[number]) => {
  form.username = account.username
  form.password = account.password
}

const submit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await auth.login(form.username, form.password, remember.value)
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
</script>

<template>
  <div class="login-page">
    <div class="login-side hide-mobile">
      <div class="side-badge">CLASS OPERATIONS CONSOLE</div>
      <h1>课堂的每一次<br />到课与回应，<br /><span>都清晰可见。</span></h1>
      <p>签到任务、实时到课、随机点名、课堂抢答与学情统计，统一在一套指挥台中完成。</p>
      <div class="side-metrics">
        <div>
          <strong class="mono">30s</strong>
          <span>动态二维码有效期</span>
        </div>
        <div>
          <strong class="mono">150m</strong>
          <span>定位签到默认半径</span>
        </div>
        <div>
          <strong class="mono">0</strong>
          <span>重复签到容忍</span>
        </div>
      </div>
    </div>

    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">CP</div>
        <div>
          <div class="login-title">ClassPulse</div>
          <div class="login-sub">课堂签到与互动系统</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" placeholder="学号 / 工号 / 管理员账号" autocomplete="username">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="submit"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <div class="form-row">
          <el-checkbox v-model="remember">记住账号</el-checkbox>
          <span class="muted tiny">演示环境可直接选择下方账号</span>
        </div>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">
          进入课堂指挥台
        </el-button>
      </el-form>

      <div class="demo-title">演示账号 · 一键填充</div>
      <div class="demo-chips">
        <button
          v-for="account in demoAccounts"
          :key="account.username"
          type="button"
          class="demo-chip"
          :style="{ '--accent': account.color }"
          @click="fill(account)"
        >
          <span class="demo-dot" />
          <span class="demo-label">{{ account.label }}</span>
          <span class="demo-user mono">{{ account.username }}</span>
        </button>
      </div>

      <div class="login-foot">
        <el-icon><Lock /></el-icon>
        <span>JWT 鉴权 · 角色权限校验 · 操作全程审计</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 56px;
  align-items: center;
  padding: 48px 7vw;
}
.login-side { position: relative; z-index: 1; max-width: 560px; }
.side-badge {
  display: inline-block;
  font-size: 10px;
  letter-spacing: 0.22em;
  color: var(--cyan);
  border: 1px solid rgba(56, 221, 248, 0.3);
  padding: 5px 10px;
  border-radius: 6px;
  margin-bottom: 22px;
}
.login-side h1 {
  margin: 0;
  font-size: clamp(34px, 4.2vw, 56px);
  line-height: 1.18;
  letter-spacing: -0.03em;
  font-weight: 650;
}
.login-side h1 span { color: var(--cyan); }
.login-side p {
  color: var(--muted);
  font-size: 14px;
  line-height: 1.9;
  max-width: 440px;
  margin: 22px 0 32px;
}
.side-metrics { display: flex; gap: 36px; }
.side-metrics div { display: flex; flex-direction: column; gap: 4px; }
.side-metrics strong { font-size: 24px; color: var(--text); letter-spacing: -0.02em; }
.side-metrics span { font-size: 11px; color: var(--muted); }

.login-card { position: relative; z-index: 1; }
.form-row { display: flex; align-items: center; justify-content: space-between; margin: 2px 0 18px; }
.tiny { font-size: 11px; }
.submit { width: 100%; height: 42px; font-size: 14px; letter-spacing: 0.02em; }

.demo-title { margin: 22px 0 10px; font-size: 11px; color: var(--muted); letter-spacing: 0.08em; }
.demo-chips { display: flex; flex-direction: column; gap: 8px; }
.demo-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: rgba(148, 163, 184, 0.04);
  color: var(--text-2);
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}
.demo-chip:hover { border-color: var(--accent); background: rgba(148, 163, 184, 0.08); }
.demo-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); }
.demo-label { font-size: 12px; }
.demo-user { margin-left: auto; font-size: 11px; color: var(--muted); }

.login-foot {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 10.5px;
}

@media (max-width: 980px) {
  .login-page { grid-template-columns: 1fr; padding: 24px; justify-items: center; }
  .login-card { width: 100%; max-width: 420px; }
}
</style>
