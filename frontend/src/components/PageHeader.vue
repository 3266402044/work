<script setup lang="ts">
defineProps<{ title: string; subtitle?: string; eyebrow?: string }>()
</script>

<template>
  <div class="page-header">
    <div class="left">
      <div v-if="eyebrow" class="eyebrow">{{ eyebrow }}</div>
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.eyebrow {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--cyan);
  text-transform: uppercase;
  margin-bottom: 6px;
}
h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
  letter-spacing: -0.015em;
}
p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
}
.actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
@media (max-width: 760px) {
  .page-header { flex-direction: column; align-items: flex-start; }
  h1 { font-size: 20px; }
}
</style>
