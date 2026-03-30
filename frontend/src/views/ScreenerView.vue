<template>
  <div class="h-full flex flex-col gap-4">
    <!-- Header bar -->
    <div class="glass-panel px-5 py-3.5 shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <span class="w-1 h-4 bg-primary rounded-full"></span>
        <h2 class="font-display font-bold text-base text-white">智能选股助手</h2>
        <span class="tag-badge">AI 驱动</span>
      </div>
      <div class="flex gap-2">
        <button @click="resetFilters" class="text-xs font-semibold text-slate-500 hover:text-slate-300 px-3 py-1.5 rounded-lg border border-darkBorder hover:border-slate-600 transition">
          重置条件
        </button>
        <button @click="runScreen" class="text-xs font-bold text-primary px-3 py-1.5 rounded-lg border border-primary/30 bg-primary/5 hover:bg-primary/10 transition">
          运行策略
        </button>
      </div>
    </div>

    <div class="flex-1 flex gap-4 min-h-0">
      <!-- Filter sidebar -->
      <div class="w-56 glass-panel flex flex-col min-h-0 overflow-y-auto p-4 shrink-0">
        <div class="data-label mb-4">筛选条件</div>

        <div class="space-y-5">
          <!-- Board -->
          <div>
            <div class="data-label mb-2.5">上市板块</div>
            <div class="space-y-1.5">
              <label v-for="(label, val) in { sh_main: '沪市主板', sz_main: '深市主板', cyb: '创业板', star: '科创板' }"
                :key="val"
                class="flex items-center gap-2.5 cursor-pointer group"
              >
                <div class="relative w-4 h-4 shrink-0">
                  <input v-model="filters.boards" :value="val" type="checkbox"
                    class="absolute inset-0 opacity-0 cursor-pointer peer" />
                  <div class="w-4 h-4 rounded border border-darkBorder bg-darkBg peer-checked:bg-primary/20 peer-checked:border-primary/60 transition-all flex items-center justify-center">
                    <svg v-if="filters.boards.includes(val)" class="w-2.5 h-2.5 text-primary" viewBox="0 0 10 10" fill="none">
                      <path d="M1.5 5L4 7.5L8.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                </div>
                <span class="text-sm text-slate-500 group-hover:text-slate-300 transition-colors">{{ label }}</span>
              </label>
            </div>
          </div>

          <div class="border-t border-darkBorder/40"></div>

          <!-- Industry -->
          <div>
            <div class="data-label mb-2">行业 (申万一级)</div>
            <select v-model="filters.industryGroup" class="cyber-input w-full text-xs h-8 py-0">
              <option value="all">全行业</option>
              <option value="tech">计算机与半导体</option>
              <option value="finance">金融地产</option>
              <option value="consumer">大消费</option>
              <option value="energy">传统能源</option>
              <option value="ev">新能源汽车</option>
            </select>
          </div>

          <!-- Market cap -->
          <div>
            <div class="data-label mb-2">市值范围 (亿)</div>
            <div class="flex items-center gap-1.5">
              <input v-model="filters.minMarketCap" type="number" placeholder="最小" class="cyber-input w-full text-xs h-8 py-0 text-center" />
              <span class="text-slate-600 text-xs shrink-0">—</span>
              <input v-model="filters.maxMarketCap" type="number" placeholder="最大" class="cyber-input w-full text-xs h-8 py-0 text-center" />
            </div>
          </div>

          <!-- PE -->
          <div>
            <div class="data-label mb-2">市盈率 PE-TTM</div>
            <div class="flex items-center gap-1.5">
              <input v-model="filters.minPe" type="number" placeholder="0" class="cyber-input w-full text-xs h-8 py-0 text-center" />
              <span class="text-slate-600 text-xs shrink-0">—</span>
              <input v-model="filters.maxPe" type="number" placeholder="100" class="cyber-input w-full text-xs h-8 py-0 text-center" />
            </div>
          </div>

          <!-- Technical pattern -->
          <div>
            <div class="data-label mb-2">技术形态</div>
            <select v-model="filters.technicalPattern" class="cyber-input w-full text-xs h-8 py-0">
              <option value="none">无指定形态</option>
              <option value="ma_cross">均线多头排列</option>
              <option value="macd_golden">MACD 金叉</option>
              <option value="volume_up">底部放量</option>
              <option value="breakout">突破近期新高</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Results table -->
      <div class="flex-1 glass-panel flex flex-col min-h-0 relative overflow-hidden">
        <div class="px-4 py-3 border-b border-darkBorder/50 flex items-center justify-between shrink-0">
          <div class="flex items-center gap-2 text-xs">
            <span class="text-slate-500">筛选结果</span>
            <span class="font-mono font-bold text-primary">{{ results.length }}</span>
            <span class="text-slate-600">只标的</span>
          </div>
          <button @click="exportCsv" class="text-[10px] font-bold text-slate-500 hover:text-primary transition font-mono uppercase tracking-widest">
            导出 CSV ↓
          </button>
        </div>

        <div class="flex-1 overflow-auto relative">
          <!-- Loading overlay -->
          <div v-if="loading" class="absolute inset-0 bg-darkBg/70 z-20 flex items-center justify-center backdrop-blur-sm">
            <div class="flex flex-col items-center gap-3">
              <div class="w-5 h-5 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
              <span class="text-[10px] text-slate-500 font-mono tracking-widest">SCANNING</span>
            </div>
          </div>

          <table class="w-full text-left border-collapse whitespace-nowrap">
            <thead class="sticky top-0 bg-darkBg/95 backdrop-blur z-10 text-[10px] font-semibold text-slate-600 uppercase tracking-[0.15em] font-mono">
              <tr>
                <th class="px-3 py-2.5">代码</th>
                <th class="px-3 py-2.5">名称</th>
                <th class="px-3 py-2.5 text-right">现价</th>
                <th class="px-3 py-2.5 text-right">涨跌幅</th>
                <th class="px-3 py-2.5 text-right">换手率</th>
                <th class="px-3 py-2.5 text-right">PE</th>
                <th class="px-3 py-2.5 text-right">总市值(亿)</th>
                <th class="px-3 py-2.5 text-center">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in results"
                :key="item.stockCode"
                class="border-b border-darkBorder/30 hover:bg-primary/3 transition-colors group/row"
              >
                <td class="px-3 py-2.5 font-mono font-bold text-white text-sm">{{ item.stockCode }}</td>
                <td class="px-3 py-2.5 text-slate-400 text-sm">{{ item.stockName }}</td>
                <td class="px-3 py-2.5 text-right font-mono text-sm font-semibold" :class="item.changeRate > 0 ? 'text-upPrice' : 'text-downPrice'">
                  {{ Number(item.currentPrice || 0).toFixed(2) }}
                </td>
                <td class="px-3 py-2.5 text-right font-mono text-xs">
                  <span class="px-2 py-0.5 rounded-md" :class="item.changeRate > 0 ? 'bg-upPrice/8 text-upPrice' : 'bg-downPrice/8 text-downPrice'">
                    {{ Number(item.changeRate || 0) > 0 ? '+' : '' }}{{ Number(item.changeRate || 0).toFixed(2) }}%
                  </span>
                </td>
                <td class="px-3 py-2.5 text-right font-mono text-xs text-slate-500">{{ Number(item.turnoverRate || 0).toFixed(2) }}%</td>
                <td class="px-3 py-2.5 text-right font-mono text-xs text-slate-500">{{ Number(item.pe || 0).toFixed(1) }}</td>
                <td class="px-3 py-2.5 text-right font-mono text-xs text-slate-500">{{ Number(item.totalMarketCap || 0).toFixed(1) }}</td>
                <td class="px-3 py-2.5 text-center">
                  <div class="flex items-center justify-center gap-1.5">
                    <button @click.stop="$router.push(`/terminal/${item.stockCode}`)"
                      class="text-[10px] font-bold text-primary px-2.5 py-1 rounded-lg border border-primary/25 bg-primary/5 hover:bg-primary/15 transition font-mono">
                      终端
                    </button>
                    <button @click.stop="$router.push(`/copilot/${item.stockCode}`)"
                      class="text-[10px] font-bold text-accent px-2.5 py-1 rounded-lg border border-accent/25 bg-accent/5 hover:bg-accent/15 transition font-mono">
                      AI
                    </button>
                    <button @click.stop="addToWatchlist(item.stockCode)"
                      class="text-[10px] font-bold text-slate-400 px-2.5 py-1 rounded-lg border border-darkBorder hover:border-slate-500 hover:text-slate-200 transition font-mono">
                      自选
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="results.length === 0 && !loading">
                <td colspan="8" class="py-16 text-center text-slate-600 text-sm font-mono">
                  没有找到符合条件的股票 · 请尝试放宽筛选条件
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import request from '../utils/request'
import { useToastStore } from '../stores/toast'
import type { ScreenerResultVO } from '../types'

const loading = ref(false)
const toast = useToastStore()
const results = ref<ScreenerResultVO[]>([])
let screenTimer: ReturnType<typeof setTimeout> | null = null
const filters = reactive({
  boards: ['sh_main', 'sz_main'],
  industryGroup: 'all',
  minMarketCap: '',
  maxMarketCap: '',
  minPe: '',
  maxPe: '',
  technicalPattern: 'none'
})

const runScreen = async () => {
  loading.value = true
  try {
    const data = await request.get('/market/screener', {
      params: {
        boards: filters.boards.join(','),
        industryGroup: filters.industryGroup,
        minMarketCap: filters.minMarketCap || undefined,
        maxMarketCap: filters.maxMarketCap || undefined,
        minPe: filters.minPe || undefined,
        maxPe: filters.maxPe || undefined,
        technicalPattern: filters.technicalPattern
      }
    })
    results.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  filters.boards = ['sh_main', 'sz_main']
  filters.industryGroup = 'all'
  filters.minMarketCap = ''
  filters.maxMarketCap = ''
  filters.minPe = ''
  filters.maxPe = ''
  filters.technicalPattern = 'none'
}

const addToWatchlist = async (stockCode: string) => {
  try {
    await request.post('/watchlist', { stockCode })
    toast.success(`已添加 ${stockCode} 到自选股`)
  } catch (error: any) {
    toast.error(error.message || '添加自选股失败')
  }
}

const exportCsv = () => {
  if (results.value.length === 0) {
    return
  }
  const header = ['代码', '名称', '现价', '涨跌幅', '换手率', '市盈率', '总市值(亿)']
  const rows = results.value.map(item => [
    item.stockCode,
    item.stockName,
    Number(item.currentPrice || 0).toFixed(2),
    Number(item.changeRate || 0).toFixed(2),
    Number(item.turnoverRate || 0).toFixed(2),
    Number(item.pe || 0).toFixed(2),
    Number(item.totalMarketCap || 0).toFixed(2)
  ])
  const csv = [header, ...rows].map(row => row.join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'smartstock-screener.csv'
  link.click()
  URL.revokeObjectURL(url)
}

const scheduleRunScreen = () => {
  if (screenTimer) {
    clearTimeout(screenTimer)
  }
  screenTimer = setTimeout(() => {
    runScreen().catch(console.error)
  }, 250)
}

watch(
  () => [
    filters.boards.join(','),
    filters.industryGroup,
    filters.minMarketCap,
    filters.maxMarketCap,
    filters.minPe,
    filters.maxPe,
    filters.technicalPattern
  ],
  () => {
    scheduleRunScreen()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (screenTimer) {
    clearTimeout(screenTimer)
  }
})
</script>
