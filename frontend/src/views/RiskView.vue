<template>
  <div class="flex h-full flex-col gap-4">
    <div class="glass-panel flex items-center justify-between px-5 py-3.5">
      <div class="flex items-center gap-3">
        <span class="h-5 w-1 rounded-full bg-gradient-to-b from-red-400 to-primary"></span>
        <h2 class="font-display text-base font-bold text-white">风险预警</h2>
        <span class="tag-badge border-red-400/20 bg-red-400/5 text-red-300">实时持仓</span>
      </div>
      <button class="btn-primary text-xs" :disabled="loading" @click="fetchAlerts">
        {{ loading ? '扫描中' : '重新扫描' }}
      </button>
    </div>

    <div class="grid min-h-0 flex-1 gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
      <section class="glass-panel flex flex-col justify-center p-5">
        <div class="data-label">当前风险数</div>
        <div class="mt-3 font-mono text-5xl font-black text-white">{{ alerts.length }}</div>
        <div class="mt-4 text-sm leading-6 text-slate-500">
          系统根据持仓集中度和持仓回撤生成轻量预警，用于辅助复盘，不替代投资决策。
        </div>
      </section>

      <section class="glass-panel min-h-0 overflow-auto">
        <table class="w-full whitespace-nowrap text-left">
          <thead class="sticky top-0 bg-darkBg/95 text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600">
            <tr>
              <th class="px-4 py-3">股票</th>
              <th class="px-4 py-3">类型</th>
              <th class="px-4 py-3">级别</th>
              <th class="px-4 py-3 text-right">数值</th>
              <th class="px-4 py-3">说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in alerts" :key="`${item.alertType}-${item.stockCode}`" class="border-t border-darkBorder/40">
              <td class="px-4 py-3 font-mono text-sm font-bold text-white">{{ item.stockCode }}</td>
              <td class="px-4 py-3 text-sm text-slate-300">{{ typeLabel(item.alertType) }}</td>
              <td class="px-4 py-3">
                <span class="rounded-md border px-2 py-1 text-[10px] font-bold uppercase tracking-widest" :class="levelClass(item.alertLevel)">
                  {{ item.alertLevel }}
                </span>
              </td>
              <td class="px-4 py-3 text-right font-mono text-sm text-primary">{{ Number(item.value || 0).toFixed(2) }}%</td>
              <td class="px-4 py-3 text-sm text-slate-500">{{ item.message }}</td>
            </tr>
            <tr v-if="alerts.length === 0 && !loading">
              <td colspan="5" class="py-16 text-center font-mono text-sm text-slate-600">暂无风险预警</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import request from '../utils/request'
import type { RiskAlertVO } from '../types'

const loading = ref(false)
const alerts = ref<RiskAlertVO[]>([])

const fetchAlerts = async () => {
  loading.value = true
  try {
    const data = await request.get<RiskAlertVO[]>('/risk/alerts')
    alerts.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

const typeLabel = (type: string) => {
  if (type === 'concentration') return '持仓集中'
  if (type === 'drawdown') return '回撤预警'
  return type
}

const levelClass = (level: string) => {
  if (level === 'high') return 'border-red-400/30 bg-red-400/10 text-red-300'
  return 'border-yellow-400/30 bg-yellow-400/10 text-yellow-300'
}

onMounted(fetchAlerts)
</script>
