<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: string
  }>(),
  { title: '暂无数据', description: '', icon: 'Box' }
)
</script>

<template>
  <div class="empty-state">
    <el-icon class="icon"><component :is="icon" /></el-icon>
    <div class="title">{{ title }}</div>
    <div v-if="description" class="desc">{{ description }}</div>
    <slot name="action" />
  </div>
</template>

<style scoped>
.desc { font-size: 12px; color: var(--muted); max-width: 320px; }
</style>
