---
title: 请求从浏览器到微服务
type: topic
source_project: agentic-mall
source_files:
  - docs/design/frontend-tracking-and-infrastructure-guide.md
  - frontend-group/vibe-operation-fe/Dockerfile
  - frontend-group/vibe-operation-fe/docker/nginx.conf.template
  - config/nginx-dev.conf
  - microservice-group/vibe-ai-center/vibe-ai-center-admin/src/main/resources/bootstrap.yml
  - scripts/observability/skywalking/
source_commit: de5eafbcbb
plan_tasks: []
status: reviewed
tags: [nginx, apisix, nacos, skywalking, gateway]
created_at: 2026-11-12
updated_at: 2026-11-12
---

# 请求从浏览器到微服务

## 一句话结论

Nginx 决定请求先到静态文件还是转到网关。Nacos 告诉微服务配置是什么、别的服务在哪。SkyWalking 在旁边记录这次请求经过了谁、哪里慢。三者同时存在，不互相替代。

小程序埋点自己怎么采集，见 [小程序埋点是怎么上报的](../frontend/miniprogram-tracking.md)。Nacos 控制台怎么点，见 [Nacos 控制台在看什么](./nacos-console-guide.md)。

## 使用场景

看一条接口为什么打到了别的服务、本地代理和生产入口是不是同一份配置、或者要分清「没配 Nacos」和「没开 SkyWalking」时用。知页笔记项目自己的单体链路不在本文，那条是 React → Spring Boot → MySQL，见 [前端、后端与基础设施关系](./frontend-backend-infrastructure-overview.md)。

## 核心原理

用三个问题区分：

| 组件 | 问的问题 | 位置 |
| --- | --- | --- |
| Nginx | 这个请求该去哪 | 入口附近，回静态文件或转发给网关 |
| Nacos | 服务在哪、配置是什么 | 微服务启动时拉配置、注册地址 |
| SkyWalking | 这次请求经过哪、哪里慢或报错 | 旁边采集，不参与转发，也不下发业务配置 |

```text
用户
→ Nginx
→ APISIX
→ 微服务（启动时使用 Nacos）
   └─ SkyWalking 在旁边采集调用链和性能数据
```

APISIX 是 API 网关，底层基于 Nginx/OpenResty，在这套系统里把它当成独立网关，不要说成「又一份前端 Nginx」。

## 在真实项目中的位置

运营端生产镜像：

```text
frontend-group/vibe-operation-fe/Dockerfile
frontend-group/vibe-operation-fe/docker/nginx.conf.template
```

构建分两段：Node 用 Vite 打出 `dist`，再用 `nginx:1.27-alpine` 托管。这份 Nginx 从 `/usr/share/nginx/html` 出页面，做 gzip、静态缓存、SPA 回落到 `index.html`、`/healthz`，并把 `/api/*` 反代到 `BACKEND_API_URL`。AI 助手的 SSE 会关代理缓冲、把超时拉长。`BACKEND_API_URL` 默认指向 APISIX。

```text
浏览器 → 运营端 Nginx → APISIX → 对应微服务
```

运营端本地开发通常不走这份生产 Nginx，由 `vite.config.ts` 的代理转到 APISIX，或直接打到本机微服务端口。

小程序本地另有 `config/nginx-dev.conf`，监听本机 `8080`，按路径把请求分到用户中心、商品中心、交易中心、促销中心。这是本地辅助，不是线上唯一入口。

Nacos 的配置声明可以看 AI 中心的 `microservice-group/vibe-ai-center/vibe-ai-center-admin/src/main/resources/bootstrap.yml` 里 `spring.cloud.nacos.config.shared-configs`。它列的是要读哪些 Data ID，例如数据库、Redis、Kafka、RocketMQ、Feign、鉴权、对象存储。服务起来后把自己的名字和地址注册上去，别的服务按名字找实例。Nacos 不给浏览器回页面，也不直接接普通业务请求。

本地 SkyWalking 在 `scripts/observability/skywalking/`。Java Agent 挂在微服务进程上采集，OAP 接收并存储，UI 看 Trace、拓扑和慢请求。它不负责路由，也不发业务配置。Agent 没启动时，业务原则上还能跑，只是没有这套监控。

## 最小示例

反向代理就是浏览器只认一个地址，由 Nginx 再转发：

```text
浏览器
→ Nginx
   ├─ 页面和静态资源：直接返回文件
   └─ /api 请求：转发给后端网关
```

## 常见问题

**项目用了 Nginx，是不是 Nginx 就是全部微服务网关？**  
不是。运营端生产配置里，前端 Nginx 后面还有 APISIX。Nginx 托管页面并把 `/api` 送去网关；APISIX 再按路由分到具体服务。

**本地 `npm run dev` 怎么没经过仓库里的 nginx.conf？**  
开发服务器用 Vite 代理。生产镜像里的 Nginx 和 `config/nginx-dev.conf` 是另外两条路径。

**SkyWalking 挂了，接口是不是就不能调？**  
它不在转发链上。看不到调用链，不表示业务请求被它拦住。

## 与相似方案的区别

Nacos 控制台笔记讲的是打开控制台后服务列表、配置列表怎么读。本文只讲它在整条链路里站哪。知页笔记项目的基础设施总览讲的是那个单体应用，不要拿来解释云 Mall 的网关。

## 面试表达

用户请求先到 Nginx：页面自己返回，接口转到 APISIX，再进微服务。微服务启动时从 Nacos 拿配置并向 Nacos 注册，服务之间按名字发现，不把 IP 写死。SkyWalking 的 Agent 在旁边记每个服务花了多久、在哪报错。三个同时在，关一个监控不会替掉网关。

## 实践与验证

本文由 `docs/design/frontend-tracking-and-infrastructure-guide.md` 归档，对照了文档里点名的运营端 Dockerfile、Nginx 模板、本地 `nginx-dev.conf`、AI 中心 `bootstrap.yml` 和 `scripts/observability/skywalking/` 这些路径。没有在这次重新部署或打开 SkyWalking UI。
