<template>
  <div class="grid h-full min-h-0 gap-5 xl:grid-cols-[340px_minmax(0,1fr)]">
    <aside class="glass-panel flex min-h-0 flex-col overflow-hidden p-5">
      <div class="mb-5 flex items-start justify-between gap-3">
        <div>
          <div class="data-label text-primary/80">AI 问答</div>
          <h1 class="mt-1 font-display text-2xl font-black tracking-tight text-white">研究副驾驶</h1>
          <p class="mt-2 text-sm leading-6 text-slate-500">像正常聊天一样围绕单只股票开新对话，不再把历史全堆进当前会话。</p>
        </div>
        <button
          type="button"
          class="shrink-0 rounded-2xl border border-primary/20 bg-primary/10 px-3 py-2 text-xs font-black tracking-[0.12em] text-primary transition hover:bg-primary/15"
          @click="startNewChat"
        >新建对话</button>
      </div>

      <div class="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
        <div>
          <label class="data-label">股票代码</label>
          <div class="mt-2 flex gap-2">
            <input
              v-model.trim="codeInput"
              type="text"
              class="cyber-input h-10 flex-1"
              placeholder="例如 600519"
              @keydown.enter.prevent="applyCode"
            />
            <button
              type="button"
              class="rounded-xl border border-primary/20 bg-primary/10 px-4 text-sm font-bold text-primary transition hover:bg-primary/15"
              @click="applyCode"
            >切换</button>
          </div>
        </div>

        <div class="rounded-[28px] border border-primary/15 bg-[linear-gradient(180deg,rgba(10,22,34,0.96),rgba(7,14,24,0.96))] p-4 shadow-[0_18px_48px_rgba(2,8,18,0.24)]">
          <div class="mb-3 flex items-center justify-between gap-2">
            <div class="data-label">当前会话</div>
            <div
              class="rounded-full border px-2.5 py-1 text-[10px] font-mono font-semibold uppercase tracking-[0.16em]"
              :class="isDraftMode ? 'border-primary/20 bg-primary/10 text-primary' : 'border-darkBorder/60 bg-darkBg/70 text-slate-400'"
            >
              {{ isDraftMode ? 'Draft' : 'History' }}
            </div>
          </div>
          <div class="rounded-2xl border border-darkBorder/60 bg-darkBg/55 p-4">
            <div class="text-sm font-semibold text-white">{{ activeThreadTitle }}</div>
            <div class="mt-2 text-xs leading-6 text-slate-500">{{ activeThreadDescription }}</div>
          </div>
        </div>

        <div class="rounded-2xl border border-darkBorder/60 bg-darkBg/60 p-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="text-lg font-display font-black text-white">{{ detail?.stockName || '未加载' }}</div>
              <div class="mt-1 text-xs font-mono text-slate-500">{{ currentCode }}</div>
            </div>
            <div class="rounded-full border border-primary/20 bg-primary/6 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.16em] text-primary/80">
              AI Context
            </div>
          </div>
          <div class="mt-4 space-y-3">
            <div class="flex items-center justify-between text-sm">
              <span class="text-slate-500">最新价</span>
              <span class="font-mono font-semibold text-white">{{ formatPrice(detail?.currentPrice) }}</span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-slate-500">涨跌幅</span>
              <span class="font-mono font-semibold" :class="Number(detail?.changeRate || 0) >= 0 ? 'text-upPrice' : 'text-downPrice'">
                {{ signedPercent(detail?.changeRate) }}
              </span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-slate-500">趋势</span>
              <span class="font-semibold text-slate-200">{{ sentiment?.trendPrediction || '--' }}</span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-slate-500">建议</span>
              <span class="font-semibold text-primary">{{ sentiment?.operationAdvice || '--' }}</span>
            </div>
          </div>
        </div>

        <div class="rounded-2xl border border-darkBorder/60 bg-darkBg/60 p-4">
          <div class="data-label mb-3">快捷问题</div>
          <div class="space-y-2">
            <button
              v-for="prompt in prompts"
              :key="prompt"
              type="button"
              class="w-full rounded-xl border border-darkBorder/60 bg-darkCard/40 px-3 py-2.5 text-left text-sm leading-6 text-slate-300 transition hover:border-primary/30 hover:bg-primary/6 hover:text-white"
              @click="usePrompt(prompt)"
            >{{ prompt }}</button>
          </div>
        </div>

        <div v-if="risk" class="rounded-2xl border border-downPrice/20 bg-downPrice/5 p-4">
          <div class="data-label mb-2 text-downPrice/80">风险摘要</div>
          <p class="text-sm leading-6 text-slate-300">{{ risk.riskWarning || '暂无风险摘要' }}</p>
        </div>

        <div class="rounded-2xl border border-darkBorder/60 bg-darkBg/60 p-4">
          <div class="mb-3 flex items-center justify-between gap-2">
            <div class="data-label">会话列表</div>
            <div class="text-[10px] font-mono text-slate-600">{{ sessions.length + 1 }} 个</div>
          </div>

          <button
            type="button"
            class="w-full rounded-2xl border px-3 py-3 text-left transition"
            :class="isDraftMode ? 'border-primary/35 bg-primary/10' : 'border-darkBorder/60 bg-darkCard/40 hover:border-primary/25 hover:bg-primary/6'"
            @click="openDraftChat"
          >
            <div class="flex items-center justify-between gap-2">
              <div class="text-sm font-semibold text-slate-100">{{ draftThreadTitle }}</div>
              <div
                class="rounded-full border px-2 py-0.5 text-[10px] font-mono uppercase tracking-[0.14em]"
                :class="isDraftMode ? 'border-primary/25 bg-primary/10 text-primary' : 'border-darkBorder/60 bg-darkBg/70 text-slate-500'"
              >
                {{ loading ? '思考中' : (draftMessages.length ? '当前' : '空白') }}
              </div>
            </div>
            <div class="mt-2 line-clamp-2 text-xs leading-6 text-slate-500">{{ draftThreadPreview }}</div>
          </button>

          <div class="mb-3 mt-4 flex items-center justify-between gap-2">
            <div class="data-label">历史记录</div>
            <div class="text-[10px] font-mono text-slate-600">{{ sessions.length }} 条</div>
          </div>

          <div v-if="sessions.length" class="max-h-[240px] space-y-2 overflow-y-auto pr-1">
            <button
              v-for="item in sessionTimeline"
              :key="item.sessionId"
              type="button"
              class="w-full rounded-2xl border px-3 py-2.5 text-left transition"
              :class="activeSessionId === item.sessionId && !isDraftMode ? 'border-primary/35 bg-primary/10' : 'border-darkBorder/60 bg-darkCard/40 hover:border-primary/25 hover:bg-primary/6'"
              @click="openSession(item.sessionId)"
            >
              <div class="line-clamp-2 text-sm font-semibold leading-6 text-slate-100">{{ item.title }}</div>
              <div class="mt-2 flex items-center justify-between gap-2 text-[10px] font-mono uppercase tracking-[0.14em] text-slate-600">
                <span>{{ formatDateTime(item.lastActive || item.createdAt || '') }}</span>
                <span class="text-primary/80">{{ activeSessionId === item.sessionId && !isDraftMode ? '查看中' : '打开' }}</span>
              </div>
            </button>
          </div>

          <div v-else class="rounded-xl border border-dashed border-darkBorder/60 bg-darkCard/30 px-3 py-5 text-center text-xs leading-6 text-slate-500">
            还没有可回看的问答记录
          </div>
        </div>
      </div>
    </aside>

    <section class="glass-panel flex h-full min-h-0 flex-col overflow-hidden">
      <div class="border-b border-darkBorder/60 px-5 py-4">
        <div class="flex items-center justify-between gap-3">
          <div>
            <div class="data-label">Conversation</div>
            <h2 class="mt-1 font-display text-xl font-black tracking-tight text-white">{{ detail?.stockName || currentCode }} 问答席</h2>
            <div class="mt-2 text-xs text-slate-500">{{ activeThreadDescription }}</div>
          </div>
          <div class="flex items-center gap-2">
            <div
              class="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[10px] font-mono font-semibold uppercase tracking-[0.18em]"
              :class="loading ? 'border-primary/30 bg-primary/8 text-primary' : 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300'"
            >
              <span class="h-1.5 w-1.5 rounded-full" :class="loading ? 'bg-primary animate-pulse' : 'bg-emerald-300'" />
              {{ loading ? 'AI 思考中' : '已连接' }}
            </div>
            <button
              type="button"
              class="rounded-xl border border-darkBorder/60 bg-darkBg/60 px-3 py-2 text-xs font-bold text-slate-400 transition hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="loading || refreshing"
              @click="refreshHistory"
            >{{ refreshing ? '同步中' : '同步记录' }}</button>
            <button
              type="button"
              class="rounded-xl border border-primary/20 bg-primary/8 px-3 py-2 text-xs font-bold text-primary transition hover:bg-primary/12 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="loading"
              @click="startNewChat"
            >新建对话</button>
          </div>
        </div>
      </div>

      <div ref="chatViewport" class="min-h-0 flex-1 overflow-y-auto bg-[radial-gradient(circle_at_top,rgba(34,211,238,0.08),transparent_34%),linear-gradient(180deg,rgba(5,10,18,0.98),rgba(6,12,22,0.9))]">
        <div class="mx-auto flex h-full w-full max-w-5xl flex-col px-5 py-6">
          <div v-if="activeChatMessages.length" class="flex flex-1 flex-col gap-4">
            <article
              v-for="message in activeChatMessages"
              :key="message.id"
              :id="`message-${message.id}`"
              class="flex"
              :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
            >
              <div class="max-w-[min(84%,760px)]">
                <div
                  class="mb-2 flex items-center gap-2 text-[11px] font-mono uppercase tracking-[0.16em]"
                  :class="message.role === 'user' ? 'justify-end text-slate-500' : 'text-primary/80'"
                >
                  <span
                    class="flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-black"
                    :class="message.role === 'user' ? 'bg-white text-darkBg' : 'bg-primary text-darkBg'"
                  >
                    {{ message.role === 'user' ? 'Q' : 'AI' }}
                  </span>
                  <span>{{ message.role === 'user' ? '你' : 'SmartStock AI' }}</span>
                  <span>{{ formatDateTime(message.createdAt) }}</span>
                </div>

                <div
                  class="rounded-[26px] border px-4 py-3.5 shadow-[0_20px_60px_rgba(4,10,18,0.22)]"
                  :class="messageBubbleClass(message)"
                >
                  <div v-if="message.status === 'pending'" class="flex items-center gap-3 text-sm text-slate-200">
                    <div class="flex items-center gap-1.5">
                      <span class="h-2 w-2 rounded-full bg-primary/90 animate-pulse" />
                      <span class="h-2 w-2 rounded-full bg-primary/60 animate-pulse [animation-delay:0.18s]" />
                      <span class="h-2 w-2 rounded-full bg-primary/40 animate-pulse [animation-delay:0.36s]" />
                    </div>
                    <div class="space-y-1">
                      <div class="font-semibold text-white">正在思考</div>
                      <div class="text-xs text-slate-400">问题已经发出，答案生成后会直接落在这里。</div>
                    </div>
                  </div>

                  <div v-else class="space-y-2">
                    <p v-if="message.role === 'user'" class="whitespace-pre-wrap text-sm leading-7">{{ message.content }}</p>
                    <div
                      v-else
                      class="markdown-body text-sm leading-7"
                      v-html="renderMessageContent(message)"
                    ></div>
                    <div
                      v-if="message.status === 'error'"
                      class="inline-flex items-center rounded-full border border-downPrice/20 bg-downPrice/10 px-2.5 py-1 text-[11px] font-semibold text-downPrice"
                    >
                      这次请求失败了，可以直接修改问题后重试
                    </div>
                  </div>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="flex flex-1 items-center justify-center">
            <div class="w-full max-w-3xl rounded-[32px] border border-darkBorder/60 bg-[linear-gradient(180deg,rgba(8,16,28,0.96),rgba(5,10,18,0.88))] p-8 text-center shadow-[0_28px_80px_rgba(2,8,18,0.45)]">
              <div class="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-[10px] font-mono font-semibold uppercase tracking-[0.18em] text-primary/80">
                <span class="live-dot" />
                AI 就绪
              </div>
              <h3 class="mt-5 font-display text-3xl font-black tracking-tight text-white">像正常聊天一样直接问</h3>
              <p class="mx-auto mt-3 max-w-2xl text-sm leading-7 text-slate-400">
                输入问题后会先显示你的消息，再立即出现 AI 的回复占位，不会再只剩一个转圈按钮。
              </p>

              <div class="mt-8 grid gap-3 text-left md:grid-cols-3">
                <button
                  v-for="prompt in prompts"
                  :key="prompt"
                  type="button"
                  class="rounded-2xl border border-darkBorder/60 bg-darkBg/60 p-4 text-sm leading-6 text-slate-300 transition hover:border-primary/30 hover:bg-primary/6 hover:text-white"
                  @click="usePrompt(prompt)"
                >
                  {{ prompt }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="shrink-0 border-t border-darkBorder/60 bg-darkBg/55 px-5 py-4">
        <div class="mx-auto w-full max-w-5xl">
          <div class="rounded-[28px] border border-darkBorder/60 bg-[#07101c] p-3 shadow-[0_18px_50px_rgba(0,0,0,0.28)] transition focus-within:border-primary/30">
            <textarea
              ref="composerRef"
              v-model="question"
              rows="1"
              class="min-h-[104px] w-full resize-none bg-transparent px-3 py-2 text-sm leading-7 text-slate-100 outline-none placeholder:text-slate-600"
              :placeholder="`向 ${detail?.stockName || currentCode} 提问，例如：短线是否应该继续观望？`"
              @keydown="handleComposerKeydown"
            ></textarea>

            <div class="mt-2 flex items-center justify-between gap-3 border-t border-darkBorder/50 px-3 pt-3">
              <div class="space-y-1">
                <div class="text-[10px] font-mono uppercase tracking-[0.18em] text-slate-600">Enter 发送，Shift + Enter 换行</div>
                <div class="text-xs text-slate-500">{{ loading ? '问题已发送，正在等待 AI 返回结果…' : (isDraftMode ? '当前是新对话，发送后会在上方形成新的聊天记录。' : '你也可以在这里继续追问，界面会按当前会话继续展开。') }}</div>
              </div>
              <button
                type="button"
                class="rounded-2xl bg-primary px-5 py-3 text-sm font-display font-black tracking-[0.14em] text-darkBg transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="loading || !question.trim()"
                @click="ask"
              >{{ loading ? '等待回复' : '发送给 AI' }}</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { renderMarkdown } from '../utils/markdown'
import { useToastStore } from '../stores/toast'
import type { QaAnswerVO, QaSessionDetailVO, QaSessionMessageVO, QaSessionVO, StockDetailVO } from '../types'

interface AnalysisSentimentVO {
  stockCode: string
  stockName: string
  sentimentScore: number | null
  sentimentLabel: string | null
  trendPrediction: string | null
  operationAdvice: string | null
  analysisSummary: string | null
  createdAt: string | null
}

interface AnalysisRiskVO {
  stockCode: string
  stockName: string
  riskWarning: string | null
  latestNews: string | null
  riskAlerts: string[]
  createdAt: string | null
}

interface ChatMessage {
  id: string
  groupId?: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  status: 'done' | 'pending' | 'error'
}

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const currentCode = ref((route.params.code as string) || '600519')
const codeInput = ref(currentCode.value)
const question = ref('')
const loading = ref(false)
const refreshing = ref(false)
const sessions = ref<QaSessionVO[]>([])
const detail = ref<StockDetailVO | null>(null)
const sentiment = ref<AnalysisSentimentVO | null>(null)
const risk = ref<AnalysisRiskVO | null>(null)
const draftMessages = ref<ChatMessage[]>([])
const sessionMessages = ref<ChatMessage[]>([])
const activeSessionId = ref<string | null>(null)
const chatViewport = ref<HTMLElement | null>(null)
const composerRef = ref<HTMLTextAreaElement | null>(null)

const prompts = computed(() => {
  const name = detail.value?.stockName || currentCode.value
  return [
    `${name} 现在更适合观望还是分批介入？`,
    `${name} 当前最需要关注的风险点是什么？`,
    `${name} 如果我只看未来 3 到 5 个交易日，该怎么理解现在的走势？`,
  ]
})

const sessionTimeline = computed(() => {
  return [...sessions.value].sort((left, right) => {
    return toTimestamp(right.lastActive || right.createdAt || '') - toTimestamp(left.lastActive || left.createdAt || '')
  })
})

const isDraftMode = computed(() => activeSessionId.value == null)

const activeSession = computed(() => {
  if (!activeSessionId.value) return null
  return sessions.value.find((item) => item.sessionId === activeSessionId.value) || null
})

const activeChatMessages = computed<ChatMessage[]>(() => {
  if (isDraftMode.value) {
    return draftMessages.value
  }
  return sessionMessages.value
})

const activeThreadTitle = computed(() => {
  if (isDraftMode.value) {
    const firstUserMessage = draftMessages.value.find((message) => message.role === 'user')
    return firstUserMessage?.content || '新对话'
  }
  return activeSession.value?.title || '历史对话'
})

const draftThreadTitle = computed(() => {
  const firstUserMessage = draftMessages.value.find((message) => message.role === 'user')
  return firstUserMessage?.content || '新建对话'
})

const draftThreadPreview = computed(() => {
  if (!draftMessages.value.length) {
    return '点击这里回到空白会话，也可以直接点右上角新建对话重新开始。'
  }
  const lastAssistantMessage = [...draftMessages.value]
    .reverse()
    .find((message) => message.role === 'assistant')
  if (!lastAssistantMessage) {
    return '你已经发出了问题，可以继续追问。'
  }
  if (lastAssistantMessage.status === 'pending') {
    return 'AI 正在生成回复，完成后会直接显示在当前会话。'
  }
  return lastAssistantMessage.content
})

const activeThreadDescription = computed(() => {
  if (isDraftMode.value) {
    if (draftMessages.value.length) {
      return '这是当前新建对话，会随你的继续追问向下展开。'
    }
    return '点击快捷问题或直接输入，开始一段新的问答。'
  }
  return `正在查看 ${formatDateTime(String(activeSession.value?.lastActive || activeSession.value?.createdAt || ''))} 的真实会话记录。`
})

const formatPrice = (value: unknown) => Number(value || 0).toFixed(2)
const signedPercent = (value: unknown) => {
  const n = Number(value || 0)
  return `${n >= 0 ? '+' : ''}${n.toFixed(2)}%`
}

const formatDateTime = (value: string) => {
  if (!value) return '--'
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const toTimestamp = (value: string) => {
  if (!value) return 0
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const timestamp = new Date(normalized).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const scrollToBottom = async (behavior: ScrollBehavior = 'smooth') => {
  await nextTick()
  if (!chatViewport.value) return
  chatViewport.value.scrollTo({
    top: chatViewport.value.scrollHeight,
    behavior,
  })
}

const openDraftChat = async () => {
  activeSessionId.value = null
  await scrollToBottom('smooth')
  await focusComposer()
}

const messageBubbleClass = (message: ChatMessage) => {
  if (message.role === 'user') {
    return 'border-primary/20 bg-[linear-gradient(135deg,rgba(34,211,238,0.22),rgba(14,116,144,0.32))] text-white'
  }
  if (message.status === 'error') {
    return 'border-downPrice/20 bg-[linear-gradient(180deg,rgba(55,16,19,0.92),rgba(25,10,14,0.96))] text-slate-100'
  }
  return 'border-darkBorder/70 bg-[linear-gradient(180deg,rgba(9,17,30,0.96),rgba(6,12,22,0.92))] text-slate-200'
}

const renderMessageContent = (message: ChatMessage) => renderMarkdown(message.content)

const focusComposer = async () => {
  await nextTick()
  composerRef.value?.focus()
}

const usePrompt = async (prompt: string) => {
  question.value = prompt
  await focusComposer()
}

const startNewChat = async () => {
  activeSessionId.value = null
  draftMessages.value = []
  sessionMessages.value = []
  question.value = ''
  await scrollToBottom('auto')
  await focusComposer()
}

const fetchDetail = async () => {
  detail.value = await request.get<StockDetailVO>(`/market/stocks/${currentCode.value}`)
}

const fetchContext = async () => {
  const [sentimentRes, riskRes] = await Promise.all([
    request.get<AnalysisSentimentVO>('/analysis/sentiment', { params: { stockCode: currentCode.value } }),
    request.get<AnalysisRiskVO>('/analysis/risk', { params: { stockCode: currentCode.value } }),
  ])
  sentiment.value = sentimentRes
  risk.value = riskRes
}

const toChatMessage = (message: QaSessionMessageVO): ChatMessage => ({
  id: message.id,
  role: message.role,
  content: message.content,
  createdAt: message.createdAt || new Date().toISOString(),
  status: 'done',
})

const fetchSessions = async () => {
  sessions.value = await request.get<QaSessionVO[]>('/qa/sessions', {
    params: { stockCode: currentCode.value, limit: 20 },
  })
  if (activeSessionId.value && !sessions.value.some((item) => item.sessionId === activeSessionId.value)) {
    activeSessionId.value = null
    sessionMessages.value = []
  }
}

const fetchSessionDetail = async (sessionId: string, behavior: ScrollBehavior = 'auto') => {
  const detail = await request.get<QaSessionDetailVO>(`/qa/sessions/${encodeURIComponent(sessionId)}`, {
    params: { limit: 100 },
  })
  sessionMessages.value = detail.messages.map(toChatMessage)
  await scrollToBottom(behavior)
}

const openSession = async (sessionId: string) => {
  activeSessionId.value = sessionId
  await fetchSessionDetail(sessionId, 'smooth')
}

const refreshHistory = async () => {
  refreshing.value = true
  try {
    await fetchSessions()
    if (activeSessionId.value) {
      await fetchSessionDetail(activeSessionId.value, 'auto')
    }
  } catch (err: any) {
    toast.error(err.message || '同步问答记录失败')
  } finally {
    refreshing.value = false
  }
}

const loadPage = async (behavior: ScrollBehavior = 'auto') => {
  draftMessages.value = []
  sessionMessages.value = []
  activeSessionId.value = null
  try {
    await Promise.all([fetchDetail(), fetchContext(), fetchSessions()])
    await scrollToBottom(behavior)
  } catch (err: any) {
    toast.error(err.message || '加载 AI 问答页面失败')
  }
}

const ask = async () => {
  const asked = question.value.trim()
  if (!asked || loading.value) return

  const wasDraftMode = isDraftMode.value
  const targetSessionId = activeSessionId.value
  loading.value = true
  question.value = ''

  const groupId = `local-${Date.now()}`
  const createdAt = new Date().toISOString()
  const pendingMessages: ChatMessage[] = [
    {
      id: `${groupId}-user`,
      groupId,
      role: 'user',
      content: asked,
      createdAt,
      status: 'done',
    },
    {
      id: `${groupId}-assistant`,
      groupId,
      role: 'assistant',
      content: '',
      createdAt,
      status: 'pending',
    },
  ]

  if (wasDraftMode) {
    draftMessages.value = [...draftMessages.value, ...pendingMessages]
  } else {
    sessionMessages.value = [...sessionMessages.value, ...pendingMessages]
  }

  await scrollToBottom('smooth')

  try {
    const response = await request.post<QaAnswerVO>(
      '/qa/ask',
      { stockCode: currentCode.value, question: asked, sessionId: targetSessionId },
      { timeout: 120000 }
    )

    if (wasDraftMode) {
      const nextDraftMessages = draftMessages.value.map((message) => {
        if (message.id !== `${groupId}-assistant`) {
          return message
        }
        return {
          ...message,
          content: response.answer,
          createdAt: response.createdAt,
          status: 'done',
        }
      })
      sessionMessages.value = nextDraftMessages
      draftMessages.value = []
    } else {
      sessionMessages.value = sessionMessages.value.map((message) => {
        if (message.id !== `${groupId}-assistant`) {
          return message
        }
        return {
          ...message,
          content: response.answer,
          createdAt: response.createdAt,
          status: 'done',
        }
      })
    }

    activeSessionId.value = response.sessionId
    await Promise.all([
      fetchSessions(),
      fetchSessionDetail(response.sessionId, 'smooth'),
      fetchContext(),
    ])
  } catch (err: any) {
    if (wasDraftMode) {
      draftMessages.value = draftMessages.value.map((message) => {
        if (message.id !== `${groupId}-assistant`) {
          return message
        }
        return {
          ...message,
          content: err.message || 'AI 问答失败',
          status: 'error',
        }
      })
    } else {
      sessionMessages.value = sessionMessages.value.map((message) => {
        if (message.id !== `${groupId}-assistant`) {
          return message
        }
        return {
          ...message,
          content: err.message || 'AI 问答失败',
          status: 'error',
        }
      })
    }
    toast.error(err.message || 'AI 问答失败')
  } finally {
    loading.value = false
    await focusComposer()
  }
}

const handleComposerKeydown = async (event: KeyboardEvent) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing) {
    return
  }
  event.preventDefault()
  await ask()
}

const applyCode = async () => {
  const nextCode = codeInput.value.trim()
  if (!nextCode || nextCode === currentCode.value) return
  router.replace(`/copilot/${nextCode}`)
}

watch(() => route.params.code, async (value) => {
  const nextCode = (value as string) || '600519'
  currentCode.value = nextCode
  codeInput.value = nextCode
  await loadPage('auto')
})

watch(activeChatMessages, async () => {
  await scrollToBottom('smooth')
}, { deep: true })

onMounted(async () => {
  await loadPage('auto')
  await focusComposer()
})
</script>
