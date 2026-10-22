# React Query 是什么

React Query（包名 `@tanstack/react-query`，现称 TanStack Query）管的是 **从服务器拿来的数据**：请求、缓存、loading、刷新。它 **不是** React 内置 API。

`useXxx` 只是 hook 命名约定。官方有 `useState`；第三方也可以导出 `useQuery`、`useNavigate`。看 import 从哪个包来。

读接口可以记：`useQuery` 是帮你拿接口数据的 hook。比自己 `useEffect` + axios + `useState` 多了缓存和自动再拉。请求函数仍是 axios，Query 不替代 axios。

## 和 Zustand 的分工

| | Zustand | React Query |
| --- | --- | --- |
| 典型数据 | 登录 token、当前用户、菜单 | 列表、详情、分页接口 |
| 从哪来 | 前端自己改 | `queryFn` 里调接口 |
| 核心问题 | 跨组件共享 | 别每个组件自己请求、自己记 loading、改完不知道刷新 |

登录态用 Zustand；列表优先用 Query。AI Hub 两者都有。运营端列表多数还是 axios + `useState`，没有这一层。

## 三个入口（读 / 写 / 遥控器）

1. **`QueryClient` + `QueryClientProvider`**：在 `App` 里 `new QueryClient()` 再包一层。这是缓存本体，**用 Query 就必须有**。没有它，`useQuery` / `useMutation` 会找不到 Client。
2. **`useQuery`：读**（GET 列表/详情）。组件一挂上、key 一变、被标过期就会发。
3. **`useMutation`：写**（POST 开通/充值/冻结）。**只有** `mutate` / `mutateAsync` 被调用才发，一进页面不会自动 POST。
4. **`useQueryClient()`**：从 Provider 取出那台缓存总台。**不是每个文件必写**，只有要 `invalidateQueries` / 手改缓存时才拿遥控器。只有展示、没有写操作的页可以不出现这个 hook。

```ts
const { data, isFetching } = useQuery({
  queryKey: ['credit-accounts', pageNum, pageSize, keyword],
  queryFn: () => listCreditAccounts({ pageNum, pageSize, keyword }),
})
```

串起来：

```text
useQuery     屏幕上那张表从哪来（缓存 + GET）
useMutation  你点的那一下 POST
QueryClient  POST 成功后喊「这类 GET 过期了，再拉」
```

## queryKey 不是请求参数，也不是返回值

它只是这块缓存的 **名字**（储物柜编号）。

| | 是什么 |
| --- | --- |
| `queryKey` | 柜门编号 |
| `queryFn` | 去取货（里面才 axios） |
| `data` | 柜里装的东西（`queryFn` resolve 的值） |
| `isFetching` | Query 自己的「正在请求」开关，后台 JSON 里没有这个字段 |

习惯上 `queryFn` 里会变的参数，`queryKey` 里都带上，否则换筛选可能拿到上一份缓存。Query **不会**把 key 当 HTTP 参数发出去；参数是你在 `queryFn` 里自己传的。

第一个字符串是 **资源名**（自己起的，如 `credit-accounts`），用来和流水 `credit-txns` 分开，也方便：

```ts
qc.invalidateQueries({ queryKey: ['credit-accounts'] })
```

只写头部，这一类的各页、各筛选都会标过期。它不发给后端。

## data / isFetching 和后台格式

hook 的返回形状是 Query 规定的（永远有 `data`、`isFetching`、`error`）。后台 **不必** 返回同名字段。

常见三层：

```text
后台信封     { code, message, data: { records, total } }
     ↓ axios 拦截器（code===200 时只交出信封里的 data）
queryFn 返回 { records, total }     ← 变成 useQuery 的 data
     ↓
页面         data.records、表格 loading={isFetching}
```

信封约定是项目 axios 层的，不是 Query 的。`queryFn` return 数组，`data` 就是数组。

## 为什么不只直接调接口

发请求这一下和 axios 一样。多的是请求前后的事。点一次、用完就扔，直接调够。列表、分页、多组件共用时，自己用一个 `setData` 会踩坑。

**竞态（最重要的一条）：** 点第 2 页发出请求 2，再点第 3 页发出请求 3。若 3 先回来已经显示第 3 页，2 后到再 `setData`，屏幕会被打回第 2 页。

`useQuery` 把 2 和 3 放进两个柜子（key 里带了 `pageNum`）。你点了 3 就只展示 3 那个柜子；2 后到只更新 2 号柜。想看第 3 页仍然是点分页 `setPageNum(3)`，Query 不替你翻页，只保证当前画面跟当前 key。

其它好处：离开再回来可先显示缓存；两个组件同一 key 只打一次接口；写完 `invalidate` 列表自动刷新；可定时轮询。

## useMutation：主动写，不是规定接口返回格式

和 `useQuery` 都会打接口，差在 **谁触发、结果放哪**：

| | `useQuery` | `useMutation` |
| --- | --- | --- |
| 典型 HTTP | GET | POST / PUT / DELETE |
| 何时发 | 自动（挂载、key 变、invalidate） | 只有 `mutate` / `mutateAsync` |
| queryKey | 有，当缓存身份证 | 没有「第几页」那种柜子 |
| 结果 | 进缓存，`data` 给表格 | 这次提交的结果；列表不会自动变 |

写操作不能做成 `useQuery`：否则一进页或一翻页就可能再 POST 一次。

**`onSuccess` / `onError` 是成功/失败之后你干什么**（toast、关抽屉、invalidate），不是「接口必须返回什么 JSON」。`mutationFn` 的 `return` 才是业务结果，会作为 `onSuccess` 的第一个参数。失败是 `mutationFn` throw/reject，进 `onError`。

- `mutate(参数)`：发出去就结束。  
- `mutateAsync(参数)`：返回 Promise，表单可以等它结束再关窗。

写在哪：和列表同一页。上面 `useQuery` 拉表，下面定义 mutation，按钮/抽屉里调用。额度页开通抽屉是 `onFinish={() => createMutation.mutateAsync(v)}`。

和按钮里直接 `await axios` 差得没有 `useQuery` 那么大。方便处主要是：`isPending`、成功后固定 invalidate、和已经用 Query 的列表配套。只有一个按钮、成功后跳走，直接 axios 够。运营端大量就是直接调。已经上了 Query 的页，Mutation 才是配套。

## isPending：包算布尔值，转圈要自己接到 UI 上

`isPending` 是 React Query 算的：`mutationFn` 进行中为 `true`，结束变 `false`。你不用自己 `setLoading`。

Query **不认识** antd / NutUI。不把这个布尔接到组件的 `loading` / `disabled` 上，界面不会转圈，请求照样在发。

```tsx
<Button loading={createMutation.isPending} onClick={() => createMutation.mutate(v)}>开通</Button>
```

额度页抽屉没用 `isPending`：`FormDrawer` `await` 了 `mutateAsync` 的 Promise，用自己的 `submitting` 给 antd Button `loading`。两种接法都可以。

## invalidate：过期不是把表格清空

`queryClient.invalidateQueries({ queryKey: ['credit-accounts'] })` 是给这一族柜子盖章：**别再当最新的**，不是 `data = undefined`。

当前页正开着对应 `useQuery` 时：盖章 → 再跑 `queryFn` → 新数据回来前通常仍显示旧 `data`，`isFetching` 可能为 true → 回来后换成新余额。意义是「缓存不能信了，再问服务器」。只 POST 成功、不 invalidate，表格还是旧余额。

没人盯着这份 query 时（已离开列表）：先记下过期，下次再进会重新拉。只动点名的前缀，`credit-txns` 不会被误伤，除非像充值那样两个都 invalidate。

Mutation 成功结果往往只是「开通成功」这一下，不是整页新列表。列表仍归 `useQuery`。QueryClient 当中间人：写成功 → 列表过期 → 列表再 GET。

## 对照仓库

AI Hub `App.tsx` 的 `QueryClientProvider`。额度页 `useQuery` + 表格 `setPageNum`；`useQueryClient` + `invalidateAccounts`；`useMutation` 开通/充值；抽屉 `mutateAsync`。请求封装 `utils/request` 拆信封。登录仍是 Zustand `src/store/auth.ts`。

## 容易错的地方

1. **以为 `useQuery` 是 React 官方。** 要装 `@tanstack/react-query`。
2. **以为 `queryKey` 就是请求参数或返回 body。** 只是缓存名；参数在 `queryFn` 里传。
3. **key 里漏了页码或筛选。** 又会抢同一个柜子，竞态回来。
4. **以为后台必须返回 `{ data, isFetching }`。** 那是 hook 的返回值。
5. **用 Query 存登录 token。** 那种用 Zustand。
6. **以为运营端也有。** 运营端多数页没有这个包。
7. **以为 `invalidate` 会把表格清空。** 是标过期再拉，旧 `data` 往往还在。
8. **以为 `onSuccess` 规定接口返回格式。** 那是成功后的副作用。
9. **以为装了 Query 按钮会自动 loading。** `isPending` 要接到具体组件库上。
10. **每个页面都写 `useQueryClient`。** 只有要指挥缓存时才需要；Provider 才是全局必须。
11. **把开通做成 `useQuery`。** 写操作必须 `mutate`，否则会自动、重复 POST。
