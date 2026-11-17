---
title: HTTP / HTTPS 面试考点
type: topic
source_project: agentic-mall
source_files:
  - frontend-group/vibe-miniprogram-fe/src/utils/getLocationCompat.ts
source_commit: de5eafbcbb
plan_tasks: []
status: reviewed
tags: [http, https, tls, certificate, secure-origin, geolocation]
created_at: 2026-11-10
updated_at: 2026-11-10
---

# HTTP / HTTPS 面试考点

## 一句话结论

HTTP 是明文网页协议；HTTPS 是 HTTP 外面套了一层 TLS 加密。https **一定有证书**，但 **https 不等于公网**。浏览器定位、Cookie 安全、微信 JS-SDK 都认的是「安全源」，不是「是不是局域网」。

## 使用场景

面试被问网络基础、为什么本地 `localhost` 能定位、为什么 `http://10.x` 不行、锁图标是什么、混合内容是什么时用本篇。不要用它替代 CORS、HTTP 头字段的专题（见同目录 Referer、Network 头笔记）。

## 核心原理

两套正交概念，不要叠成一个词：

| 维度 | 说的是什么 | 常见叫法 |
| --- | --- | --- |
| 网络在哪 | 机器能不能被互联网访问到 | **局域网** vs **公网** |
| 连法 | 路上要不要加密 | **http** vs **https（HTTP + TLS）** |

`https://xxxh5.com` 正确叫法：**公网的 https 站点**（https 域名）。不要说「https 就是公网」。局域网也能 https，公网也能 http。

```text
HTTP  = 明信片（明文）
TLS   = 上锁的箱子（加密协议，旧名 SSL）
证书  = 箱子上的身份牌（证明服务器是这个域名）
HTTPS = HTTP + TLS
```

TLS 握手时服务器必须拿出证书，没有证书就建不成 https。证书分：

- **CA 签发**：浏览器信任，地址栏有锁。线上 H5 这种。
- **自签**：也是 https，浏览器会警告，点继续后对「安全源」通常已经算数。

浏览器特批的安全源（和是不是公网无关）：

- `https://任意主机`（有 TLS）
- `http://localhost`
- `http://127.0.0.1`（本机回环地址，包不出这台电脑；`localhost` 一般解析到它或 IPv6 的 `::1`）

`http://10.x` / `http://192.168.x` 是局域网 + 明文，**不是**安全源。`navigator.geolocation` 会被直接拒绝，连授权框都不弹。

## 面试表达（1～3 分钟）

HTTP 传的是明文。HTTPS 不是另一种网页协议，是 HTTP 下面加了 TLS。TLS 握手要证书，所以 https 一定有证书；证书证明「你是这个域名」，TLS 负责加密。公网、局域网说的是机器在哪，和加不加密是两件事。线上 H5 一般是公网加 https。浏览器把 https、localhost、127.0.0.1 当成安全源，局域网 IP 的 http 不行，所以门店列表在 `http://10.x:10086` 拿不到定位，换 `http://localhost:10086` 或正式 `https://域名` 就可以。要确认 TLS，看地址栏锁，或 F12 的 Security 面板，那里有 TLS 版本和证书；页面里看到的 HTML 已经是解密后的。

## 考点清单

### 1. HTTP 和 HTTPS 有什么区别？

**口述：** HTTP 明文传输，端口默认 80。HTTPS 是 HTTP over TLS，默认 443，传输加密，能校验服务器身份，防中间人窃听和篡改。对前端的体感：地址栏有锁、接口是 `https://`、混合内容会被拦。

**可能追问：** HTTPS 会不会更慢？

**答：** 多一次握手。TLS 1.3 和会话复用后开销很小，现在默认都上 https，几乎不拿性能当不上 https 的理由。

### 2. HTTPS 是不是一定有证书？

**口述：** 一定有。TLS 握手服务器必须出示证书，浏览器用它验证域名、拿到公钥，才能把对称密钥安全送过去。没有证书就没有 https。

**可能追问：** 自签算不算 https？

**答：** 算。协议仍是 TLS。浏览器不信任这张证，会警告；用户点继续后连接仍加密。定位等 API 看的是「是不是 https 安全源」，不是「证书是不是 Let’s Encrypt」。

### 3. HTTPS 就是公网吗？

**口述：** 不是。公网 / 局域网说位置；http / https 说加密。四种组合都存在。正确叫 `https://xxx.com`：**公网 https 站点**。

### 4. TLS 是什么？和 SSL 什么关系？

**口述：** Transport Layer Security，传输层安全协议，负责加密、完整性和身份校验。SSL 是旧名，现在用 TLS（常见 1.2 / 1.3）。有人仍说「SSL 证书」，指的是同一类 X.509 证书。

**可能追问：** 证书和 TLS 谁管什么？

**答：** 证书回答「你是谁」；TLS 回答「怎么加密」。HTTPS = HTTP + TLS，TLS 依赖证书。

### 5. 怎么「看到」TLS？页面里能直接看密文吗？

**口述：** 不能在页面里打开一封 TLS 信。DOM、Network 里的响应已经是浏览器解开的明文。要确认用了 TLS：

1. 地址栏锁 → 证书有效
2. F12 → **Security**：This page is secure、证书、Connection 里的 TLS 1.2/1.3
3. Network 里 scheme 是 `https`

路上的密文要用 Wireshark 才看得到，面试说到 Security 面板即可。

### 6. 什么是安全源？为什么和定位有关？

**口述：** Chrome 把摄像头、定位、部分 Cookie 能力绑在安全源上，防止中间人在明文 http 里偷位置。安全源包括 https、localhost、127.0.0.1。`http://局域网IP` 会被拒。这是浏览器策略，前端 JS 改不了。

**可能追问：** 微信小程序要不要 https？

**答：** 小程序原生 `wx.getLocation` 不走浏览器安全源，走微信权限和隐私协议。H5 网页、公众号 JS-SDK 仍要 https 业务域名。

### 7. localhost 和 127.0.0.1 是什么？

**口述：** 都是「这台电脑自己」。`127.0.0.1` 是 IPv4 回环地址；`localhost` 是主机名，通常解析到 `127.0.0.1` 或 `::1`。浏览器把它们的 **http** 也特批成安全源。`10.x` 是网卡上的局域网地址，包会出本机网卡，浏览器不当成本机特批。

### 8. 混合内容（Mixed Content）是什么？

**口述：** https 页面里再去请求 `http://` 的脚本、接口、图片。浏览器会拦脚本和接口（主动混合内容），图片可能只警告。所以 https 站点的 API、CDN 也必须 https，或走同源反代。

### 9. 为什么线上必须 https？只为了定位吗？

**口述：** 不止定位。还有防窃听（登录态、地址）、Cookie 的 `Secure`、Service Worker、微信 JS-SDK、浏览器对明文 http 越来越严。定位只是安全源限制里前端最容易踩到的一条。

## 在真实项目中的位置

门店列表 H5 定位：`frontend-group/vibe-miniprogram-fe/src/utils/getLocationCompat.ts`。浏览器走 `navigator.geolocation`，要求安全源。用 `http://10.x:10086` 打开会直接失败；`http://localhost:10086` 或正式 https H5 域名可以。坐标系和调用链见 [小程序和 H5 怎么拿定位](./weapp-h5-geolocation.md)。

## 最小对照

| 打开方式 | 网络 | 协议 | 安全源 | 浏览器定位 |
| --- | --- | --- | --- | --- |
| `https://xxxh5.com` | 公网 | https | 是 | 能 |
| `http://xxxh5.com` | 公网 | http | 否 | 不能 |
| `http://localhost:10086` | 本机 | http | 是（特批） | 能 |
| `http://127.0.0.1:10086` | 本机回环 | http | 是（特批） | 能 |
| `http://10.x:10086` | 局域网 | http | 否 | 不能 |
| `https://10.x:10086`（含自签） | 局域网 | https | 是 | 能（可能先警告证书） |

## 常见问题

- **「https 就是有锁的网站」**：锁表示证书被浏览器信任；自签 https 也加密，只是没有那把绿锁。
- **「局域网就不能定位」**：局域网 + **http** 不能。局域网 + **https** 可以。
- **「改 JS 兼容 http://10.x 的 GPS」**：兼容不了真 GPS。最多做 IP 城市级兜底，距离不准，且依赖外网 IP 接口。
- **和 CORS 搞混**：CORS 是跨域策略；安全源是能力限制。可以同时踩：http 页调 https 接口，或 https 页调 http 接口（混合内容）。

## 与相似方案的区别

| | 本篇 | 相关笔记 |
| --- | --- | --- |
| 协议与安全源 | 本篇 | — |
| 请求头、Referer | — | [HTTP Referer](./http-referer.md)、[Network 请求头](./http-network-headers.md) |
| 跨域 | — | [XHR 与 CORS](./xhr-cors.md) |
| 小程序 / H5 怎么拿点 | 安全源是前置条件 | [小程序和 H5 怎么拿定位](./weapp-h5-geolocation.md) |

## 实践与验证

在 agentic-mall 门店列表 H5 上验证过：`http://局域网IP:10086` 打开会出现定位空态，Chrome 标「不安全」；换 localhost 后浏览器会弹定位授权。IP 定位兜底不能替代安全源上的真 GPS。
