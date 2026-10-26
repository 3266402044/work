<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'
import { useUiStore } from '@/stores/ui'
import { formatDateTime } from '@/utils/format'
import type { NotificationItem } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const ui = useUiStore()
const list = ref<NotificationItem[]>([])
const loading = ref(true)
const tab = ref<'all' | 'unread'>('all')

const filtered = computed(() =>
  tab.value === 'all' ? list.value : list.value.filter((item) => item.readFlag === 0)
)

const iconOf = (type?: string) => {
  switch (type) {
    case 'TASK': return 'Tickets'
    case 'LEAVE': return 'Document'
    case 'ROLL_CALL': return 'Aim'
    case 'QUIZ': return 'EditPen'
    case 'RACE': return 'Lightning'
    case 'ANNOUNCEMENT': return 'Bell'
    default: return 'Message'
  }
}

const load = async () => {
  loading.value = true
  try {
    list.value = await notificationApi.list()
    ui.refreshUnread()
  } finally {
    loading.value = false
  }
}

const readOne = async (item: NotificationItem) => {
  if (item.readFlag === 1) return
  await notificationApi.read(item.id)
  item.readFlag = 1
  ui.refreshUnread()
}

const readAll = async () => {
  await notificationApi.readAll()
  list.value.forEach((item) => (item.readFlag = 1))
  ui.refreshUnread()
  ElMessage.success('已全部标记为已读')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="消息中心" subtitle="签到开始、任务关闭、请假审批与课堂互动提醒" eyebrow="NOTIFICATIONS">
      <template #actions>
        <el-button :disabled="!ui.unread" @click="readAll">全部已读</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <div class="panel-head">
        <el-radio-group v-model="tab" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unread">未读 {{ ui.unread || '' }}</el-radio-button>
        </el-radio-group>
        <span class="panel-sub">共 {{ list.length }} 条 · 仅展示最近 100 条</span>
      </div>

      <div v-if="loading" class="col" style="gap: 10px">
        <div v-for="i in 5" :key="i" class="skeleton" style="height: 62px" />
      </div>
      <EmptyState v-else-if="!filtered.length" title="暂无消息" description="新的课堂提醒会出现在这里" icon="Bell" />
      <div v-else class="notice-list">
        <button
          v-for="item in filtered"
          :key="item.id"
          type="button"
          class="notice-item"
          :class="{ unread: item.readFlag === 0 }"
          @click="readOne(item)"
        >
          <div class="notice-icon">
            <el-icon><component :is="iconOf(item.type)" /></el-icon>
          </div>
          <div class="notice-body">
            <div class="notice-title">
              {{ item.title }}
              <span v-if="item.readFlag === 0" class="unread-dot" />
            </div>
            <div class="notice-content">{{ item.content }}</div>
          </div>
          <div class="notice-time mono">{{ formatDateTime(item.createTime) }}</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notice-list { display: flex; flex-direction: column; }
.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 10px;
  border-bottom: 1px solid var(--line);
  background: transparent;
  border-left: 1px solid transparent;
  border-right: none;
  border-top: none;
  text-align: left;
  cursor: pointer;
  color: inherit;
  transition: background 0.16s ease;
}
.notice-item:last-child { border-bottom: none; }
.notice-item:hover { background: rgba(148, 163, 184, 0.05); }
.notice-item.unread { background: rgba(56, 221, 248, 0.035); }
.notice-icon {
  width: 32px; height: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(56, 221, 248, 0.08);
  color: var(--cyan);
  flex-shrink: 0;
}
.notice-body { flex: 1; min-width: 0; }
.notice-title { font-size: 13px; color: var(--text); display: flex; align-items: center; gap: 8px; }
.notice-content { font-size: 12px; color: var(--muted); margin-top: 3px; line-height: 1.7; }
.notice-time { font-size: 11px; color: var(--muted); white-space: nowrap; }
.unread-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--cyan); }
@media (max-width: 760px) {
  .notice-time { display: none; }
}
</style>
