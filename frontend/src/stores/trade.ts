import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'
import type { AccountVO, PositionVO } from '../types'

export const useTradeStore = defineStore('trade', () => {
  const account = ref<AccountVO | null>(null)
  const positions = ref<PositionVO[]>([])

  const fetchAccount = async () => {
    try {
      account.value = await request.get<AccountVO>('/trade/account')
    } catch (e) {
      console.error('Failed to fetch account:', e)
    }
  }

  const fetchPositions = async () => {
    try {
      const result = await request.get<PositionVO[]>('/trade/positions')
      positions.value = Array.isArray(result) ? result : []
    } catch (e) {
      console.error('Failed to fetch positions:', e)
    }
  }

  return { account, positions, fetchAccount, fetchPositions }
})
