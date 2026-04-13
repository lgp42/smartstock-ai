<template>
  <div class="min-h-screen bg-darkBg flex text-slate-200 overflow-hidden">
    <!-- Left Branding Panel -->
    <div class="hidden lg:flex lg:w-1/2 relative overflow-hidden flex-col">
      <div class="absolute inset-0 bg-[#040C18]"></div>
      <div class="absolute inset-0 bg-grid-pattern opacity-100"></div>
      <div class="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-primary/5 blur-[120px] pointer-events-none"></div>
      <div class="absolute bottom-1/4 right-1/4 w-64 h-64 rounded-full bg-accent/4 blur-[80px] pointer-events-none"></div>

      <div class="relative z-10 flex flex-col h-full p-14">
        <div class="flex items-center gap-3 mb-auto">
          <div class="w-9 h-9 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center">
            <svg class="w-5 h-5 text-primary" viewBox="0 0 16 16" fill="none">
              <path d="M2 12 L5 7 L8 9 L11 4 L14 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="14" cy="6" r="1.2" fill="currentColor"/>
            </svg>
          </div>
          <span class="font-display font-bold text-lg text-white tracking-wide">Smart<span class="text-primary">Stock</span> <span class="text-[11px] font-mono font-semibold border border-primary/20 bg-primary/5 text-primary/80 px-1.5 py-0.5 rounded tracking-widest">AI</span></span>
        </div>

        <div class="my-auto">
          <div class="text-[11px] font-mono font-semibold text-primary/60 tracking-[0.3em] uppercase mb-5">智能交易 · 从注册开始</div>
          <h1 class="font-display font-extrabold text-5xl leading-[1.1] text-white mb-6 tracking-tight">
            开启你的<br/>
            <span class="text-primary">智能投资</span><br/>
            之旅
          </h1>
          <p class="text-slate-500 text-base leading-relaxed max-w-sm">
            注册模拟交易账户，体验 AI 驱动的 A 股分析与实战演练
          </p>
        </div>

        <div class="relative mt-auto">
          <div class="flex items-center gap-6 text-[10px] font-mono text-slate-600 uppercase tracking-widest">
            <span>● 百万模拟资金</span>
            <span>● AI 智能选股</span>
            <span>● 实时行情</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Registration Panel -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-8 bg-darkBg relative">
      <div class="absolute inset-0 bg-gradient-to-br from-darkCard/30 to-transparent pointer-events-none"></div>
      <div class="absolute top-1/4 right-1/3 w-60 h-60 rounded-full bg-primary/3 blur-[100px] pointer-events-none"></div>

      <div class="w-full max-w-sm relative z-10">
        <!-- Mobile logo -->
        <div class="flex lg:hidden items-center gap-2 mb-10 justify-center">
          <div class="w-8 h-8 rounded-xl bg-primary/10 border border-primary/30 flex items-center justify-center animate-float">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 16 16" fill="none">
              <path d="M2 12 L5 7 L8 9 L11 4 L14 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="14" cy="6" r="1.2" fill="currentColor"/>
            </svg>
          </div>
          <span class="font-display font-bold text-lg text-white">Smart<span class="text-primary">Stock</span> AI</span>
        </div>

        <div class="mb-8">
          <div class="inline-flex items-center gap-1.5 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-[10px] font-mono font-semibold text-primary/80 tracking-widest uppercase mb-4">
            <span class="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
            免费注册
          </div>
          <h2 class="font-display font-bold text-2xl text-white mb-1 tracking-tight">创建账户</h2>
          <p class="text-sm text-slate-500">注册获取百万模拟资金交易账户</p>
        </div>

        <form @submit.prevent="handleRegister" class="space-y-4">
          <div v-if="error" class="bg-red-500/8 border border-red-500/30 text-red-400 text-xs px-4 py-3 rounded-lg font-mono">
            {{ error }}
          </div>

          <div class="space-y-1.5">
            <label class="data-label">昵称</label>
            <input
              v-model="form.nickname"
              type="text"
              required
              maxlength="50"
              placeholder="你的昵称"
              class="cyber-input w-full h-10"
            />
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
              minlength="8"
              maxlength="20"
              class="cyber-input w-full h-10 tracking-widest"
              placeholder="8-20 位密码"
            />
            <!-- Password strength -->
            <div v-if="form.password" class="flex gap-1 mt-1.5">
              <div class="h-1 flex-1 rounded-full transition-all" :class="passwordStrength >= 1 ? 'bg-red-500' : 'bg-darkBorder'"></div>
              <div class="h-1 flex-1 rounded-full transition-all" :class="passwordStrength >= 2 ? 'bg-amber-500' : 'bg-darkBorder'"></div>
              <div class="h-1 flex-1 rounded-full transition-all" :class="passwordStrength >= 3 ? 'bg-emerald-500' : 'bg-darkBorder'"></div>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="data-label">确认密码</label>
            <input
              v-model="form.confirmPassword"
              type="password"
              required
              class="cyber-input w-full h-10 tracking-widest"
              placeholder="再次输入密码"
            />
            <p v-if="form.confirmPassword && form.password !== form.confirmPassword" class="text-[11px] text-red-400 font-mono mt-1">
              两次输入的密码不一致
            </p>
          </div>

          <button
            type="submit"
            :disabled="loading || !isFormValid"
            class="w-full mt-2 h-11 rounded-xl bg-primary text-darkBg font-display font-bold text-sm tracking-wide
              hover:bg-cyan-300 transition-all
              shadow-[0_0_30px_rgba(34,211,238,0.25)] hover:shadow-[0_0_40px_rgba(34,211,238,0.4)]
              disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <div v-if="loading" class="w-4 h-4 border-2 border-darkBg border-t-transparent rounded-full animate-spin"></div>
            <span v-else>注册账户</span>
          </button>
        </form>

        <p class="mt-6 text-center text-xs text-slate-600">
          已有账户？
          <router-link to="/login" class="text-primary hover:text-cyan-300 font-medium transition-colors">返回登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToastStore()
const loading = ref(false)
const error = ref('')

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/[0-9]/.test(p) && /[^A-Za-z0-9]/.test(p)) score++
  return score
})

const isFormValid = computed(() => {
  return form.nickname.trim() &&
    form.email.trim() &&
    form.password.length >= 8 &&
    form.password === form.confirmPassword
})

const handleRegister = async () => {
  if (!isFormValid.value) return
  loading.value = true
  error.value = ''
  try {
    await authStore.register(form.email, form.password, form.nickname)
    toast.success('注册成功，请登录')
    router.push('/login')
  } catch (err: any) {
    error.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>
