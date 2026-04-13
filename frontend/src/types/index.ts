// API response wrapper
export interface ApiResponse<T> {
  code: number
  data: T
  message?: string
}

// Auth
export interface LoginVO {
  userId: number
  email: string
  nickname: string
  avatar: string
  createdAt: string
  token: string
  expiresIn: number
}

export interface UserVO {
  userId: number
  email: string
  nickname: string
  avatar: string
  createdAt: string
}

export interface UserRegisterDTO {
  email: string
  password: string
  nickname: string
}

export interface UserLoginDTO {
  email: string
  password: string
}

// Market
export interface StockDetailVO {
  stockCode: string
  stockName: string
  market: string
  board: string
  industry: string
  st: boolean
  delisted: boolean
  currentPrice: number
  changeRate: number
  changeAmount: number
  volume: number
  amount: number
  open: number
  high: number
  low: number
  close: number
  preClose: number
}

export interface KlineVO {
  date: string
  open: number
  close: number
  high: number
  low: number
  volume: number
  amount: number
  changeRate: number
}

export interface MarketSnapshotVO {
  stockCode: string
  stockName: string
  currentPrice: number
  changeRate: number
}

export interface ScreenerResultVO {
  stockCode: string
  stockName: string
  market: string
  board: string
  industry: string
  st: boolean
  delisted: boolean
  currentPrice: number
  changeRate: number
  turnoverRate: number
  pe: number
  totalMarketCap: number
}

export interface StockSearchResult {
  stockCode: string
  stockName: string
}

// News
export interface NewsFlashVO {
  title: string
  summary: string
  source: string
  publishTime: string
  publishEpoch: number
  url: string
  stockCode: string
}

// Watchlist
export interface WatchlistVO {
  stockCode: string
  stockName: string
  market: string
  currentPrice: number
  changeRate: number
  sortOrder: number
}

// AI QA
export interface QaAnswerVO {
  sessionId: string
  stockCode: string
  question: string
  answer: string
  createdAt: string
}

export interface QaHistoryVO {
  id: number
  question: string
  answer: string
  createdAt: string
}

export interface QaSessionVO {
  sessionId: string
  stockCode: string | null
  title: string
  messageCount: number
  createdAt: string | null
  lastActive: string | null
}

export interface QaSessionMessageVO {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string | null
}

export interface QaSessionDetailVO {
  sessionId: string
  messages: QaSessionMessageVO[]
}

// Toast system
export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: number
  type: ToastType
  message: string
  duration?: number
}
