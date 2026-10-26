<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { studentApi, uploadApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import type { TaskVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const tasks = ref<TaskVO[]>([])
const leaves = ref<any[]>([])
const loading = ref(true)
const submitting = ref(false)
const uploading = ref(false)
const form = reactive({ taskId: undefined as number | undefined, reason: '', proofUrl: '' })

const availableTasks = computed(() =>
  tasks.value.filter((task) => !task.signed && task.taskStatus !== 'CLOSED')
)

const load = async () => {
  loading.value = true
  try {
    const [taskPage, leaveList] = await Promise.all([
      studentApi.tasks({ scope: 'ALL', page: 1, size: 100 }),
      studentApi.leaves()
    ])
    tasks.value = taskPage.list
    leaves.value = leaveList
  } finally {
    loading.value = false
  }
}

const uploadProof = async (options: any) => {
  uploading.value = true
  try {
    const result = await uploadApi.upload(options.file)
    form.proofUrl = result.url
    ElMessage.success('证明材料已上传')
  } finally {
    uploading.value = false
  }
}

const submit = async () => {
  if (!form.taskId) {
    ElMessage.warning('请选择请假对应的签到任务')
    return
  }
  if (!form.reason.trim()) {
    ElMessage.warning('请填写请假原因')
    return
  }
  submitting.value = true
  try {
    await studentApi.applyLeave(form.taskId, { reason: form.reason, proofUrl: form.proofUrl })
    ElMessage.success('请假申请已提交，等待教师审批')
    form.taskId = undefined
    form.reason = ''
    form.proofUrl = ''
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="请假申请" subtitle="选择签到任务提交请假，教师审批通过后自动登记请假记录" eyebrow="LEAVE REQUEST" />

    <div class="grid grid-main-side">
      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">发起申请</div>
          <span class="panel-sub">任务结束后 24 小时内仍可提交</span>
        </div>
        <el-form label-position="top">
          <el-form-item label="对应签到任务" required>
            <el-select v-model="form.taskId" placeholder="选择需要请假的签到任务" style="width: 100%" filterable>
              <el-option
                v-for="task in availableTasks"
                :key="task.id"
                :label="`${task.title} · ${formatDateTime(task.startTime)}`"
                :value="task.id"
              />
            </el-select>
            <div v-if="!availableTasks.length" class="muted hint">暂无可申请请假的任务（已签到或已关闭的任务不可申请）</div>
          </el-form-item>
          <el-form-item label="请假原因" required>
            <el-input v-model="form.reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请如实说明请假原因" />
          </el-form-item>
          <el-form-item label="证明材料（可选）">
            <el-upload :show-file-list="false" :http-request="uploadProof" accept="image/*,.pdf">
              <el-button :loading="uploading"><el-icon><Upload /></el-icon> 上传图片或 PDF</el-button>
            </el-upload>
            <span v-if="form.proofUrl" class="proof-ok"><el-icon><CircleCheck /></el-icon> 已上传</span>
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">提交请假申请</el-button>
        </el-form>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div class="panel-title">审批进度</div>
          <span class="panel-sub">{{ leaves.length }} 条</span>
        </div>
        <div v-if="loading" class="col" style="gap: 10px">
          <div v-for="i in 4" :key="i" class="skeleton" style="height: 88px" />
        </div>
        <EmptyState v-else-if="!leaves.length" title="暂无请假记录" icon="Document" />
        <div v-else class="leave-list">
          <div v-for="item in leaves" :key="item.id" class="leave-item">
            <div class="row-between">
              <span class="leave-title">{{ item.taskTitle }}</span>
              <StatusTag :status="item.status" />
            </div>
            <div class="leave-course">{{ item.courseName }} · {{ formatDateTime(item.startTime) }}</div>
            <div class="leave-reason">{{ item.reason }}</div>
            <div v-if="item.remark" class="leave-remark">审批备注：{{ item.remark }}</div>
            <div class="leave-foot">
              <span class="mono">{{ formatDateTime(item.createTime) }}</span>
              <span v-if="item.reviewTime" class="mono">审批 {{ formatDateTime(item.reviewTime) }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.hint { margin-top: 6px; font-size: 11px; }
.proof-ok { display: inline-flex; align-items: center; gap: 5px; color: var(--green); font-size: 12px; margin-left: 10px; }
.leave-list { display: flex; flex-direction: column; gap: 12px; }
.leave-item {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 13px 14px;
  background: var(--panel-3);
}
.leave-title { font-size: 13px; font-weight: 500; }
.leave-course { font-size: 11px; color: var(--muted); margin-top: 3px; }
.leave-reason { font-size: 12.5px; color: var(--text-2); margin-top: 8px; line-height: 1.7; }
.leave-remark { font-size: 11.5px; color: var(--amber); margin-top: 6px; }
.leave-foot {
  display: flex;
  gap: 14px;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--muted);
  margin-top: 10px;
}
</style>
