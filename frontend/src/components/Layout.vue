<template>
  <div class="flex flex-col h-screen text-slate-200">
    <!-- Navbar -->
    <header class="h-14 flex items-center justify-between px-5 border-b border-darkBorder/80 bg-darkCard/95 backdrop-blur-xl z-20 shrink-0 relative">
      <div class="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/40 to-transparent"></div>

      <div class="flex items-center space-x-8">
        <!-- Mobile menu button -->
        <button @click="mobileMenuOpen = true" class="md:hidden flex items-center justify-center w-8 h-8 rounded-lg hover:bg-white/5 transition">
          <Menu class="w-5 h-5 text-slate-400" />
        </button>

        <!-- Brand -->
        <router-link to="/dashboard" class="flex items-center gap-2.5 select-none">
          <div class="relative w-7 h-7 flex items-center justify-center">
            <div class="absolute inset-0 rounded-md bg-primary/10 border border-primary/30"></div>
            <svg class="w-4 h-4 text-primary relative z-10" viewBox="0 0 16 16" fill="none">
              <path d="M2 12 L5 7 L8 9 L11 4 L14 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="14" cy="6" r="1.2" fill="currentColor"/>
            </svg>
          </div>
          <span class="font-display font-bold text-base tracking-wide text-white">Smart<span class="text-primary">Stock</span></span>
          <span class="hidden sm:inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold uppercase tracking-widest text-primary/80 border border-primary/20 bg-primary/5 font-mono">AI</span>
        </router-link>

        <!-- Nav Links (desktop) -->
        <nav class="hidden md:flex items-center gap-1 h-full">
          <router-link
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link"
            active-class="active-nav">
            {{ link.label }}
          </router-link>
        </nav>
      </div>

      <!-- Right: Search + User -->
      <div class="flex items-center gap-3">
        <!-- Search (desktop) -->
        <div class="relative w-68 hidden lg:block" @click.stop>
          <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none">
            <Search class="h-3.5 w-3.5 text-slate-500" />
          </div>
          <input
            ref="searchInputRef"
            v-model="keyword"
            type="text"
            placeholder="代码 / 名称..."
            class="cyber-input w-full pl-9 pr-16 h-8 text-xs"
            @input="handleSearchInput"
            @focus="showResults = searchResults.length > 0"
            @keydown.enter.prevent="goToFirstSearchResult"
            @keydown.esc="closeSearchResults"
          />
          <button
            type="button"
            class="absolute inset-y-0 right-2 my-1 rounded px-2 text-[10px] font-bold text-slate-400 hover:text-primary transition font-mono"
            @click="goToFirstSearchResult"
          >
            搜索
          </button>

          <!-- Search Results Dropdown -->
          <div
            v-if="showResults"
            class="absolute top-full left-0 right-0 mt-2 rounded-xl border border-darkBorder bg-darkCard/98 shadow-2xl backdrop-blur-xl overflow-hidden z-50"
          >
            <div v-if="searchLoading" class="px-4 py-3 text-xs text-slate-500 font-mono">
              搜索中...
            </div>
            <div v-else-if="searchResults.length === 0" class="px-4 py-3 text-xs text-slate-600 font-mono">
              没有匹配股票
            </div>
            <div
              v-for="item in searchResults"
              :key="item.stockCode"
              class="flex items-center justify-between gap-3 px-4 py-2.5 border-t border-darkBorder/50 first:border-t-0 hover:bg-darkCardHover/50 transition-colors"
            >
              <button
                type="button"
                class="min-w-0 flex-1 text-left"
                @click="goToTerminal(item.stockCode)"
              >
                <div class="text-sm font-semibold text-white">{{ item.stockName }}</div>
                <div class="text-[11px] text-slate-500 font-mono">{{ item.stockCode }}</div>
              </button>
              <button
                type="button"
                class="shrink-0 rounded-lg border border-primary/20 bg-primary/5 px-2.5 py-1 text-[10px] font-bold text-primary hover:bg-primary/15 transition font-mono tracking-wider"
                @click="addToWatchlist(item.stockCode)"
              >
                +自选
              </button>
            </div>
          </div>
        </div>

        <!-- User profile dropdown -->
        <div class="relative" @click.stop>
          <button
            @click="userMenuOpen = !userMenuOpen"
            class="flex items-center gap-2 rounded-lg border border-darkBorder/60 bg-darkCard/60 px-3 py-1.5 text-xs text-slate-300 hover:border-primary/30 transition-colors"
          >
            <div class="w-5 h-5 rounded-full bg-primary/15 border border-primary/30 flex items-center justify-center">
              <User class="w-3 h-3 text-primary" />
            </div>
            <span class="hidden sm:block font-medium max-w-[80px] truncate">{{ authStore.user?.nickname || '用户' }}</span>
            <ChevronDown class="w-3 h-3 text-slate-500 transition-transform" :class="userMenuOpen ? 'rotate-180' : ''" />
          </button>

          <!-- Dropdown -->
          <Transition name="dropdown">
            <div v-if="userMenuOpen" class="absolute right-0 top-full mt-2 w-48 rounded-xl border border-darkBorder bg-darkCard/98 shadow-2xl backdrop-blur-xl overflow-hidden z-50">
              <div class="px-4 py-3 border-b border-darkBorder/50">
                <div class="text-sm font-semibold text-white truncate">{{ authStore.user?.nickname || '用户' }}</div>
                <div class="text-[11px] text-slate-500 font-mono truncate">{{ authStore.user?.email || '' }}</div>
              </div>
              <button
                @click="handleLogout"
                class="w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-slate-400 hover:text-red-400 hover:bg-red-500/5 transition-colors"
              >
                <LogOut class="w-3.5 h-3.5" />
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <!-- Mobile slide-out menu -->
    <Transition name="overlay">
      <div v-if="mobileMenuOpen" class="fixed inset-0 bg-black/60 backdrop-blur-sm z-40" @click="mobileMenuOpen = false"></div>
    </Transition>
    <Transition name="drawer">
      <div v-if="mobileMenuOpen" class="fixed inset-y-0 left-0 w-72 bg-darkCard border-r border-darkBorder z-50 flex flex-col">
        <div class="h-14 flex items-center justify-between px-5 border-b border-darkBorder/50 shrink-0">
          <span class="font-display font-bold text-base text-white">Smart<span class="text-primary">Stock</span></span>
          <button @click="mobileMenuOpen = false" class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white/5 transition">
            <X class="w-5 h-5 text-slate-400" />
          </button>
        </div>

        <!-- Mobile search -->
        <div class="p-4 border-b border-darkBorder/50">
          <div class="relative">
            <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none">
              <Search class="h-3.5 w-3.5 text-slate-500" />
            </div>
            <input
              v-model="keyword"
              type="text"
              placeholder="搜索股票..."
              class="cyber-input w-full pl-9 h-9 text-xs"
              @input="handleSearchInput"
              @keydown.enter.prevent="goToFirstSearchResult"
            />
          </div>
        </div>

        <!-- Nav links -->
        <nav class="flex-1 p-3 space-y-1 overflow-y-auto">
          <router-link
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-white/5 transition-all"
            active-class="!text-primary !bg-primary/10"
            @click="mobileMenuOpen = false"
          >
            {{ link.label }}
          </router-link>
        </nav>

        <!-- User info at bottom -->
        <div class="p-4 border-t border-darkBorder/50">
          <div class="flex items-center gap-3 mb-3">
            <div class="w-8 h-8 rounded-full bg-primary/15 border border-primary/30 flex items-center justify-center">
              <User class="w-4 h-4 text-primary" />
            </div>
            <div class="min-w-0 flex-1">
              <div class="text-sm font-semibold text-white truncate">{{ authStore.user?.nickname || '用户' }}</div>
              <div class="text-[11px] text-slate-500 font-mono truncate">{{ authStore.user?.email || '' }}</div>
            </div>
          </div>
          <button
            @click="handleLogout"
            class="w-full flex items-center justify-center gap-2 rounded-lg border border-darkBorder py-2 text-xs text-slate-400 hover:text-red-400 hover:border-red-500/30 transition-colors"
          >
            <LogOut class="w-3.5 h-3.5" />
            退出登录
          </button>
        </div>
      </div>
    </Transition>

    <!-- Main Workspace -->
    <main class="flex-1 overflow-auto bg-darkBg relative p-5">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, LogOut, Menu, X, User, ChevronDown } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import request from '../utils/request'

const navLinks = [
  { to: '/dashboard', label: '自选大本营' },
  { to: '/screener', label: '智能选股' },
  { to: '/terminal', label: '行情终端' },
  { to: '/news', label: '市场快讯' },
  { to: '/portfolio', label: '我的资产' },
]

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()

const keyword = ref('')
const searchResults = ref<Array<{ stockCode: string; stockName: string }>>([])
const searchLoading = ref(false)
const showResults = ref(false)
// template ref — vue-tsc 5.x may not track template-only usage
const searchInputRef = ref<HTMLInputElement | null>(null)
const mobileMenuOpen = ref(false)
const userMenuOpen = ref(false)
let searchTimer: number | undefined

void searchInputRef

const searchStocks = async () => {
  const trimmedKeyword = keyword.value.trim()
  if (!trimmedKeyword) {
    searchResults.value = []
    showResults.value = false
    return
  }

  searchLoading.value = true
  try {
    const result = await request.get('/market/stocks/search', {
      params: { keyword: trimmedKeyword, limit: 8 }
    })
    searchResults.value = Array.isArray(result) ? result : []
    showResults.value = true
  } catch {
    searchResults.value = []
    showResults.value = true
  } finally {
    searchLoading.value = false
  }
}

const handleSearchInput = () => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => { searchStocks() }, 200)
}

const closeSearchResults = () => { showResults.value = false }

const goToTerminal = (stockCode: string) => {
  keyword.value = ''
  searchResults.value = []
  showResults.value = false
  mobileMenuOpen.value = false
  router.push(`/terminal/${stockCode}`)
}

const goToFirstSearchResult = async () => {
  const trimmedKeyword = keyword.value.trim()
  if (!trimmedKeyword) return
  if (searchResults.value.length === 0) await searchStocks()
  const firstItem = searchResults.value[0]
  if (firstItem) goToTerminal(firstItem.stockCode)
}

const addToWatchlist = async (stockCode: string) => {
  try {
    await request.post('/watchlist', { stockCode })
    toast.success(`已添加 ${stockCode} 到自选股`)
    closeSearchResults()
  } catch (error: any) {
    toast.error(error.message || '添加自选股失败')
  }
}

const handleLogout = async () => {
  await authStore.logout()
  mobileMenuOpen.value = false
  userMenuOpen.value = false
  router.push('/login')
}

const handleClickOutside = () => {
  closeSearchResults()
  userMenuOpen.value = false
}

onMounted(() => {
  window.addEventListener('click', handleClickOutside)
  // Fetch user profile if we have a token but no user data
  if (authStore.isLoggedIn && !authStore.user?.nickname) {
    authStore.fetchProfile()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleClickOutside)
  if (searchTimer) window.clearTimeout(searchTimer)
})
</script>

<style scoped>
.nav-link {
  @apply h-8 inline-flex items-center px-3 rounded-lg text-sm font-medium text-slate-500
    hover:text-slate-200 hover:bg-white/5 transition-all;
}
.active-nav {
  @apply text-primary bg-primary/10 hover:bg-primary/10;
}

/* Dropdown transitions */
.dropdown-enter-active { transition: all 0.15s ease-out; }
.dropdown-leave-active { transition: all 0.1s ease-in; }
.dropdown-enter-from { opacity: 0; transform: translateY(-4px) scale(0.95); }
.dropdown-leave-to { opacity: 0; transform: translateY(-4px) scale(0.95); }

/* Mobile drawer transitions */
.overlay-enter-active { transition: opacity 0.2s ease; }
.overlay-leave-active { transition: opacity 0.15s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }

.drawer-enter-active { transition: transform 0.25s ease-out; }
.drawer-leave-active { transition: transform 0.2s ease-in; }
.drawer-enter-from { transform: translateX(-100%); }
.drawer-leave-to { transform: translateX(-100%); }
</style>
