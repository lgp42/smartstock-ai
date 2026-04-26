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
  phone?: string
  nickname: string
  avatar: string
  createdAt: string
  token: string
  expiresIn: number
}

export interface UserVO {
  userId: number
  email: string
  phone?: string
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

export interface PhoneLoginDTO {
  phone: string
  password: string
}

export interface PhoneRegisterDTO {
  phone: string
  password: string
  nickname: string
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

export interface PageVO<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

export interface AccountVO {
  userId: number
  totalAssets: number
  availableCash: number
  frozenCash: number
  positionValue: number
  totalProfit: number
  profitRate: number
}

export interface PositionVO {
  stockCode: string
  stockName: string
  quantity: number
  availableQuantity: number
  costPrice: number
  currentPrice: number
  marketValue: number
  profit: number
  profitRate: number
}

export interface OrderVO {
  orderId: number
  stockCode: string
  stockName: string
  orderType: 'buy' | 'sell'
  price: number
  quantity: number
  amount: number
  fee: number
  status: 'pending' | 'filled' | 'cancelled'
  filledPrice?: number
  filledQuantity?: number
  filledTime?: string
}

export interface TradeRecordVO {
  recordId: number
  stockCode: string
  stockName: string
  tradeType: 'buy' | 'sell'
  price: number
  quantity: number
  amount: number
  fee: number
  tradeTime: string
}

export interface TradeOrderDTO {
  stockCode: string
  price: number
  quantity: number
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

export interface RiskAlertVO {
  alertType: string
  alertLevel: string
  stockCode: string
  message: string
  value: number
}

export interface BacktestRunDTO {
  stockCode: string
  initialCapital: number
  limit: number
}

export interface BacktestResultVO {
  stockCode: string
  strategyType: string
  initialCapital: number
  finalCapital: number
  totalReturn: number
  returnRate: number
  maxDrawdown: number
  tradeCount: number
}

// Toast system
export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: number
  type: ToastType
  message: string
  duration?: number
}
