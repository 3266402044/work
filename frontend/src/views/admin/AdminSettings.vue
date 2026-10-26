<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const values = ref<Record<string, string>>({})
const loading = ref(true)
const saving = ref(false)
const resetting = ref(false)

const groups = [
  {
    title: '签到默认设置',
    desc: '影响新建签到任务的默认参数，教师仍可在创建时单独调整',
    keys: ['default.sign.duration.minutes', 'default.location.radius', 'late.threshold.minutes']
  },
  {
    title: '随机点名设置',
    desc: '控制点名响应时长、异常提醒阈值与出勤同步能力',
    keys: ['rollcall.response.seconds', 'rollcall.absence.warning', 'allow.sync.rollcall.attendance']
  },
  {
    title: '学习参与度权重',
    desc: '各项权重共同决定参与度分数，总分 0-100，可按教学重点调整',
    keys: [
      'participation.sign.weight',
      'participation.late.weight',
      'participation.rollcall.weight',
      'participation.quiz.weight',
      'participation.race.weight'
    ]
  }
]

const raw = ref<Array<{ key: string; value: string; description: string }>>([])

const load = async () => {
  loading.value = true
  try {
    raw.value = await adminApi.settings()
    const map: Record<string, string> = {}
    raw.value.forEach((item) => (map[item.key] = item.value))
    values.value = map
  } finally {
    loading.value = false
  }
}

const descOf = (key: string) => raw.value.find((item) => item.key === key)?.description || key

const isBool = (key: string) => key === 'allow.sync.rollcall.attendance'

const isNumber = (key: string) => !isBool(key)

const save = async () => {
  saving.value = true
  try {
    await adminApi.saveSettings(values.value)
    ElMessage.success('系统设置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

const resetDemo = async () => {
  try {
    await ElMessageBox.confirm(
      '该操作会清空当前业务数据并重建演示数据，确认继续？',
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
    <PageHeader title="系统设置" subtitle="配置签到默认参数、随机点名规则与参与度权重" eyebrow="SYSTEM SETTINGS">
      <template #actions>
        <el-button :loading="loading" @click="load">重新加载</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="col" style="gap: 16px">
      <div v-for="i in 3" :key="i" class="skeleton" style="height: 200px" />
    </div>

    <div v-else class="col" style="gap: 16px">
      <section v-for="group in groups" :key="group.title" class="panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">{{ group.title }}</div>
            <div class="panel-sub" style="margin-top: 3px">{{ group.desc }}</div>
          </div>
        </div>
        <div class="setting-grid">
          <div v-for="key in group.keys" :key="key" class="setting-item">
            <div class="setting-label">
              <span>{{ descOf(key) }}</span>
              <code class="mono">{{ key }}</code>
            </div>
            <el-switch
              v-if="isBool(key)"
              v-model="values[key]"
              active-value="true"
              inactive-value="false"
            />
            <el-input v-else v-model="values[key]" class="setting-input" />
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">演示数据</div>
          <span class="panel-sub">仅在本地演示环境使用</span>
        </div>
        <div class="row-between wrap" style="gap: 12px">
          <p class="muted" style="font-size: 12px; margin: 0; max-width: 620px; line-height: 1.8">
            重置会删除所有签到任务、签到记录、点名、小测与消息，并重新生成账号、班级、课程与近 7 天演示数据。
            生产环境请务必禁用该能力。
          </p>
          <el-button type="danger" plain :loading="resetting" @click="resetDemo">重置演示数据</el-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head"><div class="panel-title">安全说明</div></div>
        <ul class="security-list">
          <li>所有密码使用 BCrypt 加密存储，签到口令与二维码令牌仅保存哈希。</li>
          <li>JWT 默认有效期 12 小时，角色权限在服务端校验，前端隐藏按钮不作为权限依据。</li>
          <li>关键操作均写入审计日志，可在「系统日志」中按模块与时间检索。</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.setting-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px 24px; }
.setting-item { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px 14px; border: 1px solid var(--line); border-radius: 8px; background: var(--panel-3); }
.setting-label { display: flex; flex-direction: column; gap: 3px; font-size: 12.5px; color: var(--text-2); }
.setting-label code { font-size: 10px; color: var(--muted); }
.setting-input { width: 130px; }
.security-list { margin: 0; padding-left: 18px; color: var(--muted); font-size: 12px; line-height: 2; }
@media (max-width: 900px) { .setting-grid { grid-template-columns: 1fr; } }
</style>
