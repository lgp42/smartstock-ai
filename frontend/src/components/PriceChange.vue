<template>
  <span
    class="font-mono inline-flex items-center"
    :class="[sizeClass, colorClass]"
  >
    <span v-if="showBadge" class="px-2 py-0.5 rounded-md" :class="bgClass">
      {{ displayValue }}
    </span>
    <span v-else>{{ displayValue }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  value: number
  format?: 'percent' | 'amount'
  size?: 'xs' | 'sm' | 'base'
  showBadge?: boolean
}>(), {
  format: 'percent',
  size: 'xs',
  showBadge: false,
})

const num = computed(() => Number(props.value || 0))

const displayValue = computed(() => {
  const sign = num.value >= 0 ? '+' : ''
  if (props.format === 'percent') return `${sign}${num.value.toFixed(2)}%`
  return `${sign}${num.value.toFixed(2)}`
})

const colorClass = computed(() => {
  if (num.value > 0) return 'text-upPrice'
  if (num.value < 0) return 'text-downPrice'
  return 'text-slate-400'
})

const bgClass = computed(() => {
  if (num.value > 0) return 'bg-upPrice/8'
  if (num.value < 0) return 'bg-downPrice/8'
  return 'bg-slate-800/50'
})

const sizeClass = computed(() => {
  switch (props.size) {
    case 'xs': return 'text-xs'
    case 'sm': return 'text-sm'
    case 'base': return 'text-base'
  }
})
</script>
