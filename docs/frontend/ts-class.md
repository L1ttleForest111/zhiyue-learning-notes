# TypeScript 类（对照运营端 SSEClient）

主体是 TypeScript / JavaScript 的面向对象写法，不是 React。React 页面仍是函数组件；类用在「管连接、管会话」这类工具上。

对照文件：

- 类定义：`frontend-group/vibe-operation-fe/src/components/business/AIAssistantWidget/utils/sseClient.ts`
- 创建实例：`sessionManager.ts` 里 `new SSEClient({...})`
- 对外取/建会话：`sessionManager.ts` 的 `getOrCreate`（类内部不调用，给 React 组件用）
- 调用方法：`ChatPanel` 里 `client.sendMessage(...)`

---

## 1. 类是什么

类是一份模板：字段放状态，方法放行为。`new` 一次得到一个实例，各实例的数据互不影响。

可以把它想成「一份状态 + 一组函数」捆在一起，**不要当成某一个函数**。字段像这个对象的变量；方法像操作这些变量的函数。每次 `new` 复制出一套。

```ts
export class SSEClient {
  private url: string;
  private sessionId: string;

  constructor(options: SSEClientOptions = {}) {
    this.url = options.baseUrl || '...';
    this.sessionId = generateSessionId();
  }

  sendMessage(message: string, sessionId?: string) {
    this.setupEventListeners(/* ... */);
  }

  private setupEventListeners(/* ... */) {}
}
```

类开头这些字段：

```ts
private url: string;
private agentCode: string;
private status: SSEStatus = SSEStatus.IDLE;
private retryCount: number = 0;
```

是**这个实例自己的变量**。有的带初始值（`status`、`retryCount`），有的只声明类型，真正赋值在 `constructor` 里（`url`、`agentCode`、`sessionId`）。

对照普通函数：`function send() { let retryCount = 0; }` 里的变量调用结束就没了。类字段在 `new` 之后一直活在这个对象上，`sendMessage`、`close` 都通过 `this.retryCount` 读写同一份。

和 Java 几乎同一套词：`class`、`constructor`、`this`、`private`。

---

## 2. constructor 和外面传入的值

构造函数。`new SSEClient({...})` 时跑一次，给这个新对象填初始值。

`sessionManager.ts` 里创建时：

```ts
const sseClient = new SSEClient({
  baseUrl: opts?.sseUrl,
  agentCode: opts?.agentCodeForSSE ?? agentCode,
});
```

括号里这份对象，就是构造函数参数 `options`：

```ts
constructor(options: SSEClientOptions = {}) {
  this.url = options.baseUrl || '...默认地址';
  this.agentCode = options.agentCode || 'rmall-biz-assistant';
}
```

| 外面传入 | 构造函数里 |
|---|---|
| `baseUrl: opts?.sseUrl` | `options.baseUrl` → 赋给 `this.url` |
| `agentCode: opts?.agentCodeForSSE ?? agentCode` | `options.agentCode` → 赋给 `this.agentCode` |

这是外面提供初始配置，类自己收进 `this`，不是外面直接改 `sseClient.url`。`= {}` 表示什么都不传时 `options` 是空对象，后面用默认地址和默认 agent。

这时 `this` 就是刚刚 `new` 出来的那个实例。建完以后，变量 `sseClient` 和构造函数里的 `this` 是同一个对象。

---

## 3. this 是谁

`this` 指向「当前这个实例」，不是 C 语言那种要自己写 `*`、`&` 的指针。

看调用点的点号左边，不看方法写在哪个文件：

```ts
const client = inst.sseClient;
client.sendMessage(displayContent, inst.sessionId, { onMessage: ... });
```

这次 `sendMessage` 里面的 `this` 就是 `client`。

| 写法 | 读法 |
|---|---|
| `this.sessionId = sessionId` | 改这个实例自己的会话 ID |
| `this.agentCode` | 读这个实例创建时记下的 agent |
| `this.setupEventListeners(...)` | 在这个实例上登记监听 |
| `this.eventSource` | 这个实例自己的那条 SSE 连接 |

再 `new` 一个 `SSEClient`，会有另一份 `url`、`sessionId`、`eventSource`，两套 `this` 互不干扰。

C / C++ / Go / Rust 才有显式指针。Java、TypeScript、Python 里对象传递的是引用，`this` 表示「这次方法挂在哪个实例上」。

---

## 4. private：谁能赋值

`private` 挡的是**别的文件、别的对象**，不是挡这个类自己。

构造函数写在 `SSEClient` 里面，所以可以 `this.url = options.baseUrl`。`sendMessage` 里读 `this.url` 同样可以。

外面不行：

```ts
const sseClient = new SSEClient({ baseUrl: '...' });
sseClient.url = '...';   // 报错，url 是 private
```

- 不写修饰符（或写 `public`）：外面能调，例如 `sendMessage`、`setSessionId`、`getOrCreate`
- `private`：只许类内部用，例如 `setupEventListeners`、`createInstance`、字段 `url`

`getOrCreate` 在 `sessionManager.ts` 内部没有调用，因为它是对外方法，给 `index.tsx`、`ChatDrawer`、`ChatPanel`、`HistoryPanel` 用，和 `sendMessage` 一样。

TypeScript 的 `private` 主要在编译期检查；转成 JS 后拦不住运行时访问。真正的 JS 私有字段是 `#eventSource` 这种写法，这份代码没用。

---

## 5. setSessionId：也是给私有变量传值

算赋值，但走的是类对外开的门，不是直接改 `sseClient.sessionId`。

```ts
sseClient.setSessionId(sessionId);

setSessionId(sessionId: string): void {
  this.sessionId = sessionId;  // 还是类自己在改 private 字段
}
```

外面只能调 `setSessionId`，不能写 `sseClient.sessionId = sessionId`。

和构造函数同一套路：外面给值，里面写到 `this`。差别是时机——`constructor` 在 `new` 时赋一次；`setSessionId` 是对象已经存在之后再改。会话管理器先 `new SSEClient`，再用当前这条对话的 `sessionId` 覆盖构造函数里临时生成的那个。

`getSessionId()` 是只读的 getter。

---

## 6. 怎么调用（三步）

1. `new SSEClient({...})` 创建实例（会话管理器里做）
2. 把实例存起来（挂在当前会话的 `inst.sseClient` 上）
3. 需要时 `实例.方法()`：

```ts
const client = inst.sseClient;
client.setSessionId(inst.sessionId);
client.sendMessage(displayContent, inst.sessionId, {
  onMessage: (data) => { /* 一帧字 */ },
});
```

点号左边是实例，右边是方法。

和普通接口不同：`await getDecorationPageList()` 一次返回整包；`sendMessage` 不能 `return` 完整回复，所以把 `onMessage` 这类回调传进去，数据一帧帧回来时再通知组件。

---

## 7. 和 React 怎么叠

| 层 | 写法 | 例子 |
|---|---|---|
| 界面 | React 函数组件 + `interface` 描述 props | `ChatPanel` |
| 工具 | TS 类 + `private` + `this` | `SSEClient` |

组件负责渲染；类负责连接状态。流程：`new` → 存到会话 → 用户点发送 → `client.sendMessage()` → 类内部听 SSE → 通过回调把字交回组件。

`interface` 是 TypeScript 的类型声明（约定对象有哪些字段），不是 React 语法，也不是类。

---

## 8. 和 Java 对照

| Java | TypeScript |
|---|---|
| `class Foo` | `class Foo` |
| 构造函数 `Foo(...)` | `constructor(...)` |
| `this.xxx` | `this.xxx` |
| `private` / `public` | `private` / 默认公开 |
| `new Foo()` | `new Foo()` |
| `obj.method()` | `obj.method()` |
| setter `setXxx` | `setSessionId(...)` |

---

## 9. 疑问对照

| 疑问 | 结论 |
|---|---|
| 类能不能当一个函数看？字段是不是初始值？ | 字段 ≈ 这个对象的变量/初始状态；方法 ≈ 操作这份状态的函数。类本身是「状态 + 一组函数」，不是某一个函数。 |
| `new SSEClient({ baseUrl, agentCode })` 是不是外部赋值？ | 是外面提供初始配置。这份对象就是 `constructor(options)` 的 `options`，类内部赋给 `this.url` / `this.agentCode`。 |
| `url` 是 `private`，构造函数里 `this.url = ...` 为什么可以？ | `private` 只挡外面。类自己的 `constructor` 和方法可以读写。外面不能 `sseClient.url = ...`。 |
| `setSessionId` 算不算给私有变量传值？ | 算。走公开方法，由类内部改 `this.sessionId`。和构造函数同一套路，只是发生在 `new` 之后。 |
| `getOrCreate` 类内部没调用？ | 对外方法，给 React 组件用，不需要在类内部再调一遍。 |
