# Fetch 与 Axios：前端调用接口的选择

## 结论先行

- 新项目、依赖简单、请求逻辑不复杂：优先使用浏览器原生 `fetch`，再按项目需要封装一层请求函数。
- 需要统一鉴权、请求/响应拦截、超时、上传进度或已有 Axios 体系：使用 Axios 更省心。
- Axios 不是 `fetch` 的替代标准；它是基于浏览器 `XMLHttpRequest`（在 Node.js 中使用适配器）的 HTTP 客户端库。两者都能调用 REST API。

当前项目没有安装 Axios，且已有统一的 `request<T>` 封装，因此继续使用 `fetch` 是合适的。

## 基础差异

| 对比项 | Fetch | Axios |
| --- | --- | --- |
| 来源 | 浏览器和现代 Node.js 的内置 API | 第三方 npm 包 |
| 引入方式 | 直接使用全局 `fetch` | `import axios from 'axios'` |
| 安装体积 | 无额外依赖 | 有额外依赖和打包体积 |
| HTTP 非 2xx 状态 | Promise 仍会成功，需要自行检查 `response.ok` | 默认 Promise 会 reject，可在 `catch` 中处理 |
| JSON 处理 | 需显式调用 `response.json()` | 默认解析 JSON，结果在 `response.data` |
| 超时 | 使用 `AbortController` 自行实现 | 可用 `timeout` 配置 |
| 拦截器 | 没有内建能力，需要封装 | 内建请求、响应拦截器 |
| 请求取消 | `AbortController` | 支持 `AbortController` 的 `signal` |
| 上传/下载进度 | 浏览器端不如 Axios 直接 | `onUploadProgress`、`onDownloadProgress` 更方便 |
| Cookie | 同源请求默认携带；跨域需 `credentials: 'include'` | 跨域需 `withCredentials: true` |

## Fetch 的优点与局限

### 优点

1. **原生且标准**：浏览器直接提供，不需要额外安装和学习新的基础 API。
2. **依赖更少**：减少包体积和供应链依赖，适合本项目这类简单的 REST API 调用。
3. **与 Web 平台能力一致**：可直接配合 `Request`、`Response`、`Headers`、`FormData`、`AbortController` 和流式响应使用。
4. **封装自由度高**：可按照项目的接口返回格式、鉴权和错误规范设计统一请求层。

### 局限

1. **HTTP 错误不会自动抛异常**：`404`、`500` 等响应会得到一个正常的 `Response`，必须判断 `response.ok`。
2. **JSON 需要手动解析**：需调用 `response.json()`，并注意空响应或非 JSON 错误页可能导致解析失败。
3. **没有内建超时和拦截器**：需要用 `AbortController` 和自定义封装补齐。
4. **配置项偏底层**：项目规模较大时，认证、重试、日志和错误转换若分散在业务代码中会难维护。

## Axios 的优点与局限

### 优点

1. **工程化功能更完整**：拦截器适合集中附加 Token、统一处理 `401`、转换错误结构和记录日志。
2. **默认错误行为直观**：非 `2xx` 响应会进入 `catch`，业务代码通常更简短。
3. **常见需求更易配置**：超时、进度回调、请求实例和默认配置都有现成支持。
4. **历史项目兼容性较好**：对旧浏览器或已有 Axios 生态的项目迁移成本更低。

### 局限

1. **额外依赖**：需要安装、更新和审查第三方包；简单项目未必值得引入。
2. **不能替代业务状态判断**：即使 HTTP 状态是 `200`，接口也可能返回业务失败，例如 `{ code: 1001, message: '登录已过期' }`；仍须检查业务码。
3. **抽象不等于免维护**：错误拦截器、重试策略或 Token 刷新逻辑设计不当，同样会让问题难以追踪。

## 当前项目的 Fetch 封装解读

已有的 `request<T>` 统一处理了两层成功条件：

1. `response.ok`：HTTP 状态为 `2xx`；
2. `body.code === 0`：后端约定的业务成功码。

`T` 表示接口响应中 `data` 字段的类型。例如获取笔记数组时可写成 `request<Note[]>('/notes')`，调用方能得到正确的 TypeScript 类型提示。

建议将公共逻辑集中在该封装中，组件只负责传入路径、方法和请求体，不要在每个组件重复处理响应和错误。

## 更稳健的 Fetch 示例

下面的封装覆盖 JSON 请求、HTTP/业务错误、无响应体和超时取消。`Content-Type` 只在确实发送 JSON 请求体时设置，避免给 `GET` 请求附加不必要的请求头。

```ts
interface ApiResponse<T> {
  code: number;
  message?: string;
  data: T;
}

async function request<T>(
  path: string,
  init: RequestInit = {},
  timeoutMs = 10_000,
): Promise<T> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);
  const headers = new Headers(init.headers);

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers,
      signal: controller.signal,
    });

    const body = (await response.json()) as ApiResponse<T>;

    if (!response.ok || body.code !== 0) {
      throw new Error(body.message || '请求失败');
    }

    return body.data;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('请求超时，请稍后重试');
    }
    throw error;
  } finally {
    window.clearTimeout(timer);
  }
}
```

注意：上传 `FormData` 时不要手动设置 `Content-Type`，浏览器会自动附带正确的 `multipart/form-data` boundary。

## Axios 对等示例

如果项目后续决定使用 Axios，先安装依赖：

```bash
npm install axios
```

然后创建实例并在拦截器中集中处理公共逻辑：

```ts
import axios from 'axios';

interface ApiResponse<T> {
  code: number;
  message?: string;
  data: T;
}

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

async function request<T>(path: string) {
  const response = await client.get<ApiResponse<T>>(path);
  const body = response.data;

  if (body.code !== 0) {
    throw new Error(body.message || '请求失败');
  }

  return body.data;
}
```

不要把长期有效的敏感凭据放进浏览器本地存储；认证方案应结合后端的 Cookie、安全属性、过期与刷新策略设计。

## 选择建议

| 场景 | 建议 |
| --- | --- |
| 小型 React/Vite 项目，接口数量有限 | `fetch` + 统一封装 |
| 已有 Axios 依赖，或团队已维护 Axios 拦截器 | 延续 Axios |
| 需要集中 Token、刷新令牌、错误转换 | 两者都能做；Axios 拦截器更直接 |
| 需要上传/下载进度 | Axios 更方便 |
| 关注依赖最少、标准 Web API | Fetch |
| 页面存在缓存、自动重试、失效刷新需求 | 在 Fetch 或 Axios 之上配合 TanStack Query / SWR |

TanStack Query、SWR 负责的是服务端数据的缓存与同步，不是 `fetch` 或 Axios 的直接替代品；它们可以把其中任意一个作为底层请求函数。
