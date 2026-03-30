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

// Trade
export interface AccountVO {
  userId: number
  totalAssets: number
  availableCash: number
  frozenCash: number
  positionValue: number
  totalProfit: number
  profitRate: number
}

export interface OrderVO {
  orderId: number
  stockCode: string
  stockName: string
  orderType: string
  price: number
  quantity: number
  amount: number
  fee: number
  status: string
  filledPrice: number
  filledQuantity: number
  filledTime: string
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

export interface BuyOrderDTO {
  stockCode: string
  price: number
  quantity: number
}

export interface SellOrderDTO {
  stockCode: string
  price: number
  quantity: number
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

// Pagination
export interface PageVO<T> {
  total: number
  page: number
  pageSize: number
  records: T[]
}

// Toast system
export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: number
  type: ToastType
  message: string
  duration?: number
}
