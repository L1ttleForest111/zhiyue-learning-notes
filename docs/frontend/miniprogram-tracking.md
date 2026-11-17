---
title: 小程序埋点是怎么上报的
type: topic
source_project: agentic-mall
source_files:
  - docs/design/frontend-tracking-and-infrastructure-guide.md
  - frontend-group/vibe-miniprogram-fe/src/lib/tracking-sdk/index.ts
  - frontend-group/vibe-miniprogram-fe/src/common/tracking/index.ts
  - frontend-group/vibe-miniprogram-fe/src/common/tracking/init.ts
  - frontend-group/vibe-miniprogram-fe/src/pages/goods-detail/hooks.ts
source_commit: de5eafbcbb
plan_tasks: []
status: reviewed
tags: [tracking, sdk, payload, request-header, weapp]
created_at: 2026-11-12
updated_at: 2026-11-12
---

# 小程序埋点是怎么上报的

## 一句话结论

埋点是把用户行为收成事件，交给内嵌 SDK 补齐公共字段、排队后上报。业务页面只说「发生了什么」，不自己拼请求。上报地址是 `POST /api/v1/rmall/data/report`。

## 使用场景

给小程序加点击、加购、搜索、收藏这类行为记录时用。普通 H5 预览没有小程序对象时，入口会换成空壳 SDK，避免页面崩溃。Nginx、Nacos、SkyWalking 怎么分工，见 [请求从浏览器到微服务](../backend/request-path-nginx-nacos-skywalking.md)。Chrome 里怎么看请求头，见 [Chrome Network 中的 HTTP 请求与响应头](./http-network-headers.md)。

## 核心原理

```text
用户行为
→ 前端采集事件
→ SDK 补充并整理数据
→ SDK 排队、合并并上传
→ 后端埋点接口
→ Kafka 等后续数据处理链路
```

SDK 是一套现成能力，不是又一种 `import` 语法。模块说的是代码怎么被导入；SDK 说的是这组代码能采集、排队、上传、重试。

本项目的埋点 SDK 来自 `sr-sdk-wxapp v1.8.8-dev.7`，已经复制进仓库并改成云 Mall 自己的版本。所以它既是第三方来源，也是仓库里自己维护的模块。

入口按环境选实现：

```text
检查当前运行环境
├─ 存在小程序环境：加载真正的小程序埋点 SDK
└─ 不存在小程序环境：提供不会报错的空壳 SDK
```

空壳里的 `noop` 是 no operation，什么也不做。

底层是默认导出。上层埋点模块先 `import trackingSDK from '@/lib/tracking-sdk'`，再 `export { trackingSDK }`。只 import 不会让别的文件从当前文件再导入；重新导出之后，业务才能写 `import { trackingSDK } from '@/common/tracking'`。`@` 是 `src` 的路径别名。

## 在真实项目中的位置

| 职责 | 路径 |
| --- | --- |
| 按环境选择 SDK | `frontend-group/vibe-miniprogram-fe/src/lib/tracking-sdk/index.ts` |
| 真正的小程序 SDK | `frontend-group/vibe-miniprogram-fe/src/lib/tracking-sdk/sdk/weapp` |
| 业务统一入口 | `frontend-group/vibe-miniprogram-fe/src/common/tracking/index.ts` |
| 初始化和上报地址 | `frontend-group/vibe-miniprogram-fe/src/common/tracking/init.ts` |
| 加购调用点 | `frontend-group/vibe-miniprogram-fe/src/pages/goods-detail/hooks.ts` |

加购成功后页面调用 `tracking.addToCart(...)`。`addToCart()` 把业务参数交给 `fire()`，再调用 `trackingSDK.track('add_to_cart', ...)`。后面补用户、页面、门店、渠道、会话和时间，清洗，进队列，由 SDK 调接口。页面不再自己发请求。

上报地址由网关基础地址拼出来：`${TARO_APP_BASE_URL}/api/v1/rmall/data/report`。

改采集、队列、上传或排查 SDK 故障时，再读 `sdk/weapp`。日常加一个事件，停在 `src/common/tracking`。

## 最小示例

一次上报可以看成：

```http
POST /api/v1/rmall/data/report
X-WX-APPID: wx123456
Content-Type: application/json

[
  {
    "action_type": "add_to_cart",
    "action_param": { "sku_id": "10001", "sku_num": 1 }
  }
]
```

Header 放「这个请求是谁、什么格式」。Payload（这里就是 Body）放「这次要记的行为」。本项目用 `X-WX-APPID` 让后端认出小程序和租户。批量上传多条事件时，公共 AppID 放 Header，只传一次。

请求经过的顺序是：小程序 → Nginx → API 网关 → 鉴权、租户、限流、追踪等拦截器 → Controller。网关和拦截器读统一 Header，不必先看懂每个接口不一样的 JSON。

Header 并不比 Payload 更安全。走 HTTPS 时两边都加密。安全取决于身份校验、权限和日志里不要打出凭证。

## 常见问题

**只 import 了 SDK，别的文件为什么还是用不了？**  
`import` 只给当前文件用。要给别人用，还得 `export`。

**H5 预览里埋点没发出去，是不是坏了？**  
入口发现不是小程序环境时会换成空壳，方法调用被吃掉，用来避免没有微信对象时崩溃。

**能不能把 AppID、租户、Token 都放进 Body？**  
技术上可以。网关就得解析每种 Body，每个接口都要重复公共字段，批量事件还会重复带同一份信息。GET 和文件上传也没有同一种 Body。

## 与相似方案的区别

页面里直接 `request.post` 也能把事件发出去，但用户、门店、渠道、失败重试会在每个页面复制一遍。统一入口加 SDK 是把这些收口。请求头的通用字段说明不在本文展开，用 Network 笔记。

## 面试表达

埋点不是多写一个接口。页面调用 `tracking.addToCart` 这类方法，SDK 补上公共字段，排队后 `POST /api/v1/rmall/data/report`。AppID 放 Header，事件放 Body。小程序环境才加载真 SDK，普通网页预览用空壳。

## 实践与验证

本文由 `docs/design/frontend-tracking-and-infrastructure-guide.md` 归档，没有在这次重新触发一次真实加购上报。上报是否到达后端，要以 Network 里出现该 POST 为准。
