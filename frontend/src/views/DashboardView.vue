<template>
  <div class="h-full flex flex-col space-y-6">
    <!-- Top KPI Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="glass-panel p-4 flex flex-col justify-center">
        <div class="data-label mb-1.5 flex items-center gap-1.5"><span class="live-dot"></span>{{ marketCards[0].stockName }}</div>
        <div class="font-mono text-2xl font-bold flex items-baseline gap-2">
          {{ formatIndexValue(marketCards[0].currentPrice) }}
          <span class="text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[0].changeRate)">{{ formatChangeRate(marketCards[0].changeRate) }}</span>
        </div>
        <div class="text-[11px] text-slate-600 font-mono mt-1">{{ marketCards[0].stockCode }}</div>
      </div>
      <div class="glass-panel p-4 flex flex-col justify-center">
        <div class="data-label mb-1.5 flex items-center gap-1.5"><span class="live-dot bg-blue-400 shadow-[0_0_8px_rgba(96,165,250,0.8)]"></span>{{ marketCards[1].stockName }}</div>
        <div class="font-mono text-2xl font-bold flex items-baseline gap-2">
          {{ formatIndexValue(marketCards[1].currentPrice) }}
          <span class="text-sm font-medium font-mono" :class="getPriceColorClass(marketCards[1].changeRate)">{{ formatChangeRate(marketCards[1].changeRate) }}</span>
        </div>
        <div class="text-[11px] text-slate-600 font-mono mt-1">{{ marketCards[1].stockCode }}</div>
      </div>
      <div class="glass-panel p-4 flex flex-col justify-center">
        <div class="data-label mb-1.5">可用资金 (CNY)</div>
        <div class="font-mono text-2xl font-bold text-white">
          {{ formatMoney(account?.availableCash || 0) }}
        </div>
      </div>
      <div class="glass-panel p-4 flex flex-col justify-center relative overflow-hidden border-t-2 border-t-primary/50">
        <div class="absolute inset-0 bg-primary/3"></div>
        <div class="relative z-10">
          <div class="data-label text-primary/70 mb-1.5">总资产市值</div>
          <div class="font-mono text-3xl font-bold text-white tracking-tight font-display">
            {{ formatMoney(account?.totalAssets || 0) }}
          </div>
          <div class="mt-2.5 tag-badge">
            <span class="live-dot mr-1.5"></span>
            {{ syncLabel }}
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content Area -->
    <div class="flex-1 flex gap-6 min-h-0">
      <!-- Left Watchlist -->
      <div class="w-2/3 glass-panel flex flex-col min-h-0 relative">
        <div class="p-4 border-b border-darkBorder/50 flex justify-between items-center shrink-0">
          <h3 class="font-display font-bold text-base flex items-center gap-2">
            <span class="w-1 h-4 bg-primary rounded-full"></span>
            自选股票池
          </h3>
          <div class="flex gap-2">
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
                class="border-b border-darkBorder/30 hover:bg-primary/3 transition-colors cursor-pointer group/row"
              >
                <td class="p-3">
                  <span class="font-mono font-bold text-white text-sm">{{ item.stockCode }}</span>
                </td>
                <td class="p-3 hidden md:table-cell text-slate-400 text-sm">
                  {{ item.stockName }}
                </td>
                <td class="p-3 text-right font-mono text-sm font-semibold" :class="getPriceColorClass(item.changeRate)">
                  {{ Number(item.currentPrice || 0).toFixed(2) }}
                </td>
                <td class="p-3 text-right font-mono text-xs" :class="getPriceColorClass(item.changeRate)">
                  <span class="px-2 py-1 rounded-md inline-block" :class="item.changeRate >= 0 ? 'bg-upPrice/8' : 'bg-downPrice/8'">
                    {{ item.changeRate >= 0 ? '+' : '' }}{{ Number(item.changeRate || 0).toFixed(2) }}%
                  </span>
                </td>
                <td class="p-3 text-center">
                  <button @click.stop="removeFromWatchlist(item.stockCode)" class="w-7 h-7 rounded-lg border border-darkBorder text-slate-600 hover:text-red-400 hover:border-red-500/30 hover:bg-red-500/8 flex items-center justify-center opacity-0 group-hover/row:opacity-100 transition-all text-sm">
                    ×
                  </button>
                </td>
              </tr>
              <tr v-if="watchlist.length === 0">
                <td colspan="5" class="p-10 text-center text-slate-600 text-sm font-mono">自选股为空 · 请在顶部搜索框添加</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right Panel: Intelligence Mock -->
      <div class="w-1/3 flex flex-col space-y-6 min-h-0">
        <!-- AI Intelligence Module -->
        <div class="glass-panel p-4 shrink-0 border-l-2 border-l-downPrice/60 cursor-pointer hover:bg-darkCardHover/30 transition group" @click="$router.push('/news')">
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
        <div class="glass-panel p-4 flex-1 relative overflow-hidden flex flex-col">
          <div class="absolute top-0 right-0 w-40 h-40 bg-primary/3 rounded-bl-full blur-3xl pointer-events-none"></div>
          <div class="flex-1 flex flex-col min-h-0 relative z-10">
            <div class="data-label mb-3 shrink-0">市场快讯</div>
            <div class="space-y-3 overflow-y-auto flex-1">
              <div
                v-for="(item, index) in marketNews"
                :key="`${item.source}-${item.publishTime}-${index}`"
                class="group border-l-2 pl-3 transition-all cursor-pointer py-0.5"
                :class="newsBorderClass(index)"
                @click="$router.push('/news')"
              >
                <div class="text-[10px] mb-1 font-mono" :class="newsTimeClass(index)">{{ item.publishTime }} · {{ item.source }}</div>
                <div class="text-sm text-slate-400 group-hover:text-slate-200 transition-colors line-clamp-2 leading-snug">{{ item.title }}</div>
              </div>
              <div v-if="marketNews.length === 0" class="text-xs text-slate-600 font-mono py-2">
                暂无市场快讯
              </div>
            </div>
          </div>
          <button @click="$router.push('/news')" class="w-full mt-3 border border-darkBorder text-slate-500 hover:text-primary hover:border-primary/30 py-2.5 rounded-lg text-[10px] font-bold transition uppercase tracking-widest font-mono shrink-0 relative z-10">
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
import type { AccountVO, MarketSnapshotVO, WatchlistVO } from '../types'

const router = useRouter()
const toast = useToastStore()
const watchlist = ref<WatchlistVO[]>([])
const account = ref<AccountVO | null>(null)
const marketSnapshots = ref<MarketSnapshotVO[]>([])
const lastSyncTime = ref('')
const marketNews = ref<Array<{ publishTime: string; source: string; title: string; url: string }>>([])
const sortMode = ref<'default' | 'changeDesc' | 'changeAsc'>('default')
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
  marketSnapshots.value[1] || { stockCode: '399001', stockName: '深证成指', currentPrice: 0, changeRate: 0 }
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

const formatMoney = (val: number) => {
  return '¥' + Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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
    const [snapshots, acc, wl, news] = await Promise.all([
      request.get('/market/snapshots').catch(() => []),
      request.get('/trade/account'),
      request.get('/watchlist'),
      request.get('/news/flash', { params: { limit: 3 } }).catch(() => null)
    ])
    marketSnapshots.value = Array.isArray(snapshots) ? snapshots : []
    account.value = acc
    watchlist.value = wl as any[]
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
    toast.success('已从自选股移除')
  } catch (err) {
    console.error(err)
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
