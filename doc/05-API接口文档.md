# 智能股票交易辅助系统 - API 接口文档

**版本**: v0.0.1-SNAPSHOT  
**最后更新**: 2026-04-26  
**文档状态**: 已按当前代码实现对齐

---

## 1. 接口总说明

- 协议：HTTP
- 请求格式：JSON
- 响应格式：JSON
- 字符编码：UTF-8
- Base URL：`http://127.0.0.1:8080/api`

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 当前错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误或业务校验失败 |
| 401 | 未登录或 Token 无效 |
| 404 | 用户或股票不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 用户已存在 |
| 2001 | 股票不存在 |
| 4001 | 自选股数量超限 |
| 4002 | 自选股已存在 |

### 鉴权方式

受保护接口需要请求头：

```text
Authorization: Bearer <token>
```

---

## 2. 已实现接口

## 2.1 认证接口

### POST /auth/register

注册用户。

请求体：

```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "用户昵称"
}
```

校验规则：

- `email` 必须是合法邮箱
- `password` 长度 8-20
- `nickname` 必填

### POST /auth/login

用户登录。

请求体：

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

响应体 `data` 字段：

```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "用户昵称",
  "avatar": null,
  "createdAt": "2026-03-18T00:00:00",
  "token": "jwt-token",
  "expiresIn": 604800
}
```

说明：

- 邮箱会按小写规范化查询

### POST /auth/register/phone

手机号注册。

请求体：

```json
{
  "phone": "13800138000",
  "password": "password123",
  "nickname": "用户昵称"
}
```

说明：

- 当前为手机号 + 密码注册，不包含短信验证码
- 系统会为手机号用户生成内部邮箱占位值，用于兼容现有用户表非空约束

### POST /auth/login/phone

手机号登录。

请求体：

```json
{
  "phone": "13800138000",
  "password": "password123"
}
```

### POST /auth/logout

用户登出，当前 Token 会进入黑名单。

---

## 2.2 用户接口

### GET /users/me

获取当前登录用户。

### PUT /users/me

更新当前登录用户资料。

请求体：

```json
{
  "nickname": "新昵称",
  "avatar": "https://example.com/avatar.png"
}
```

说明：

- 当前只支持修改 `nickname` 和 `avatar`

### PUT /users/me/password

修改当前登录用户密码。

请求体：

```json
{
  "oldPassword": "old-password",
  "newPassword": "new-password"
}
```

说明：

- 需要先校验旧密码
- 新密码长度 8-20 位

---

## 2.3 行情接口

### GET /market/stocks/search

搜索股票。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 是 | 股票代码或名称 |
| `limit` | Integer | 否 | 默认 10，范围 1-50 |

示例：

```text
GET /api/market/stocks/search?keyword=平安&limit=3
```

### GET /market/stocks/{stockCode}

获取股票详情。

当前返回字段包含：

- `stockCode`
- `stockName`
- `market`
- `board`
- `industry`
- `st`
- `delisted`
- `currentPrice`
- `changeRate`
- `changeAmount`
- `volume`
- `amount`
- `high`
- `low`
- `open`
- `close`
- `preClose`

### GET /market/stocks/{stockCode}/kline

获取 K 线。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `period` | String | 否 | 默认 `day`，支持 `1min/5min/15min/30min/60min/day/week/month` |
| `limit` | Integer | 否 | 默认 100，范围 1-500 |

说明：

- 当前不支持 `startDate`
- 当前不支持 `endDate`
- 缓存命中和远端请求都会按 `limit` 截断

### GET /market/stocks/{stockCode}/indicators

获取技术指标。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `indicators` | String | 否 | 默认 `macd`，支持 `macd` / `kdj` / `rsi` / `ma` / `boll`，也支持逗号组合 |
| `period` | String | 否 | 默认 `day`，支持 `1min/5min/15min/30min/60min/day/week/month` |
| `limit` | Integer | 否 | 默认 100，范围 1-500 |

说明：

- 多指标组合会按指标类型顺序拼接返回，单条数据中 `type` 字段区分指标类型

### GET /market/snapshots

获取市场指数快照。

返回字段包含：

- `stockCode`
- `stockName`
- `currentPrice`
- `changeRate`

### GET /market/screener

智能选股列表接口。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `boards` | String | 否 | 多个板块逗号分隔：`sh_main,sz_main,cyb,star` |
| `industryGroup` | String | 否 | 默认 `all`，支持 `tech/finance/consumer/energy/ev` |
| `minMarketCap` | Decimal | 否 | 最小总市值（亿） |
| `maxMarketCap` | Decimal | 否 | 最大总市值（亿） |
| `minPe` | Decimal | 否 | 最小 PE |
| `maxPe` | Decimal | 否 | 最大 PE |
| `minTurnoverRate` | Decimal | 否 | 最小换手率 |
| `maxTurnoverRate` | Decimal | 否 | 最大换手率 |
| `minChangeRate` | Decimal | 否 | 最小涨跌幅 |
| `maxChangeRate` | Decimal | 否 | 最大涨跌幅 |
| `excludeSt` | Boolean | 否 | 默认 `true` |
| `excludeDelisted` | Boolean | 否 | 默认 `true` |
| `technicalPattern` | String | 否 | 默认 `none`，支持 `none/ma_cross/macd_golden/volume_up/breakout/near_high/low_vol_pullback` |

返回字段额外包含：

- `board`
- `st`
- `delisted`

### GET /market/screener/page

智能选股分页接口。

在 `/market/screener` 参数基础上新增：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | Integer | 否 | 默认 1，最小 1 |
| `pageSize` | Integer | 否 | 默认 20，范围 1-100 |

---

## 2.4 自选股接口

### GET /watchlist

查询当前用户自选股。

### POST /watchlist

添加自选股。

请求体：

```json
{
  "stockCode": "000001"
}
```

规则：

- 单用户最多 50 只
- 不能重复添加

### DELETE /watchlist/{stockCode}

删除自选股。

### POST /watchlist/batch

批量添加自选股。

请求体：

```json
{
  "stockCodes": ["000001", "600519"]
}
```

### DELETE /watchlist/batch

批量删除自选股。

请求体：

```json
{
  "stockCodes": ["000001", "600519"]
}
```

### PUT /watchlist/sort

更新自选股排序。

请求体：

```json
{
  "items": [
    { "stockCode": "600519", "sortOrder": 0 },
    { "stockCode": "000001", "sortOrder": 1 }
  ]
}
```

---

## 2.5 新闻接口

### GET /news/flash

获取新闻快讯列表。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `limit` | Integer | 否 | 默认 10，范围 1-300 |
| `source` | String | 否 | 新闻来源筛选 |
| `stockCode` | String | 否 | 关联股票代码筛选 |
| `keyword` | String | 否 | 标题/摘要关键词筛选 |

说明：

- 未带筛选条件时会优先命中缓存
- 当前来源：`新浪财经`、`财联社`、`东方财富`

### GET /news/flash/page

获取新闻分页列表。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | Integer | 否 | 默认 1，最小 1 |
| `pageSize` | Integer | 否 | 默认 20，范围 1-100 |
| `source` | String | 否 | 新闻来源筛选 |
| `stockCode` | String | 否 | 关联股票代码筛选 |
| `keyword` | String | 否 | 标题/摘要关键词筛选 |

---

## 2.6 AI 分析接口

### GET /analysis/health

检查外部分析服务连通性。

说明：

- 当前也需要登录态
- 返回外部服务健康状态和时间戳

### POST /analysis/stock

发起股票分析。

请求体：

```json
{
  "stockCode": "600519",
  "reportType": "brief",
  "forceRefresh": false
}
```

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 是 | 股票代码 |
| `reportType` | String | 否 | 默认 `detailed`，支持 `simple/detailed/full/brief` |
| `forceRefresh` | Boolean | 否 | 是否强制刷新远端分析 |

说明：

- 当前会同步等待外部分析结果返回
- 成功后会把请求和分析结果写入 `ai_analysis`

### GET /analysis/history

查询当前用户的分析历史。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 否 | 股票代码筛选 |
| `page` | Integer | 否 | 默认 1 |
| `pageSize` | Integer | 否 | 默认 20 |

返回字段包含：

- `id`
- `stockCode`
- `stockName`
- `analysisType`
- `operationAdvice`
- `trendPrediction`
- `sentimentScore`
- `analysisSummary`
- `createdAt`

### GET /analysis/sentiment

查询指定股票最近一次分析的情绪摘要。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 是 | 股票代码，不能为空字符串 |

说明：

- 若当前用户没有该股票历史分析，会先触发一次新的股票分析

### GET /analysis/risk

查询指定股票最近一次分析的风险摘要。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 是 | 股票代码，不能为空字符串 |

说明：

- 若当前用户没有该股票历史分析，会先触发一次新的股票分析

---

## 2.7 AI 问答接口

### POST /qa/ask

发起 AI 问答。

请求体：

```json
{
  "stockCode": "600519",
  "question": "这只股票现在更适合观望还是分批介入？"
}
```

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 是 | 当前提问关联的股票代码 |
| `question` | String | 是 | 用户问题，最大 2000 字符 |

说明：

- 问答会优先复用该用户最近一次该股票分析结果
- 若没有对应分析记录，会先自动触发一次股票分析

### GET /qa/history

查询当前用户最近问答历史。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `limit` | Integer | 否 | 默认 10，范围 1-20 |

返回字段包含：

- `id`
- `question`
- `answer`
- `createdAt`

---

## 2.8 模拟交易接口

### GET /trade/account

查询当前用户模拟账户。

### POST /trade/buy

模拟买入。

请求体：

```json
{
  "stockCode": "600519",
  "price": 1688.00,
  "quantity": 100
}
```

规则：

- 买入数量必须是 100 的整数倍
- 买入价格最多保留 2 位小数
- 买入价大于等于当前价时立即成交
- 买入价低于当前价时进入 `pending`，冻结订单金额和手续费

### POST /trade/sell

模拟卖出。

请求体：

```json
{
  "stockCode": "600519",
  "price": 1688.00,
  "quantity": 100
}
```

规则：

- 卖出数量必须是 100 的整数倍
- 卖出价格最多保留 2 位小数
- 卖出价小于等于当前价时立即成交
- 卖出价高于当前价时进入 `pending`，冻结对应可用持仓

### GET /trade/orders

查询订单列表。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 否 | 股票代码筛选 |
| `orderType` | String | 否 | `buy` / `sell` |
| `status` | String | 否 | `pending` / `filled` / `cancelled` |
| `page` | Integer | 否 | 默认 1 |
| `pageSize` | Integer | 否 | 默认 20，范围 1-100 |

### DELETE /trade/orders/{orderId}

撤销订单。

说明：

- 只允许撤销 `pending` 订单
- 撤销待成交买单会释放冻结资金
- 撤销待成交卖单会释放冻结持仓
- 已成交订单不能撤销

### GET /trade/positions

查询当前持仓。

### GET /trade/records

查询交易记录。

参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `stockCode` | String | 否 | 股票代码筛选 |
| `tradeType` | String | 否 | `buy` / `sell` |
| `startDate` | String | 否 | `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `endDate` | String | 否 | `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss` |
| `page` | Integer | 否 | 默认 1 |
| `pageSize` | Integer | 否 | 默认 20，范围 1-100 |

---

## 2.9 风险预警接口

### GET /risk/alerts

查询当前账户轻量风险预警。

返回字段包含：

- `alertType`
- `alertLevel`
- `stockCode`
- `message`
- `value`

说明：

- 当前根据账户现金、总资产、持仓盈亏等实时计算
- 不是后台主动推送告警系统

---

## 2.10 回测接口

### POST /backtest/run

运行轻量买入持有回测。

请求体：

```json
{
  "stockCode": "600519",
  "initialCapital": 1000000,
  "limit": 120
}
```

返回字段包含：

- `stockCode`
- `strategyType`
- `initialCapital`
- `finalCapital`
- `totalReturn`
- `returnRate`
- `maxDrawdown`
- `tradeCount`

说明：

- 当前策略为买入持有
- 使用 K 线第一根收盘价买入，最后一根收盘价卖出
- 根据期间净值计算最大回撤
- 不落库到 `backtest_results`

---

## 2.11 WebSocket 行情

### /ws/market

客户端连接后发送股票代码，例如：

```text
600519
```

服务端会立即返回一次股票详情 JSON，并每 5 秒推送一次最新快照。客户端重新发送股票代码会切换订阅标的。

---

## 3. 当前未实现接口

以下接口或能力未实现，不应对外宣称可用：

- 短信验证码
- 自选股分组管理
- 盘口五档明细
- 资金流向明细
- 真实交易所对接
- 完整量化平台级回测引擎
- 后台主动推送式风险告警系统

---

## 4. 当前接口与旧文档差异

为了避免误解，特别说明：

- 当前 `kline` 没有 `startDate`、`endDate`
- 当前 `kline` 支持 `1s` 实时秒级聚合窗口，但没有落库型秒级历史 K 线
- 当前 `indicators` 支持 `macd/kdj/rsi/ma/boll` 和逗号组合
- 当前 WebSocket 为行情快照订阅推送，不是复杂订阅中心
- 当前已有 AI 分析、历史、情绪摘要、风险摘要接口
- 当前已有 AI 问答和问答历史接口
- 当前模拟交易支持订单列表、轻量限价挂单和待成交撤单
- 当前已有轻量回测和轻量风险预警接口

---

## 5. 调试建议

推荐调试顺序：

1. 注册
2. 登录拿 Token
3. 调行情接口
4. 调 AI 分析接口
5. 调 AI 问答接口
6. 调自选股接口
7. 调风险预警和回测接口

如果是受保护接口，请确认请求头已带：

```text
Authorization: Bearer <login token>
```
