<template>
  <div class="flex h-full flex-col gap-4">
    <div class="glass-panel flex items-center justify-between px-5 py-3.5">
      <div class="flex items-center gap-3">
        <span class="h-5 w-1 rounded-full bg-gradient-to-b from-primary to-accent"></span>
        <h2 class="font-display text-base font-bold text-white">账户设置</h2>
        <span class="tag-badge border-primary/20 bg-primary/5 text-primary/80">安全中心</span>
      </div>
    </div>

    <div class="grid flex-1 content-start gap-4 lg:grid-cols-2">
      <section class="glass-panel p-5">
        <div class="data-label mb-5">基础资料</div>
        <div class="space-y-5">
          <label class="block">
            <span class="data-label mb-2 block">昵称</span>
            <input v-model.trim="profileForm.nickname" class="cyber-input w-full" />
          </label>
          <label class="block">
            <span class="data-label mb-2 block">头像地址</span>
            <input v-model.trim="profileForm.avatar" class="cyber-input w-full" placeholder="https://..." />
          </label>
          <div class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-xl border border-darkBorder/50 bg-darkBg/50 p-3">
              <div class="data-label">邮箱</div>
              <div class="mt-2 truncate font-mono text-sm text-slate-300">{{ authStore.user?.email || '-' }}</div>
            </div>
            <div class="rounded-xl border border-darkBorder/50 bg-darkBg/50 p-3">
              <div class="data-label">手机号</div>
              <div class="mt-2 truncate font-mono text-sm text-slate-300">{{ authStore.user?.phone || '-' }}</div>
            </div>
          </div>
          <button class="btn-primary w-full" :disabled="profileSaving" @click="saveProfile">
            {{ profileSaving ? '保存中' : '保存资料' }}
          </button>
        </div>
      </section>

      <section class="glass-panel p-5">
        <div class="data-label mb-5">修改密码</div>
        <div class="space-y-5">
          <label class="block">
            <span class="data-label mb-2 block">旧密码</span>
            <input v-model="passwordForm.oldPassword" type="password" class="cyber-input w-full" autocomplete="current-password" />
          </label>
          <label class="block">
            <span class="data-label mb-2 block">新密码</span>
            <input v-model="passwordForm.newPassword" type="password" class="cyber-input w-full" autocomplete="new-password" />
          </label>
          <label class="block">
            <span class="data-label mb-2 block">确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" class="cyber-input w-full" autocomplete="new-password" />
          </label>
          <button class="btn-primary w-full" :disabled="passwordSaving" @click="savePassword">
            {{ passwordSaving ? '提交中' : '更新密码' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const authStore = useAuthStore()
const toast = useToastStore()
const profileSaving = ref(false)
const passwordSaving = ref(false)

const profileForm = reactive({
  nickname: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const syncProfileForm = () => {
  profileForm.nickname = authStore.user?.nickname || ''
  profileForm.avatar = authStore.user?.avatar || ''
}

const saveProfile = async () => {
  profileSaving.value = true
  try {
    await request.put('/users/me', {
      nickname: profileForm.nickname,
      avatar: profileForm.avatar || null
    })
    await authStore.fetchProfile()
    syncProfileForm()
    toast.success('资料已更新')
  } catch (error: any) {
    toast.error(error.message || '资料保存失败')
  } finally {
    profileSaving.value = false
  }
}

const savePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await authStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    toast.success('密码已更新')
  } catch (error: any) {
    toast.error(error.message || '密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchProfile()
  }
  syncProfileForm()
})
</script>
