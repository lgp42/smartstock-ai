import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import type { LoginVO, UserVO } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref<UserVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  const clearSession = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const login = async (email: string, password: string) => {
    const data = await request.post<LoginVO>('/auth/login', { email, password })
    token.value = data.token
    localStorage.setItem('token', data.token)
    user.value = {
      userId: data.userId,
      email: data.email,
      phone: data.phone,
      nickname: data.nickname,
      avatar: data.avatar,
      createdAt: data.createdAt,
    }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const loginByPhone = async (phone: string, password: string) => {
    const data = await request.post<LoginVO>('/auth/login/phone', { phone, password })
    token.value = data.token
    localStorage.setItem('token', data.token)
    user.value = {
      userId: data.userId,
      email: data.email,
      phone: data.phone,
      nickname: data.nickname,
      avatar: data.avatar,
      createdAt: data.createdAt,
    }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const register = async (email: string, password: string, nickname: string) => {
    await request.post('/auth/register', { email, password, nickname })
  }

  const registerByPhone = async (phone: string, password: string, nickname: string) => {
    await request.post('/auth/register/phone', { phone, password, nickname })
  }

  const changePassword = async (oldPassword: string, newPassword: string) => {
    await request.put('/users/me/password', { oldPassword, newPassword })
  }

  const logout = async () => {
    try {
      await request.post('/auth/logout')
    } catch { /* ignore */ }
    clearSession()
  }

  const loadUser = () => {
    const stored = localStorage.getItem('user')
    if (stored) {
      try { user.value = JSON.parse(stored) } catch { /* ignore */ }
    }
  }

  const fetchProfile = async () => {
    try {
      const data = await request.get<UserVO>('/users/me')
      user.value = data
      localStorage.setItem('user', JSON.stringify(data))
      return data
    } catch (error) {
      clearSession()
      throw error
    }
  }

  // Initialize from localStorage
  loadUser()

  return {
    token,
    user,
    isLoggedIn,
    login,
    loginByPhone,
    register,
    registerByPhone,
    changePassword,
    logout,
    loadUser,
    fetchProfile,
    clearSession,
  }
})
