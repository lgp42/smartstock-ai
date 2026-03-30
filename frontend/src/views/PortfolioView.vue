<template>
  <div class="flex h-full min-h-0 flex-col gap-4">
    <!-- Account overview -->
    <div class="glass-panel relative shrink-0 overflow-hidden px-6 py-5">
      <div class="absolute inset-y-0 right-0 w-1/2 bg-gradient-to-l from-downPrice/5 to-transparent pointer-events-none"></div>
      <div class="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none"></div>

      <div class="relative z-10 flex flex-col gap-5 xl:flex-row xl:items-stretch">
        <!-- Total assets -->
        <div class="xl:w-1/3 xl:border-r xl:border-darkBorder/50 xl:pr-8">
          <div class="data-label mb-2">总资产市值</div>
          <div class="font-mono text-4xl font-bold tracking-tight text-white font-display">
            {{ formatMoney(account?.totalAssets) }}
          </div>
          <div class="mt-3 tag-badge w-fit">
            <span class="live-dot mr-1.5"></span>
            {{ syncLabel }}
          </div>
        </div>

        <!-- Cash breakdown -->
        <div class="xl:w-1/3 xl:border-r xl:border-darkBorder/50 xl:px-8">
          <div class="data-label mb-2">可用资金</div>
          <div class="font-mono text-2xl font-bold text-slate-200">{{ formatMoney(account?.availableCash) }}</div>
          <div class="mt-3 grid grid-cols-2 gap-2">
            <div class="rounded-xl border border-darkBorder/60 bg-darkBg/60 p-3">
              <div class="data-label mb-1">持仓市值</div>
              <div class="font-mono text-sm font-semibold text-white">{{ formatMoney(account?.positionValue) }}</div>
            </div>
            <div class="rounded-xl border border-darkBorder/60 bg-darkBg/60 p-3">
              <div class="data-label mb-1">冻结资金</div>
              <div class="font-mono text-sm font-semibold text-white">{{ formatMoney(account?.frozenCash) }}</div>
            </div>
          </div>
        </div>

        <!-- PnL -->
        <div class="relative xl:w-1/3 xl:pl-8">
          <div class="data-label mb-2">累计总收益率</div>
          <div class="font-mono text-3xl font-bold tracking-tight" :class="Number(account?.profitRate || 0) >= 0 ? 'text-upPrice' : 'text-downPrice'">
            {{ signedPercent(account?.profitRate) }}
          </div>
          <div class="font-mono text-sm text-slate-500 mt-1">{{ formatMoney(account?.totalProfit) }}</div>
          <div class="mt-4 rounded-xl border border-darkBorder/50 bg-darkBg/50 p-3 text-xs text-slate-500 font-mono space-y-1.5">
            <div class="flex items-center justify-between">
              <span>更新频率</span>
              <span class="text-downPrice/80">1s 轮询</span>
            </div>
            <div class="flex items-center justify-between">
              <span>记录精度</span>
              <span class="text-slate-400">支持到秒</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tabs + table -->
    <div class="glass-panel flex min-h-0 flex-1 flex-col overflow-hidden">
      <!-- Tab bar -->
      <div class="shrink-0 border-b border-darkBorder/50 px-4 pt-3">
        <div class="flex items-center gap-1">
          <button
            class="px-4 py-2.5 text-sm font-semibold transition-colors border-b-2 focus:outline-none"
            :class="tab === 'positions' ? 'border-primary text-white' : 'border-transparent text-slate-500 hover:text-slate-300'"
            @click="tab = 'positions'"
          >当前持仓</button>
          <button
            class="px-4 py-2.5 text-sm font-semibold transition-colors border-b-2 focus:outline-none"
            :class="tab === 'history' ? 'border-primary text-white' : 'border-transparent text-slate-500 hover:text-slate-300'"
            @click="tab = 'history'"
          >历史明细</button>
        </div>

        <!-- History filters -->
        <div v-if="tab === 'history'" class="flex flex-wrap items-end gap-2 pb-3 pt-2.5">
          <div>
            <div class="data-label mb-1">开始时间</div>
            <input v-model="historyFilters.startDate" type="datetime-local" step="1" class="cyber-input w-[200px] text-xs h-8 py-0 font-mono" />
          </div>
          <div>
            <div class="data-label mb-1">结束时间</div>
            <input v-model="historyFilters.endDate" type="datetime-local" step="1" class="cyber-input w-[200px] text-xs h-8 py-0 font-mono" />
          </div>
          <div>
            <div class="data-label mb-1">方向</div>
            <select v-model="historyFilters.tradeType" class="cyber-input w-24 text-xs h-8 py-0">
              <option value="">全部</option>
              <option value="buy">买入</option>
              <option value="sell">卖出</option>
            </select>
          </div>
          <div class="flex gap-1.5 items-end pb-0">
            <button class="h-8 rounded-lg border border-darkBorder px-3 text-xs font-semibold text-slate-500 hover:text-slate-300 hover:border-slate-600 transition font-mono" @click="applyTodayRange">今日</button>
            <button class="h-8 rounded-lg border border-darkBorder px-3 text-xs font-semibold text-slate-500 hover:text-slate-300 hover:border-slate-600 transition font-mono" @click="applyRecentMinutesRange(60)">近1h</button>
            <button class="h-8 rounded-lg border border-darkBorder px-3 text-xs font-semibold text-slate-500 hover:text-slate-300 hover:border-slate-600 transition font-mono" @click="applyRecentMinutesRange(15)">近15m</button>
            <button class="h-8 rounded-lg border border-primary/30 bg-primary/5 px-3 text-xs font-bold text-primary hover:bg-primary/10 transition font-mono" @click="loadHistory">查询</button>
            <button class="h-8 rounded-lg border border-darkBorder px-3 text-xs font-semibold text-slate-500 hover:text-slate-300 transition font-mono" @click="resetHistoryFilters">重置</button>
          </div>
        </div>
      </div>

      <!-- Table area -->
      <div class="flex-1 overflow-auto">
        <!-- Positions table -->
        <table v-if="tab === 'positions'" class="w-full border-collapse whitespace-nowrap text-left">
          <thead class="sticky top-0 z-10 bg-darkBg/95 text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600 backdrop-blur font-mono">
            <tr>
              <th class="px-4 py-2.5">代码名称</th>
              <th class="px-4 py-2.5 text-right">可卖 / 总持仓</th>
              <th class="px-4 py-2.5 text-right">持仓成本</th>
              <th class="px-4 py-2.5 text-right">当前价</th>
              <th class="px-4 py-2.5 text-right">盈亏</th>
              <th class="px-4 py-2.5 text-center">操作</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr v-for="p in positions" :key="p.stockCode"
              class="border-b border-darkBorder/30 hover:bg-primary/3 transition-colors">
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-8 w-8 items-center justify-center rounded-xl border border-darkBorder bg-darkBg text-xs font-mono font-bold text-slate-500 shrink-0">
                    {{ p.stockCode.substring(0, 2) }}
                  </div>
                  <div>
                    <div class="font-semibold text-white">{{ p.stockName }}</div>
                    <div class="text-[11px] text-slate-600 font-mono">{{ p.stockCode }}</div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-right font-mono">
                <div class="font-bold text-white">{{ p.availableQuantity }}</div>
                <div class="text-[11px] text-slate-600">/ {{ p.quantity }} 股</div>
              </td>
              <td class="px-4 py-3 text-right font-mono text-slate-400 text-sm">{{ formatPrice(p.costPrice) }}</td>
              <td class="px-4 py-3 text-right font-mono text-white text-sm font-semibold">{{ formatPrice(p.currentPrice) }}</td>
              <td class="px-4 py-3 text-right font-mono font-semibold text-sm" :class="Number(p.profit || 0) >= 0 ? 'text-upPrice' : 'text-downPrice'">
                {{ signed(p.profit) }}
              </td>
              <td class="px-4 py-3 text-center">
                <button
                  class="rounded-lg border border-darkBorder px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:border-upPrice/40 hover:text-upPrice/80 transition font-mono"
                  @click="$router.push(`/terminal/${p.stockCode}`)">
                  去平仓
                </button>
              </td>
            </tr>
            <tr v-if="positions.length === 0">
              <td colspan="6" class="py-16 text-center text-slate-600 text-sm font-mono">当前没有持有任何仓位</td>
            </tr>
          </tbody>
        </table>

        <!-- History table -->
        <table v-if="tab === 'history'" class="w-full border-collapse whitespace-nowrap text-left">
          <thead class="sticky top-0 z-10 bg-darkBg/95 text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600 backdrop-blur font-mono">
            <tr>
              <th class="px-4 py-2.5">时间</th>
              <th class="px-4 py-2.5">股票</th>
              <th class="px-4 py-2.5 text-center">方向</th>
              <th class="px-4 py-2.5 text-right">成交均价</th>
              <th class="px-4 py-2.5 text-right">数量</th>
              <th class="px-4 py-2.5 text-right">发生金额</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr v-for="r in history" :key="r.recordId"
              class="border-b border-darkBorder/30 hover:bg-primary/3 transition-colors">
              <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ formatTradeTime(r.tradeTime) }}</td>
              <td class="px-4 py-3">
                <span class="font-semibold text-white">{{ r.stockName }}</span>
                <span class="ml-2 font-mono text-xs text-slate-600">{{ r.stockCode }}</span>
              </td>
              <td class="px-4 py-3 text-center">
                <span class="rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest font-mono"
                  :class="r.tradeType === 'buy' ? 'bg-upPrice/10 text-upPrice' : 'bg-downPrice/10 text-downPrice'">
                  {{ r.tradeType === 'buy' ? '买入' : '卖出' }}
                </span>
              </td>
              <td class="px-4 py-3 text-right font-mono text-slate-300 text-sm">{{ formatPrice(r.price) }}</td>
              <td class="px-4 py-3 text-right font-mono text-slate-400 text-sm">{{ r.quantity }}</td>
              <td class="px-4 py-3 text-right font-mono font-semibold text-white text-sm">{{ formatMoney(r.amount) }}</td>
            </tr>
            <tr v-if="history.length === 0">
              <td colspan="6" class="py-16 text-center text-slate-600 text-sm font-mono">这段时间暂无交易记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import request from '../utils/request'
import type { AccountVO, PositionVO, TradeRecordVO, PageVO } from '../types'

const tab = ref<'positions' | 'history'>('positions')
const account = ref<AccountVO | null>(null)
const positions = ref<PositionVO[]>([])
const history = ref<TradeRecordVO[]>([])
const lastSyncTime = ref('')
const historyFilters = reactive({
  startDate: '',
  endDate: '',
  tradeType: ''
})

let pollTimer: number | undefined

const syncLabel = computed(() => (lastSyncTime.value ? `1 秒刷新 · ${lastSyncTime.value}` : '1 秒刷新'))

const formatMoney = (val: unknown) => {
  return '¥' + Number(val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatPrice = (val: unknown) => Number(val || 0).toFixed(2)

const signed = (val: unknown) => {
  const num = Number(val || 0)
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}`
}

const signedPercent = (val: unknown) => {
  const num = Number(val || 0)
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`
}

const formatTradeTime = (value: string) => (value || '').replace('T', ' ')

const toDateTimeLocal = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const applyTodayRange = () => {
  const now = new Date()
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  historyFilters.startDate = toDateTimeLocal(start)
  historyFilters.endDate = toDateTimeLocal(now)
}

const applyRecentMinutesRange = (minutes: number) => {
  const now = new Date()
  const start = new Date(now.getTime() - minutes * 60 * 1000)
  historyFilters.startDate = toDateTimeLocal(start)
  historyFilters.endDate = toDateTimeLocal(now)
}

const resetHistoryFilters = async () => {
  historyFilters.startDate = ''
  historyFilters.endDate = ''
  historyFilters.tradeType = ''
  await loadHistory()
}

const loadOverview = async () => {
  const [accountResult, positionResult] = await Promise.all([
    request.get('/trade/account'),
    request.get('/trade/positions')
  ])
  account.value = accountResult
  positions.value = Array.isArray(positionResult) ? positionResult : []
  lastSyncTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

const loadHistory = async () => {
  const histResult = await request.get<PageVO<TradeRecordVO>>('/trade/records', {
    params: {
      pageSize: 100,
      startDate: historyFilters.startDate || undefined,
      endDate: historyFilters.endDate || undefined,
      tradeType: historyFilters.tradeType || undefined
    }
  })
  history.value = (histResult as any)?.records || []
}

const loadData = async () => {
  try {
    await Promise.all([loadOverview(), loadHistory()])
  } catch (error) {
    console.error(error)
  }
}

const startPolling = () => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
  }
  pollTimer = window.setInterval(() => {
    loadOverview().catch(console.error)
    if (tab.value === 'history') {
      loadHistory().catch(console.error)
    }
  }, 1000)
}

watch(tab, () => {
  if (tab.value === 'history') {
    loadHistory().catch(console.error)
  }
})

onMounted(() => {
  loadData()
  startPolling()
})

onBeforeUnmount(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
  }
})
</script>
