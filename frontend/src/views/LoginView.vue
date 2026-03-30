<template>
  <div class="min-h-screen bg-darkBg flex text-slate-200 overflow-hidden">
    <!-- Left Branding Panel -->
    <div class="hidden lg:flex lg:w-1/2 relative overflow-hidden flex-col">
      <!-- Background layers -->
      <div class="absolute inset-0 bg-[#040C18]"></div>
      <div class="absolute inset-0 bg-grid-pattern opacity-100"></div>
      <!-- Radial glow -->
      <div class="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>
      <div class="absolute bottom-1/4 right-1/4 w-64 h-64 rounded-full bg-accent/4 blur-[80px] pointer-events-none"></div>

      <!-- Content -->
      <div class="relative z-10 flex flex-col h-full p-14">
        <!-- Logo -->
        <div class="flex items-center gap-3 mb-auto">
          <div class="w-9 h-9 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-primary" viewBox="0 0 16 16" fill="none">
              <path d="M2 12 L5 7 L8 9 L11 4 L14 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="14" cy="6" r="1.2" fill="currentColor"/>
            </svg>
          </div>
          <span class="font-display font-bold text-lg text-white tracking-wide">Smart<span class="text-primary">Stock</span> <span class="text-[11px] font-mono font-semibold border border-primary/20 bg-primary/5 text-primary/80 px-1.5 py-0.5 rounded tracking-widest">AI</span></span>
        </div>

        <!-- Headline -->
        <div class="my-auto">
          <div class="text-[11px] font-mono font-semibold text-primary/60 tracking-[0.3em] uppercase mb-5">量化智能 · 精准决策</div>
          <h1 class="font-display font-extrabold text-5xl leading-[1.1] text-white mb-6 tracking-tight">
            机构级别<br/>
            <span class="text-primary">AI 驱动</span><br/>
            财富管理
          </h1>
          <p class="text-slate-500 text-base leading-relaxed max-w-sm">
            实时算法洞察、智能选股、K 线分析全部集成于一个终端界面
          </p>
        </div>

        <!-- Animated chart -->
        <div class="relative mt-auto">
          <!-- Fake sparkline chart -->
          <div class="flex items-end gap-px h-20 opacity-60 mb-3">
            <div v-for="(h, i) in sparkline" :key="i"
              class="flex-1 rounded-sm transition-all duration-1000"
              :class="i === sparkline.length - 1 ? 'bg-primary' : h > 50 ? 'bg-upPrice/60' : 'bg-downPrice/60'"
              :style="{ height: h + '%' }"
            ></div>
          </div>
          <div class="flex items-center justify-between text-[10px] font-mono text-slate-600 uppercase tracking-widest">
            <span>● AES-256 加密</span>
            <span>● 毫秒级行情</span>
            <span>● T+1 模拟</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Login Panel -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-8 bg-darkBg relative">
      <!-- Subtle glow behind form -->
      <div class="absolute inset-0 bg-gradient-to-br from-darkCard/30 to-transparent pointer-events-none"></div>

      <div class="w-full max-w-sm relative z-10">
        <!-- Mobile logo -->
        <div class="flex lg:hidden items-center gap-2 mb-10 justify-center">
          <div class="w-8 h-8 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 16 16" fill="none">
              <path d="M2 12 L5 7 L8 9 L11 4 L14 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="14" cy="6" r="1.2" fill="currentColor"/>
            </svg>
          </div>
          <span class="font-display font-bold text-lg text-white">Smart<span class="text-primary">Stock</span> AI</span>
        </div>

        <div class="mb-8">
          <h2 class="font-display font-bold text-2xl text-white mb-1 tracking-tight">进入终端</h2>
          <p class="text-sm text-slate-500">输入凭证访问交易系统</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4">
          <div v-if="error" class="bg-red-500/8 border border-red-500/30 text-red-400 text-xs px-4 py-3 rounded-lg font-mono">
            {{ error }}
          </div>

          <div class="space-y-1.5">
            <label class="data-label">邮箱地址</label>
            <input
              v-model="form.email"
              type="email"
              required
              placeholder="name@company.com"
              class="cyber-input w-full h-10"
            />
          </div>

          <div class="space-y-1.5">
            <label class="data-label">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              class="cyber-input w-full h-10 tracking-widest"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full mt-2 h-11 rounded-xl bg-primary text-darkBg font-display font-bold text-sm tracking-wide
              hover:bg-cyan-300 transition-all
              shadow-[0_0_30px_rgba(34,211,238,0.25)] hover:shadow-[0_0_40px_rgba(34,211,238,0.4)]
              disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <div v-if="loading" class="w-4 h-4 border-2 border-darkBg border-t-transparent rounded-full animate-spin"></div>
            <span v-else>进入终端</span>
          </button>
        </form>

        <p class="mt-6 text-center text-xs text-slate-600">
          还没有账户？
          <router-link to="/register" class="text-primary hover:text-cyan-300 font-medium transition-colors">注册账户</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()
const loading = ref(false)
const error = ref('')

const form = reactive({
  email: '',
  password: ''
})

// Animated sparkline
const sparkline = ref<number[]>([])
const generateSparkline = () => {
  const pts: number[] = []
  let last = 45
  for (let i = 0; i < 60; i++) {
    last = Math.max(10, Math.min(90, last + (Math.random() - 0.48) * 12))
    pts.push(Math.round(last))
  }
  sparkline.value = pts
}

const handleLogin = async () => {
  loading.value = true
  error.value = ''
  try {
    await authStore.login(form.email, form.password)
    toast.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  generateSparkline()
  setInterval(() => {
    // Animate by shifting + adding one bar
    sparkline.value.shift()
    const last = sparkline.value[sparkline.value.length - 1] ?? 50
    const next = Math.max(10, Math.min(90, last + (Math.random() - 0.48) * 12))
    sparkline.value.push(Math.round(next))
  }, 800)
})
</script>
