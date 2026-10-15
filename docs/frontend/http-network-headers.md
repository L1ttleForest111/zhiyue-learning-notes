# Chrome Network 中的 HTTP 请求与响应头

本文面向刚开始使用浏览器开发者工具排查接口问题的前端同学，介绍 Chrome DevTools 的 Network 面板中 `General`、`Request Headers`、`Response Headers` 等区域，以及常见请求方法、状态码和 `Content-Type`。

以下面的 AI 流式接口为例：

```text
POST https://aihome-qa.rmall-solution.com/admin/ai/assistant/stream
```

它返回：

```http
HTTP/2 200 OK
Content-Type: text/event-stream
```

这表示请求成功建立了 SSE 流式连接，服务端可以持续向前端推送 AI 回复片段。

## 一、一次 HTTP 请求包含什么

浏览器调用接口时会发送：

```text
请求方法 + 请求地址
Request Headers（请求头）
Request Body / Payload（请求体）
```

服务器处理后返回：

```text
Status Code（状态码）
Response Headers（响应头）
Response Body（响应体）
```

完整过程可以表示为：

```text
浏览器
  │
  │ 请求方法、URL、请求头、请求体
  ▼
服务器或网关
  │
  │ 状态码、响应头、响应体
  ▼
浏览器处理结果并更新页面
```

Network 请求详情中的常见标签：

| 标签 | 作用 |
| --- | --- |
| `Headers` | 查看地址、方法、状态码、请求头和响应头。 |
| `Payload` | 查看提交给服务器的查询参数、表单或请求正文。 |
| `EventStream` | 查看 SSE 流中的每一条事件。 |
| `Response` | 查看服务器返回的原始响应正文。 |
| `Preview` | 以更便于阅读的方式预览 JSON、HTML、图片等内容。 |
| `Initiator` | 查看是哪段前端代码或哪个资源发起了请求。 |
| `Timing` | 查看排队、DNS、TCP、TLS、等待响应和下载等耗时。 |

## 二、General：请求概览

`General` 是 Chrome 汇总的请求信息，不是一组真正发送给服务器的 HTTP Header。

常见字段包括：

```text
Request URL
Request Method
Status Code
Remote Address
Referrer Policy
```

### 1. Request URL

示例：

```text
https://aihome-qa.rmall-solution.com/admin/ai/assistant/stream
```

完整 URL 可能由以下部分组成：

```text
https://example.com:8443/users?page=2&size=10#profile
│       │           │     │                   │
协议    域名         端口  路径和查询参数       锚点
```

| 部分 | 示例 | 作用 |
| --- | --- | --- |
| 协议 | `https` | 规定通信方式。 |
| 域名 | `example.com` | 指定目标服务器。 |
| 端口 | `8443` | 指定服务器监听端口。 |
| 路径 | `/users` | 指定接口或资源。 |
| 查询参数 | `?page=2&size=10` | 携带查询条件。 |
| 锚点 | `#profile` | 只用于浏览器页面内部定位，不会发送给服务器。 |

常见协议及默认端口：

| 协议 | 用途 | 默认端口 |
| --- | --- | ---: |
| `http` | 未加密 HTTP | `80` |
| `https` | 加密 HTTP | `443` |
| `ws` | WebSocket | `80` |
| `wss` | 加密 WebSocket | `443` |

示例接口使用 HTTPS 且没有显式写端口，因此默认连接 `443`。

### 2. Request Method

`Request Method` 表示客户端希望对目标资源执行什么操作。

常见方法：

| 方法 | 常见含义 | 通常是否有请求体 | 示例 |
| --- | --- | --- | --- |
| `GET` | 查询资源 | 通常没有 | 查询用户列表。 |
| `POST` | 创建资源或提交操作 | 通常有 | 新增用户、发送 AI 消息。 |
| `PUT` | 完整更新或替换资源 | 通常有 | 完整修改用户信息。 |
| `PATCH` | 局部更新资源 | 通常有 | 只修改用户状态。 |
| `DELETE` | 删除资源 | 通常没有 | 删除用户。 |
| `OPTIONS` | 查询服务器支持能力 | 通常没有 | CORS 跨域预检。 |
| `HEAD` | 只获取响应头 | 没有 | 检查文件是否存在或是否变化。 |
| `CONNECT` | 建立网络隧道 | 特殊用途 | HTTPS 代理。 |
| `TRACE` | 回显请求链路 | 通常没有 | 网络诊断，生产环境通常禁用。 |

#### `GET`

用于读取数据：

```http
GET /api/users?page=1&size=20
```

特点：

- 参数通常放在 URL 查询参数中；
- 容易被浏览器、代理和 CDN 缓存；
- URL 可以直接复制和访问；
- 按 HTTP 语义不应修改服务端数据；
- 不适合在 URL 中携带密码、Token 等敏感信息；
- URL 长度有限，不适合传输大量数据。

#### `POST`

用于创建资源或提交一项操作：

```http
POST /api/users
Content-Type: application/json

{
  "name": "张三"
}
```

AI 流式对话也常使用 `POST`，因为请求体需要携带用户问题、会话 ID、上下文、模型参数和附件信息。

#### `PUT` 与 `PATCH`

`PUT` 通常表示完整更新：

```http
PUT /api/users/1001
Content-Type: application/json

{
  "name": "张三",
  "phone": "13800000000",
  "enabled": true
}
```

`PATCH` 通常表示局部更新：

```http
PATCH /api/users/1001
Content-Type: application/json

{
  "enabled": false
}
```

具体语义仍以项目的接口约定为准。

#### `OPTIONS`

跨域请求满足特定条件时，浏览器会先自动发送 `OPTIONS` 预检请求，询问服务器：

- 是否允许当前网页来源；
- 是否允许 `POST`、`PUT` 等方法；
- 是否允许 `Authorization`、`Content-Type` 等请求头；
- 是否允许携带 Cookie。

预检通过后，浏览器才发送真正的业务请求。

#### 安全方法与幂等方法

“安全”表示按约定只读取信息，不修改服务器状态。常见安全方法有：

- `GET`；
- `HEAD`；
- `OPTIONS`。

“幂等”表示相同请求执行一次或多次，服务器最终状态应当相同。

| 方法 | 通常是否幂等 |
| --- | --- |
| `GET` | 是 |
| `PUT` | 是 |
| `DELETE` | 是 |
| `HEAD` | 是 |
| `OPTIONS` | 是 |
| `POST` | 通常不是 |
| `PATCH` | 不一定 |

例如，同一笔新增订单的 `POST` 请求重复发送，可能生成两张订单，因此通常不幂等。对同一个用户重复执行 `DELETE`，最终状态都是该用户不存在，因此通常视为幂等。

### 3. Status Code

状态码表示服务器对 HTTP 请求的处理结果，由三位数字组成。

| 范围 | 分类 | 含义 |
| --- | --- | --- |
| `1xx` | 信息响应 | 请求已收到，继续处理。 |
| `2xx` | 成功 | 请求已成功处理或接收。 |
| `3xx` | 重定向 | 需要跳转或使用缓存。 |
| `4xx` | 客户端错误 | 请求参数、身份、权限或访问方式有问题。 |
| `5xx` | 服务端错误 | 服务器、网关或上游服务处理失败。 |

#### `1xx`：信息响应

| 状态码 | 名称 | 含义 |
| --- | --- | --- |
| `100` | Continue | 请求头已收到，客户端可以继续上传请求体。 |
| `101` | Switching Protocols | 同意切换协议，WebSocket 握手时常见。 |
| `103` | Early Hints | 提前提示浏览器预加载资源。 |

#### `2xx`：成功

| 状态码 | 名称 | 常见场景 |
| --- | --- | --- |
| `200` | OK | 查询、更新或普通接口调用成功。 |
| `201` | Created | 成功创建新资源。 |
| `202` | Accepted | 已接收任务，但后台尚未完成。 |
| `204` | No Content | 操作成功，但没有响应正文。 |
| `206` | Partial Content | 返回文件或视频的一部分。 |

`200 OK` 只表示 HTTP 层成功，不一定表示业务成功。例如：

```http
HTTP/2 200 OK
Content-Type: application/json
```

```json
{
  "code": 50001,
  "message": "用户中心服务不可用",
  "data": null
}
```

这里 HTTP 请求成功到达并得到响应，但业务处理失败。`200` 是 HTTP 状态码，`50001` 是项目自定义业务码。

`201 Created` 适合创建资源，响应可能同时返回新资源地址：

```http
HTTP/2 201 Created
Location: /api/users/1001
```

`202 Accepted` 常用于报表生成、视频转码、批量导入和 AI 异步任务。它表示任务已接收，不表示任务已执行完成。

`204 No Content` 常用于删除成功。前端收到 `204` 后不应继续调用 `response.json()`，因为没有响应正文。

#### `3xx`：重定向与缓存

| 状态码 | 名称 | 常见场景 |
| --- | --- | --- |
| `301` | Moved Permanently | 永久重定向。 |
| `302` | Found | 临时重定向。 |
| `303` | See Other | 提交后跳转到另一个 GET 地址。 |
| `304` | Not Modified | 资源未变化，使用本地缓存。 |
| `307` | Temporary Redirect | 临时重定向，并保留原请求方法。 |
| `308` | Permanent Redirect | 永久重定向，并保留原请求方法。 |

服务器通过 `Location` 告诉浏览器跳转地址：

```http
HTTP/2 301 Moved Permanently
Location: https://example.com
```

`304 Not Modified` 不是错误。浏览器可以携带 `If-None-Match` 或 `If-Modified-Since` 询问资源是否变化，服务器确认没有变化后返回 `304`，浏览器直接使用本地缓存。

`302` 在部分客户端中可能把原来的 `POST` 改成 `GET`；`307` 明确要求保留原请求方法和请求体。

#### `4xx`：客户端请求问题

| 状态码 | 名称 | 常见原因 |
| --- | --- | --- |
| `400` | Bad Request | 参数格式错误、JSON 解析失败。 |
| `401` | Unauthorized | 未登录、Token 无效或过期。 |
| `403` | Forbidden | 已识别用户，但没有权限。 |
| `404` | Not Found | 接口、路由或资源不存在。 |
| `405` | Method Not Allowed | 接口存在，但请求方法不支持。 |
| `406` | Not Acceptable | 无法返回客户端要求的格式。 |
| `408` | Request Timeout | 客户端发送请求超时。 |
| `409` | Conflict | 用户名重复、状态或版本冲突。 |
| `410` | Gone | 资源已永久删除。 |
| `413` | Content Too Large | 请求体或上传文件过大。 |
| `415` | Unsupported Media Type | 请求正文格式不受支持。 |
| `422` | Unprocessable Content | 格式正确，但业务校验失败。 |
| `429` | Too Many Requests | 请求过于频繁，被限流。 |

可以这样区分 `401` 和 `403`：

```text
401：不知道你是谁，或者登录状态已失效。
403：知道你是谁，但你没有访问权限。
```

`404` 可能由业务服务、Kong、Nginx 或 CDN 返回，需要结合响应内容判断是“业务数据不存在”还是“路由没匹配到”。

`415` 常见于后端要求 `application/json`，前端却发送 `text/plain`。`413` 可能是网关或 CDN 在请求到达业务服务前就因大小限制而返回。

`429` 响应可能带有：

```http
Retry-After: 60
```

表示建议 60 秒后重试。

#### `5xx`：服务器或网关问题

| 状态码 | 名称 | 常见原因 |
| --- | --- | --- |
| `500` | Internal Server Error | 后端代码、数据库或未捕获异常。 |
| `501` | Not Implemented | 服务器未实现该功能。 |
| `502` | Bad Gateway | 网关未从后端获得有效响应。 |
| `503` | Service Unavailable | 服务不可用或没有健康实例。 |
| `504` | Gateway Timeout | 网关等待后端响应超时。 |
| `507` | Insufficient Storage | 服务器存储空间不足。 |

可以这样区分常见网关错误：

```text
502：连接上游失败，或上游返回了无效响应。
503：当前没有可用服务，或服务正在维护、过载。
504：请求已经发给上游，但等待响应超时。
```

如果网关通过 Nacos 找不到健康服务实例，可能返回 `503`。

#### SSE 接口中的 `200`

流式接口返回：

```http
HTTP/2 200 OK
Content-Type: text/event-stream
```

表示：

- HTTP 连接成功；
- SSE 响应流已经建立；
- 服务端开始发送事件。

它不保证之后的 AI 处理始终成功。响应头发送后，服务器不能再把状态码从 `200` 改成 `500`，因此后续错误可能作为流内事件返回：

```text
event: error
data: {"code":"MODEL_ERROR","message":"模型服务暂不可用"}
```

### 4. Remote Address

示例：

```text
103.220.64.100:443
```

表示浏览器实际建立连接的远程 IP 和端口，也可能显示 IPv6：

```text
[2402:4e00:1200:ed00::1]:443
```

`Remote Address` 不一定是业务服务的真实地址。如果响应头中还有：

```http
Server: TencentEdgeOne
Via: kong/2.5.1
```

请求链路可能是：

```text
浏览器
  ↓
Tencent EdgeOne 边缘节点
  ↓
Kong API 网关
  ↓
实际 AI 后端服务
```

因此，URL 中的域名、`Remote Address` 和 Nacos 中的后端内网 IP 可以完全不同。

### 5. Referrer Policy

示例：

```text
no-referrer
```

它决定浏览器是否发送 `Referer` 请求头，以及发送多少来源信息。

常见策略：

| 策略 | 含义 |
| --- | --- |
| `no-referrer` | 完全不发送来源地址。 |
| `origin` | 只发送协议、域名和端口。 |
| `same-origin` | 只有同源请求才发送。 |
| `strict-origin` | 只发送来源站点，HTTPS 降级到 HTTP 时不发送。 |
| `strict-origin-when-cross-origin` | 同源发送完整地址，跨域只发送站点信息。 |
| `no-referrer-when-downgrade` | 通常发送完整来源，HTTPS 降级到 HTTP 时不发送。 |
| `unsafe-url` | 总是发送完整地址，可能泄漏查询参数，不推荐。 |

关于 Referer 的更多说明见 [HTTP Referer（来源地址）](./http-referer.md)。

## 三、Response Headers：服务器返回给浏览器的信息

响应头告诉浏览器：

- 响应正文是什么格式；
- 是否允许跨域读取；
- 是否允许携带 Cookie；
- 是否允许缓存；
- 内容是否压缩；
- 请求经过了哪些代理或网关。

响应头可能由业务服务、Kong、Nginx、CDN 或 EdgeOne 共同添加，不一定全部来自业务代码。

### 1. `Access-Control-Allow-Origin`

```http
Access-Control-Allow-Origin: https://aihome-qa.rmall-solution.com
```

表示允许该网页来源跨域读取响应。

一个 Origin 由以下三部分组成：

```text
协议 + 域名 + 端口
```

下面是不同的 Origin：

```text
http://example.com
https://example.com
https://example.com:8443
https://api.example.com
```

也可以使用：

```http
Access-Control-Allow-Origin: *
```

表示允许任意来源，但使用 Cookie 等凭证时通常必须返回明确来源，不能简单使用 `*`。

### 2. `Access-Control-Allow-Credentials`

```http
Access-Control-Allow-Credentials: true
```

表示跨域请求允许携带 Cookie、HTTP 认证信息或客户端证书。

前端也要启用凭证：

```ts
fetch(url, {
  credentials: 'include',
})
```

Axios 中是：

```ts
axios.get(url, {
  withCredentials: true,
})
```

服务端允许凭证而前端没有启用时，跨域 Cookie 仍不会按预期发送。

### 3. `Cache-Control`

```http
Cache-Control: no-cache
```

控制浏览器、代理和 CDN 如何缓存响应。

| 值 | 含义 |
| --- | --- |
| `no-cache` | 可以保存，但再次使用前必须向服务器验证。 |
| `no-store` | 不允许保存响应内容。 |
| `max-age=3600` | 可以缓存 3600 秒。 |
| `public` | 浏览器和共享缓存都可以缓存。 |
| `private` | 只允许当前用户的浏览器缓存。 |
| `must-revalidate` | 缓存过期后必须向服务器验证。 |
| `immutable` | 有效期内资源不会变化。 |

`no-cache` 并不等于完全不缓存；真正明确禁止存储的是 `no-store`。

SSE 接口通常避免缓存和代理缓冲，否则可能导致流式内容不能及时到达前端。

### 4. `Content-Type`

```http
Content-Type: text/event-stream
```

表示服务器实际返回的是 SSE 事件流。完整写法还可能包括字符编码：

```http
Content-Type: text/event-stream; charset=utf-8
```

常见类型如下。

#### 文本和网页

| Content-Type | 用途 |
| --- | --- |
| `text/plain` | 普通文本。 |
| `text/html` | HTML 页面。 |
| `text/css` | CSS 文件。 |
| `text/csv` | CSV 表格。 |
| `text/event-stream` | SSE 流式事件。 |

#### API 数据

| Content-Type | 用途 |
| --- | --- |
| `application/json` | JSON 接口数据。 |
| `application/xml` | XML 数据。 |
| `application/problem+json` | 标准化 JSON 错误信息。 |
| `application/graphql-response+json` | GraphQL JSON 响应。 |

#### 表单和上传

| Content-Type | 用途 |
| --- | --- |
| `application/x-www-form-urlencoded` | 普通键值对表单。 |
| `multipart/form-data` | 文件与表单字段混合上传。 |

使用 `FormData` 上传文件时，不要手动设置 `multipart/form-data`，浏览器会自动生成包含 `boundary` 的正确响应头值。

#### 文件

| Content-Type | 用途 |
| --- | --- |
| `application/pdf` | PDF 文件。 |
| `application/zip` | ZIP 压缩包。 |
| `application/octet-stream` | 通用二进制或下载文件。 |
| `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Excel `.xlsx`。 |
| `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Word `.docx`。 |

#### 图片和音视频

| Content-Type | 用途 |
| --- | --- |
| `image/png` | PNG 图片。 |
| `image/jpeg` | JPEG 图片。 |
| `image/webp` | WebP 图片。 |
| `image/svg+xml` | SVG 图片。 |
| `audio/mpeg` | MP3 音频。 |
| `audio/wav` | WAV 音频。 |
| `video/mp4` | MP4 视频。 |
| `application/vnd.apple.mpegurl` | HLS 播放列表。 |

### 5. `Date`

```http
Date: Tue, 13 Oct 2026 07:55:35 GMT
```

表示生成响应的时间。HTTP 日期通常使用 GMT，中国标准时间需要加 8 小时。

这个字段可能由业务服务器、网关或 CDN 添加。

### 6. `Vary`

```http
Vary: Origin
```

表示缓存响应时，必须根据 `Origin` 区分不同版本，避免不同来源错误复用同一份 CORS 响应。

其他常见值：

```http
Vary: Accept-Encoding
Vary: Accept-Language
Vary: Origin, Accept-Encoding
```

分别表示需要区分压缩算法、语言和来源。

### 7. `Server`

```http
Server: TencentEdgeOne
```

表示最后向浏览器返回响应的服务器或代理软件。其他常见值包括：

```text
nginx
Apache
cloudflare
envoy
```

它不一定代表实际运行业务代码的服务器。出于安全考虑，有些系统会隐藏该字段。

### 8. `Via`

```http
Via: kong/2.5.1
```

表示请求经过 Kong API 网关。网关通常负责：

- 路由转发；
- 登录认证；
- 权限校验；
- 限流；
- 跨域；
- 日志；
- 负载均衡；
- 服务发现。

### 9. `Eo-Cache-Status`

```http
Eo-Cache-Status: MISS
```

这是腾讯 EdgeOne 的自定义字段，不是通用 HTTP 标准字段。

| 状态 | 含义 |
| --- | --- |
| `HIT` | 命中边缘缓存，未访问源站。 |
| `MISS` | 未命中缓存，继续访问源站。 |
| `BYPASS` | 根据规则跳过缓存。 |
| `EXPIRED` | 原缓存已经过期。 |
| `REVALIDATED` | 向源站验证后继续使用缓存。 |

SSE 接口一般不应被普通 CDN 缓存，出现 `MISS` 或 `BYPASS` 通常是合理的。

### 10. `Eo-Log-Uuid`

```http
Eo-Log-Uuid: 7491423821294464248
```

这是 EdgeOne 生成的请求追踪标识，可用于查询边缘节点日志。类似字段还可能叫：

```text
X-Request-Id
X-Trace-Id
Trace-Id
Request-Id
```

排查线上问题时，应优先保留这类 ID、请求时间和接口地址。

### 11. `X-Kong-Proxy-Latency`

```http
X-Kong-Proxy-Latency: 3
```

表示 Kong 自身处理请求大约用了 3 毫秒，可能包括路由匹配、认证、限流和请求头改写。

### 12. `X-Kong-Upstream-Latency`

```http
X-Kong-Upstream-Latency: 84
```

表示 Kong 请求实际后端服务并等待其开始响应大约用了 84 毫秒。

对 SSE 接口而言，这通常接近后端返回响应头或首个片段所需时间，不是 AI 完整回答的总耗时。

### 13. 其他常见响应头

| 响应头 | 作用 |
| --- | --- |
| `Content-Length` | 响应正文的字节长度。 |
| `Content-Encoding` | 响应使用的压缩算法，如 `gzip`、`br`。 |
| `Content-Disposition` | 控制文件在线预览或下载，以及下载文件名。 |
| `Location` | 重定向地址或新建资源地址。 |
| `Set-Cookie` | 要求浏览器保存 Cookie。 |
| `ETag` | 资源版本标识，用于协商缓存。 |
| `Last-Modified` | 资源最后修改时间。 |
| `Retry-After` | 建议客户端等待多久后重试。 |
| `WWW-Authenticate` | 告诉客户端需要哪种身份认证。 |
| `Access-Control-Expose-Headers` | 允许前端 JavaScript 读取额外响应头。 |

## 四、Request Headers：浏览器发送给服务器的信息

请求头告诉服务器：

- 要访问哪个域名、路径；
- 使用什么请求方法；
- 请求正文是什么格式；
- 希望收到什么格式；
- 当前用户的身份凭证；
- 请求来自哪个页面；
- 浏览器支持哪些语言和压缩算法。

### 1. HTTP/2 和 HTTP/3 伪头字段

截图中以冒号开头的是伪头字段，不是普通自定义 Header。

#### `:authority`

```http
:authority: aihome-qa.rmall-solution.com
```

相当于 HTTP/1.1 中的：

```http
Host: aihome-qa.rmall-solution.com
```

服务器或网关根据它判断目标域名和路由。

#### `:method`

```http
:method: POST
```

表示请求方法，与 General 中的 `Request Method` 相同。General 是 Chrome 汇总展示，`:method` 是协议中的字段。

#### `:path`

```http
:path: /admin/ai/assistant/stream
```

表示请求路径和查询参数，不包含协议和域名。例如：

```text
https://example.com/api/users?page=1
```

会被拆为：

```http
:scheme: https
:authority: example.com
:path: /api/users?page=1
```

#### `:scheme`

```http
:scheme: https
```

表示使用 HTTPS。HTTPS 提供传输加密和服务器证书校验，但不能自动防止 XSS、SQL 注入、越权和 Token 泄漏。

### 2. `Accept`

```http
Accept: text/event-stream
```

表示客户端希望服务器返回 SSE 事件流。

常见值：

| Accept | 希望收到的内容 |
| --- | --- |
| `application/json` | JSON 数据。 |
| `text/html` | HTML 页面。 |
| `text/plain` | 普通文本。 |
| `text/event-stream` | SSE 事件流。 |
| `application/pdf` | PDF 文件。 |
| `image/avif,image/webp,image/*` | 浏览器支持的图片。 |
| `*/*` | 任意类型都可以。 |

可以使用 `q` 指定优先级：

```http
Accept: application/json, text/plain;q=0.8, */*;q=0.5
```

表示最希望收到 JSON，其次是普通文本，最后才接受其他格式。`q` 默认是 `1`，数值越大优先级越高。

### 3. `Accept-Encoding`

```http
Accept-Encoding: gzip, deflate, br, zstd
```

表示浏览器支持的响应压缩算法。

| 算法 | 特点 |
| --- | --- |
| `gzip` | 兼容性好，最常见。 |
| `deflate` | 较早的压缩格式。 |
| `br` | Brotli，压缩率通常高于 gzip。 |
| `zstd` | Zstandard，速度和压缩率较均衡。 |

服务器选择压缩算法后，会在响应中返回：

```http
Content-Encoding: gzip
```

浏览器会自动解压。SSE 需要防止压缩器或代理为凑数据块而缓冲内容，导致流式输出延迟。

### 4. `Accept-Language`

```http
Accept-Language: zh-CN,zh;q=0.9
```

表示首选中国大陆简体中文，其次接受一般中文。

后端可以据此决定错误提示、页面、日期等内容使用哪种语言，但只有项目实现了国际化逻辑时才会生效。

### 5. `Content-Type`

请求中的 `Content-Type` 表示前端发送给服务器的请求正文格式。

JSON 请求：

```http
Content-Type: application/json
```

```json
{
  "message": "你好"
}
```

普通表单：

```http
Content-Type: application/x-www-form-urlencoded
```

```text
username=zhangsan&password=123456
```

文件上传：

```http
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
```

纯文本：

```http
Content-Type: text/plain; charset=utf-8
```

二进制数据：

```http
Content-Type: application/octet-stream
```

### 6. `Authorization`

```http
Authorization: Bearer eyJhbGciOiJIUzI1Ni...
```

常用于携带访问令牌。服务器据此判断用户身份、登录状态和权限。

不要在文档、聊天记录或截图中公开完整 Token。拿到有效 Token 的人可能可以冒充当前用户。

### 7. `Cookie`

```http
Cookie: SESSION=abc123; tenantId=1001
```

Cookie 常用于保存 Session ID、登录状态、CSRF Token、租户和灰度标识。

如果 Cookie 设置了 `HttpOnly`，前端 JavaScript 不能通过 `document.cookie` 读取，但浏览器仍可以按规则自动发送。

### 8. `Origin`

```http
Origin: https://aihome-qa.rmall-solution.com
```

表示请求来自哪个网页 Origin，服务器用它进行 CORS 判断。它通常只有协议、域名和端口，不包含具体页面路径。

### 9. `Referer`

```http
Referer: https://example.com/orders/detail
```

表示请求由哪个页面发起，是否发送及发送多少由 Referrer Policy 决定。

| 字段 | 通常包含的内容 |
| --- | --- |
| `Origin` | 协议、域名和端口。 |
| `Referer` | 可能包含具体页面路径和查询参数。 |

### 10. `User-Agent`

```http
User-Agent: Mozilla/5.0 ...
```

描述浏览器、版本、操作系统和设备类型。它可用于兼容处理和统计，但可以被伪造，不应作为安全认证依据。

### 11. `Content-Length`

```http
Content-Length: 256
```

表示请求正文的字节长度。HTTP/2、流式请求或分块传输中不一定总能看到该字段。

### 12. `Sec-Fetch-*`

浏览器可能发送：

```http
Sec-Fetch-Site: same-origin
Sec-Fetch-Mode: cors
Sec-Fetch-Dest: empty
```

| 字段 | 作用 |
| --- | --- |
| `Sec-Fetch-Site` | 表示同源、同站还是跨站请求。 |
| `Sec-Fetch-Mode` | 表示请求模式，如 `cors`、`navigate`。 |
| `Sec-Fetch-Dest` | 表示目标类型，如图片、脚本、文档。 |

服务器可以使用这些字段辅助判断异常跨站请求和防御 CSRF。

### 13. 请求追踪字段

常见字段：

```http
X-Request-Id: 6dd6e3de-...
```

或者：

```http
traceparent: 00-4bf92f3577b34da6...-00f067aa0ba902b7-01
```

它们用于把同一次请求经过的 EdgeOne、Kong、业务服务、Redis、MySQL 等日志串联起来。

### 14. 其他常见请求头

| 请求头 | 作用 |
| --- | --- |
| `If-None-Match` | 携带 ETag，询问资源是否变化。 |
| `If-Modified-Since` | 询问资源在指定时间后是否修改。 |
| `Range` | 只请求文件的指定字节范围。 |
| `X-CSRF-Token` | 携带 CSRF 防护令牌。 |
| `Idempotency-Key` | 防止支付、下单等 POST 请求重复执行。 |
| `Access-Control-Request-Method` | CORS 预检中声明之后要使用的方法。 |
| `Access-Control-Request-Headers` | CORS 预检中声明之后要发送的请求头。 |

## 五、`Accept` 与 `Content-Type` 的区别

这是最容易混淆的两个字段。

假设前端发送 JSON，并希望收到 SSE：

```http
POST /admin/ai/assistant/stream
Content-Type: application/json
Accept: text/event-stream
```

请求体：

```json
{
  "message": "请介绍一下 Nacos"
}
```

服务器响应：

```http
HTTP/2 200 OK
Content-Type: text/event-stream
```

响应体：

```text
data: {"content":"Nacos"}

data: {"content":"可以理解为"}

data: {"content":"微服务的通讯录"}
```

对应关系：

| 所在位置 | Header | 含义 |
| --- | --- | --- |
| Request Headers | `Content-Type` | 客户端发送的请求正文格式。 |
| Request Headers | `Accept` | 客户端希望收到的响应格式。 |
| Response Headers | `Content-Type` | 服务器实际返回的响应正文格式。 |

文件上传接口可能是：

```http
Content-Type: multipart/form-data
Accept: application/json
```

含义是“我上传的是表单和文件，希望服务器处理后返回 JSON”。

## 六、SSE、普通 JSON 与 WebSocket

### 1. 普通 JSON 接口

```text
请求一次
→ 服务器处理完成
→ 一次性返回完整 JSON
→ 连接结束
```

适合列表、详情、新增、修改、删除等普通业务接口。

### 2. SSE

```text
请求一次
→ 建立长连接
→ 服务器持续向浏览器单向推送事件
```

响应类型：

```http
Content-Type: text/event-stream
```

标准事件可能包含：

```text
event: message
id: 123
retry: 3000
data: {"content":"你好"}
```

| 字段 | 作用 |
| --- | --- |
| `data` | 事件数据。 |
| `event` | 事件类型。 |
| `id` | 事件编号，可辅助断线恢复。 |
| `retry` | 建议客户端断线后等待多久重连。 |

浏览器原生 `EventSource` 通常使用 `GET`。AI 接口为了提交复杂 JSON，常用 `fetch + POST`，再通过 `response.body` 读取流。

### 3. WebSocket

WebSocket 建立后允许浏览器和服务器双向主动发送消息，适合聊天室、实时协作和游戏等双向高频通信。

| 对比项 | 普通 HTTP JSON | SSE | WebSocket |
| --- | --- | --- | --- |
| 返回方式 | 一次性 | 服务端持续推送 | 双向持续通信 |
| 常见类型 | `application/json` | `text/event-stream` | 协议升级后使用帧 |
| 典型用途 | CRUD 接口 | AI 流式回答、通知 | 聊天、协作、游戏 |
| 实现复杂度 | 低 | 中 | 较高 |

## 七、结合示例接口还原调用链

根据以下响应头：

```http
Server: TencentEdgeOne
Via: kong/2.5.1
Eo-Cache-Status: MISS
X-Kong-Proxy-Latency: 3
X-Kong-Upstream-Latency: 84
```

可以推测链路为：

```text
前端页面
  │
  │ POST /admin/ai/assistant/stream
  │ Content-Type: application/json
  │ Accept: text/event-stream
  ▼
Tencent EdgeOne
  │ 未命中边缘缓存，继续访问源站
  ▼
Kong API 网关
  │ 网关自身处理约 3ms
  │ 等待上游开始响应约 84ms
  ▼
AI 后端服务
  │
  │ 200 OK
  │ Content-Type: text/event-stream
  ▼
持续返回 AI 内容片段
  │
  ▼
前端边接收边更新页面
```

需要注意：

1. `Remote Address` 多半是 EdgeOne 节点，不一定是业务服务器；
2. `200 OK` 表示流已经建立，不保证之后每个业务事件都成功；
3. `X-Kong-Upstream-Latency` 通常不是 AI 完整回答耗时；
4. SSE 内部仍可能通过 `event: error` 返回错误；
5. `Access-Control-Allow-Origin` 和 `Access-Control-Allow-Credentials` 决定浏览器是否允许前端跨域读取带凭证的响应。

## 八、接口排查顺序

### 1. 先看 General

检查：

- URL 是否正确；
- 方法是否正确；
- 状态码属于 `2xx`、`4xx` 还是 `5xx`；
- `Remote Address` 是否符合预期；
- 请求是否被重定向。

### 2. 再看 Request Headers 和 Payload

检查：

- `Content-Type` 是否符合后端要求；
- `Authorization` 或 Cookie 是否存在、是否过期；
- `Origin` 是否正确；
- 请求参数名、类型和嵌套结构是否正确；
- 是否把 JSON 错误地发送成普通文本或表单。

### 3. 查看 Response Headers

检查：

- `Content-Type` 是否符合预期；
- CORS 响应头是否允许当前来源；
- 响应来自业务服务、网关还是 CDN；
- 是否有请求追踪 ID；
- 是否发生缓存、压缩或重定向。

### 4. 查看 Response 或 EventStream

检查：

- HTTP 成功时业务码是否仍然失败；
- 错误信息来自业务服务还是网关；
- SSE 是否持续收到事件；
- SSE 是否在流内返回 `error` 事件；
- 数据是否符合前端约定格式。

### 5. 查看 Timing

重点关注：

- 排队时间是否过长；
- DNS、TCP 或 SSL 是否耗时；
- `Waiting for server response` 是否过长；
- 普通请求下载是否过慢；
- SSE 长时间保持连接是否属于正常业务行为。

## 九、快速记忆

```text
General
= Chrome 汇总的请求概况。

Request Headers
= 浏览器告诉服务器“我是谁、我要什么、我发送的是什么”。

Request Payload
= 浏览器真正提交的业务参数。

Status Code
= 服务器从 HTTP 层告诉浏览器处理结果。

Response Headers
= 服务器告诉浏览器“我返回的是什么、能否缓存、能否跨域读取”。

Response / EventStream
= 服务器真正返回的业务内容。
```

最重要的三个格式字段：

```text
Request Content-Type
= 我发送给服务器的正文格式。

Request Accept
= 我希望服务器返回的格式。

Response Content-Type
= 服务器实际返回的正文格式。
```
