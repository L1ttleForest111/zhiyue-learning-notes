---
title: 小米高级前端：云 Mall 对齐计划
type: plan
source_project: agentic-mall
source_artifact: "CodeBuddy Canvas: Xiaomi Senior Fe Plan"
status: active
tags:
  - frontend
  - career-plan
  - agentic-mall
updated_at: 2026-10-15
---

# 小米高级前端：云 Mall 对齐计划

目标岗位：高级前端开发工程师（海外小米商城）。练兵场：`agentic-mall` 运营端、小程序、Buddy / AI 助手。

建议周期 12 周。用这个仓把 JD 变成三件可讲的经历，而不是人在仓库里就算满足。

CodeBuddy 中保留一份可勾选的 `Xiaomi Senior Fe Plan` Canvas，用于日常推进；Canvas 不进入 Git，长期目标、稳定任务编号和正式进度以本文为准。

STAR 只在真正完成对应经历后填写文末草稿，不编造结果和数字。每次概念问答或代码实践收尾时使用 `move-learning-notes`：优先合并已有主题笔记，真实改动另存为项目案例。本计划只在完成任务或阶段复盘时更新。

归档规范见[跨项目学习笔记归档工作流](../project/learning-note-archive-workflow.md)，实践案例统一进入 [`agentic-mall` 案例目录](../cases/agentic-mall/README.md)。

## 对照 JD 的缺口

| 职位要求 | 这个仓 / 现在 | 要补到什么程度 |
| --- | --- | --- |
| JS / HTML / CSS 与规范 | 日常在改页面，概念还在补 | 闭包、事件循环、this、层叠与布局要能现场写 |
| React、TS、Node、Webpack | 仓是 React + TS + Vite；几乎无 Webpack | 独立拆组件 + 类型；补 Webpack 心智 |
| 架构设计、性能优化 | 仓里有微前端、复杂页，尚未主导一次优化 | 必须有数字：体积、渲染次数、耗时 |
| LLM/Agent、CodeBuddy、Prompt | 已在用 CodeBuddy、读助手、写过 Skill | 从「会用工具」变成「嵌进研发流程」 |
| 前端低代码平台 | 仓有 demoKit、装修、Pencil | 亲手改一版，能画配置到渲染 |
| 本科 + 3 年、团队协作 | 项目给不了年限 | 用 MR、联调、文档证明协作；年限是硬门槛 |

精力分配：地基 15% · 独立交付 35% · 架构 20% · AI/低代码 20% · 包装 10%。

## 阶段目标

| 阶段 | 时间 | 目标 |
| --- | --- | --- |
| P0 地基 | 第 1–2 周 | 不看代码也能讲清 TS 类、Hook、请求与 SSE |
| P1 独立交付 | 第 3–6 周 | 独立做完一块业务，能讲需求、拆分、状态、接口 |
| P2 架构性能 | 第 7–9 周 | 有一次带数字的性能或拆分经历 |
| P3 AI 与低代码 | 第 10–11 周 | 把助手 / Skill / 装修讲成落地故事 |
| P4 面试包装 | 第 12 周 | 三道 STAR + Webpack 对照，能裸讲 15 分钟 |

P0–P1 在仓里先碰这些文件：

- 助手：`AIAssistantWidget`、`sseClient`、`sessionManager`
- 日历：`pages/promotion/tools` 下的 board、快照、状态边界

先画数据流，再改一处用户能感知的行为，避免只读不写。

P2–P3：性能必须带数字；Agent 把 SSE 进 UI、Skill 触发、失败表现连成一段话；低代码改一版 demoKit 或装修，画出配置到组件树。

## 任务

勾选表示完成。P0 没过关不要跳到微前端。每周至少对着代码讲 15 分钟。完成任务时，在该任务下补充主题笔记、项目案例或验证结果链接。

### P0 地基

- [ ] `P0-01` 用自己的话讲清 `useState` / `useEffect` / `useMemo` / `useCallback` / `useContext`，不依赖项目变量名
  - 学习材料：[React 常用 Hook 说明](../frontend/react-hooks.md)
- [ ] `P0-02` 讲清 class、this、constructor、private；对照运营端 SSEClient 能指出哪段是类、哪段是 React
  - 学习材料：[TypeScript 类（对照运营端 SSEClient）](../frontend/ts-class.md)
- [ ] `P0-03` 讲清 fetch、axios、EventSource 差别，以及为什么流式聊天不能当普通 JSON 接口写
  - 学习材料：[Fetch、Axios 与 EventSource](../frontend/fetch-axios-eventsource.md)、[HTTP 请求与响应头](../frontend/http-network-headers.md)
- [ ] `P0-04` 读促销日历页：`tools/index.tsx`、快照、状态边界、CSS grid 条带，画一张数据流

### P1 独立交付

- [ ] `P1-01` 独立交付一块：日历交互 / 空态错误态 / 助手历史与重试三选一，自己拆组件并提 MR
- [ ] `P1-02` 给这块补齐类型：禁止 `any`，接口响应、组件 Props、联合类型能讲为什么这样写
- [ ] `P1-03` 日历条带或助手气泡布局：flex/grid、百分比定位、为何不用甘特库，能对着代码讲

### P2 架构性能

- [ ] `P2-01` 跑一遍 `vibe-operation-fe` 构建，看产物体积；记下最大依赖，写一条可执行的拆包建议
- [ ] `P2-02` 用 React DevTools 看日历或助手重渲染，改一处 memo / 状态下沉，前后对比次数或卡顿
- [ ] `P2-03` 搞清运营端如何嵌 Buddy（wujie）：主子应用通信、样式隔离、失败时页面怎样

### P3 AI 与低代码

- [ ] `P3-01` 把 AI 助手讲成故事：sessionId / agentCode、SSE 帧如何进 UI、失败文案从哪来
- [x] `P3-02` 再写或改一个业务 Skill（已完成 `move-learning-notes`）；能讲触发词、安全边界、何时不该自动运行
- [ ] `P3-03` 整理 3 条用过的有效提示：带文件、带约束、带验收；当作 Prompt Engineering 例子
- [ ] `P3-04` 改一版 demoKit 或装修相关页：配置/配方如何变成组件树，画出“配置 → 渲染”

### P4 面试包装

- [ ] `P4-01` 对照 Vite：entry、loader/plugin、code split、HMR。仓里用 Vite，面试要能迁到 Webpack 词
- [ ] `P4-02` 准备 3 道 STAR：独立业务、性能数字、Agent/Skill 落地。每道 3 分钟，不看代码能讲

## 面试官可能追问

### JS / HTML / CSS

- `var` / `let` / `const`、闭包、事件循环：宏任务微任务各举一个页面里的例子
- `this` 在普通函数、箭头函数、class 方法里分别指向谁
- flex 与 grid 何时用哪个；日历条带为什么能用百分比而不是甘特库
- 浏览器从输入 URL 到首屏，缓存和重绘回流怎么查

### React / TS / 构建

- 函数组件为什么不能用 class 那套 this；Hook 的规则是什么、违反了会怎样
- `useMemo` / `useCallback` 不写会怎样、乱写又会怎样
- 受控输入、Context 默认值 vs Provider；不写 Provider 会怎样
- TS：interface 与 type、泛型在请求封装里怎么用、为什么要禁 `any`
- Vite 和 Webpack 各解决什么；code split、loader、HMR 用对方的词怎么说
- Node 在前端链路里干什么：脚本、本地服务、构建，不是让你写 Nest

### 架构与性能

- 这一页的状态该放组件、Context 还是服务端；说错了怎么拆
- 长列表 / 日历卡顿：量了什么、改了什么、数字多少
- 打包体积最大的包是谁、怎么异步拆、对首屏有什么影响
- 微前端（wujie）解决什么、样式和路由怎么隔离、失败降级
- 运营端直连 81xx/91xx 与走网关，本地调试怎么选

### LLM / Agent

- SSE 和普通 REST、WebSocket 的差别；聊天为什么用 EventSource
- sessionId、agentCode、clientMessageId 各解决什么问题
- 流式一帧怎么进 UI；中断、重试、失败文案从哪来
- Skill 什么时候该自动触发、什么时候必须手动；你写过哪个
- Agent 怎么嵌进前端研发：改代码、跑页面、写文档、提 MR，而不是只聊天
- Prompt 怎么写才稳定：带路径、带约束、带验收，举三条真例子

### 低代码

- 低代码和「配置驱动 UI」差别；demoKit / 装修的配置长什么样
- 一份页面配方如何变成组件树；非法配置怎么降级
- 可视化搭建和手写 React 如何共存，避免两套 UI
- 如果做最小低代码：schema、物料、渲染器怎么切

### 协作与门槛

- 讲一个和后端 / 设计师对不齐的例子，怎么对齐契约
- MR 里希望 reviewer 看到什么；说明怎么写
- 3 年不够怎么诚实说：用项目复杂度证明深度，不要编年限
- 为什么想做海外商城前端；这个仓哪些能力和小米商城相近

## 什么时候算符合能力要求

必须：

1. 三道裸讲故事：独立业务一块、一次带数字的性能、一次 Agent/Skill 落地。每道 3 分钟，不看代码。
2. 现场能写：Hook 与列表、一个 TS 接口类型、一个 CSS 布局。被追问「为什么」能答到权衡。

加分：SSE、微前端、低代码 schema、CodeBuddy Skill、Vite/Webpack 对照。用仓里的真路径举例。

补不上的部分：

- 本科与 3 年工作经验是招聘硬门槛，这个仓给不了。不要编年限。
- 海外小米商城的多语言、多币种、合规，本仓没有对等模块，不要装做过。
- Webpack 本仓几乎不用。P4 用 Vite 经历翻译过去即可，不必在仓里强行引入 Webpack。

## STAR 草稿（P4 填写）

### 故事 1：独立业务

- 情境：
- 任务：
- 行动：
- 结果：

### 故事 2：性能数字

- 情境：
- 任务：
- 行动：
- 结果（数字）：

### 故事 3：Agent / Skill 落地

- 情境：
- 任务：
- 行动：
- 结果：
