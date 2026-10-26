<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { studentApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import ProgressRing from '@/components/ProgressRing.vue'
import EmptyState from '@/components/EmptyState.vue'

const data = ref<any>(null)
const loading = ref(true)

const scoreColor = computed(() => {
  const score = data.value?.score || 0
  if (score >= 85) return 'var(--green)'
  if (score >= 70) return 'var(--cyan)'
  if (score >= 60) return 'var(--amber)'
  return 'var(--coral)'
})

const level = computed(() => {
  const score = data.value?.score || 0
  if (score >= 85) return '课堂参与积极'
  if (score >= 70) return '参与度良好'
  if (score >= 60) return '仍有提升空间'
  return '需要重点关注'
})

const load = async () => {
  loading.value = true
  try {
    data.value = await studentApi.participation()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="学习参与度" subtitle="综合签到、迟到控制、点名响应、小测完成与抢答参与" eyebrow="PARTICIPATION">
      <template #actions><el-button :loading="loading" @click="load">刷新</el-button></template>
    </PageHeader>

    <div v-if="loading" class="grid grid-main-side">
      <div class="skeleton" style="height: 320px" />
      <div class="skeleton" style="height: 320px" />
    </div>

    <div v-else-if="!data" class="panel">
      <EmptyState title="参与度数据加载失败" icon="WarningFilled">
        <template #action><el-button type="primary" plain @click="load">重试</el-button></template>
      </EmptyState>
    </div>

    <template v-else>
      <div class="grid grid-main-side">
        <section class="panel score-panel">
          <div class="score-left">
            <ProgressRing :value="data.score" label="综合参与度" :sublabel="data.range" :size="176" :color="scoreColor" :stroke="10" />
          </div>
          <div class="score-right">
            <div class="score-level" :style="{ color: scoreColor }">{{ level }}</div>
            <p class="score-desc">
              参与度仅作为课堂辅助统计，分数由各项行为加权计算，所有来源可追溯。出勤与请假记录由服务端签到数据生成。
            </p>
            <div class="score-legend">
              <div v-for="item in data.items" :key="item.name" class="legend-row">
                <div class="row-between" style="margin-bottom: 5px">
                  <span>{{ item.name }}</span>
                  <span class="mono">{{ item.score }} <span class="muted">/ 权重 {{ item.weight }}%</span></span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: Math.min(100, item.score) + '%', background: scoreColor }" />
                </div>
                <div class="legend-detail">{{ item.detail }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">追溯来源</div>
            <span class="panel-sub">最近 30 天</span>
          </div>
          <div v-if="!data.sources?.length" class="muted" style="font-size: 12px">暂无参与记录</div>
          <div v-else class="source-list">
            <div v-for="(source, index) in data.sources" :key="index" class="source-item">
              <span class="source-type" :class="source.type.toLowerCase()">{{ source.type === 'SIGN' ? '签到' : '点名' }}</span>
              <div class="source-body">
                <div class="source-title">{{ source.title }}</div>
                <div class="source-detail">{{ source.detail }}</div>
              </div>
              <div class="source-time mono">{{ formatDateTime(source.time) }}</div>
            </div>
          </div>
        </section>
      </div>

      <section class="panel section-gap">
        <div class="panel-head">
          <div class="panel-title">计分说明</div>
        </div>
        <div class="formula">
          <div class="formula-item">
            <span class="mono cyan">签到出勤</span>
            <p>正常签到、教师补签与已批准请假计入出勤，缺勤会拉低该项得分。</p>
          </div>
          <div class="formula-item">
            <span class="mono teal">迟到控制</span>
            <p>按正常签到占全部应到任务的比例计算，迟到记录单独统计。</p>
          </div>
          <div class="formula-item">
            <span class="mono amber">点名响应</span>
            <p>课堂随机点名中及时点击“我在，准备回答”的比例。</p>
          </div>
          <div class="formula-item">
            <span class="mono coral">小测完成</span>
            <p>完成教师发布的课堂小测或投票，提交即计入完成度。</p>
          </div>
          <div class="formula-item">
            <span class="mono">抢答参与</span>
            <p>参与课堂抢答的次数，服务端按提交顺序排名。</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.score-panel { display: flex; gap: 28px; align-items: center; }
.score-left { flex-shrink: 0; }
.score-right { flex: 1; min-width: 0; }
.score-level { font-size: 22px; font-weight: 650; letter-spacing: -0.02em; }
.score-desc { font-size: 12px; color: var(--muted); line-height: 1.85; margin: 8px 0 18px; }
.score-legend { display: flex; flex-direction: column; gap: 13px; }
.legend-row { font-size: 12px; color: var(--text-2); }
.legend-detail { font-size: 10.5px; color: var(--muted); margin-top: 3px; }
.source-list { display: flex; flex-direction: column; max-height: 330px; overflow: auto; }
.source-item { display: flex; gap: 10px; padding: 9px 0; border-bottom: 1px solid var(--line); align-items: flex-start; }
.source-item:last-child { border-bottom: none; }
.source-type {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
  margin-top: 2px;
}
.source-type.sign { color: var(--teal); background: rgba(45, 212, 191, 0.1); }
.source-type.roll_call { color: var(--amber); background: rgba(245, 185, 66, 0.1); }
.source-body { flex: 1; min-width: 0; }
.source-title { font-size: 12px; color: var(--text-2); }
.source-detail { font-size: 10.5px; color: var(--muted); margin-top: 2px; }
.source-time { font-size: 10px; color: var(--muted); white-space: nowrap; }
.formula { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 16px; }
.formula-item p { font-size: 11.5px; color: var(--muted); line-height: 1.75; margin: 6px 0 0; }
.formula-item span { font-size: 12px; }

@media (max-width: 1100px) {
  .formula { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .score-panel { flex-direction: column; align-items: flex-start; }
  .formula { grid-template-columns: 1fr; }
  .source-time { display: none; }
}
</style>
