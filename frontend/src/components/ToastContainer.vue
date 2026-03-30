<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto flex items-center gap-3 rounded-xl border px-4 py-3 shadow-2xl backdrop-blur-xl min-w-[280px] max-w-[400px]"
          :class="toastClass(toast.type)"
        >
          <div class="shrink-0 w-5 h-5 flex items-center justify-center">
            <svg v-if="toast.type === 'success'" class="w-4 h-4" viewBox="0 0 16 16" fill="none"><path d="M3 8.5L6.5 12L13 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            <svg v-else-if="toast.type === 'error'" class="w-4 h-4" viewBox="0 0 16 16" fill="none"><path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            <svg v-else-if="toast.type === 'warning'" class="w-4 h-4" viewBox="0 0 16 16" fill="none"><path d="M8 3v6M8 12v1" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            <svg v-else class="w-4 h-4" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="5" stroke="currentColor" stroke-width="1.5"/><path d="M8 6v4M8 11.5v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          </div>
          <span class="text-sm font-medium flex-1">{{ toast.message }}</span>
          <button @click="removeToast(toast.id)" class="shrink-0 text-current opacity-50 hover:opacity-100 transition-opacity text-lg leading-none">&times;</button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useToastStore } from '../stores/toast'
import type { ToastType } from '../types'

const toastStore = useToastStore()
const toasts = computed(() => toastStore.toasts)
const removeToast = (id: number) => toastStore.remove(id)

const toastClass = (type: ToastType) => {
  switch (type) {
    case 'success': return 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
    case 'error': return 'bg-red-500/10 border-red-500/30 text-red-400'
    case 'warning': return 'bg-amber-500/10 border-amber-500/30 text-amber-400'
    case 'info': return 'bg-cyan-500/10 border-cyan-500/30 text-cyan-400'
  }
}
</script>

<style scoped>
.toast-enter-active {
  transition: all 0.3s ease-out;
}
.toast-leave-active {
  transition: all 0.2s ease-in;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
.toast-move {
  transition: transform 0.2s;
}
</style>
