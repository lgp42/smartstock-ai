<template>
  <div class="h-full flex flex-col gap-4">
    <!-- Header -->
    <div class="glass-panel px-5 py-3.5 shrink-0 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <span class="w-1 h-4 bg-primary rounded-full"></span>
        <h2 class="font-display font-bold text-base text-white">市场快讯</h2>
        <span class="tag-badge text-primary/70 border-primary/20 bg-primary/5">AI 情报</span>
      </div>
      <div class="flex items-center gap-2">
        <input v-model="keyword" type="text" placeholder="搜索关键词 / 股票代码..."
          class="cyber-input h-8 w-56 text-xs" />
        <button class="text-xs font-semibold text-primary px-3 py-1.5 rounded-lg border border-primary/25 bg-primary/5 hover:bg-primary/10 transition">
          订阅推送
        </button>
      </div>
    </div>

    <div class="flex-1 flex gap-4 min-h-0">
      <!-- Sidebar -->
      <div class="w-52 glass-panel flex flex-col min-h-0 shrink-0 p-3">
        <!-- Categories -->
        <div class="space-y-0.5">
          <button
            v-for="(cat, id) in categories"
            :key="id"
            @click="activeCategory = id"
            class="w-full text-left px-3 py-2.5 rounded-lg transition-all text-sm font-medium flex items-center justify-between"
            :class="activeCategory === id
              ? 'bg-primary/10 text-white border-l-2 border-primary pl-2.5'
              : 'text-slate-500 hover:bg-darkCardHover/40 hover:text-slate-300 border-l-2 border-transparent pl-2.5'"
          >
            <span>{{ cat.name }}</span>
            <span v-if="cat.unread" class="inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-md text-[10px] font-bold bg-upPrice/15 text-upPrice font-mono">
              {{ cat.unread }}
            </span>
          </button>
        </div>

        <div class="border-t border-darkBorder/40 mt-4 pt-4">
          <div class="data-label mb-3 px-1">热门主题</div>
          <div class="flex flex-wrap gap-1.5 px-1">
            <span class="px-2 py-1 border border-darkBorder/60 bg-darkBg/50 text-slate-500 rounded-lg text-[11px] hover:text-slate-300 hover:border-slate-600 cursor-pointer transition font-mono">#低空经济</span>
            <span class="px-2 py-1 border border-upPrice/20 bg-upPrice/5 text-upPrice/70 rounded-lg text-[11px] hover:bg-upPrice/10 cursor-pointer transition font-mono">#固态电池</span>
            <span class="px-2 py-1 border border-darkBorder/60 bg-darkBg/50 text-slate-500 rounded-lg text-[11px] hover:text-slate-300 cursor-pointer transition font-mono">#鸿蒙原生</span>
            <span class="px-2 py-1 border border-primary/20 bg-primary/5 text-primary/70 rounded-lg text-[11px] hover:bg-primary/10 cursor-pointer transition font-mono">#算力租赁</span>
          </div>
        </div>
      </div>

      <!-- News feed -->
      <div ref="feedContainerRef" class="flex-1 glass-panel flex flex-col min-h-0 overflow-y-auto p-4" @scroll.passive="handleFeedScroll">
        <div class="space-y-3 max-w-3xl mx-auto w-full">
          <div
            v-for="(news, index) in filteredNews"
            :key="news.id || `${news.url || news.title}-${news.time}-${index}`"
            class="rounded-xl border bg-darkBg/40 hover:bg-darkCardHover/30 transition-all group relative overflow-hidden cursor-pointer"
            :class="news.importance === 'high' ? 'border-upPrice/20' : 'border-darkBorder/50'"
            @dblclick="openNews(news.url)"
          >
            <!-- Left accent bar -->
            <div class="absolute top-0 left-0 w-0.5 h-full"
              :class="news.importance === 'high' ? 'bg-upPrice' : news.importance === 'medium' ? 'bg-primary/50' : 'bg-darkBorder/60'">
            </div>

            <div class="pl-4 pr-4 pt-3.5 pb-3.5">
              <!-- Meta row -->
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-mono text-[11px] font-semibold"
                    :class="news.importance === 'high' ? 'text-upPrice' : 'text-slate-500'">
                    {{ news.time }}
                  </span>
                  <span class="tag-badge"
                    :class="news.importance === 'high' ? 'border-upPrice/20 bg-upPrice/5 text-upPrice/70' : ''">
                    {{ categories[news.category]?.name }}
                  </span>
                  <span v-for="tag in news.tags" :key="tag" class="text-[11px] text-primary/60 font-mono">#{{ tag }}</span>
                </div>
                <div class="opacity-0 group-hover:opacity-100 transition-opacity flex gap-3">
                  <button class="text-[11px] text-slate-500 hover:text-slate-300 transition font-mono">分享</button>
                  <button class="text-[11px] text-slate-500 hover:text-slate-300 transition font-mono">收藏</button>
                </div>
              </div>

              <!-- Title -->
              <h3 class="text-sm font-semibold text-slate-200 leading-snug group-hover:text-white transition-colors mb-1.5"
                @click="openNews(news.url)">
                {{ news.title }}
              </h3>

              <!-- Summary -->
              <p v-if="news.summary && news.summary !== news.title" class="text-xs text-slate-500 leading-relaxed line-clamp-2">
                {{ news.summary }}
              </p>

              <!-- Related stocks -->
              <div v-if="news.relatedStocks.length" class="mt-2.5 pt-2.5 border-t border-darkBorder/30 flex items-center gap-3">
                <span class="data-label shrink-0">相关</span>
                <div class="flex gap-2 flex-wrap">
                  <div v-for="stock in news.relatedStocks" :key="stock.code"
                    class="flex items-center gap-1.5 border border-darkBorder/60 bg-darkBg/60 px-2 py-1 rounded-lg cursor-pointer hover:border-primary/30 transition">
                    <span class="text-xs font-semibold text-white">{{ stock.name }}</span>
                    <span class="text-[10px] font-mono text-slate-600">{{ stock.code }}</span>
                    <span class="text-[10px] font-mono font-bold" :class="stock.change > 0 ? 'text-upPrice' : 'text-downPrice'">
                      {{ stock.change > 0 ? '+' : '' }}{{ stock.change }}%
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="filteredNews.length === 0" class="py-16 text-center text-slate-600 text-sm font-mono">
            暂无该分类下的资讯内容
          </div>

          <div class="flex justify-center py-4">
            <button @click="loadMore"
              class="text-[11px] font-bold text-slate-500 hover:text-primary transition font-mono uppercase tracking-widest border border-darkBorder hover:border-primary/30 px-6 py-2.5 rounded-lg">
              加载更多 ↓
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import request from '../utils/request'

const activeCategory = ref('all')
const keyword = ref('')
const newsData = ref<any[]>([])
const loading = ref(false)
const hasMore = ref(true)
const feedContainerRef = ref<HTMLElement | null>(null)
const currentPage = ref(1)
const pageSize = 20
let mainScrollContainer: HTMLElement | null = null

const categoryLabels: Record<string, string> = {
  all: '全部快讯',
  macro: '宏观政策',
  industry: '行业追踪',
  company: '公司公告',
  ai_report: 'AI 交易研报'
}

const categories = computed(() => {
  const counts = newsData.value.reduce((acc, item) => {
    acc[item.category] = (acc[item.category] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  return Object.fromEntries(
    Object.entries(categoryLabels).map(([id, name]) => [id, { name, unread: id === 'all' ? 0 : (counts[id] || 0) }])
  ) as Record<string, { name: string; unread: number }>
})

const filteredNews = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return newsData.value.filter(item => {
    if (activeCategory.value !== 'all' && item.category !== activeCategory.value) {
      return false
    }
    if (!query) {
      return true
    }
    return [item.title, item.summary, item.source].filter(Boolean).some(text => String(text).toLowerCase().includes(query))
  })
})

const categorizeNews = (item: any) => {
  const text = `${item.title || ''} ${item.summary || ''}`
  if (/(央行|政策|经济|财政|利率|降准|降息)/.test(text)) return 'macro'
  if (/(公告|财报|业绩|分红|回购)/.test(text)) return 'company'
  if (/(AI|模型|算力|算法)/i.test(text)) return 'ai_report'
  return 'industry'
}

const extractTags = (item: any) => {
  const text = `${item.title || ''} ${item.summary || ''}`
  const candidates = ['央行', '货币政策', '新能源', '固态电池', '算力', '白酒', '银行', '保险']
  return candidates.filter(tag => text.includes(tag)).slice(0, 2)
}

const padNumber = (value: number) => String(value).padStart(2, '0')

const formatNewsTime = (item: any, index: number) => {
  if (item?.publishEpoch) {
    const date = new Date(Number(item.publishEpoch) * 1000)
    if (!Number.isNaN(date.getTime())) {
      return `${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`
    }
  }
  if (item?.publishTime) {
    return String(item.publishTime)
  }
  return `--:---${index}`
}

const mapNewsItem = (item: any, index: number) => ({
  id: item.url || `${item.source || '快讯'}-${item.publishEpoch || item.publishTime || index}-${item.title || index}`,
  category: categorizeNews(item),
  importance: item.source === '财联社' ? 'high' : 'medium',
  time: formatNewsTime(item, index),
  tags: extractTags(item),
  title: item.title || '暂无标题',
  summary: item.summary || item.title || '',
  relatedStocks: [],
  url: item.url || '',
  source: item.source || '快讯'
})

const loadNews = async (reset = false) => {
  loading.value = true
  let shouldContinueLoading = false
  try {
    const result: { total: number; page: number; pageSize: number; records: any[] } = await request.get('/news/flash/page', {
      params: { page: currentPage.value, pageSize }
    })
    const records = Array.isArray(result?.records) ? result.records.map(mapNewsItem) : []
    if (reset) {
      newsData.value = records
    } else {
      newsData.value = [...newsData.value, ...records]
    }
    hasMore.value = newsData.value.length < Number(result?.total || 0) && records.length === pageSize
    await nextTick()
    shouldContinueLoading = hasMore.value && getScrollableContainers().some(container => {
      const notScrollable = container.scrollHeight <= container.clientHeight + 20
      return notScrollable || isNearBottom(container)
    })
  } finally {
    loading.value = false
  }
  if (shouldContinueLoading) {
    loadMore().catch(console.error)
  }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) {
    return
  }
  currentPage.value += 1
  try {
    await loadNews(false)
  } catch (error) {
    currentPage.value -= 1
    throw error
  }
}

const openNews = (url?: string) => {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  mainScrollContainer = document.querySelector('main')
  mainScrollContainer?.addEventListener('scroll', handleFeedScroll, { passive: true })
  loadNews(true).catch(console.error)
})

onBeforeUnmount(() => {
  mainScrollContainer?.removeEventListener('scroll', handleFeedScroll)
})

const getScrollableContainers = () => [feedContainerRef.value, mainScrollContainer].filter(Boolean) as HTMLElement[]

const isNearBottom = (container: HTMLElement) => {
  return container.scrollTop + container.clientHeight >= container.scrollHeight - 120
}

const shouldAutoLoadMore = () => {
  if (loading.value || !hasMore.value) {
    return false
  }
  return getScrollableContainers().some(container => {
    const notScrollable = container.scrollHeight <= container.clientHeight + 20
    return notScrollable || isNearBottom(container)
  })
}

const handleFeedScroll = () => {
  if (!shouldAutoLoadMore()) {
    return
  }
  loadMore().catch(console.error)
}
</script>
