# SmartStock AI 代码库参考

## 1. 项目定位

当前仓库是一个股票行情与 AI 分析辅助系统，包含：

- Spring Boot 后端。
- Vue 3 前端。
- MySQL 数据库初始化脚本。
- Redis 缓存与 JWT 黑名单。
- 东方财富行情和新闻接口适配。
- 外部 Python AI 分析服务集成。
- Gemini 智能问答集成。
- Docker 中间件配置。

不要把它理解成完整量化交易平台或真实交易系统。它更适合作为“行情查询 + AI 分析 + 自选股 + 演示级模拟交易”的课程/作品项目。

## 2. 根目录

项目根目录：`/Users/antares2/Projects/java/smartstock-ai`

重要目录：

- `src/main/java/com/smartstock`：后端源码。
- `src/main/resources`：后端配置和资源。
- `src/test/java/com/smartstock`：后端测试。
- `frontend`：Vue 前端。
- `doc`：项目文档、数据库脚本、图。
- `deploy/middleware`：Docker 中间件配置。
- `mcp-servers`：辅助 MCP/工具相关内容。

重要文件：

- `pom.xml`：后端 Maven 配置。
- `src/main/resources/application.yml`：后端配置。
- `frontend/package.json`：前端依赖与脚本。
- `doc/sql/init.sql`：数据库初始化脚本。
- `doc/05-API接口文档.md`：API 文档。
- `doc/03-技术架构文档.md`：架构说明。

## 3. 后端技术栈

- Spring Boot 3.5.11
- Java 17
- Maven
- MyBatis Plus 3.5.x
- MySQL 8
- Redis 7
- JWT：JJWT 0.12.x
- BCrypt：Spring Security Crypto
- WebClient：调用外部服务
- Resilience4j：外部接口重试、熔断、限流、隔离
- Springdoc OpenAPI

当前是单体应用，不是微服务。

## 4. 后端包结构

`src/main/java/com/smartstock`：

- `client`：外部接口客户端。
  - `EastMoneyClient`：东方财富行情、搜索、K 线、实时行情、选股候选数据。
  - `SinaNewsClient`：新浪财经新闻。
  - `ClsNewsClient`：财联社新闻。
  - `EastMoneyNewsClient`：东方财富新闻。
  - `RemoteHttpClient` / `WebClientRemoteHttpClient`：HTTP 抽象与实现。
- `common`：通用能力。
  - `Result`：统一响应壳。
  - `BusinessException`：业务异常。
  - `ErrorCode`：错误码。
  - `GlobalExceptionHandler`：全局异常处理。
- `config`：配置。
  - `JwtAuthInterceptor`：JWT 拦截器。
  - `WebConfig`：拦截器注册。
  - `SecurityConfig`：密码编码等安全配置。
  - `CacheConfig`：缓存。
  - `OpenApiConfig`：OpenAPI。
  - `AnalysisServiceProperties` / `GeminiProperties`：AI 配置。
- `controller`：REST Controller。
- `dto`：请求 DTO。
- `entity`：数据库实体。
- `mapper`：MyBatis Plus Mapper。
- `service`：服务接口。
- `service/impl`：服务实现。
- `service/support`：行情辅助组装逻辑。
- `util`：工具类。
- `vo`：响应 VO。

## 5. 当前后端接口模块

### 用户与认证

文件：

- `UserController`
- `UserService`
- `UserServiceImpl`
- `User`
- `UserMapper`
- `JwtUtil`
- `JwtAuthInterceptor`

接口：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/register/phone`
- `POST /api/auth/login/phone`
- `POST /api/auth/logout`
- `GET /api/users/me`
- `PUT /api/users/me`
- `PUT /api/users/me/password`

要点：

- 使用邮箱或手机号注册/登录。
- 密码使用 BCrypt。
- 登录后返回 JWT。
- 登出会把 token 放进 Redis 黑名单。
- 更新用户只覆盖昵称和头像。
- 修改密码需要旧密码校验。

### 行情

文件：

- `MarketController`
- `MarketService`
- `MarketServiceImpl`
- `EastMoneyClient`
- `RealtimeKlineAssembler`
- `RealtimeSecondSeriesAssembler`

接口：

- `GET /api/market/stocks/search`
- `GET /api/market/stocks/{stockCode}`
- `GET /api/market/stocks/{stockCode}/kline`
- `GET /api/market/stocks/{stockCode}/indicators`
- `GET /api/market/snapshots`
- `GET /api/market/screener`
- `GET /api/market/screener/page`

要点：

- 股票搜索优先查 `stock_info`，必要时查东方财富。
- K 线支持日、周、月、分钟周期，以代码实际支持为准。
- 技术指标已有 MACD、KDJ、RSI、MA、BOLL，并支持逗号组合查询。
- WebSocket `/ws/market` 支持发送股票代码后立即返回行情快照，并每 5 秒推送一次。
- 智能选股已有筛选接口，支持板块、行业组、市值、PE、换手率、涨跌幅、ST/退市过滤、技术形态等。

### 自选股

文件：

- `WatchlistController`
- `WatchlistServiceImpl`
- `UserWatchlist`
- `UserWatchlistMapper`

接口：

- `GET /api/watchlist`
- `POST /api/watchlist`
- `DELETE /api/watchlist/{stockCode}`
- `POST /api/watchlist/batch`
- `DELETE /api/watchlist/batch`
- `PUT /api/watchlist/sort`

要点：

- 单用户上限 50。
- 不允许重复添加。
- 当前支持排序、批量添加和批量删除；分组管理仍未实现。

### 新闻

文件：

- `NewsController`
- `NewsServiceImpl`
- `News`
- `NewsMapper`
- `SinaNewsClient`
- `ClsNewsClient`
- `EastMoneyNewsClient`

接口：

- `GET /api/news/flash`
- `GET /api/news/flash/page`

要点：

- 聚合新浪财经、财联社、东方财富。
- 支持来源、股票代码、关键词筛选。
- 新闻会落库到 `news`。
- 无筛选时会优先使用缓存。

### AI 分析

文件：

- `AnalysisController`
- `AnalysisServiceImpl`
- `AnalysisServiceProperties`
- `AiAnalysis`
- `AiAnalysisMapper`

接口：

- `GET /api/analysis/health`
- `POST /api/analysis/stock`
- `GET /api/analysis/history`
- `GET /api/analysis/sentiment`
- `GET /api/analysis/risk`

要点：

- Java 后端通过 HTTP 调用外部 Python AI 分析服务。
- 默认分析服务地址来自 `SMARTSTOCK_ANALYSIS_BASE_URL`。
- 分析结果写入 `ai_analysis`。
- 情绪摘要、风险摘要优先复用最近分析结果。

### 智能问答

文件：

- `QaController`
- `QaServiceImpl`
- `GeminiProperties`
- `QaHistory`
- `QaHistoryMapper`

接口：

- `POST /api/qa/ask`
- `GET /api/qa/history`
- `GET /api/qa/sessions`
- `GET /api/qa/sessions/{sessionId}`

要点：

- 通过 Gemini API 生成回答。
- 会读取最近一次股票分析作为上下文。
- 问答写入 `qa_history`。
- 前端已有 Copilot 页面。

### 模拟交易

文件：

- `TradeController`
- `TradeServiceImpl`
- `Account`
- `TradeOrder`
- `Position`
- `TradeRecord`
- 相关 Mapper 和 VO/DTO

接口：

- `GET /api/trade/account`
- `POST /api/trade/buy`
- `POST /api/trade/sell`
- `GET /api/trade/orders`
- `DELETE /api/trade/orders/{orderId}`
- `GET /api/trade/positions`
- `GET /api/trade/records`

要点：

- 当前是演示级模拟交易。
- 买入价大于等于当前价时立即成交，低于当前价时进入 `pending` 并冻结资金。
- 卖出价小于等于当前价时立即成交，高于当前价时进入 `pending` 并冻结可用持仓。
- 已成交订单不能撤销，待成交订单撤销后释放冻结资金或持仓。
- 买入数量要求 100 的整数倍。
- 卖出检查可用持仓。
- 有手续费、印花税、价格范围校验、账户刷新。
- 不要默认扩展成真实交易系统。

### 风险预警与回测

文件：

- `RiskController`
- `RiskServiceImpl`
- `BacktestController`
- `BacktestServiceImpl`
- `RiskAlertVO`
- `BacktestRunDTO`
- `BacktestResultVO`

接口：

- `GET /api/risk/alerts`
- `POST /api/backtest/run`

要点：

- 风险预警根据账户现金、总资产、持仓盈亏实时计算，不写入 `risk_alerts`。
- 回测为买入持有轻量测算，不写入 `backtest_results`。
- 前端已有 `RiskView.vue` 和 `BacktestView.vue`。

## 6. 前端结构

`frontend/src`：

- `App.vue`：根组件。
- `main.ts`：入口。
- `router/index.ts`：路由和登录态守卫。
- `utils/request.ts`：Axios 封装。
- `types/index.ts`：前端类型。
- `stores/auth.ts`：认证状态。
- `stores/toast.ts`：提示状态。
- `components/Layout.vue`：主布局。
- `components/EmptyState.vue`：空态。
- `components/PriceChange.vue`：涨跌展示。
- `components/ToastContainer.vue`：Toast。
- `views/LoginView.vue`：登录。
- `views/RegisterView.vue`：注册。
- `views/DashboardView.vue`：仪表盘/自选。
- `views/TerminalView.vue`：个股终端。
- `views/CopilotView.vue`：AI 问答。
- `views/ScreenerView.vue`：智能选股。
- `views/NewsView.vue`：新闻。
- `views/TradeView.vue`：模拟交易。
- `views/RiskView.vue`：风险预警。
- `views/BacktestView.vue`：策略回测。
- `views/SettingsView.vue`：账户设置。

前端请求约定：

- 所有 API 走 `/api` baseURL。
- `request.ts` 自动读取 localStorage token 并设置 Authorization。
- 后端返回 code=200 时，`request.ts` 直接返回 `data`。
- 401 会清除登录态并跳转登录页。

新增页面时：

1. 在 `views` 新增页面。
2. 在 `router/index.ts` 加路由。
3. 在 `Layout.vue` 里按需加导航入口。
4. 在 `types/index.ts` 补类型。
5. 用 `request.ts` 调接口。

## 7. 数据库

初始化脚本：`doc/sql/init.sql`

当前代码实际使用的表：

- `users`
- `stock_info`
- `news`
- `user_watchlist`
- `ai_analysis`
- `qa_history`
- `accounts`
- `orders`
- `positions`
- `trade_records`

预留或规划表：

- `user_profiles`
- `stock_prices`
- `strategies`
- `backtest_results`
- `risk_alerts`
- `stop_loss_profit`

不要看到表就默认功能已实现。必须检查 Controller/Service。

## 8. 配置

后端配置文件：`src/main/resources/application.yml`

常用环境变量：

- `SMARTSTOCK_DB_URL`
- `SMARTSTOCK_DB_USERNAME`
- `SMARTSTOCK_DB_PASSWORD`
- `SMARTSTOCK_REDIS_HOST`
- `SMARTSTOCK_REDIS_PORT`
- `SMARTSTOCK_REDIS_PASSWORD`
- `SMARTSTOCK_REDIS_DATABASE`
- `SMARTSTOCK_JWT_SECRET`
- `SMARTSTOCK_JWT_EXPIRATION_SECONDS`
- `SMARTSTOCK_SERVER_PORT`
- `SMARTSTOCK_EASTMONEY_BASE_URL`
- `SMARTSTOCK_EASTMONEY_HIS_URL`
- `SMARTSTOCK_EASTMONEY_SEARCH_URL`
- `SMARTSTOCK_ANALYSIS_BASE_URL`
- `SMARTSTOCK_ANALYSIS_TIMEOUT`
- `SMARTSTOCK_GEMINI_BASE_URL`
- `SMARTSTOCK_GEMINI_API_KEY`
- `SMARTSTOCK_GEMINI_MODEL`
- `SMARTSTOCK_GEMINI_TIMEOUT`

不要在代码里硬编码生产密钥。`application.yml` 里的默认值只能当开发便利。

## 9. 常见开发路径

### 新增后端接口

1. 查是否已有类似 Controller/Service/VO/DTO。
2. 新增或复用 DTO。
3. 新增或复用 VO。
4. 在 Service 接口加方法。
5. 在 ServiceImpl 实现业务。
6. 在 Controller 暴露接口。
7. 加必要的测试。
8. 前端需要时补 `types/index.ts` 和页面调用。

### 新增数据库表

1. 更新 `doc/sql/init.sql`。
2. 新增 `entity`。
3. 新增 `mapper`。
4. 如需返回前端，新增 VO。
5. 只在确实需要时更新文档。

### 新增前端能力

1. 先确认后端接口路径和返回字段。
2. 在 `types/index.ts` 补类型。
3. 在页面中用 `request.get/post/put/delete`。
4. 保持深色数据密集风格。
5. 跑 `npm run build`。

### 接入外部服务

1. 优先在 `client` 下新增或扩展客户端。
2. 配置项放到 `application.yml`，用环境变量覆盖。
3. 外部响应解析只做必要容错。
4. 如果服务不稳定，优先复用已有 Resilience4j/RemoteHttpClient 模式。

## 10. 测试

已有测试覆盖：

- JWT 工具和拦截器。
- 全局异常处理。
- API 参数校验。
- 行情客户端与行情服务。
- 新闻客户端与新闻服务。
- 交易服务。
- 用户服务。
- 实时 K 线辅助组装。
- 基础设施配置。

常用命令：

```bash
mvn test
```

指定测试：

```bash
mvn test -Dtest=TradeServiceImplTest
```

```bash
mvn test -Dtest=MarketServiceImplTest,EastMoneyClientTest
```

前端：

```bash
cd frontend && npm run build
```

## 11. 未完成或只适合轻量实现的方向

未完成/规划项：

- 分时图专用接口。
- 盘口数据。
- 资金流向。
- 自选股分组管理。
- 后台主动风险预警推送。
- 监控告警。
- CI/CD。
- 多环境 profile 治理。
- Kafka。
- MinIO。
- LangChain4j。
- 微服务拆分。

实现建议：

- 回测已有轻量版本，不要一开始升级成完整量化平台。
- 风险预警已有轻量版本，不要一开始做推送系统。
- WebSocket 已做行情快照推送，不要绑定复杂订阅中心。
- 模拟交易不要继续深挖真实交易细节，除非用户明确要求。

## 12. 部署和服务器

项目里有 `deploy/middleware/docker-compose.yml`，用于中间件。

用户曾提供过服务器连接资料，位于：

```text
/Users/antares2/Downloads/LocalSend/YunUbuntu
```

连接服务器前不要泄露私钥或密码内容。可以读取配置并使用 SSH 执行检查命令。

已知服务器：

- 用户：`root`
- 主机：`146.190.49.37`
- 私钥：`/Users/antares2/Downloads/LocalSend/YunUbuntu/id_ed25519`
- 私钥有 passphrase，配置 JSON 中的 password 可用于解锁。

连接示例不要把密码明文写入最终回复。

## 13. 重要取舍

项目的最高价值不是“交易功能逼真”，而是：

1. 行情数据可用。
2. AI 分析可用。
3. 自选和新闻辅助决策可用。
4. 前端体验完整。
5. 回测和风险预警能形成演示闭环。

做功能时优先围绕这五点收敛。
