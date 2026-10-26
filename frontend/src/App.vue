<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { connectRealtime, disconnectRealtime, restoreStudentPrompts } from '@/composables/realtime'
import StudentOverlay from '@/components/StudentOverlay.vue'

const auth = useAuthStore()
const ui = useUiStore()

watch(
  () => auth.token,
  (token) => {
    if (token) {
      connectRealtime()
      ui.refreshUnread()
      restoreStudentPrompts()
    } else {
      disconnectRealtime()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (auth.token) {
    connectRealtime()
    ui.refreshUnread()
    restoreStudentPrompts()
  }
})
</script>

<template>
  <router-view />
  <StudentOverlay v-if="auth.isLoggedIn && auth.role === 'STUDENT'" />
</template>
