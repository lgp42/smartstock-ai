<template>
  <div class="flex h-full min-h-0 flex-col gap-4 xl:flex-row">
    <!-- Main chart section -->
    <section class="glass-panel flex min-h-0 flex-1 flex-col overflow-hidden p-4 sm:p-5">
      <!-- Stock header -->
      <div class="border-b border-darkBorder/60 pb-4 mb-3">
        <div class="flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between">
          <div class="flex items-start gap-4">
            <!-- Stock icon -->
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-primary/20 bg-primary/5 text-xl font-display font-black text-primary">
              {{ stockName ? stockName.slice(0, 1) : (code ? code.slice(0, 2) : '--') }}
            </div>
            <div class="space-y-1.5">
              <div class="flex flex-wrap items-center gap-2">
                <h1 class="font-display text-2xl font-black tracking-tight text-white sm:text-3xl">
                  {{ stockName || '未选择股票' }}
                </h1>
                <span class="tag-badge font-mono">{{ code }}</span>
                <span v-if="detail?.board" class="tag-badge border-primary/20 bg-primary/5 text-primary/70">{{ detail.board }}</span>
                <span v-if="detail?.industry" class="tag-badge border-downPrice/20 bg-downPrice/5 text-downPrice/80">{{ detail.industry }}</span>
              </div>

              <div v-if="detail" class="flex flex-wrap items-end gap-x-4 gap-y-1">
                <div class="flex items-end gap-3">
                  <span class="font-mono text-4xl font-black tracking-tight sm:text-5xl" :class="priceColor">
                    {{ formatPrice(detail.currentPrice) }}
                  </span>
                  <span class="rounded-lg px-2.5 py-1 text-sm font-semibold font-mono" :class="bgPriceColor">
                    {{ signed(detail.changeAmount) }} ({{ signedPercent(detail.changeRate) }})
                  </span>
                </div>
                <div class="flex items-center gap-1.5 rounded-full border border-darkBorder bg-darkCard/50 px-2.5 py-1 text-[10px] text-slate-500 font-mono">
                  <span class="live-dot"></span>
                  {{ refreshLabel }}
                </div>
              </div>
            </div>
          </div>

          <!-- Quick stats -->
          <div class="flex flex-wrap items-center gap-x-6 gap-y-3 xl:justify-end">
            <div v-for="stat in quickStats" :key="stat.label">
              <div class="data-label">{{ stat.label }}</div>
              <div class="mt-0.5 font-mono text-sm font-semibold" :class="stat.cls || 'text-slate-200'">{{ stat.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Period & Indicator toolbar -->
      <div class="flex items-center gap-4 pb-3 border-b border-darkBorder/40 mb-3">
        <div class="flex items-center gap-2">
          <span class="data-label">周期</span>
          <div class="flex gap-1">
            <button
              v-for="period in PERIOD_OPTIONS"
              :key="period.value"
              type="button"
              class="rounded-md px-2.5 py-1 text-xs font-semibold transition font-mono"
              :class="period.value === activePeriod ? 'bg-primary/15 text-primary border border-primary/30' : 'text-slate-500 hover:bg-darkCardHover/50 hover:text-slate-300 border border-transparent'"
              @click="switchPeriod(period)"
            >{{ period.label }}</button>
          </div>
        </div>

        <div class="h-3 w-px bg-darkBorder/60"></div>

        <div class="flex items-center gap-2">
          <span class="data-label">指标</span>
          <div class="flex gap-1">
            <button
              v-for="ind in INDICATOR_OPTIONS"
              :key="ind.value"
              type="button"
              class="rounded-md px-2.5 py-1 text-xs font-semibold transition font-mono"
              :class="ind.value === activeIndicator ? 'bg-accent/15 text-accent border border-accent/30' : 'text-slate-500 hover:bg-darkCardHover/50 hover:text-slate-300 border border-transparent'"
              @click="switchIndicator(ind.value)"
            >{{ ind.label }}</button>
          </div>
        </div>
      </div>

      <!-- Chart + side panel -->
      <div class="grid flex-1 gap-4 min-h-0 xl:grid-cols-[minmax(0,1fr)_240px]">
        <!-- KlineCharts container -->
        <div class="relative rounded-xl overflow-hidden bg-[#050D1A] border border-darkBorder/30 min-h-[400px]">
          <div ref="chartContainerRef" class="w-full h-full"></div>
          <div v-if="chartLoading" class="absolute inset-0 z-10 flex items-center justify-center bg-darkBg/70 backdrop-blur-sm">
            <div class="flex flex-col items-center gap-3">
              <div class="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
              <span class="text-xs text-slate-500 font-mono tracking-widest">LOADING</span>
            </div>
          </div>
          <div v-if="!code" class="absolute inset-0 z-10 flex items-center justify-center border border-dashed border-darkBorder/40 rounded-xl">
            <span class="text-sm text-slate-600 font-mono tracking-[0.2rem]">暂无数据</span>
          </div>
        </div>

        <!-- Side analysis panel -->
        <div class="flex flex-col gap-3">
          <div class="rounded-xl border border-darkBorder/50 bg-darkCard/40 p-4">
            <div class="data-label mb-3">分时速览 · {{ activePeriodLabel }}</div>
            <div class="space-y-3 text-sm">
              <div class="flex items-center justify-between py-1.5 border-b border-darkBorder/30">
                <span class="text-slate-500 text-xs">振幅</span>
                <span class="font-mono text-slate-200 text-xs">{{ amplitudeLabel }}</span>
              </div>
              <div class="flex items-center justify-between py-1.5 border-b border-darkBorder/30">
                <span class="text-slate-500 text-xs">价差</span>
                <span class="font-mono text-slate-200 text-xs">{{ spreadLabel }}</span>
              </div>
              <div class="flex items-center justify-between py-1.5">
                <span class="text-slate-500 text-xs">量能状态</span>
                <span class="font-mono text-xs" :class="volumeStatusClass">{{ volumeStatus }}</span>
              </div>
            </div>
          </div>

          <div class="rounded-xl border border-darkBorder/50 bg-darkCard/40 p-4">
            <div class="data-label mb-3">关键价位</div>
            <div class="space-y-3 text-sm">
              <div class="flex items-center justify-between py-1.5 border-b border-darkBorder/30">
                <span class="text-slate-500 text-xs">压力位</span>
                <span class="font-mono text-upPrice text-xs">{{ resistanceLabel }}</span>
              </div>
              <div class="flex items-center justify-between py-1.5 border-b border-darkBorder/30">
                <span class="text-slate-500 text-xs">支撑位</span>
                <span class="font-mono text-downPrice text-xs">{{ supportLabel }}</span>
              </div>
              <div class="flex items-center justify-between py-1.5">
                <span class="text-slate-500 text-xs">均价参考</span>
                <span class="font-mono text-slate-200 text-xs">{{ averagePriceLabel }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Trade panel -->
    <aside class="w-full xl:w-[320px] shrink-0">
      <div class="glass-panel p-5 h-full flex flex-col">
        <div class="mb-5 flex items-center justify-between">
          <div>
            <div class="data-label">模拟交易</div>
            <h3 class="mt-1 font-display text-lg font-black tracking-tight text-white">委托面板</h3>
          </div>
          <span class="tag-badge">A股 T+1</span>
        </div>

        <!-- Buy/Sell toggle -->
        <div class="mb-5 flex rounded-xl border border-darkBorder bg-darkBg/50 p-1">
          <button
            type="button"
            class="flex-1 rounded-lg py-2 text-sm font-bold transition"
            :class="side === 'buy' ? 'bg-upPrice text-white shadow-[0_6px_20px_rgba(239,68,68,0.25)]' : 'text-slate-500 hover:text-slate-300'"
            @click="side = 'buy'"
          >买入</button>
          <button
            type="button"
            class="flex-1 rounded-lg py-2 text-sm font-bold transition"
            :class="side === 'sell' ? 'bg-downPrice text-white shadow-[0_6px_20px_rgba(16,185,129,0.25)]' : 'text-slate-500 hover:text-slate-300'"
            @click="side = 'sell'"
          >卖出</button>
        </div>

        <div class="space-y-3 flex-1">
          <div class="rounded-xl bg-darkBg/60 p-4 border border-darkBorder/40 focus-within:border-primary/30 transition-colors">
            <div class="mb-2 flex items-center justify-between">
              <label class="data-label">委托价格</label>
              <span class="text-[10px] text-slate-600 font-mono">{{ activePeriodLabel }}</span>
            </div>
            <div class="relative">
              <span class="absolute left-0 top-1/2 -translate-y-1/2 font-mono text-slate-600 text-sm">¥</span>
              <input
                v-model.number="price"
                type="number"
                step="0.01"
                class="w-full bg-transparent pl-5 text-right font-mono text-2xl font-bold text-white placeholder-slate-700 focus:outline-none"
                placeholder="0.00"
              />
            </div>
          </div>

          <div class="rounded-xl bg-darkBg/60 p-4 border border-darkBorder/40 focus-within:border-primary/30 transition-colors">
            <div class="mb-2 flex items-center justify-between">
              <label class="data-label">委托数量</label>
              <span class="text-[10px] text-slate-600 font-mono">100 股整数倍</span>
            </div>
            <input
              v-model.number="quantity"
              type="number"
              step="100"
              class="w-full bg-transparent text-right font-mono text-2xl font-bold text-white placeholder-slate-700 focus:outline-none"
              placeholder="0"
            />
            <div class="mt-3 grid grid-cols-4 gap-1.5">
              <button
                v-for="lot in quickLots"
                :key="lot"
                type="button"
                class="rounded-lg border border-darkBorder bg-darkCard/50 py-1 text-[11px] font-mono font-semibold text-slate-500 transition hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                @click="quantity = lot"
              >{{ lot }}</button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3 px-1">
            <div>
              <div class="data-label">可用资金</div>
              <div class="mt-1 font-mono text-xs font-semibold text-white">{{ formatMoney(account?.availableCash) }}</div>
            </div>
            <div class="text-right">
              <div class="data-label">预计交易额</div>
              <div class="mt-1 font-mono text-xs font-semibold text-white">{{ formatMoney(estimatedAmount) }}</div>
            </div>
          </div>
        </div>

        <button
          type="button"
          class="mt-4 w-full rounded-xl py-3.5 text-sm font-display font-black uppercase tracking-[0.2em] text-white transition"
          :class="side === 'buy'
            ? 'bg-upPrice shadow-[0_8px_30px_rgba(239,68,68,0.25)] hover:shadow-[0_8px_40px_rgba(239,68,68,0.4)] hover:bg-red-500'
            : 'bg-downPrice shadow-[0_8px_30px_rgba(16,185,129,0.25)] hover:shadow-[0_8px_40px_rgba(16,185,129,0.4)] hover:bg-emerald-500'"
          @click="submitOrder"
        >
          确认{{ side === 'buy' ? '买入' : '卖出' }}
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { init as klineInit, dispose as klineDispose, type Chart as KlineChart, type Crosshair, type Period, type KLineData } from 'klinecharts'
import request from '../utils/request'
import { useToastStore } from '../stores/toast'
import type { StockDetailVO, AccountVO } from '../types'

type PeriodValue = '1s' | '1min' | '5min' | '15min' | '30min' | '60min' | 'day' | 'week' | 'month'
type IndicatorValue = 'macd' | 'kdj' | 'rsi'

const PERIOD_OPTIONS: Array<{ label: string; value: PeriodValue; kline: Period }> = [
  { label: '1秒', value: '1s',    kline: { type: 'second', span: 1 } },
  { label: '1分', value: '1min',  kline: { type: 'minute', span: 1 } },
  { label: '5分', value: '5min',  kline: { type: 'minute', span: 5 } },
  { label: '15分', value: '15min', kline: { type: 'minute', span: 15 } },
  { label: '30分', value: '30min', kline: { type: 'minute', span: 30 } },
  { label: '60分', value: '60min', kline: { type: 'minute', span: 60 } },
  { label: '日K', value: 'day',   kline: { type: 'day', span: 1 } },
  { label: '周K', value: 'week',  kline: { type: 'week', span: 1 } },
  { label: '月K', value: 'month', kline: { type: 'month', span: 1 } },
]

const INDICATOR_OPTIONS: Array<{ label: string; value: IndicatorValue; klineName: string }> = [
  { label: 'MACD', value: 'macd', klineName: 'MACD' },
  { label: 'KDJ',  value: 'kdj',  klineName: 'KDJ' },
  { label: 'RSI',  value: 'rsi',  klineName: 'RSI' },
]

const quickLots = [100, 500, 1000, 2000]

const route = useRoute()
const toast = useToastStore()
const chartContainerRef = ref<HTMLDivElement | null>(null)
const code = ref((route.params.code as string) || '000001')
const stockName = ref('')
const detail = ref<StockDetailVO | null>(null)
const account = ref<AccountVO | null>(null)
const side = ref<'buy' | 'sell'>('buy')
const price = ref(0)
const quantity = ref(100)
const activePeriod = ref<PeriodValue>('day')
const activeIndicator = ref<IndicatorValue>('macd')
const lastQuoteSync = ref('')
const lastPollSync = ref('')
const chartLoading = ref(true)
const latestKlines = ref<any[]>([])
const chartData = ref<KLineData[]>([])

let chartInstance: KlineChart | null = null
let indicatorPaneId: string | null = null
let currentIndicatorKlineName = 'MACD'
let resizeObserver: ResizeObserver | null = null
let quoteTimer: number | undefined
let pollingBusy = false
let accountPollCount = 0
let lastCrosshair: Crosshair | null = null

const liveBarSubscribers = new Set<(data: KLineData) => void>()

// ─── Computed ───────────────────────────────────────────────────────────────
const activePeriodLabel = computed(() => PERIOD_OPTIONS.find(p => p.value === activePeriod.value)?.label ?? activePeriod.value)
const estimatedAmount = computed(() => Number(price.value || 0) * Number(quantity.value || 0))
const refreshLabel = computed(() => {
  if (lastPollSync.value && lastQuoteSync.value && lastPollSync.value !== lastQuoteSync.value) {
    return `1s · 刷新 ${lastPollSync.value} · 行情 ${lastQuoteSync.value}`
  }
  if (lastPollSync.value) return `1s · ${lastPollSync.value}`
  if (lastQuoteSync.value) return `1s · ${lastQuoteSync.value}`
  return '1s 连接中'
})

const quickStats = computed(() => [
  { label: '昨收', value: formatPrice(detail.value?.preClose) },
  { label: '今开', value: formatPrice(detail.value?.open) },
  { label: '最高', value: formatPrice(detail.value?.high), cls: 'text-upPrice' },
  { label: '最低', value: formatPrice(detail.value?.low), cls: 'text-downPrice' },
  { label: '成交额', value: formatAmount(detail.value?.amount) },
  { label: '成交量', value: formatVolume(detail.value?.volume) },
])

const amplitudeLabel = computed(() => {
  const { high, low, preClose } = detail.value || {}
  if (!preClose || !high || !low) return '--'
  return `${(((Number(high) - Number(low)) / Number(preClose)) * 100).toFixed(2)}%`
})

const spreadLabel = computed(() => {
  const { high, low } = detail.value || {}
  if (!high || !low) return '--'
  return (Number(high) - Number(low)).toFixed(2)
})

const averagePriceLabel = computed(() => {
  if (!latestKlines.value.length) return '--'
  const closes = latestKlines.value.map(k => Number(k.close || 0)).filter(Boolean)
  if (!closes.length) return '--'
  return (closes.reduce((a, b) => a + b, 0) / closes.length).toFixed(2)
})

const supportLabel = computed(() => {
  if (!latestKlines.value.length) return '--'
  const lows = latestKlines.value.map(k => Number(k.low || 0)).filter(Boolean)
  return lows.length ? Math.min(...lows).toFixed(2) : '--'
})

const resistanceLabel = computed(() => {
  if (!latestKlines.value.length) return '--'
  const highs = latestKlines.value.map(k => Number(k.high || 0)).filter(Boolean)
  return highs.length ? Math.max(...highs).toFixed(2) : '--'
})

const volumeStatus = computed(() => {
  if (latestKlines.value.length < 6) return '样本不足'
  const latest = Number(latestKlines.value[latestKlines.value.length - 1]?.volume || 0)
  const history = latestKlines.value.slice(-6, -1).map(k => Number(k.volume || 0))
  const avg = history.reduce((a, b) => a + b, 0) / Math.max(history.length, 1)
  if (!latest || !avg) return '样本不足'
  if (latest >= avg * 1.4) return '放量'
  if (latest <= avg * 0.7) return '缩量'
  return '平稳'
})

const volumeStatusClass = computed(() => {
  if (volumeStatus.value === '放量') return 'text-upPrice'
  if (volumeStatus.value === '缩量') return 'text-downPrice'
  return 'text-slate-200'
})

const priceColor = computed(() => {
  if (!detail.value) return 'text-slate-100'
  const r = Number(detail.value.changeRate)
  return r > 0 ? 'text-upPrice' : r < 0 ? 'text-downPrice' : 'text-slate-100'
})

const bgPriceColor = computed(() => {
  if (!detail.value) return 'bg-slate-800/50 text-slate-400'
  const r = Number(detail.value.changeRate)
  return r > 0 ? 'bg-upPrice/12 text-upPrice' : r < 0 ? 'bg-downPrice/12 text-downPrice' : 'bg-slate-800/50 text-slate-400'
})

// ─── Formatters ─────────────────────────────────────────────────────────────
const formatPrice = (v: unknown) => Number(v || 0).toFixed(2)
const signed = (v: unknown) => { const n = Number(v || 0); return `${n >= 0 ? '+' : ''}${n.toFixed(2)}` }
const signedPercent = (v: unknown) => { const n = Number(v || 0); return `${n >= 0 ? '+' : ''}${n.toFixed(2)}%` }
const formatMoney = (v: unknown) => `¥ ${Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const formatAmount = (v: unknown) => {
  const n = Number(v || 0)
  if (n >= 1e8) return `${(n / 1e8).toFixed(2)} 亿`
  if (n >= 1e4) return `${(n / 1e4).toFixed(2)} 万`
  return formatMoney(v)
}
const formatVolume = (v: unknown) => {
  const n = Number(v || 0)
  if (n >= 1e8) return `${(n / 1e8).toFixed(2)} 亿股`
  if (n >= 1e4) return `${(n / 1e4).toFixed(2)} 万股`
  return `${n.toFixed(0)} 股`
}

const parseKlineDate = (dateStr: string): number => {
  // Handle both '2024-01-01' and '2024-01-01 09:30:00'
  return new Date(dateStr.includes('T') ? dateStr : dateStr.replace(' ', 'T')).getTime()
}

const toChartData = (klineRows: any[]): KLineData[] => klineRows.map(item => ({
  timestamp: parseKlineDate(item.date),
  open: Number(item.open),
  close: Number(item.close),
  high: Number(item.high),
  low: Number(item.low),
  volume: Number(item.volume || 0),
  turnover: Number(item.amount || 0),
}))

const cloneBar = (bar: KLineData): KLineData => ({ ...bar })

const canApplyIncrementalUpdate = (previousRows: KLineData[], nextRows: KLineData[]) => {
  if (!previousRows.length || !nextRows.length) return false
  if (nextRows.length < previousRows.length || nextRows.length > previousRows.length + 1) return false

  const sharedLength = Math.min(previousRows.length, nextRows.length)
  const prefixLength = nextRows.length === previousRows.length ? sharedLength - 1 : sharedLength
  for (let index = 0; index < prefixLength; index += 1) {
    if (previousRows[index]?.timestamp !== nextRows[index]?.timestamp) return false
  }

  if (nextRows.length === previousRows.length) {
    return previousRows[sharedLength - 1]?.timestamp === nextRows[sharedLength - 1]?.timestamp
  }
  return true
}

const emitLiveBar = (bar: KLineData) => {
  liveBarSubscribers.forEach(callback => callback(cloneBar(bar)))
}

// ─── Chart ──────────────────────────────────────────────────────────────────
const KLINE_DARK_STYLES = {
  grid: {
    horizontal: { show: true, size: 1, color: '#182840', style: 'dashed' as const, dashedValue: [4, 2] },
    vertical: { show: false },
  },
  candle: {
    bar: {
      upColor: '#EF4444',
      downColor: '#10B981',
      noChangeColor: '#64748b',
      upBorderColor: '#EF4444',
      downBorderColor: '#10B981',
      noChangeBorderColor: '#64748b',
      upWickColor: '#EF4444',
      downWickColor: '#10B981',
      noChangeWickColor: '#64748b',
    },
    tooltip: {
      // 只在鼠标悬浮时才显示 OHLCV，避免常驻文字撞到蜡烛
      showRule: 'follow_cross' as const,
      showType: 'rect' as const,
      rect: {
        color: '#0C1828',
        borderColor: '#22D3EE33',
        borderRadius: 6,
        borderSize: 1,
        offsetLeft: 8,
        offsetTop: 8,
        offsetRight: 8,
        paddingLeft: 10,
        paddingRight: 10,
        paddingTop: 8,
        paddingBottom: 8,
      },
      text: {
        size: 11,
        family: 'JetBrains Mono',
        color: '#94a3b8',
        marginLeft: 8,
        marginTop: 4,
        marginRight: 8,
        marginBottom: 4,
      },
    },
  },
  indicator: {
    tooltip: {
      showRule: 'follow_cross' as const,
      text: {
        size: 11,
        family: 'JetBrains Mono',
        color: '#64748b',
        marginLeft: 8,
        marginTop: 4,
        marginRight: 8,
        marginBottom: 4,
      },
    },
  },
  xAxis: {
    tickText: { color: '#475569', family: 'JetBrains Mono', size: 11 },
    axisLine: { show: true, color: '#182840', size: 1 },
  },
  yAxis: {
    tickText: { color: '#475569', family: 'JetBrains Mono', size: 11 },
    axisLine: { show: false },
  },
  crosshair: {
    horizontal: {
      line: { color: '#22D3EE60' },
      text: { backgroundColor: '#0C1828', borderColor: '#22D3EE40', color: '#e2e8f0', family: 'JetBrains Mono' },
    },
    vertical: {
      line: { color: '#22D3EE60' },
      text: { backgroundColor: '#0C1828', borderColor: '#22D3EE40', color: '#e2e8f0', family: 'JetBrains Mono' },
    },
  },
  separator: { color: '#182840' },
}

const restoreCrosshair = () => {
  if (!lastCrosshair || !chartInstance) return
  const chart = chartInstance as KlineChart & { setCrosshair?: (crosshair: Crosshair, options?: Record<string, unknown>) => void }
  if (typeof chart.setCrosshair !== 'function') return
  window.requestAnimationFrame(() => {
    chart.setCrosshair?.(lastCrosshair as Crosshair, { notExecuteAction: true })
  })
}

const reloadChartData = (nextData: KLineData[], resetViewport = false) => {
  chartData.value = nextData
  if (!chartInstance) return

  const activePeriodOption = PERIOD_OPTIONS.find(period => period.value === activePeriod.value)?.kline ?? { type: 'day', span: 1 }
  chartInstance.setSymbol({ ticker: code.value, pricePrecision: 2, volumePrecision: 0 })
  chartInstance.setPeriod(activePeriodOption)
  chartInstance.setDataLoader({
    getBars: ({ callback }) => {
      callback(chartData.value.map(cloneBar), false)
    },
    subscribeBar: ({ callback }) => {
      liveBarSubscribers.add(callback)
    },
    unsubscribeBar: () => {
      liveBarSubscribers.clear()
    },
  })
  chartInstance.setOffsetRightDistance(12)
  if (resetViewport) {
    chartInstance.scrollToRealTime(0)
  }
  restoreCrosshair()
}

const initChart = () => {
  if (!chartContainerRef.value) return
  resizeObserver?.disconnect()
  resizeObserver = null
  liveBarSubscribers.clear()
  lastCrosshair = null
  if (chartInstance) {
    klineDispose(chartContainerRef.value)
    chartInstance = null
    indicatorPaneId = null
    currentIndicatorKlineName = INDICATOR_OPTIONS.find(o => o.value === activeIndicator.value)?.klineName ?? 'MACD'
  }
  chartInstance = klineInit(chartContainerRef.value, {
    styles: KLINE_DARK_STYLES,
    locale: 'zh-CN',
    timezone: 'Asia/Shanghai',
  })
  if (!chartInstance) return

  // MA on main pane
  chartInstance.createIndicator('MA', false, { id: 'candle_pane' })

  // Volume sub-pane
  chartInstance.createIndicator('VOL')

  // Active indicator pane (persists across switches)
  currentIndicatorKlineName = INDICATOR_OPTIONS.find(o => o.value === activeIndicator.value)?.klineName ?? 'MACD'
  indicatorPaneId = chartInstance.createIndicator(currentIndicatorKlineName)
  chartInstance.subscribeAction('onCrosshairChange', (crosshair) => {
    lastCrosshair = crosshair as Crosshair
  })

  resizeObserver = new ResizeObserver(() => { chartInstance?.resize() })
  resizeObserver.observe(chartContainerRef.value)
}

const applyKlineData = (klineRows: any[], silent = false) => {
  latestKlines.value = klineRows
  const nextData = toChartData(klineRows)
  if (silent && canApplyIncrementalUpdate(chartData.value, nextData) && nextData.length) {
    chartData.value = nextData
    emitLiveBar(nextData[nextData.length - 1])
    return
  }
  reloadChartData(nextData, !silent)
}

// ─── Data Fetching ───────────────────────────────────────────────────────────
const fetchDetail = async () => {
  if (!code.value) return
  try {
    const res: any = await request.get(`/market/stocks/${code.value}`)
    detail.value = res
    stockName.value = res.stockName
    if (!price.value || side.value === 'buy') price.value = Number(res.currentPrice || 0)
    lastQuoteSync.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) { console.error(e) }
}

const fetchAcc = async () => {
  try { account.value = await request.get('/trade/account') } catch (e) { console.error(e) }
}

const fetchKLine = async (silent = false) => {
  if (!code.value) return
  if (!silent) chartLoading.value = true
  try {
    const kline = await request.get(`/market/stocks/${code.value}/kline`, {
      params: { period: activePeriod.value, limit: 200 }
    })
    const rows = Array.isArray(kline) ? kline : []
    applyKlineData(rows, silent)
  } catch (e) { console.error(e) }
  finally {
    if (!silent) chartLoading.value = false
  }
}

const switchPeriod = (periodOption: typeof PERIOD_OPTIONS[0]) => {
  activePeriod.value = periodOption.value
  fetchKLine()
}

const switchIndicator = (ind: IndicatorValue) => {
  activeIndicator.value = ind
  if (!chartInstance) return
  const newKlineName = INDICATOR_OPTIONS.find(o => o.value === ind)?.klineName ?? 'MACD'
  if (newKlineName === currentIndicatorKlineName) return

  if (indicatorPaneId) {
    // 先往同一个 pane 里加新指标，再移除旧指标，避免 pane 被自动销毁后无处落脚
    chartInstance.createIndicator(newKlineName, false, { id: indicatorPaneId })
    chartInstance.removeIndicator({ paneId: indicatorPaneId, name: currentIndicatorKlineName })
  } else {
    indicatorPaneId = chartInstance.createIndicator(newKlineName)
  }
  currentIndicatorKlineName = newKlineName
}

const refreshAll = async () => {
  await Promise.all([fetchDetail(), fetchKLine(), fetchAcc()])
}

const submitOrder = async () => {
  try {
    await request.post(`/trade/${side.value}`, {
      stockCode: code.value,
      price: price.value,
      quantity: quantity.value
    })
    toast.success(`${side.value === 'buy' ? '买入' : '卖出'} ${code.value} 委托成功，数量: ${quantity.value}`)
    await refreshAll()
  } catch (err: any) {
    toast.error(err.message || '委托失败')
  }
}

const startQuotePolling = () => {
  if (quoteTimer) window.clearInterval(quoteTimer)
  pollingBusy = false
  accountPollCount = 0
  quoteTimer = window.setInterval(async () => {
    if (pollingBusy) return
    pollingBusy = true
    lastPollSync.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    try {
      await Promise.all([fetchDetail(), fetchKLine(true)])
      accountPollCount += 1
      if (accountPollCount >= 5) {
        accountPollCount = 0
        await fetchAcc()
      }
    } catch (e) {
      console.error(e)
    } finally {
      pollingBusy = false
    }
  }, 1000)
}

onMounted(async () => {
  initChart()
  await refreshAll()
  startQuotePolling()
})

watch(() => route.params.code, async (newCode) => {
  code.value = (newCode as string) || '000001'
  initChart()
  await refreshAll()
  startQuotePolling()
})

onBeforeUnmount(() => {
  if (quoteTimer) window.clearInterval(quoteTimer)
  pollingBusy = false
  resizeObserver?.disconnect()
  if (chartContainerRef.value) klineDispose(chartContainerRef.value)
  chartInstance = null
})
</script>
