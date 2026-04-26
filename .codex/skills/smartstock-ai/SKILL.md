---
name: smartstock-ai
description: SmartStock AI 项目专用开发指南。处理 /Users/antares2/Projects/java/smartstock-ai 仓库时使用，包括阅读代码、修改 Spring Boot 后端、Vue 前端、股票行情、技术指标、新闻、自选股、AI 分析、智能问答、智能选股、模拟交易、MySQL/Redis 配置、Docker 中间件、Python AI 服务集成、部署和测试。
---

# SmartStock AI

## 使用原则

先读代码，再读文档。项目文档有参考价值，但部分内容已滞后：当前仓库已经有 Vue 前端、新闻模块、智能选股、AI 分析、智能问答和模拟交易。

保持改动小而准。不要为了一个功能顺手重构周边代码，不要添加未来才可能用到的抽象、兼容层、开关或大而全的错误处理。

需要模块结构、接口边界、配置项、数据库表和常见开发路径时，读取 `references/codebase.md`。

## 后端开发

后端是 Spring Boot 3.5.11 + Java 17 + MyBatis Plus 单体应用。

遵循现有分层：

- `controller`：REST API 入口。
- `dto`：请求体。
- `vo`：响应数据。
- `entity`：数据库实体。
- `mapper`：MyBatis Plus Mapper。
- `service`：服务接口。
- `service/impl`：业务实现。
- `client`：外部 HTTP 接口适配。
- `common`：统一返回、异常、错误码。
- `config`：配置和拦截器。

接口返回统一用 `Result.ok(...)`。业务错误抛 `BusinessException`，优先复用现有 `ErrorCode`。

鉴权接口通过 `UserContext.getUserId(request)` 获取当前用户，不要自己解析 JWT。

参数校验放在系统边界：DTO 注解、Controller 参数检查、外部接口返回解析。内部方法之间不要层层重复校验不可能发生的情况。

## 前端开发

前端是 `frontend/` 下的 Vue 3 + Vite + TypeScript 应用。

遵循现有结构：

- 页面放在 `frontend/src/views/*View.vue`。
- 共享组件放在 `frontend/src/components`。
- 路由在 `frontend/src/router/index.ts`。
- API 请求统一使用 `frontend/src/utils/request.ts`。
- 前后端类型放在 `frontend/src/types/index.ts`。
- 登录态使用 `frontend/src/stores/auth.ts`。

`request.ts` 已经自动加 `Authorization: Bearer <token>`，并解包后端 `{ code, message, data }` 响应。页面里不要再重复处理统一响应壳。

视觉上延续当前深色金融终端风格，优先做可用的业务页面，不做营销落地页。

## 产品边界

项目核心是“股票行情 + AI 分析 + 自选股 + 辅助决策”。实现功能时优先服务这个定位。

模拟交易已存在，默认只保持演示级能力：账户、买入、卖出、轻量限价挂单、撤单、订单列表、持仓、交易记录。除非用户明确要求，不要扩展成真实交易所级别的撮合、T+1、停牌、除权除息等复杂规则。

风险预警和回测已有轻量版：能展示结果、能说明逻辑、能服务课程/演示闭环即可。不要误写成完整量化平台或后台主动告警系统。

## 常用验证

后端全量测试：

```bash
mvn test
```

后端局部测试：

```bash
mvn test -Dtest=MarketServiceImplTest,QueryParamValidationTest
```

前端构建：

```bash
cd frontend && npm run build
```

本地启动后端：

```bash
mvn spring-boot:run
```

本地启动前端：

```bash
cd frontend && npm run dev
```
