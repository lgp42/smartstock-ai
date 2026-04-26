<template>
  <div class="flex h-full min-h-0 flex-col gap-4">
    <div class="glass-panel flex items-center justify-between px-5 py-3.5">
      <div class="flex items-center gap-3">
        <span class="h-5 w-1 rounded-full bg-gradient-to-b from-upPrice to-downPrice"></span>
        <h2 class="font-display text-base font-bold text-white">模拟交易台</h2>
        <span class="tag-badge border-primary/20 bg-primary/5 text-primary/80">限价挂单</span>
      </div>
      <button class="btn-primary text-xs" :disabled="loading" @click="loadData">
        {{ loading ? '刷新中' : '刷新账户' }}
      </button>
    </div>

    <section class="grid shrink-0 gap-3 md:grid-cols-4">
      <div v-for="item in accountMetrics" :key="item.label" class="stat-card min-h-[96px]">
        <div class="data-label">{{ item.label }}</div>
        <div class="mt-2 font-mono text-xl font-black" :class="item.cls">{{ item.value }}</div>
      </div>
    </section>

    <div class="grid min-h-0 flex-1 gap-4 xl:grid-cols-[340px_minmax(0,1fr)]">
      <section class="glass-panel flex flex-col gap-4 p-5">
        <div>
          <div class="data-label mb-3">下单</div>
          <div class="grid grid-cols-2 gap-2">
            <button
              type="button"
              class="btn-buy py-2"
              :class="orderForm.side === 'buy' ? '' : 'opacity-45'"
              @click="orderForm.side = 'buy'"
            >买入</button>
            <button
              type="button"
              class="btn-sell py-2"
              :class="orderForm.side === 'sell' ? '' : 'opacity-45'"
              @click="orderForm.side = 'sell'"
            >卖出</button>
          </div>
        </div>

        <label class="block">
          <span class="data-label mb-2 block">股票代码</span>
          <input v-model.trim="orderForm.stockCode" class="cyber-input w-full" placeholder="600519" />
        </label>
        <label class="block">
          <span class="data-label mb-2 block">限价</span>
          <input v-model.number="orderForm.price" type="number" min="0.01" step="0.01" class="cyber-input w-full" />
        </label>
        <label class="block">
          <span class="data-label mb-2 block">数量</span>
          <input v-model.number="orderForm.quantity" type="number" min="100" step="100" class="cyber-input w-full" />
        </label>

        <button
          type="button"
          class="rounded-xl px-4 py-3 font-display text-sm font-black tracking-[0.14em] text-white transition disabled:cursor-not-allowed disabled:opacity-50"
          :class="orderForm.side === 'buy' ? 'bg-upPrice hover:bg-red-500' : 'bg-downPrice hover:bg-emerald-500'"
          :disabled="submitting"
          @click="submitOrder"
        >
          {{ submitting ? '提交中' : orderForm.side === 'buy' ? '提交买单' : '提交卖单' }}
        </button>

        <div class="rounded-xl border border-darkBorder/50 bg-darkBg/50 p-3 text-xs leading-6 text-slate-500">
          买价低于当前价或卖价高于当前价会进入待成交队列；撤单会释放冻结资金或持仓。
        </div>
      </section>

      <section class="grid min-h-0 gap-4 lg:grid-rows-[minmax(0,1fr)_minmax(0,1fr)]">
        <div class="grid min-h-0 gap-4 lg:grid-cols-2">
          <div class="glass-panel min-h-0 overflow-auto">
            <div class="sticky top-0 z-10 border-b border-darkBorder/50 bg-darkBg/95 px-4 py-3">
              <div class="data-label">当前持仓</div>
            </div>
            <table class="w-full whitespace-nowrap text-left">
              <thead class="text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600">
                <tr>
                  <th class="px-4 py-3">股票</th>
                  <th class="px-4 py-3 text-right">可用/总量</th>
                  <th class="px-4 py-3 text-right">盈亏</th>
                  <th class="px-4 py-3 text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in positions" :key="item.stockCode" class="border-t border-darkBorder/40">
                  <td class="px-4 py-3">
                    <div class="font-mono text-sm font-bold text-white">{{ item.stockCode }}</div>
                    <div class="text-xs text-slate-500">{{ item.stockName }}</div>
                  </td>
                  <td class="px-4 py-3 text-right font-mono text-sm text-slate-300">{{ item.availableQuantity }}/{{ item.quantity }}</td>
                  <td class="px-4 py-3 text-right font-mono text-sm" :class="numClass(item.profit)">
                    {{ money(item.profit) }}
                  </td>
                  <td class="px-4 py-3 text-center">
                    <button class="rounded-lg border border-downPrice/25 px-2.5 py-1 text-[10px] font-bold text-downPrice hover:bg-downPrice/10" @click="fillSellForm(item)">
                      卖出
                    </button>
                  </td>
                </tr>
                <tr v-if="positions.length === 0">
                  <td colspan="4" class="py-12 text-center font-mono text-sm text-slate-600">暂无持仓</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="glass-panel min-h-0 overflow-auto">
            <div class="sticky top-0 z-10 border-b border-darkBorder/50 bg-darkBg/95 px-4 py-3">
              <div class="data-label">待成交订单</div>
            </div>
            <table class="w-full whitespace-nowrap text-left">
              <thead class="text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600">
                <tr>
                  <th class="px-4 py-3">订单</th>
                  <th class="px-4 py-3 text-right">价格/数量</th>
                  <th class="px-4 py-3 text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pendingOrders" :key="item.orderId" class="border-t border-darkBorder/40">
                  <td class="px-4 py-3">
                    <div class="font-mono text-sm font-bold text-white">{{ item.stockCode }}</div>
                    <div class="text-xs" :class="item.orderType === 'buy' ? 'text-upPrice' : 'text-downPrice'">{{ typeLabel(item.orderType) }}</div>
                  </td>
                  <td class="px-4 py-3 text-right font-mono text-sm text-slate-300">
                    {{ money(item.price) }} / {{ item.quantity }}
                  </td>
                  <td class="px-4 py-3 text-center">
                    <button class="rounded-lg border border-red-400/25 px-2.5 py-1 text-[10px] font-bold text-red-300 hover:bg-red-400/10" @click="cancelOrder(item.orderId)">
                      撤单
                    </button>
                  </td>
                </tr>
                <tr v-if="pendingOrders.length === 0">
                  <td colspan="3" class="py-12 text-center font-mono text-sm text-slate-600">暂无待成交订单</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="glass-panel min-h-0 overflow-auto">
          <div class="sticky top-0 z-10 flex items-center justify-between border-b border-darkBorder/50 bg-darkBg/95 px-4 py-3">
            <div class="data-label">成交记录</div>
            <span class="font-mono text-[10px] text-slate-600">{{ records.length }} 条</span>
          </div>
          <table class="w-full whitespace-nowrap text-left">
            <thead class="text-[10px] font-semibold uppercase tracking-[0.15em] text-slate-600">
              <tr>
                <th class="px-4 py-3">股票</th>
                <th class="px-4 py-3">方向</th>
                <th class="px-4 py-3 text-right">价格</th>
                <th class="px-4 py-3 text-right">数量</th>
                <th class="px-4 py-3 text-right">费用</th>
                <th class="px-4 py-3 text-right">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in records" :key="item.recordId" class="border-t border-darkBorder/40">
                <td class="px-4 py-3 font-mono text-sm font-bold text-white">{{ item.stockCode }}</td>
                <td class="px-4 py-3 text-sm" :class="item.tradeType === 'buy' ? 'text-upPrice' : 'text-downPrice'">{{ typeLabel(item.tradeType) }}</td>
                <td class="px-4 py-3 text-right font-mono text-sm text-slate-300">{{ money(item.price) }}</td>
                <td class="px-4 py-3 text-right font-mono text-sm text-slate-300">{{ item.quantity }}</td>
                <td class="px-4 py-3 text-right font-mono text-sm text-slate-500">{{ money(item.fee) }}</td>
                <td class="px-4 py-3 text-right font-mono text-xs text-slate-500">{{ dateTime(item.tradeTime) }}</td>
              </tr>
              <tr v-if="records.length === 0">
                <td colspan="6" class="py-12 text-center font-mono text-sm text-slate-600">暂无成交记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import request from '../utils/request'
import { useToastStore } from '../stores/toast'
import type { AccountVO, OrderVO, PageVO, PositionVO, TradeOrderDTO, TradeRecordVO } from '../types'

type OrderSide = 'buy' | 'sell'

const toast = useToastStore()
const loading = ref(false)
const submitting = ref(false)
const account = ref<AccountVO | null>(null)
const positions = ref<PositionVO[]>([])
const orders = ref<OrderVO[]>([])
const records = ref<TradeRecordVO[]>([])

const orderForm = reactive<TradeOrderDTO & { side: OrderSide }>({
  side: 'buy',
  stockCode: '600519',
  price: 100,
  quantity: 100
})

const pendingOrders = computed(() => orders.value.filter(item => item.status === 'pending'))

const accountMetrics = computed(() => [
  { label: '总资产', value: money(account.value?.totalAssets), cls: 'text-white' },
  { label: '可用资金', value: money(account.value?.availableCash), cls: 'text-primary' },
  { label: '冻结资金', value: money(account.value?.frozenCash), cls: 'text-accent' },
  { label: '累计盈亏', value: money(account.value?.totalProfit), cls: numClass(account.value?.totalProfit) },
])

const loadData = async () => {
  loading.value = true
  try {
    const [accountData, positionData, orderPage, recordPage] = await Promise.all([
      request.get<AccountVO>('/trade/account'),
      request.get<PositionVO[]>('/trade/positions'),
      request.get<PageVO<OrderVO>>('/trade/orders', { params: { page: 1, pageSize: 30 } }),
      request.get<PageVO<TradeRecordVO>>('/trade/records', { params: { page: 1, pageSize: 30 } })
    ])
    account.value = accountData
    positions.value = Array.isArray(positionData) ? positionData : []
    orders.value = Array.isArray(orderPage.records) ? orderPage.records : []
    records.value = Array.isArray(recordPage.records) ? recordPage.records : []
  } finally {
    loading.value = false
  }
}

const submitOrder = async () => {
  if (!orderForm.stockCode || !orderForm.price || !orderForm.quantity) return
  submitting.value = true
  try {
    const payload = {
      stockCode: orderForm.stockCode,
      price: Number(orderForm.price),
      quantity: Number(orderForm.quantity)
    }
    const order = await request.post<OrderVO>(`/trade/${orderForm.side}`, payload)
    toast.success(order.status === 'pending' ? '订单已挂起' : '订单已成交')
    await loadData()
  } catch (error: any) {
    toast.error(error.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

const cancelOrder = async (orderId: number) => {
  try {
    await request.delete(`/trade/orders/${orderId}`)
    toast.success('撤单成功')
    await loadData()
  } catch (error: any) {
    toast.error(error.message || '撤单失败')
  }
}

const fillSellForm = (position: PositionVO) => {
  orderForm.side = 'sell'
  orderForm.stockCode = position.stockCode
  orderForm.price = Number(position.currentPrice || position.costPrice || 0)
  orderForm.quantity = Math.max(100, Math.min(position.availableQuantity, position.quantity))
}

const money = (value?: number) => `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const numClass = (value?: number) => Number(value || 0) >= 0 ? 'text-upPrice' : 'text-downPrice'
const typeLabel = (type: string) => type === 'buy' ? '买入' : '卖出'
const dateTime = (value: string) => {
  if (!value) return '--'
  const date = new Date(value.includes('T') ? value : value.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadData)
</script>
