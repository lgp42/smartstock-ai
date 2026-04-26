<template>
  <div class="h-full flex flex-col gap-4 md:gap-6">
    <!-- Top KPI Cards -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4 shrink-0">
      <!-- Index Card 1 -->
      <div class="stat-card hover-lift">
        <div class="data-label mb-1.5 flex items-center gap-1.5">
          <span class="live-dot"></span>{{ marketCards[0].stockName }}
        </div>
        <div class="font-mono text-xl md:text-2xl font-bold flex items-baseline gap-2">
          {{ formatIndexValue(marketCards[0].currentPrice) }}
          <span class="text-xs md:text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[0].changeRate)">{{ formatChangeRate(marketCards[0].changeRate) }}</span>
        </div>
        <div class="text-[11px] text-slate-600 font-mono mt-1">{{ marketCards[0].stockCode }}</div>
      </div>

      <!-- Index Card 2 -->
      <div class="stat-card hover-lift">
        <div class="data-label mb-1.5 flex items-center gap-1.5">
          <span class="live-dot bg-blue-400 shadow-[0_0_8px_rgba(96,165,250,0.8)]"></span>{{ marketCards[1].stockName }}
        </div>
        <div class="font-mono text-xl md:text-2xl font-bold flex items-baseline gap-2">
          {{ formatIndexValue(marketCards[1].currentPrice) }}
          <span class="text-xs md:text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[1].changeRate)">{{ formatChangeRate(marketCards[1].changeRate) }}</span>
        </div>
        <div class="text-[11px] text-slate-600 font-mono mt-1">{{ marketCards[1].stockCode }}</div>
      </div>

      <!-- Index Card 3 -->
      <div class="stat-card hover-lift">
        <div class="data-label mb-1.5 flex items-center gap-1.5">
          <span class="live-dot bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]"></span>{{ marketCards[2].stockName }}
        </div>
        <div class="font-mono text-xl md:text-2xl font-bold flex items-baseline gap-2">
          {{ formatIndexValue(marketCards[2].currentPrice) }}
          <span class="text-xs md:text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[2].changeRate)">{{ formatChangeRate(marketCards[2].changeRate) }}</span>
        </div>
        <div class="text-[11px] text-slate-600 font-mono mt-1">{{ marketCards[2].stockCode }}</div>
      </div>

      <!-- Index Card 4 — highlighted -->
      <div class="stat-card hover-lift col-span-2 lg:col-span-1 border-t-2 border-t-primary/50">
        <div class="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent pointer-events-none"></div>
        <div class="absolute -right-8 -top-8 w-32 h-32 rounded-full bg-primary/8 blur-3xl pointer-events-none"></div>
        <div class="relative z-10">
          <div class="data-label text-primary/70 mb-1.5 flex items-center gap-1.5">
            <span class="live-dot mr-0.5"></span>{{ marketCards[3].stockName }}
          </div>
          <div class="font-mono text-2xl md:text-3xl font-bold tracking-tight font-display flex items-baseline gap-2">
            {{ formatIndexValue(marketCards[3].currentPrice) }}
            <span class="text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[3].changeRate)">{{ formatChangeRate(marketCards[3].changeRate) }}</span>
          </div>
          <div class="mt-2.5 tag-badge">
            <span class="live-dot mr-1.5"></span>
            {{ syncLabel }}
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col lg:flex-row gap-4 md:gap-6 min-h-0">
      <!-- Watchlist -->
      <div class="flex-1 lg:flex-[2] glass-panel flex flex-col min-h-0 relative">
        <div class="p-4 border-b border-darkBorder/50 flex justify-between items-center shrink-0">
          <h3 class="font-display font-bold text-base flex items-center gap-2">
            <span class="w-1 h-5 bg-gradient-to-b from-primary to-primary/30 rounded-full"></span>
            自选股票池
          </h3>
          <div class="flex gap-2">
            <button
              v-if="selectedCodes.length > 0"
              class="text-xs font-semibold text-red-300 hover:text-red-200 px-3 py-1.5 rounded-lg border border-red-400/30 hover:border-red-400/60 transition"
              @click="removeSelected"
            >
              批量移除 {{ selectedCodes.length }}
            </button>
            <button
              class="text-xs font-semibold text-slate-500 hover:text-slate-300 px-3 py-1.5 rounded-lg border border-darkBorder hover:border-slate-600 transition"
              @click="toggleSort"
            >
              {{ sortLabel }}
            </button>
            <button
              class="relative text-xs font-bold text-primary px-3 py-1.5 rounded-lg border border-primary/30 bg-primary/5 hover:bg-primary/10 transition"
              @click="$router.push('/screener')"
            >
              <span class="absolute -top-0.5 -right-0.5 w-1.5 h-1.5 bg-primary rounded-full animate-ping"></span>
              智能选股
            </button>
          </div>
        </div>

        <div class="flex-1 overflow-auto p-2 group">
          <table class="w-full text-left border-collapse whitespace-nowrap">
            <thead class="sticky top-0 bg-darkBg/95 backdrop-blur z-10 text-[10px] font-semibold text-slate-600 uppercase tracking-[0.15em] font-mono">
              <tr>
                <th class="p-3 w-10">
                  <input
                    type="checkbox"
                    class="h-3.5 w-3.5 rounded border-darkBorder bg-darkBg accent-cyan-400"
                    :checked="allVisibleSelected"
                    @change="toggleSelectAll"
                  />
                </th>
                <th class="p-3">代码</th>
                <th class="p-3 hidden md:table-cell">股票名称</th>
                <th class="p-3 text-right">最新价</th>
                <th class="p-3 text-right">涨跌幅</th>
                <th class="p-3 text-center">操作</th>
              </tr>
            </thead>
            <tbody class="text-sm">
              <tr
                v-for="item in displayWatchlist"
                :key="item.stockCode"
                @click="goToTerminal(item.stockCode)"
                class="border-b border-darkBorder/30 hover:bg-primary/[0.03] transition-colors cursor-pointer group/row"
              >
                <td class="p-3" @click.stop>
                  <input
                    v-model="selectedCodes"
                    :value="item.stockCode"
                    type="checkbox"
                    class="h-3.5 w-3.5 rounded border-darkBorder bg-darkBg accent-cyan-400"
                  />
                </td>
                <td class="p-3">
                  <div class="flex items-center gap-2.5">
                    <div class="hidden sm:flex w-8 h-8 rounded-xl border border-darkBorder/60 bg-darkBg/80 items-center justify-center text-[10px] font-mono font-bold text-slate-500 shrink-0">
                      {{ item.stockCode.substring(0, 2) }}
                    </div>
                    <div>
                      <span class="font-mono font-bold text-white text-sm">{{ item.stockCode }}</span>
                      <div class="md:hidden text-[11px] text-slate-500 mt-0.5">{{ item.stockName }}</div>
                    </div>
                  </div>
                </td>
                <td class="p-3 hidden md:table-cell text-slate-400 text-sm">
                  {{ item.stockName }}
                </td>
                <td class="p-3 text-right font-mono text-sm font-semibold" :class="getPriceColorClass(item.changeRate)">
                  {{ Number(item.currentPrice || 0).toFixed(2) }}
                </td>
                <td class="p-3 text-right font-mono text-xs" :class="getPriceColorClass(item.changeRate)">
                  <span class="px-2 py-1 rounded-lg inline-block" :class="item.changeRate >= 0 ? 'bg-upPrice/8' : 'bg-downPrice/8'">
                    {{ item.changeRate >= 0 ? '+' : '' }}{{ Number(item.changeRate || 0).toFixed(2) }}%
                  </span>
                </td>
                <td class="p-3 text-center">
                  <div class="flex justify-center gap-1">
                    <button
                      v-if="sortMode === 'default'"
                      @click.stop="moveWatchlist(item.stockCode, -1)"
                      class="w-7 h-7 rounded-lg border border-darkBorder text-slate-600 hover:text-primary hover:border-primary/30 hover:bg-primary/8 flex items-center justify-center opacity-0 group-hover/row:opacity-100 transition-all text-xs"
                    >↑</button>
                    <button
                      v-if="sortMode === 'default'"
                      @click.stop="moveWatchlist(item.stockCode, 1)"
                      class="w-7 h-7 rounded-lg border border-darkBorder text-slate-600 hover:text-primary hover:border-primary/30 hover:bg-primary/8 flex items-center justify-center opacity-0 group-hover/row:opacity-100 transition-all text-xs"
                    >↓</button>
                    <button @click.stop="removeFromWatchlist(item.stockCode)" class="w-7 h-7 rounded-lg border border-darkBorder text-slate-600 hover:text-red-400 hover:border-red-500/30 hover:bg-red-500/8 flex items-center justify-center opacity-0 group-hover/row:opacity-100 transition-all text-sm">
                      ×
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="watchlist.length === 0">
                <td colspan="6" class="py-16 text-center">
                  <div class="flex flex-col items-center gap-3">
                    <div class="w-12 h-12 rounded-2xl border border-darkBorder/60 bg-darkBg/60 flex items-center justify-center">
                      <svg class="w-5 h-5 text-slate-600" viewBox="0 0 20 20" fill="none">
                        <path d="M10 4v12M4 10h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                      </svg>
                    </div>
                    <span class="text-sm text-slate-600 font-mono">自选股为空</span>
                    <span class="text-xs text-slate-700">在顶部搜索框搜索并添加股票</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right Panel: Intelligence -->
      <div class="lg:flex-1 flex flex-col gap-4 md:gap-6 min-h-0">
        <!-- AI Intelligence Module -->
        <div class="glass-panel p-4 shrink-0 border-l-2 border-l-downPrice/60 cursor-pointer hover:bg-darkCardHover/30 transition-all group hover-lift" @click="$router.push('/news')">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <span class="live-dot bg-downPrice shadow-[0_0_8px_rgba(16,185,129,0.8)]"></span>
              <span class="data-label text-downPrice/80">AI 交易情报</span>
            </div>
            <span class="text-[10px] text-slate-600 group-hover:text-primary transition font-mono">详情 →</span>
          </div>
          <p class="text-sm text-slate-400 leading-relaxed">
            算法分析显示科技板块存在强力买入动能。机构建仓迹象在 <span class="bg-primary/10 text-primary px-1.5 py-0.5 rounded font-mono text-xs">600519.SH</span> 中显著放大。
          </p>
        </div>

        <!-- Market News -->
        <div class="glass-panel p-4 flex-1 relative overflow-hidden flex flex-col min-h-[200px]">
          <div class="absolute top-0 right-0 w-40 h-40 bg-primary/3 rounded-bl-full blur-3xl pointer-events-none"></div>
          <div class="flex-1 flex flex-col min-h-0 relative z-10">
            <div class="data-label mb-3 shrink-0 flex items-center gap-2">
              市场快讯
              <span class="inline-block w-1.5 h-1.5 rounded-full bg-primary/60 animate-pulse"></span>
            </div>
            <div class="space-y-3 overflow-y-auto flex-1">
              <div
                v-for="(item, index) in marketNews"
                :key="`${item.source}-${item.publishTime}-${index}`"
                class="group/news border-l-2 pl-3 transition-all cursor-pointer py-1 rounded-r-lg hover:bg-white/[0.02]"
                :class="newsBorderClass(index)"
                @click="$router.push('/news')"
              >
                <div class="text-[10px] mb-1 font-mono" :class="newsTimeClass(index)">{{ item.publishTime }} · {{ item.source }}</div>
                <div class="text-sm text-slate-400 group-hover/news:text-slate-200 transition-colors line-clamp-2 leading-snug">{{ item.title }}</div>
              </div>
              <div v-if="marketNews.length === 0" class="text-xs text-slate-600 font-mono py-2">
                暂无市场快讯
              </div>
            </div>
          </div>
          <button @click="$router.push('/news')" class="w-full mt-3 border border-darkBorder text-slate-500 hover:text-primary hover:border-primary/30 py-2.5 rounded-xl text-[10px] font-bold transition uppercase tracking-widest font-mono shrink-0 relative z-10 hover:bg-primary/[0.03]">
            查看更多快讯 →
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useToastStore } from '../stores/toast'
import type { MarketSnapshotVO, WatchlistVO } from '../types'

const router = useRouter()
const toast = useToastStore()
const watchlist = ref<WatchlistVO[]>([])
const marketSnapshots = ref<MarketSnapshotVO[]>([])
const lastSyncTime = ref('')
const marketNews = ref<Array<{ publishTime: string; source: string; title: string; url: string }>>([])
const sortMode = ref<'default' | 'changeDesc' | 'changeAsc'>('default')
const selectedCodes = ref<string[]>([])
let pollTimer: number | undefined

const sortLabel = computed(() => {
  if (sortMode.value === 'changeDesc') {
    return '涨幅优先'
  }
  if (sortMode.value === 'changeAsc') {
    return '跌幅优先'
  }
  return '默认排序'
})

const syncLabel = computed(() => (lastSyncTime.value ? `1 秒刷新 · ${lastSyncTime.value}` : '1 秒刷新'))

const marketCards = computed(() => ([
  marketSnapshots.value[0] || { stockCode: '000001', stockName: '上证指数', currentPrice: 0, changeRate: 0 },
  marketSnapshots.value[1] || { stockCode: '399001', stockName: '深证成指', currentPrice: 0, changeRate: 0 },
  marketSnapshots.value[2] || { stockCode: '399006', stockName: '创业板指', currentPrice: 0, changeRate: 0 },
  marketSnapshots.value[3] || { stockCode: '000688', stockName: '科创50', currentPrice: 0, changeRate: 0 },
]))

const displayWatchlist = computed(() => {
  const list = [...watchlist.value]
  if (sortMode.value === 'changeDesc') {
    return list.sort((a, b) => Number(b.changeRate || 0) - Number(a.changeRate || 0))
  }
  if (sortMode.value === 'changeAsc') {
    return list.sort((a, b) => Number(a.changeRate || 0) - Number(b.changeRate || 0))
  }
  return list
})

const allVisibleSelected = computed(() => (
  displayWatchlist.value.length > 0
  && displayWatchlist.value.every(item => selectedCodes.value.includes(item.stockCode))
))

const formatIndexValue = (val: number) => {
  return Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatChangeRate = (val: number) => {
  const num = Number(val || 0)
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`
}

const getPriceColorClass = (changeRate: number) => {
  if (changeRate > 0) return 'text-upPrice'
  if (changeRate < 0) return 'text-downPrice'
  return 'text-slate-400'
}

const loadData = async () => {
  try {
    const [snapshots, wl, news] = await Promise.all([
      request.get('/market/snapshots').catch(() => []),
      request.get('/watchlist'),
      request.get('/news/flash', { params: { limit: 3 } }).catch(() => null)
    ])
    marketSnapshots.value = Array.isArray(snapshots) ? snapshots : []
    watchlist.value = wl as any[]
    selectedCodes.value = selectedCodes.value.filter(code =>
      watchlist.value.some(item => item.stockCode === code)
    )
    lastSyncTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    if (Array.isArray(news) && news.length > 0) {
      marketNews.value = news.map(item => ({
        publishTime: item.publishTime || '--:--',
        source: item.source || '快讯',
        title: item.title || item.summary || '暂无标题',
        url: item.url || ''
      }))
    }
  } catch (err) {
    console.error(err)
  }
}

const toggleSort = () => {
  if (sortMode.value === 'default') {
    sortMode.value = 'changeDesc'
    return
  }
  if (sortMode.value === 'changeDesc') {
    sortMode.value = 'changeAsc'
    return
  }
  sortMode.value = 'default'
}

const removeFromWatchlist = async (code: string) => {
  try {
    await request.delete(`/watchlist/${code}`)
    watchlist.value = watchlist.value.filter(item => item.stockCode !== code)
    selectedCodes.value = selectedCodes.value.filter(item => item !== code)
    toast.success('已从自选股移除')
  } catch (err) {
    console.error(err)
  }
}

const removeSelected = async () => {
  if (selectedCodes.value.length === 0) return
  try {
    await request.delete('/watchlist/batch', {
      data: { stockCodes: selectedCodes.value }
    })
    watchlist.value = watchlist.value.filter(item => !selectedCodes.value.includes(item.stockCode))
    selectedCodes.value = []
    toast.success('已批量移除自选股')
  } catch (error: any) {
    toast.error(error.message || '批量移除失败')
  }
}

const toggleSelectAll = () => {
  if (allVisibleSelected.value) {
    selectedCodes.value = selectedCodes.value.filter(code =>
      !displayWatchlist.value.some(item => item.stockCode === code)
    )
    return
  }
  selectedCodes.value = Array.from(new Set([
    ...selectedCodes.value,
    ...displayWatchlist.value.map(item => item.stockCode)
  ]))
}

const moveWatchlist = async (stockCode: string, delta: number) => {
  const index = watchlist.value.findIndex(item => item.stockCode === stockCode)
  const nextIndex = index + delta
  if (index < 0 || nextIndex < 0 || nextIndex >= watchlist.value.length) return
  const nextList = [...watchlist.value]
  const [item] = nextList.splice(index, 1)
  nextList.splice(nextIndex, 0, item)
  watchlist.value = nextList.map((row, rowIndex) => ({ ...row, sortOrder: rowIndex }))
  try {
    await request.put('/watchlist/sort', {
      items: watchlist.value.map((row, rowIndex) => ({
        stockCode: row.stockCode,
        sortOrder: rowIndex
      }))
    })
    toast.success('自选股排序已保存')
  } catch (error: any) {
    toast.error(error.message || '排序保存失败')
    await loadData()
  }
}

const goToTerminal = (code: string) => {
  router.push(`/terminal/${code}`)
}

const newsBorderClass = (index: number) => {
  if (index === 0) return 'border-primary/30 hover:border-primary/70'
  if (index === 1) return 'border-upPrice/20 hover:border-upPrice/50'
  return 'border-darkBorder/60 hover:border-slate-500/50'
}

const newsTimeClass = (index: number) => {
  if (index === 0) return 'text-primary/70'
  return 'text-slate-600'
}

onMounted(() => {
  loadData()
  pollTimer = window.setInterval(() => {
    loadData().catch(console.error)
  }, 1000)
})

onBeforeUnmount(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
  }
})
</script>
