# 智能股票交易辅助系统 - UI 设计与 Prompt 指南

根据后端现有的核心能力（鉴权、行情、自选、交易），前端如果要有极佳的交互体验，建议在 Stitch（或 Midjourney/UIzard 等工具）中重点设计以下 **4 个核心页面**。

你可以直接复制我提供的 Prompt（提示词）到对应的 UI 设计工具/AI 生成工具中。由于我们做的是给个人投资者使用的“专业且现代”的辅助系统，整体风格应该偏向 **深色模式（Dark Mode）、数据密集、赛博高科技感（Cyberpunk / Fintech）**。

---

## 页面一：登录与鉴权页 (Auth Page)

这是用户的第一印象。不需要太复杂，但要体现出金融科技的专业感。

*   **功能对应**：`/api/auth/login`, `/api/auth/register`
*   **核心元素**：左右分栏布局（左侧是大气的 3D 金融插画/行情抽象线，右侧是极简的磨砂玻璃体登录/注册表单）。
*   **Stitch / AI 生成 Prompt (英文效果最好)**:
    > **Prompt**: Modern fintech web dashboard login page, split screen layout. Left side: dark mode with glowing abstract stock market charts, elegant 3D metallic elements, neon blue and emerald green accents. Right side: clean glassmorphism login form, blurred background, minimal input fields for 'Email' and 'Password', prominent 'Sign In' button with a subtle gradient matching the neon colors. Cybersecurity and professional investment theme. High resolution, UI/UX design, Dribbble style.

---

## 页面二：全局概览与自选股池 (Dashboard & Watchlist)

这是用户登录后的首屏，用来快速查看他关心的股票并提供全局搜索入口。这也是一个高频交互页面。

*   **功能对应**：`/api/watchlist`, `/api/market/stocks/search`
*   **核心元素**：
    1.  **全局顶部导航**：包括 Logo、强大的剧中搜索框（用于搜股票）、右侧用户信息与“可用资金”概览。
    2.  **左侧或主体区**：自选股列表/卡片。采用数据网格（Data Grid）展示，包含代码、名称、最新价、涨跌幅折线微缩图（Sparkline）。
    3.  **右侧边栏（可选）**：当前大盘指数速览。
*   **Stitch / AI 生成 Prompt**:
    > **Prompt**: Advanced responsive web dashboard for stock trading, dark theme. Top navigation bar with a prominent global search input and user profile showing 'Account Balance: $1,000,000'. Main content area: A sophisticated data grid showing a watchlist of stocks list view. Columns include Ticker, Company Name, Current Price (some red for decrease, some green for increase), 24h Change %, and a small sparkline chart line. Cyberpunk finance aesthetic, deep slate gray background, high contrast typography, glowing data points, sleek and professional UI, clean layout, futuristic fintech terminal.

---

## 页面三：个股详情与交易终端 (Stock Detail & Trade Terminal) -- **最重头的页面**

这是整个系统最核心的页面，包含了复杂的 K 线图、盘口数据和买卖操作面板。它应该看起来像一个专业的交易员工作台。

*   **功能对应**：`/api/market/stocks/{stockCode}`, `.../kline`, `.../indicators`, `/api/trade/buy`, `/api/trade/sell`
*   **核心元素**：
    1.  **左侧/主视图**：大面积的 K 线图容器。顶部有周期切换 Tab（日、周、月、30分钟、60分钟），底部有指标切换（MACD、KDJ、RSI）。
    2.  **顶部面板**：股票基础信息（名称、代码、巨幅居中的当前价、高开低收等核心数值面板）。
    3.  **右侧边栏交易面板**：包含“买入”、“卖出”的独立 Tab 组件。包含“价格”、“数量（100整数倍提示）”输入框，“最大可买/可卖”提示文本，以及硕大的、带强烈视觉反馈的开仓/平仓按钮。
*   **Stitch / AI 生成 Prompt**:
    > **Prompt**: Professional stock trading terminal web UI, individual stock detail view, dark mode. Huge central interactive candlestick chart (K-line) with technical indicators (MACD volume bars below). Top header showing ticker 'AAPL', massive current price in green with plus sign, and minor stats (Open, High, Low). Right sidebar: A sleek order entry panel with 'Buy' and 'Sell' tabs, numeric input fields for Price and Quantity, 'Available Balance' info text, and a massive primary gradient button for 'Execute Trade'. Bloomberg terminal inspired but modern, glassmorphism containers, neon accents, highly detailed, data-heavy, professional UX/UI design.

---

## 页面四：模拟账户与历史记录 (Portfolio & Trade Records)

这里是用户管理自己资产和复盘的地方。

*   **功能对应**：`/api/trade/account`, `/api/trade/positions`, `/api/trade/records`
*   **核心元素**：
    1.  **顶部资产卡片**：展示总资产、可用现金、冻结资金、持仓市值、总收益和收益率。用大数字突出盈亏。
    2.  **下方 Tab 切换面板**：
        *   **当前持仓**：表格展示持有股票代码、成本价、现价、盈亏比例，附带快捷“卖出”按钮。
        *   **交易记录**：按时间倒序排列的流水账表格，明确显示“买入/卖出”、“成交价”、“成交量”。并附带时间筛选器。
*   **Stitch / AI 生成 Prompt**:
    > **Prompt**: Personal investment portfolio web dashboard UI, dark theme. Top section features a large summary card showing 'Total Asset Value', 'Available Cash', and glowing 'Total Return %' in high contrast typography. Below that, a clean tabbed panel with two tabs: 'Current Positions' and 'Trade History'. Active tab shows a modern data table detailing owned assets with columns for Ticker, Cost Basis, Current Price, PnL, and subtle quick 'Sell' action buttons on each row. Clean lines, neon mint and hot pink subtle gradients on dark navy background, highly legible fonts, financial technology app design.

---

## 💡 接下来你的工作流建议

1.  将上述 **Prompt** 复制到 Stitch 或其他设计工具中，生成初始视觉稿。
2.  如果不满意，你可以调整 Prompt 中的颜色描述（例如把暗黑改成明亮 `light mode`，把红绿习惯根据你的喜好调整为 `red for up, green for down`，因为大陆股市是红涨绿跌，而在北美是绿涨红跌）。
3.  在 Figma 中，基于生成的视觉稿，拆解出**颜色规范 (Design Tokens)** 和 **间距/字号规范**。
4.  如果你搞定了或者有了初步效果图，把图或者规范扔给我，我就可以直接开始手撕 Vite + Vue 3 的前端代码了！
