<template>
  <div class="flex h-full flex-col gap-4">
    <div class="glass-panel flex items-center justify-between px-5 py-3.5">
      <div class="flex items-center gap-3">
        <span class="h-5 w-1 rounded-full bg-gradient-to-b from-primary to-accent"></span>
        <h2 class="font-display text-base font-bold text-white">策略回测</h2>
        <span class="tag-badge border-primary/20 bg-primary/5 text-primary/80">买入持有</span>
      </div>
      <button class="btn-primary text-xs" :disabled="loading" @click="runBacktest">
        {{ loading ? '回测中' : '运行回测' }}
      </button>
    </div>

    <div class="grid min-h-0 flex-1 gap-4 xl:grid-cols-[320px_minmax(0,1fr)]">
      <section class="glass-panel p-5">
        <div class="space-y-5">
          <label class="block">
            <span class="data-label mb-2 block">股票代码</span>
            <input v-model="form.stockCode" class="cyber-input w-full" placeholder="600519" />
          </label>
          <label class="block">
            <span class="data-label mb-2 block">初始资金</span>
            <input v-model.number="form.initialCapital" type="number" class="cyber-input w-full" />
          </label>
          <label class="block">
            <span class="data-label mb-2 block">K 线数量</span>
            <input v-model.number="form.limit" type="number" min="2" max="500" class="cyber-input w-full" />
          </label>
        </div>
      </section>

      <section class="grid content-start gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div v-for="item in metrics" :key="item.label" class="stat-card min-h-[128px]">
          <div class="data-label">{{ item.label }}</div>
          <div class="mt-3 font-mono text-2xl font-black" :class="item.cls">{{ item.value }}</div>
        </div>
        <div class="glass-panel p-5 md:col-span-2 xl:col-span-4">
          <div class="data-label mb-3">说明</div>
          <p class="text-sm leading-7 text-slate-500">
            当前为轻量买入持有回测：以第一根 K 线收盘价买入，最后一根 K 线收盘价卖出，并基于期间净值计算最大回撤。
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import request from '../utils/request'
import type { BacktestResultVO, BacktestRunDTO } from '../types'

const loading = ref(false)
const result = ref<BacktestResultVO | null>(null)
const form = reactive<BacktestRunDTO>({
  stockCode: '600519',
  initialCapital: 100000,
  limit: 120
})

const runBacktest = async () => {
  loading.value = true
  try {
    result.value = await request.post<BacktestResultVO>('/backtest/run', form)
  } finally {
    loading.value = false
  }
}

const money = (value?: number) => `¥${Number(value || 0).toFixed(2)}`
const percent = (value?: number) => `${Number(value || 0).toFixed(4)}%`

const metrics = computed(() => [
  { label: '初始资金', value: money(result.value?.initialCapital), cls: 'text-slate-100' },
  { label: '最终资产', value: money(result.value?.finalCapital), cls: 'text-primary' },
  { label: '总收益', value: money(result.value?.totalReturn), cls: Number(result.value?.totalReturn || 0) >= 0 ? 'text-upPrice' : 'text-downPrice' },
  { label: '收益率 / 回撤', value: `${percent(result.value?.returnRate)} / ${percent(result.value?.maxDrawdown)}`, cls: 'text-accent' },
])
</script>
