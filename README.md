# 智能股票交易辅助系统（smartstock-ai）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)

## 项目现状

`smartstock-ai` 当前是一个可运行的前后端分离项目，主要提供股票行情、AI 分析、自选股、新闻、智能选股、模拟交易、风险预警、轻量回测和 JWT 鉴权能力。

当前已实现：

- 用户注册、登录、登出、获取当前用户、更新当前用户
- 手机号注册、手机号登录、修改密码
- JWT 鉴权和 Redis Token 黑名单
- 东方财富行情接入
- 股票搜索、股票详情、市场快照、K 线、技术指标（`MACD`、`KDJ`、`RSI`、`MA`、`BOLL`）
- 多指标组合查询，例如 `macd,kdj,ma`
- 自选股查询、添加、删除、批量添加、批量删除、排序
- 新闻快讯、新闻分页、来源/股票/关键词筛选
- 智能选股和分页智能选股
- AI 股票分析、分析历史、情绪摘要、风险摘要、AI 问答
- 模拟交易买入、卖出、限价挂单、撤单、订单列表、持仓、账户、交易记录
- WebSocket 行情订阅：`/ws/market`
- 轻量风险预警：`/api/risk/alerts`
- 轻量买入持有回测：`/api/backtest/run`
- Vue 3 前端终端、新闻、智能选股、模拟交易、风险预警、回测、账户设置页面
- 基础参数校验、统一异常处理、单元测试

当前仍不作为本仓库交付范围：

- 微服务拆分
- Kafka、MinIO、LangChain4j 集成
- 真实证券交易所对接
- 交易所级撮合、T+1、停牌、除权除息等完整模拟规则
- 完整量化平台级回测引擎

## 技术栈

- 后端：Spring Boot 3.5.11、Java 17、MyBatis Plus
- 前端：Vue 3、Vite、TypeScript、Pinia、Tailwind 风格 CSS
- 数据库：MySQL 8.0
- 缓存：Redis 7.x
- 鉴权：JWT、BCrypt
- 行情源：东方财富 HTTP 接口
- AI：外部 Python 分析服务、Gemini API
- 构建：Maven

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+

### 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE smartstock_ai DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p smartstock_ai < doc/sql/init.sql
```

### 配置说明

项目使用 `src/main/resources/application.yml`，支持环境变量覆盖。当前主要配置项：

- `SMARTSTOCK_DB_URL`
- `SMARTSTOCK_DB_USERNAME`
- `SMARTSTOCK_DB_PASSWORD`
- `SMARTSTOCK_REDIS_HOST`
- `SMARTSTOCK_REDIS_PORT`
- `SMARTSTOCK_REDIS_PASSWORD`
- `SMARTSTOCK_JWT_SECRET`
- `SMARTSTOCK_SERVER_PORT`
- `SMARTSTOCK_ANALYSIS_BASE_URL`
- `SMARTSTOCK_ANALYSIS_TIMEOUT`
- `SMARTSTOCK_GEMINI_API_KEY`
- `SMARTSTOCK_GEMINI_MODEL`

### 启动服务

```bash
mvn test
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

启动后默认访问：

- 服务地址：`http://127.0.0.1:8080`
- API Base URL：`http://127.0.0.1:8080/api`
- 前端地址：`http://127.0.0.1:5173`

## 当前 API 范围

- 认证：`/api/auth/register`、`/api/auth/login`、`/api/auth/logout`
- 手机号认证：`/api/auth/register/phone`、`/api/auth/login/phone`
- 用户：`/api/users/me`、`/api/users/me/password`
- 行情：`/api/market/stocks/search`、`/api/market/stocks/{stockCode}`、`/kline`、`/indicators`、`/snapshots`
- 智能选股：`/api/market/screener`、`/api/market/screener/page`
- 自选股：`/api/watchlist`、`/api/watchlist/batch`、`/api/watchlist/sort`
- 新闻：`/api/news/flash`、`/api/news/flash/page`
- AI：`/api/analysis/health`、`/api/analysis/stock`、`/api/analysis/history`、`/api/analysis/sentiment`、`/api/analysis/risk`
- 问答：`/api/qa/ask`、`/api/qa/history`
- 交易：`/api/trade/account`、`/buy`、`/sell`、`/orders`、`/orders/{orderId}`、`/positions`、`/records`
- 风险预警：`/api/risk/alerts`
- 回测：`/api/backtest/run`
- WebSocket：`/ws/market`

## 测试现状

当前仓库已包含并通过的测试主要覆盖：

- JWT 工具和拦截器
- 异常状态码
- 行情客户端缓存裁剪和实时 K 线组装
- 行情服务指标与参数校验
- `MA`、`BOLL`、多指标组合查询
- 交易服务账户刷新和交易记录校验
- 风险预警、轻量回测、WebSocket 行情推送
- 控制器参数校验

## 文档

- [项目概述](./doc/01-项目概述.md)
- [产品需求文档](./doc/02-产品需求文档.md)
- [技术架构文档](./doc/03-技术架构文档.md)
- [数据库设计](./doc/04-数据库设计.md)
- [API 接口文档](./doc/05-API接口文档.md)
- [开发指南](./doc/06-开发指南.md)

---

**版本**：`0.0.1-SNAPSHOT`  
**最后更新**：`2026-04-26`
