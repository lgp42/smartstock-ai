# 智能股票交易辅助系统（smartstock-ai）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)

## 项目现状

`smartstock-ai` 当前是一个可运行的单体后端项目，主要提供股票行情、自选股、模拟交易和 JWT 鉴权能力。

当前已实现：

- 用户注册、登录、登出、获取当前用户、更新当前用户
- JWT 鉴权和 Redis Token 黑名单
- 东方财富行情接入
- 股票搜索、股票详情、K 线、技术指标（`MACD`、`KDJ`、`RSI`）
- 自选股查询、添加、删除
- 模拟交易买入、卖出、持仓、账户、交易记录
- 基础参数校验、统一异常处理、单元测试

当前未实现或仅为规划：

- 前端项目代码
- AI 行情解读、新闻情绪分析、智能问答
- 策略回测
- 风险预警
- 微服务拆分
- Kafka、MinIO、LangChain4j、Claude API 集成
- WebSocket 实时推送

## 技术栈

- 后端：Spring Boot 3.5.11、Java 17、MyBatis Plus
- 数据库：MySQL 8.0
- 缓存：Redis 7.x
- 鉴权：JWT、BCrypt
- 行情源：东方财富 HTTP 接口
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

### 启动服务

```bash
mvn test
mvn spring-boot:run
```

启动后默认访问：

- 服务地址：`http://127.0.0.1:8080`
- API Base URL：`http://127.0.0.1:8080/api`

## 当前 API 范围

- 认证：`/api/auth/register`、`/api/auth/login`、`/api/auth/logout`
- 用户：`/api/users/me`
- 行情：`/api/market/stocks/search`、`/api/market/stocks/{stockCode}`、`/kline`、`/indicators`
- 自选股：`/api/watchlist`
- 交易：`/api/trade/account`、`/buy`、`/sell`、`/positions`、`/records`

## 测试现状

当前仓库已包含并通过的测试主要覆盖：

- JWT 工具和拦截器
- 异常状态码
- 行情客户端缓存裁剪
- 行情服务指标与参数校验
- 交易服务账户刷新和交易记录校验
- 控制器参数校验

当前测试总数：`28`

## 文档

- [项目概述](./doc/01-项目概述.md)
- [产品需求文档](./doc/02-产品需求文档.md)
- [技术架构文档](./doc/03-技术架构文档.md)
- [数据库设计](./doc/04-数据库设计.md)
- [API 接口文档](./doc/05-API接口文档.md)
- [开发指南](./doc/06-开发指南.md)

## 说明

- 本仓库当前只有后端，没有前端代码。
- 文档中所有“未实现/规划中”项均不代表当前仓库可直接使用。

---

**版本**：`0.0.1-SNAPSHOT`  
**最后更新**：`2026-03-18`
