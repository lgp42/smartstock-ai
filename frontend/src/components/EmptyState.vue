<template>
  <div class="flex flex-col items-center justify-center py-16 text-center">
    <div class="w-12 h-12 rounded-2xl border border-darkBorder bg-darkCard/50 flex items-center justify-center mb-4">
      <slot name="icon">
        <svg class="w-5 h-5 text-slate-600" viewBox="0 0 20 20" fill="none">
          <rect x="2" y="4" width="16" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/>
          <path d="M6 9h8M6 12h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </slot>
    </div>
    <p class="text-sm text-slate-600 font-mono">{{ message }}</p>
    <p v-if="hint" class="text-xs text-slate-700 mt-1">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message: string
  hint?: string
}>()
</script>
