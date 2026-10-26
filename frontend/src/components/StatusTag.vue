<script setup lang="ts">
import { computed } from 'vue'
import { taskStatusClass } from '@/utils/format'

const props = defineProps<{ status?: string; label?: string }>()

const labels: Record<string, string> = {
  ONGOING: '进行中',
  NOT_STARTED: '未开始',
  ENDED: '已结束',
  CLOSED: '已提前关闭',
  NORMAL: '正常',
  LATE: '迟到',
  LEAVE: '请假',
  ABSENT: '未签到',
  MANUAL: '补签',
  PENDING: '待处理',
  RESPONDED: '已响应',
  NO_RESPONSE: '未响应',
  SKIPPED: '已跳过',
  CANCELED: '已撤销',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  RUNNING: '进行中',
  FINISHED: '已结束',
  LEAVE_PENDING_RECORD: '已批准请假'
}

const cls = computed(() => {
  if (['ONGOING', 'NOT_STARTED', 'ENDED', 'CLOSED'].includes(props.status || '')) {
    return taskStatusClass(props.status)
  }
  switch (props.status) {
    case 'NORMAL':
    case 'RESPONDED':
    case 'APPROVED':
    case 'FINISHED':
      return 'status-normal'
    case 'LATE':
    case 'NO_RESPONSE':
    case 'PENDING':
    case 'MANUAL':
      return 'status-warning'
    case 'LEAVE':
    case 'RUNNING':
      return 'status-not-started'
    case 'ABSENT':
    case 'REJECTED':
      return 'status-closed'
    default:
      return 'status-ended'
  }
})

const text = computed(() => props.label || labels[props.status || ''] || props.status || '—')
</script>

<template>
  <span class="status-tag" :class="cls"><i class="dot" />{{ text }}</span>
</template>
