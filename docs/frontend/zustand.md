# Zustand 是什么

Zustand 是 React 的 **全局状态库**，一个 npm 第三方包。把登录、菜单、权限这类「很多组件都要读」的数据放在组件树外面，任意地方用 hook 取、改，改了会自动重渲染。

它不管打包，也不管发请求。打包是 Vite / Webpack；请求是 axios / fetch；Zustand 只管前端内存里的共享状态。

## 和 useState 的差别

- `useState` 是 React 自带的，管 **当前这个组件** 的状态（写在页面里，也只属于这个页面组件）。
- Zustand 适合 **跨组件、跨路由还要共享** 的数据。也可以给某一个功能单独建一份 store，不必塞进唯一一个超级全局。

和 Redux 比：没有强制 action / reducer / 最外层 Provider，API 更短。和 Context 比：组件一般只订自己用到的字段，不是整棵树一改全刷。

## 最核心：`create`

日常 90% 就是这一个函数。从 `'zustand'` 引入，调用一次造出 store，返回值本身就是 hook。

```ts
import { create } from 'zustand'

export const useAuthStore = create((set) => ({
  token: null as string | null,
  nickname: '',
  setAuth: (token: string, nickname: string) => set({ token, nickname }),
  clearAuth: () => set({ token: null, nickname: '' }),
}))
```

- `create(...)`：建仓库。
- `(set) => ({ ... })`：初始数据 + 改数据的方法。`set` 是 Zustand 给的更新函数。
- 需要读当下快照时，第二个参数是 `get`：`create((set, get) => ({ isLoggedIn: () => !!get().token }))`。

## 怎么用（三处）

造出来的 hook 上，常用就这几下：

| 写法 | 何时用 |
| --- | --- |
| `useAuthStore((s) => s.token)` | **组件里**订阅，变了会重渲染 |
| `useAuthStore.getState()` | **组件外**读一眼（axios 拦截器里取 token） |
| 取出的 `setAuth` / `clearAuth` | 组件里改；组件外则 `useAuthStore.getState().clearAuth()` |
| `useAuthStore.setState({ token: null })` | 不经过你写的方法、直接改（少用） |

组件里写入、读取：

```tsx
function LoginPage() {
  const setAuth = useAuthStore((s) => s.setAuth)
  async function onSubmit() {
    const res = await apiLogin()
    setAuth(res.token, res.nickname)
  }
  return <button onClick={onSubmit}>登录</button>
}

function Header() {
  const nickname = useAuthStore((s) => s.nickname)
  const clearAuth = useAuthStore((s) => s.clearAuth)
  return (
    <div>
      {nickname}
      <button onClick={clearAuth}>退出</button>
    </div>
  )
}
```

拦截器不是 React 组件，不能调 hook：

```ts
const token = useAuthStore.getState().token
if (token) config.headers.Authorization = `Bearer ${token}`
useAuthStore.getState().clearAuth()
```

同一份 store：登录页 `setAuth`，顶栏立刻显示昵称，请求自动带 token，不用父子一层层传 props。

## 可选：中间件仍包在 `create` 外

不是另一套主 API。

- `persist`：写入 localStorage，刷新还在。
- `devtools`：接 Redux DevTools。
- `immer`：用「改草稿」的写法更新深层对象。

没有这些，裸 `create` 也能干活。先把 `create` + 选择器 + `getState` 用熟。

## 对照仓库

运营端 `src/store/`（登录 `auth.ts`，请求里 `useAuthStore.getState().token`）。AI Hub 的 `src/store/auth.ts` 更短：`create((set, get) => ...)`，`setAuth` 里先写 storage 再 `set({ token, user })`，`isAuthenticated` 用 `get()` 读当前 token。

## 容易错的地方

1. **把 Zustand 当成 React 官方 API。** 它是第三方包，要安装、从 `'zustand'` 引入。
2. **在非组件里写 `useAuthStore()`。** hook 只能在组件 / 自定义 hook 里。拦截器用 `getState()`。
3. **组件里订整个 store：`useAuthStore()` 无选择器。** 仓库里任何字段变了都可能重渲染。写成 `useAuthStore((s) => s.token)`。
4. **以为一个项目只能有一个 store。** 登录一份、菜单一份很常见。
5. **用 Zustand 管只在一个表单里的输入。** 那种用 `useState` 即可。
