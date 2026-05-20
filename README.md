# 智能股票交易辅助系统（SmartStock AI）

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)

## 项目概述

SmartStock AI 是一个前后端分离的智能股票交易辅助系统，提供行情数据、AI 深度分析、智能选股、模拟交易、风险预警、轻量回测等功能。

**线上地址**: <https://limits-easter-sci-opt.trycloudflare.com>

### 已实现功能

- 用户注册、登录、登出（含手机号方式）、JWT 鉴权 + Redis Token 黑名单
- 东方财富行情接入：股票搜索、市场快照、K 线、技术指标（MACD/KDJ/RSI/MA/BOLL）
- 自选股管理：增删改查、批量操作、排序
- 新闻快讯：分页、来源/股票/关键词筛选
- AI 分析：股票深度分析、情绪摘要、风险摘要、AI 智能问答（Gemini + 外部分析引擎）
- 模拟交易：买入/卖出、限价挂单、撤单、持仓管理、账户、交易记录
- WebSocket 行情实时推送
- 轻量风险预警、止损止盈
- 轻量买入持有回测
- Vue 3 前端：终端、新闻、智能选股、交易、风险、回测等页面

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.5.11, Java 17, MyBatis Plus |
| 前端 | Vue 3, Vite, TypeScript, Pinia, Tailwind CSS, ECharts |
| 数据库 | MySQL 8.4 |
| 缓存 | Redis 7.4 |
| 鉴权 | JWT + BCrypt |
| 行情源 | 东方财富 HTTP 接口 |
| AI | Gemini API + Python 分析引擎（FastAPI） |
| 部署 | Docker, Nginx, Cloudflare Tunnel |

## 部署架构

```
                         ┌─────────────────────────────┐
                         │   Cloudflare Tunnel           │
                         │   (trycloudflare.com)         │
                         └─────────────┬───────────────┘
                                       │
                         ┌─────────────▼───────────────┐
                         │   Nginx (port 80)            │
                         │   ├── /           → 前端 SPA │
                         │   ├── /api/*      → Java :8080 │
                         │   ├── /ws/*       → Java :8080 │
                         │   ├── /hermes/*   → Hermes :18787 │
                         │   └── /api/chat/* → Hermes :18787 │
                         └──┬──────────┬────────────────┘
                            │          │
              ┌─────────────▼──┐  ┌───▼──────────────────┐
              │  Backend :8080 │  │  Hermes WebUI :18787 │
              │  (Spring Boot) │  │  (AI Chat Interface) │
              └───────┬────────┘  └──────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
  ┌─────▼─────┐ ┌────▼────┐ ┌─────▼──────────┐
  │ MySQL:3306│ │Redis:6379│ │ Analysis :8000 │
  │ (Docker)  │ │(Docker) │ │ (Python/FastAPI)│
  └───────────┘ └─────────┘ └────────────────┘
```

## 服务器信息

| 项目 | 值 |
|------|-----|
| **IP** | 146.190.49.37 |
| **配置** | DigitalOcean SFO3, 1vCPU / 2GB / 70GB |
| **系统** | Ubuntu 24.04 (Noble) |
| **域名** | Cloudflare Tunnel (`limits-easter-sci-opt.trycloudflare.com`) |

### SSH 连接

```bash
ssh -i /path/to/id_ed25519 root@146.190.49.37
```

### 服务器目录结构

```
/opt/
├── smartstock-ai/          # 部署配置（Docker Compose、env、SQL）
│   ├── deploy/middleware/   # docker-compose.yml, .env, app.env, redis.conf
│   └── doc/sql/            # 数据库初始化脚本
└── smartstock-app/         # 构建产物
    ├── backend/             # app.jar
    ├── frontend/            # Vue dist 文件 + nginx 静态资源
    └── nginx/               # default.conf（含 Hermes 路由）
```

### Docker 容器

| 容器 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| smartstock-mysql | mysql:8.4 | 3306 | 数据库 |
| smartstock-redis | redis:7.4-alpine | 6379 | 缓存 |
| smartstock-backend | eclipse-temurin:21-jre | 8080 | Java 后端 |
| smartstock-frontend | nginx:1.25-alpine | 80 (host) | 前端 + 反向代理 |
| smartstock-cloudflared | cloudflare/cloudflared | - | Cloudflare 隧道 |
| stock-server | Python/FastAPI | 8000 | AI 分析引擎 |

## 本地开发

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+
- Node.js 18+ / pnpm

### 初始化数据库

```bash
mysql -u root -p < doc/sql/init.sql
```

### 环境变量

配置 `deploy/middleware/app.env`，或直接设置环境变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SMARTSTOCK_DB_URL` | 数据库连接 | `jdbc:mysql://127.0.0.1:3306/smartstock_ai?...` |
| `SMARTSTOCK_DB_USERNAME` | 数据库用户 | `root` |
| `SMARTSTOCK_DB_PASSWORD` | 数据库密码 | - |
| `SMARTSTOCK_REDIS_HOST` | Redis 地址 | `127.0.0.1` |
| `SMARTSTOCK_REDIS_PORT` | Redis 端口 | `6379` |
| `SMARTSTOCK_REDIS_PASSWORD` | Redis 密码 | - |
| `SMARTSTOCK_JWT_SECRET` | JWT 签名密钥 | - |
| `SMARTSTOCK_SERVER_PORT` | 服务端口 | `8080` |
| `SMARTSTOCK_ANALYSIS_BASE_URL` | 分析引擎地址 | `http://146.190.49.37:8000` |
| `SMARTSTOCK_GEMINI_API_KEY` | Gemini API Key | - |
| `SMARTSTOCK_GEMINI_MODEL` | Gemini 模型 | `gemini-3-pro-preview` |
| `SMARTSTOCK_PUBLIC_URL` | 线上地址 | `https://limits-easter-sci-opt.trycloudflare.com` |

### 启动

```bash
# 后端
mvn spring-boot:run

# 前端
cd frontend
pnpm install
pnpm run dev
```

启动后访问：
- 后端: `http://127.0.0.1:8080`
- API 文档: `http://127.0.0.1:8080/swagger-ui.html`
- 前端: `http://127.0.0.1:5173`

### 运行测试

```bash
mvn test
```

## 部署

### 前置条件

服务器上需要安装 Docker 和 Docker Compose。

### 首次部署

```bash
# 1. 本地构建
mvn clean package -DskipTests
cd frontend && pnpm build && cd ..

# 2. 上传到服务器
scp -i key root@146.190.49.37:/opt/smartstock-app/backend/app.jar  \
    server/target/*.jar
scp -i key -r frontend/dist/* root@146.190.49.37:/opt/smartstock-app/frontend/

# 3. 上传启动中间件
cd deploy/middleware
scp -i key docker-compose.yml app.env redis.conf root@146.190.49.37:/opt/smartstock-ai/middleware/
scp -i key deploy/nginx/default.conf root@146.190.49.37:/opt/smartstock-app/nginx/

# 4. 服务器上启动
ssh root@146.190.49.37

# 加载环境变量
source /opt/smartstock-ai/middleware/app.env

# 启动 MySQL + Redis
cd /opt/smartstock-ai/middleware
docker compose up -d

# 启动后端
docker run -d --name smartstock-backend --network host \
  -v /opt/smartstock-app/backend/app.jar:/app/app.jar \
  eclipse-temurin:21-jre java -jar /app/app.jar

# 启动前端 Nginx
docker run -d --name smartstock-frontend --network host \
  -v /opt/smartstock-app/frontend:/usr/share/nginx/html:ro \
  -v /opt/smartstock-app/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro \
  nginx:1.25-alpine

# 启动 Cloudflare Tunnel
docker run -d --name smartstock-cloudflared \
  cloudflare/cloudflared tunnel --no-autoupdate --url http://127.0.0.1:80
```

### 更新部署

```bash
# 1. 停止旧容器
ssh root@146.190.49.37 "docker stop smartstock-backend smartstock-frontend && \
  docker rm smartstock-backend smartstock-frontend"

# 2. 上传新构建
scp -i key server/target/*.jar root@146.190.49.37:/opt/smartstock-app/backend/app.jar
scp -i key -r frontend/dist/* root@146.190.49.37:/opt/smartstock-app/frontend/

# 3. 启动新容器（命令同首次部署）
```

## API 范围

| 模块 | 端点 |
|------|------|
| 认证 | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, `/api/auth/register/phone`, `/api/auth/login/phone` |
| 用户 | `/api/users/me`, `/api/users/me/password` |
| 行情 | `/api/market/stocks/search`, `/api/market/stocks/{code}`, `/api/market/stocks/{code}/kline`, `/api/market/stocks/{code}/indicators`, `/api/market/snapshots` |
| 智能选股 | `/api/market/screener`, `/api/market/screener/page` |
| 自选股 | `/api/watchlist`, `/api/watchlist/batch`, `/api/watchlist/sort` |
| 新闻 | `/api/news/flash`, `/api/news/flash/page` |
| AI | `/api/analysis/health`, `/api/analysis/stock`, `/api/analysis/history`, `/api/analysis/sentiment`, `/api/analysis/risk` |
| 问答 | `/api/qa/ask`, `/api/qa/history` |
| 交易 | `/api/trade/account`, `/api/trade/buy`, `/api/trade/sell`, `/api/trade/orders`, `/api/trade/positions`, `/api/trade/records` |
| 风险 | `/api/risk/alerts` |
| 回测 | `/api/backtest/run` |
| WebSocket | `/ws/market` |

## 目录结构

```
smartstock-ai/
├── server/                     # Java 后端
│   └── src/main/java/com/smartstock/
│       ├── controller/         # REST 控制器
│       ├── service/            # 业务逻辑
│       ├── mapper/             # MyBatis Mapper
│       ├── entity/             # 实体类
│       ├── config/             # Spring 配置
│       └── utils/              # 工具类
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── views/              # 页面组件
│       ├── components/         # 通用组件
│       ├── stores/             # Pinia 状态管理
│       ├── api/                # API 调用
│       └── router/             # 路由
├── deploy/                     # 部署配置
│   ├── middleware/             # Docker Compose、env、redis.conf
│   └── nginx/                  # Nginx 配置（含 Hermes 路由）
├── doc/                        # 文档
│   ├── sql/                    # 数据库初始化脚本
│   ├── 01-项目概述.md
│   ├── 02-产品需求文档.md
│   ├── 03-技术架构文档.md
│   ├── 04-数据库设计.md
│   ├── 05-API接口文档.md
│   └── 06-开发指南.md
└── deploy/middleware/          # Docker Compose 及环境配置
    ├── docker-compose.yml
    ├── app.env                 # 生产环境变量
    ├── app.env.example         # 环境变量模板
    └── redis.conf
```

## 文档

- [项目概述](./doc/01-项目概述.md)
- [产品需求文档](./doc/02-产品需求文档.md)
- [技术架构文档](./doc/03-技术架构文档.md)
- [数据库设计](./doc/04-数据库设计.md)
- [API 接口文档](./doc/05-API接口文档.md)
- [开发指南](./doc/06-开发指南.md)

---

**版本**: `0.0.1-SNAPSHOT`  
**最后更新**: `2026-05-20`
