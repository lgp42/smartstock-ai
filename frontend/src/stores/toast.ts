import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ToastMessage, ToastType } from '../types'

let nextId = 0

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastMessage[]>([])

  const add = (type: ToastType, message: string, duration = 3000) => {
    const id = nextId++
    toasts.value.push({ id, type, message, duration })
    if (duration > 0) {
      setTimeout(() => remove(id), duration)
    }
  }

  const remove = (id: number) => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  const success = (message: string, duration?: number) => add('success', message, duration)
  const error = (message: string, duration?: number) => add('error', message, duration ?? 5000)
  const warning = (message: string, duration?: number) => add('warning', message, duration)
  const info = (message: string, duration?: number) => add('info', message, duration)

  return { toasts, add, remove, success, error, warning, info }
})
