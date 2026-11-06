# Vitest 是什么

Vitest 是前端的 **单元测试跑手**，Vite 团队做的官方工具，一个单独的 npm 包。它把你写的 `.test.ts` / `.spec.ts` 当考试卷执行，用 `expect` 对比「实际结果」和「期望结果」。

它不是 React Hook，也不像 ESLint 那样扫全项目写法。没写测试文件，它就没事可做。

## 不是打包时执行

`vite build` / `npm run dev` **不会**跑 Vitest。AI Hub 两条命令是分开的：

| 命令 | 干什么 |
| --- | --- |
| `npm run build` → `tsc -b && vite build` | 类型检查 + 打出 `dist` |
| `npm run test` → `vitest run` | 跑测试，跑完退出 |
| `npm run test:watch` → `vitest` | 改代码自动再跑 |
| `npm run lint` | ESLint 查写法 |

CI 可能「先 lint、再 test、再 build」串在一起，那是流水线自己排的，不是 Vite 打包内置步骤。

页面文件（例如积分页 `credit/index.tsx`）也不是打包时执行：打包只是把 TSX 编进产物；抽屉、提交接口是用户打开页面、点按钮之后才跑。

## 和 ESLint 的差别

| | 看什么 | 看哪类文件 |
| --- | --- | --- |
| ESLint | 写没写错（`any`、废变量、Hook 用法） | 几乎所有 `.ts` / `.tsx` |
| Vitest | 跑出来对不对 | **只有**你写的 `.test.ts` / `.spec.ts` |

`.test.ts` 不是 Vitest 去「检查」正式代码的开关。正式代码给用户看；测试文件是另外写的试卷，用户永远看不到。

## 脚手架会不会自带

**不会。** 官方 `npm create vite` 默认装的是 Vite（开发服务器 + 打包），不装 Vitest。要自己 `npm i -D vitest`。测组件时再加 `jsdom`、`@testing-library/react`。

Vite 和 Vitest 是一家的两个包，配置能共用，装 Vite ≠ 装了测试。

- 官方生成的 `vite.config.ts` 一般是 `import { defineConfig } from 'vite'`
- 把测试写进同一份配置后，改成 `from 'vitest/config'`：Vite 的 `plugins` / `server` / `build` 仍认，另外多认 `test: { ... }`
- 不是打包器自带了测试，而是 Vitest 提供了一份更宽的 `defineConfig`

运营端常拆成两份：`vite.config.ts` 只给 Vite，`vitest.config.ts` 只给测试。AI Hub 合在一份里。

`vite.config.ts` 里的 `test.include: ['src/**/*.{test,spec}.{ts,tsx}']` 只决定 **哪些文件算卷子**。没有 `.test.` 的页面不会被跑。

## 校验的是什么：默认只有跑法，考题自己补

Vitest **没有**一套默认业务规则（不会自动验登录、费率、积分页）。

配置里默认/项目补的是 **怎么跑**：

- `include`：找哪些文件
- `environment: 'jsdom'`：假装有浏览器（才能用 `localStorage`）
- `setupFiles`：每场开考前的公共准备

真正的对错全在测试文件里的 `expect(...)`。没写 `expect` 的逻辑，Vitest 完全不管。刚装好、一个测试文件都没有时，跑起来往往是「没找到测试」。

## 常用四个函数

都从 `'vitest'` import，不是 React 的。

| 函数 | 人话 | 是否在判对错 |
| --- | --- | --- |
| `describe('标题', () => { ... })` | 试卷分组，标题给人看、出现在终端 | 否 |
| `it('题干', () => { ... })` | 一道题；也可以写成 `test`，意思一样 | 否，只是容器 |
| `beforeEach(() => { ... })` | **每一道** `it` 开始前先跑（不是只跑一次） | 否，只是准备 |
| `expect(实际值).匹配器(期望值)` | 标准答案 | **是** |

匹配器常用：

- `toBe(x)`：同一个值，适合 boolean、字符串、`null`
- `toEqual(obj)`：对象字段一样就过，不必是同一块内存
- `toBeNull()`：等于 `toBe(null)`

顺序：`beforeEach` → 第 1 个 `it` → `beforeEach` → 第 2 个 `it` … 题和题之间用 `beforeEach` 清状态，避免上一题的 token 留到下一题。

`expect` 不改业务代码，只提问。真正登录、退出的是你调用的业务函数（例如 store 的 `setAuth` / `logout`）。

## 能测什么

都是「调用代码 → 看结果」，对象不同：

1. **纯函数**：输入固定，输出固定（费率换算）。
2. **状态（Zustand）**：直接调 store，不打开浏览器。
3. **组件**（`.test.tsx`）：jsdom 里渲染、点一下，看文案在不在。
4. **请求**：可以假装接口返回什么；真正的后端可以不启动。

不能替代人手打开积分页点分页。那是 E2E（例如 Playwright）。

## 和 Jest

干同一件事：跑试卷。`describe` / `it` / `expect` 几乎一样。Vitest 故意兼容这套 API。

| | Jest | Vitest |
| --- | --- | --- |
| 谁做的 | 更老，Facebook | Vite 团队 |
| 通常跟谁 | Webpack / Umi | Vite |
| 编译测试代码 | 自己再配一套 | 复用 Vite 的别名、TS |
| 假函数 | `jest.fn()` / `jest.mock()` | `vi.fn()` / `vi.mock()` |

Umi 3（鄂尔多斯 `retail-admin`）脚手架 **自带 Jest**：命令是 `umi test`，一般不用再 `npm i jest`。`@types/jest` 是给 TypeScript 看类型，不是 Jest 本体。erdos **没有** Vitest。

Vite 项目 **不自带** Vitest，要额外装。新前端用 Vitest、erdos 用 Jest，是脚手架年代不同，不是测试思想不同。

## 容易错

1. 以为 Vitest 像 ESLint，装上就会扫全项目。
2. 以为 `npm run build` / `npm run dev` 会跑测试。
3. 以为 `*.test.ts` 是「被检查的正式代码」；它是试卷。
4. 以为 `describe` / `it` / `beforeEach` 在校验业务；只有 `expect` 在判卷。
5. 以为 Vite 的 `defineConfig` 自带测试；要从 `vitest/config` 引入才会认 `test` 字段。
6. 把 Jest 和 Vitest 当成完全不同的测试思想；卷子写法几乎相同。
