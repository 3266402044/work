<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'

const list = ref<any[]>([])
const total = ref(0)
const loading = ref(true)
const page = ref(1)
const size = ref(20)
const filters = reactive({ module: 'ALL', userId: undefined as number | undefined, range: [] as string[] })

const modules = ['ALL', 'AUTH', 'TASK', 'SIGN', 'LEAVE', 'ROLL_CALL', 'QUIZ', 'RACE', 'ADMIN', 'ANNOUNCEMENT']

const load = async () => {
  loading.value = true
  try {
    const data = await adminApi.logs({
      page: page.value,
      size: size.value,
      module: filters.module,
      userId: filters.userId,
      from: filters.range?.[0],
      to: filters.range?.[1]
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="系统日志" subtitle="按用户、模块和日期检索关键操作审计记录" eyebrow="AUDIT LOG">
      <template #actions><el-button :loading="loading" @click="load">刷新</el-button></template>
    </PageHeader>

    <section class="panel filters">
      <div class="filter-row">
        <el-select v-model="filters.module" style="width: 150px" @change="search">
          <el-option v-for="item in modules" :key="item" :label="item === 'ALL' ? '全部模块' : item" :value="item" />
        </el-select>
        <el-input-number v-model="filters.userId" :min="1" :controls="false" placeholder="用户ID" style="width: 130px" @change="search" />
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
          @change="search"
        />
        <span class="muted total">共 {{ total }} 条</span>
      </div>
    </section>

    <section class="panel section-gap">
      <div v-if="loading" class="col" style="gap: 8px">
        <div v-for="i in 8" :key="i" class="skeleton" style="height: 42px" />
      </div>
      <EmptyState v-else-if="!list.length" title="暂无日志" icon="Monitor" />
      <template v-else>
        <el-table :data="list" size="small" max-height="560">
          <el-table-column label="时间" width="170">
            <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="用户ID" width="80" prop="userId" />
          <el-table-column label="模块" width="110">
            <template #default="{ row }"><span class="chip">{{ row.module }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="140" prop="action" />
          <el-table-column label="目标ID" width="90" prop="targetId" />
          <el-table-column label="详情" min-width="240">
            <template #default="{ row }"><span class="text-2">{{ row.detail }}</span></template>
          </el-table-column>
          <el-table-column label="IP" width="130">
            <template #default="{ row }"><span class="mono muted">{{ row.ip }}</span></template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="load"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.filters { padding: 16px 20px; }
.filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.total { margin-left: auto; font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
@media (max-width: 760px) { .total { margin-left: 0; } .pager { justify-content: center; } }
</style>
