# 前端怎么接收服务器数据：JSON、SSE、WebSocket 与文件

本文重点解释普通响应、流式响应和长连接的差别；Fetch 与 Axios 的工程化选择见 [Fetch 与 Axios：前端调用接口的选择](./fetch-vs-axios.md)。

先把三个容易混在一起的概念分开：

- `fetch`、Axios、`EventSource`、`WebSocket`：前端使用的工具或 API。
- 普通 HTTP、SSE、WebSocket：通信方式。
- JSON、文本、Blob、ArrayBuffer：一次数据的内容格式。

所以“Axios 和 JSON”不能直接并列比较：Axios 是取数据的工具，JSON 是拿到的数据格式。一次普通 HTTP 请求可以返回 JSON，也可以返回文本、图片或 Excel。

最常见的通信方式可以先记成：

- 普通 HTTP：问一次，完整回答一次。
- SSE：前端先连上，服务器以后可以沿着这条连接单向推送事件。
- WebSocket：前端先连上，之后双方都可以沿着同一条连接发消息。

## 1. fetch：浏览器自带的发请求

`fetch` 是浏览器给的函数，不用安装。你告诉它网址和方式，它去发 HTTP，返回一个 Promise。服务器返回响应头后，`fetch()` 的 Promise 就会完成；下面的 `res.json()` 还会继续等待并读取完整正文。

```js
const res = await fetch('https://example.com/api/list', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    Authorization: 'Bearer token',
  },
  body: JSON.stringify({ pageNum: 1, pageSize: 20 }),
});

const json = await res.json(); // 自己把正文解析成对象
console.log(json);
```

普通 JSON 场景的特点：

- GET、POST、PUT、DELETE 都行，body 想带就带
- `await fetch()` 先拿到 `Response`，`await res.json()` 读取完整 JSON 后才继续
- token、超时、统一判断 `code` 都要自己写
- HTTP 状态码 404、500 时，`fetch` **不会自动当失败**，还要自己看 `res.ok`

适合：偶尔发一两个请求，或者不想多装库。

---

## 2. axios：把 fetch 那种事打包好的库

axios 是 npm 包装，本质还是发 HTTP，等完整响应。差别是常用功能已经帮你做好了。

先安装：`npm install axios`。

```js
import axios from 'axios';

const { data } = await axios.post('https://example.com/api/list', {
  pageNum: 1,
  pageSize: 20,
});
console.log(data); // 已经是解析好的对象，不用再 json()
```

也可以先做一个「预制实例」，后面每次请求自动带上域名、超时、token：

```js
const http = axios.create({
  baseURL: 'https://example.com',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// 发出去之前：统一加 token
http.interceptors.request.use((config) => {
  config.headers.Authorization = 'Bearer token';
  return config;
});

// 回来之后：统一看业务码
http.interceptors.response.use((response) => {
  if (response.data.code === 0) return response;
  return Promise.reject(new Error(response.data.message));
});

const { data } = await http.post('/api/list', { pageNum: 1 });
```

特点：

- 和 `fetch` 一样：一次请求，一次完整 JSON
- POST body 直接传对象，它帮你转成 JSON 字符串
- 超时、拦截器、错误处理比手写 `fetch` 方便
- 返回结构多包一层：真正的业务数据一般在 `response.data` 里

适合：后台页面里大量列表、提交、删除这种普通接口。

---

## 3. EventSource：接服务器「一句句推」的流

有的接口不是一次给完，比如 AI 对话：模型边想边吐字。服务器会保持连接，按帧往下写：

```text
event: message.delta
data: {"delta":"你好"}

event: message.delta
data: {"delta":"世界"}
```

`EventSource` 就是浏览器专门接这种流的 API。连接开着，每来一帧就触发一次回调。

```js
const es = new EventSource('https://example.com/api/stream');

es.onopen = () => {
  console.log('连接通了');
};

es.addEventListener('message.delta', (event) => {
  const data = JSON.parse(event.data);
  console.log('来了一小段：', data.delta);
});

es.onerror = () => {
  console.log('连接出错或断开');
};

es.close(); // 不用了就关掉
```

特点：

- 不是等整包，而是一帧帧拿
- `onopen`：连接刚建立（还不一定有业务数据）
- `addEventListener('事件名', ...)`：某种帧来了就叫你一次
- 原生 `EventSource` **只能 GET**，不能自己选 POST，也不能在请求里塞 JSON body
- 所以「对话内容必须放 POST body」时，原生这个 API 不够用，要用能 POST 的实现（例如 `extended-eventsource`，内部其实用 `fetch` 去连，再按 SSE 格式切帧）

适合：聊天打字效果、进度推送、日志实时刷。

---

## 4. WebSocket：双方使用同一条连接发消息

WebSocket 也是浏览器原生 API。前端先发起连接，握手成功后触发 `open`；之后前端和服务器都可以主动发送消息。

```js
const socket = new WebSocket('wss://example.com/chat');

socket.onopen = () => {
  socket.send(JSON.stringify({ text: '你好' }));
};

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到消息', message);
};

socket.onclose = () => {
  console.log('连接已结束');
};
```

`event.data` 不一定是 JSON。它可能是普通字符串，也可能是二进制；双方需要提前约定每条消息怎么编码。

云 Mall 小程序直播没有直接写 `new WebSocket()`，而是使用腾讯 Chat SDK。业务代码监听：

```ts
chat.on(TencentCloudChat.EVENT.MESSAGE_RECEIVED, handleMessageReceived)
```

腾讯 SDK 负责底层实时连接、心跳和重连，业务代码只接收 SDK 转换好的消息事件。对应源码：

```text
frontend-group/vibe-miniprogram-fe/src/subpackages/live/hooks/useImChat.ts
```

这里还有一个容易混淆的地方：`/app/live/im/user-sig` 是普通 JSON 接口，只负责返回登录凭证。拿到凭证后调用 `chat.login()`，才进入 SDK 管理的实时通信阶段。

## 5. 除了 JSON，普通 HTTP 还能返回什么

### 5.1 JSON：业务接口最常见

```http
Content-Type: application/json
```

适合列表、详情、保存结果、登录凭证等结构化数据。前端通常得到对象：

```js
const data = await response.json();
```

小程序项目的绝大多数 `request<T>()` 接口属于这一类，例如直播 IM 凭证接口：

```text
frontend-group/vibe-miniprogram-fe/src/services/live/index.ts
```

### 5.2 普通文本或 HTML

```http
Content-Type: text/plain
Content-Type: text/html
```

前端可以通过 `response.text()` 读取。文本适合简单内容、日志和 CSV；HTML 通常由浏览器直接显示。

项目的小程序请求封装还会检查腾讯云 WAF 返回的 HTML 拦截页。也就是说，前端原本期待 JSON，异常情况下却可能收到 HTML，不能看到 HTTP 200 就直接认定它一定是业务 JSON：

```text
frontend-group/vibe-miniprogram-fe/src/services/request.ts
```

### 5.3 Blob：把二进制当成一个整体

Blob 可以理解成浏览器中的“文件数据”。Excel、PDF、ZIP、图片下载经常使用：

```js
const response = await fetch('/api/export');
const blob = await response.blob();
const url = URL.createObjectURL(blob);
```

运营端很多导出接口显式设置了：

```ts
responseType: 'blob'
```

例如商品 Excel 导出：

```text
frontend-group/vibe-operation-fe/src/api/product/product.ts
```

Blob 仍然可以通过一次普通 HTTP 响应返回；它不是 SSE，也不是 WebSocket。

### 5.4 ArrayBuffer：直接处理原始字节

```js
const bytes = await response.arrayBuffer();
```

它比 Blob 更靠近原始内存字节，适合加解密、压缩包解析、音视频处理和自定义二进制协议。大部分普通业务页面不需要直接操作它。微信小程序的网络 API 也支持把响应声明为 `arraybuffer`。

### 5.5 无正文响应

服务器可以返回：

```http
HTTP/1.1 204 No Content
```

它表示操作成功，但没有响应正文。前端这时不能继续调用 `response.json()`，否则会因为正文为空而解析失败。删除、只执行动作不需要返回结果的接口可能这样设计。

### 5.6 浏览器直接消费的资源

图片、CSS、JavaScript、字体和音视频也都是 HTTP 响应，只是一般不由业务代码手动调用 `response.json()`：

```html
<img src="/cover.jpg">
<video src="/demo.mp4"></video>
```

浏览器根据 `Content-Type` 把响应交给图片解码器、视频播放器等模块处理。

## 6. FormData 是发送格式，不是接收方式

文件上传常见代码：

```js
const form = new FormData();
form.append('file', file);
await fetch('/api/upload', {
  method: 'POST',
  body: form,
});
```

这里 `multipart/form-data` 描述的是前端发给服务器的请求体。服务器处理完成后，仍然很可能返回普通 JSON：

```text
上传请求：multipart/form-data
上传响应：application/json
```

因此，讨论接口时要分别问：

1. 前端发送的正文是什么格式？
2. 服务器返回的正文是什么格式？
3. 连接是一次性结束，还是保持并继续传消息？

## 7. 这个项目里能看到的方式

- 普通 JSON：小程序的列表、详情、提交、登录凭证等绝大多数接口。
- SSE：运营端 AI 设备聊天使用 `extended-eventsource`；AI 报表还使用 `fetch + ReadableStream` 手动解析 SSE。
- WebSocket/实时长连接：小程序直播弹幕通过腾讯 Chat SDK 接收 `MESSAGE_RECEIVED` 事件。
- Blob：运营端商品、优惠券、门店、导购等 Excel 模板或报表下载。
- `multipart/form-data`：运营端图片、视频、Excel 等文件上传；这是请求格式。
- HTML 异常响应：小程序统一请求层识别腾讯云 WAF 拦截页。

对应源码入口：

```text
frontend-group/vibe-operation-fe/src/utils/deviceChatStream.ts
frontend-group/vibe-operation-fe/src/api/report/ai.ts
frontend-group/vibe-miniprogram-fe/src/subpackages/live/hooks/useImChat.ts
frontend-group/vibe-operation-fe/src/api/product/product.ts
frontend-group/vibe-miniprogram-fe/src/services/request.ts
```

## 8. 快速选择

- 查询列表、提交表单并等待结果：普通 HTTP + JSON。
- AI 边生成边显示、进度或日志持续通知：SSE。
- 聊天、弹幕、协同编辑，双方都要随时发消息：WebSocket 或封装它的 SDK。
- 下载 Excel、PDF、ZIP：普通 HTTP + Blob。
- 处理原始二进制字节：ArrayBuffer。
- 上传文件：请求体使用 FormData，响应通常仍是 JSON。

一句话记忆：

```text
JSON、文本、Blob：回答的内容是什么。
普通 HTTP、SSE、WebSocket：双方怎么持续通信。
fetch、Axios、EventSource、WebSocket API：前端用什么工具处理。
```
