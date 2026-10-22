# Promise 是什么

Promise 是「以后会有结果」的盒子。网络请求、动态 `import()`、定时器都不会立刻给值，就先给你这个对象，等成功或失败再通知你。

它是 JavaScript 语言里的标准，不是 React API。`async` / `await` 是写 Promise 的另一种语法。

## 三个状态

只能走一次，不能从成功改回失败，也不能反悔。

| 状态 | 含义 | 怎么进去 |
| --- | --- | --- |
| **pending**（进行中） | 还没完 | 刚创建时 |
| **fulfilled**（成功） | 有结果了 | 调用 `resolve(值)` |
| **rejected**（失败） | 出错了 | 调用 `reject(原因)`，或执行里 `throw` |

成功之后的值常叫 resolved value；失败原因常常是 `Error`。

## 怎么造一个

```js
const p = new Promise((resolve, reject) => {
  // 里面是异步活
  成功了 → resolve(结果)
  失败了 → reject(new Error('原因'))
})
```

`resolve` / `reject` 是构造时 Promise 给你的两个函数，不是关键字。

```js
new Promise((resolve) => resolve(1))              // 立刻成功，值 1
new Promise((_, reject) => reject(new Error('x'))) // 立刻失败
Promise.resolve(1)    // 语法糖，已经成功
Promise.reject(err)   // 语法糖，已经失败
```

`new Promise(() => {})` 既不 `resolve` 也不 `reject`，会 **永远停在 pending**。

动态导入官方就是返回 Promise：

```js
import('./page.js')
// pending → 下载完 fulfilled（模块对象）
// 404 / 网络错误 → rejected
```

## 怎么等结果

两种写法同一件事。

```js
p.then((value) => { /* 成功 */ })
 .catch((err) => { /* 失败 */ })
 .finally(() => { /* 无论成败 */ })
```

```js
try {
  const value = await p   // 成功：拿到值；失败：抛错，进 catch
} catch (err) {
  // 相当于 .catch
}
```

`await` 只能写在 `async function` 里。`async` 函数本身也返回 Promise：`return x` 等于 `resolve(x)`；函数里 `throw` 等于 `reject`。

## 串联

```js
fetch(url)
  .then((res) => res.json())
  .then((data) => data.list)
  .catch((err) => console.error(err))
```

某一步 `throw` 或返回 `Promise.reject`，后面的 `then` 会跳过，直到最近的 `catch`。

## 几个一起等

- `Promise.all([a, b])`：都成功才成功；有一个 reject 整组失败。
- `Promise.allSettled`：都结束，不论成败。
- `Promise.race`：谁先结束听谁的。

先会 `all` 即可。

## 对照：路由懒加载为什么是 Promise

运营端用 `lazy(() => import('@/pages/...'))`。`import()` 返回 Promise，官方 `React.lazy` 要求这个函数成功时给出 `{ default: 组件 }`。

自愈包装里（`lazyWithRetry`）：

- `await factory()`：等动态导入这个 Promise。
- `return mod`：async 函数成功。
- `throw err`：async 函数失败（reject），不是 chunk 加载问题时交给 ErrorBoundary。
- 发版后旧 chunk 404 要刷新页面时：`return new Promise(() => {})`，一直 pending，Suspense 停在 loading，避免刷新前闪错误页。

`Suspense` 是 React 官方：子组件（这里是还在下载的 lazy 页）没准备好时显示 `fallback`。和 Promise 的配合是：pending → 显示 loading；fulfilled → 渲染页面；rejected → 错误边界。

## 容易错的地方

1. **以为 `reject` 会立刻把整个程序停掉。** 它只把这个 Promise 标成失败。没人 `.catch` / `try/catch` 才会变成未处理错误。
2. **以为 `await` 失败会返回 `undefined`。** 失败是抛错。
3. **把「一直 pending」当成成功。** 既不 resolve 也不 reject 就是一直等。
4. **`resolve` 传入另一个 Promise。** 外层会跟内层的成败走。
5. **把 Promise 当成 React 特有。** 请求、定时器、动态 `import` 都会用。

记住三件事：**pending / 成功 / 失败；成功用 resolve（或 return）；失败用 reject（或 throw）。**
