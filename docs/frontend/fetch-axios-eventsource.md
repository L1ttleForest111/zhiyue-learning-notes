# fetch、axios、EventSource 是什么，有什么差别

这三样都是浏览器里「跟服务器说话」的方式，差别在：**要不要等整包结果**，以及 **谁帮你把脏活干了**。

- `fetch`：浏览器自带的发请求能力
- `axios`：基于请求封装的第三方库，用起来更省事
- `EventSource`：专门用来接服务器持续推过来的事件（SSE）

可以记成：

- `fetch` / `axios`：寄一封信，等回信整封拆开
- `EventSource`：打电话不挂，对方一句句说

---

## 1. fetch：浏览器自带的发请求

`fetch` 是浏览器给的函数，不用安装。你告诉它网址和方式，它去发 HTTP，回来一个 Promise。

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

特点：

- GET、POST、PUT、DELETE 都行，body 想带就带
- 等服务器把这次响应全部发完，`await` 才继续
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

## 4. 三者对照

| | fetch | axios | EventSource |
|---|---|---|---|
| 是什么 | 浏览器自带 | 第三方 HTTP 库 | 浏览器的 SSE 客户端 |
| 要不要安装 | 不用 | 要 `npm install axios` | 原生不用；要 POST 时才换扩展库 |
| HTTP 方法 | 都行 | 都行 | 原生只有 GET |
| 请求体 | 可以带 | 可以带 | 原生不能带 |
| 何时拿到数据 | 整份响应结束 | 整份响应结束 | 连接不关，来一帧拿一帧 |
| 典型写法 | `const json = await res.json()` | `const { data } = await axios.post(...)` | `es.addEventListener('xxx', ...)` |
| 典型场景 | 少量请求、自己封装 | 普通增删改查 | 流式对话、实时推送 |

`fetch` 和 `axios` 是一类：都是「普通 HTTP」。axios 是 fetch 的好用包装。

`EventSource` 是另一类：协议还是 HTTP，但响应类型是 `text/event-stream`，服务器会一直写、一直 flush。

---

## 5. 怎么选

- 只要完整结果（列表、保存、删除、撤回）→ `axios`（或 `fetch`）
- 要边生成边展示 → `EventSource`（或能 POST 的 SSE 库）
- 不要混：用 axios 去 `await` 一条 SSE，会等到流结束才给你一大段文本，中间的字出不来；用 EventSource 去拉一个普通 JSON 接口，也接不对，因为那边不是按帧推的

一句话：`fetch` 是原材料，`axios` 是做好的工具箱，`EventSource` 是听电话的听筒。
