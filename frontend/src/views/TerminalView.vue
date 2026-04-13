<template>
  <div class="flex h-full min-h-0 flex-col gap-4">
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

          <div class="flex flex-wrap items-center gap-2 xl:justify-end">
            <button
              type="button"
              class="rounded-lg border border-darkBorder/60 bg-darkCard/50 px-3 py-2 text-xs font-semibold text-slate-300 transition hover:border-slate-500 hover:text-white"
              @click="addToWatchlist"
            >加入自选</button>
            <button
              type="button"
              class="rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-semibold text-primary transition hover:bg-primary/10"
              @click="openCopilot"
            >打开 AI 问答</button>
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
      <div class="grid flex-1 gap-4 min-h-0 xl:grid-cols-[minmax(0,1fr)_360px]">
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
        <div class="flex min-h-0 flex-col gap-3">
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

          <div class="flex min-h-0 flex-1 flex-col rounded-xl border border-primary/20 bg-[linear-gradient(180deg,rgba(10,20,36,0.92),rgba(6,12,24,0.96))] p-4 shadow-[0_20px_60px_rgba(34,211,238,0.08)]">
            <div class="mb-3 flex items-start justify-between gap-3">
              <div>
                <div class="data-label text-primary/80">AI 问答</div>
                <div class="mt-1 font-display text-lg font-black tracking-tight text-white">交易台副驾驶</div>
              </div>
              <button
                type="button"
                class="rounded-lg border border-primary/20 bg-primary/5 px-2.5 py-1 text-[11px] font-mono font-semibold text-primary transition hover:bg-primary/10 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="qaLoading"
                @click="fetchQaHistory"
              >刷新</button>
            </div>

            <div class="mb-3 flex flex-wrap gap-1.5">
              <button
                v-for="item in qaSuggestions"
                :key="item"
                type="button"
                class="rounded-full border border-darkBorder/60 bg-darkBg/60 px-2.5 py-1 text-[11px] font-medium text-slate-400 transition hover:border-primary/30 hover:text-primary"
                @click="qaQuestion = item"
              >{{ item }}</button>
            </div>

            <div class="min-h-0 flex-1 overflow-y-auto pr-1">
              <div v-if="qaHistory.length" class="space-y-3">
                <article
                  v-for="item in qaHistory"
                  :key="item.id"
                  class="rounded-xl border border-darkBorder/50 bg-darkBg/50 p-3"
                >
                  <div class="mb-2 flex items-start gap-2">
                    <span class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white text-[10px] font-black text-darkBg">Q</span>
                    <p class="text-sm font-semibold leading-6 text-slate-100">{{ item.question }}</p>
                  </div>
                  <div class="flex items-start gap-2">
                    <span class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-[10px] font-black text-darkBg">AI</span>
                    <div class="markdown-body text-sm leading-6 text-slate-300" v-html="renderAnswer(item.answer)"></div>
                  </div>
                  <div class="mt-3 text-right text-[10px] font-mono text-slate-600">{{ formatDateTime(item.createdAt) }}</div>
                </article>
              </div>
              <div v-else class="flex h-full min-h-[180px] items-center justify-center rounded-xl border border-dashed border-darkBorder/50 bg-darkBg/40 px-4 text-center text-sm leading-6 text-slate-500">
                还没有问答记录。输入一个问题，直接让 AI 结合当前股票分析给出结论。
              </div>
            </div>

            <div class="mt-3 border-t border-darkBorder/40 pt-3">
              <textarea
                v-model="qaQuestion"
                rows="4"
                class="w-full resize-none rounded-xl border border-darkBorder/60 bg-darkBg/70 px-3 py-3 text-sm leading-6 text-slate-100 outline-none transition placeholder:text-slate-600 focus:border-primary/30"
                :placeholder="`围绕 ${stockName || code} 提问，例如：现在适合观望还是分批介入？`"
                @keydown.enter.exact.prevent="askAi"
              ></textarea>
              <div class="mt-3 flex items-center justify-between gap-3">
                <div class="text-[10px] font-mono uppercase tracking-[0.18em] text-slate-600">Enter 发送</div>
                <button
                  type="button"
                  class="rounded-xl bg-primary px-4 py-2.5 text-sm font-display font-black tracking-[0.14em] text-darkBg transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="qaLoading || !qaQuestion.trim()"
                  @click="askAi"
                >{{ qaLoading ? '分析中' : '发送问题' }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { init as klineInit, dispose as klineDispose, type Chart as KlineChart, type Crosshair, type Period, type KLineData } from 'klinecharts'
import request from '../utils/request'
import { renderMarkdown } from '../utils/markdown'
import { useToastStore } from '../stores/toast'
import type { StockDetailVO, QaAnswerVO, QaHistoryVO } from '../types'

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

const route = useRoute()
const router = useRouter()
const toast = useToastStore()
const chartContainerRef = ref<HTMLDivElement | null>(null)
const code = ref((route.params.code as string) || '000001')
const stockName = ref('')
const detail = ref<StockDetailVO | null>(null)
const activePeriod = ref<PeriodValue>('day')
const activeIndicator = ref<IndicatorValue>('macd')
const lastQuoteSync = ref('')
const lastPollSync = ref('')
const chartLoading = ref(true)
const latestKlines = ref<any[]>([])
const chartData = ref<KLineData[]>([])
const qaQuestion = ref('')
const qaLoading = ref(false)
const qaHistory = ref<QaHistoryVO[]>([])

const renderAnswer = (content: string) => renderMarkdown(content)

let chartInstance: KlineChart | null = null
let indicatorPaneId: string | null = null
let currentIndicatorKlineName = 'MACD'
let resizeObserver: ResizeObserver | null = null
let quoteTimer: number | undefined
let pollingBusy = false
let lastCrosshair: Crosshair | null = null

const liveBarSubscribers = new Set<(data: KLineData) => void>()

// ─── Computed ───────────────────────────────────────────────────────────────
const activePeriodLabel = computed(() => PERIOD_OPTIONS.find(p => p.value === activePeriod.value)?.label ?? activePeriod.value)
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

const qaSuggestions = computed(() => {
  const target = stockName.value || code.value
  return [
    `${target} 短线怎么看？`,
    `${target} 现在最大的风险是什么？`,
    `${target} 适合继续观望还是分批介入？`,
  ]
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
const formatDateTime = (value: string) => {
  if (!value) return '--'
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
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
    lastQuoteSync.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) { console.error(e) }
}

const fetchQaHistory = async () => {
  try {
    qaHistory.value = await request.get<QaHistoryVO[]>('/qa/history', { params: { limit: 8 } })
  } catch (e) {
    console.error(e)
  }
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
  await Promise.all([fetchDetail(), fetchKLine()])
}

const addToWatchlist = async () => {
  if (!code.value) return
  try {
    await request.post('/watchlist', { stockCode: code.value })
    toast.success(`已添加 ${code.value} 到自选股`)
  } catch (err: any) {
    toast.error(err.message || '添加自选股失败')
  }
}

const openCopilot = () => {
  router.push(`/copilot/${code.value}`)
}

const askAi = async () => {
  if (!code.value || !qaQuestion.value.trim() || qaLoading.value) return
  stopQuotePolling()
  qaLoading.value = true
  try {
    const response = await request.post<QaAnswerVO>(
      '/qa/ask',
      {
        stockCode: code.value,
        question: qaQuestion.value.trim(),
      },
      {
        timeout: 120000,
      }
    )
    qaHistory.value = [
      {
        id: Date.now(),
        question: response.question,
        answer: response.answer,
        createdAt: response.createdAt,
      },
      ...qaHistory.value,
    ].slice(0, 8)
    qaQuestion.value = ''
  } catch (err: any) {
    toast.error(err.message || 'AI 问答失败')
  } finally {
    qaLoading.value = false
    startQuotePolling()
  }
}

const stopQuotePolling = () => {
  if (quoteTimer) {
    window.clearInterval(quoteTimer)
    quoteTimer = undefined
  }
  pollingBusy = false
}

const startQuotePolling = () => {
  stopQuotePolling()
  quoteTimer = window.setInterval(async () => {
    if (pollingBusy) return
    pollingBusy = true
    lastPollSync.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    try {
      await Promise.all([fetchDetail(), fetchKLine(true)])
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
  await fetchQaHistory()
  startQuotePolling()
})

watch(() => route.params.code, async (newCode) => {
  code.value = (newCode as string) || '000001'
  qaQuestion.value = ''
  initChart()
  await refreshAll()
  await fetchQaHistory()
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
