# Webpack 是什么

面试提问和口述见 [Webpack 面试提问](./webpack-interview.md)，本文只讲概念。

Webpack 是一个 **打包器（bundler）**。它把项目里互相 `import` 的 `.js` / `.ts` / `.tsx` / `.css` 等文件收成浏览器（或小程序）能运行的产物。

它不是脚手架，也不是 React。脚手架负责生成文件夹；React 负责怎么写组件；Webpack 负责「源码 → 可运行文件」。

## 为什么需要它

浏览器不能直接执行你仓库里那一堆模块、TypeScript、JSX。打包器要做四件事：

1. 从入口出发，画出依赖图（A 引用 B，B 引用 C）。
2. 按文件类型做翻译（TS → JS，JSX → JS，SCSS → CSS）。
3. 合并、压缩，按需要拆成多个文件。
4. 输出到 `dist/`（或小程序的 `dist/weapp`）。

日常说的「打包」就是这第 4 步：小程序把 `dist/weapp` 交给微信开发者工具，Web 把 `dist` 放到服务器。同类工具还有 **Rollup**、**esbuild**，都能把模块收成更少的文件，但不一定每次都在打你要上传的那份 `dist`。

## 上线打包，和「预编译依赖」不是一回事

你理解的打包 = **整项目打进 `dist`，拿去上线或导入微信**。这叫上线构建。

Vite 还多一步，发生在 `npm run dev`，**不产生要上传的 `dist`**：

开发时 Vite 不想先把整个项目打进 `dist`。业务代码尽量按文件给浏览器。但 `node_modules` 里的 React、antd 往往是成百个小文件，还经常是浏览器不吃的 `require`。于是用 **esbuild** 先把这些 **第三方库** 收成少数几个浏览器能 `import` 的文件，放在本地缓存（例如 `node_modules/.vite`）。这叫 **依赖预构建（pre-bundle）**，只加速本地开发。

| | 上线打包（你理解的打包） | Vite 开发时的 esbuild |
| --- | --- | --- |
| 什么时候 | `build` / `build:weapp` | `npm run dev` 启动时 |
| 打谁 | 整个项目 | 主要是第三方依赖 |
| 打到哪 | `dist/`，要上传 / 给微信 | 本地缓存，不上传 |
| 运营端谁来干 | **Rollup** | **esbuild** |
| 小程序谁来干 | **Webpack 5** → `dist/weapp` | 本仓没有这一层 |

esbuild 本身也可以当完整打包器直接打 `dist`。在 Vite 这句话里，它主要被雇来做开发前的依赖预构建（生产压缩有时也会用它，见下）。

### esbuild 具体做了什么（Vite 开发）

对象主要是 **别人写的库**，不是你的页面。

假设业务里写了 `import React from 'react'`，`react` 在 `node_modules` 里可能是几十上百个文件，而且是 `require()`。浏览器打不开这种包。

Vite 启动时大致会：

1. 扫项目依赖（React、antd、dayjs……）。
2. 交给 esbuild：把每个库内部的很多文件 **合成很少几个文件**，并把 CommonJS 转成浏览器能 `import` 的 ESM。
3. 写到本地缓存，例如 `node_modules/.vite/deps/react.js`。
4. 之后开发时，浏览器向开发服务器要 React，拿到的是这份已经收好的文件，而不是去拉一百个小文件。

另外，开发时你改 `src/` 里的 `.ts` / `.tsx`，Vite 也会用 esbuild **很快地去掉类型、把 TS 变成 JS** 再给浏览器。这一步是「翻译当前这个文件」，**没有**把整个项目写进 `dist`。

所以 esbuild 在这里干的是：**把依赖收干净 + 把 TS 快速译成 JS**，让本地刷新快。产物是缓存和内存，不是上线包。

### Rollup 具体做了什么（Vite 生产 `vite build`）

对象是 **整个应用**：你的页面 + 用到的库，要变成可以放到服务器上的 `dist`。

`vite build` 时大致会：

1. 从 `index.html`（里面引用的入口 JS）开始，跟着 `import` 把用到的文件全部串起来。
2. 做 **tree-shaking**：你没引用到的函数尽量删掉，减小体积。Rollup 擅长这个。
3. **拆文件**：入口一个包，路由懒加载再拆成别的 `.js`，带 hash 文件名（如 `index-a1b2c3.js`），方便浏览器缓存。
4. 处理 CSS、静态资源，生成最终的 `index.html`，让它指向这些带 hash 的文件。
5. 压缩代码。Vite 默认压缩往往还是交给 **esbuild**（只做缩小体积，不当这一次的主打包器）。

输出一般是：

```text
dist/
  index.html
  assets/
    index-xxxxx.js
    vendor-xxxxx.js   （第三方库常会进这类文件）
    index-xxxxx.css
```

这才是你理解的打包：把 `dist` 交给 Nginx / CDN，用户打开的是这些文件。

一句话：**esbuild（开发）= 先把库收拾好，好让本地跑得快；Rollup（生产）= 把整个应用收成要上线的 `dist`。**

## Webpack 是不是同时干了 esbuild 和 Rollup 的活

可以这么理解，但 **不是** Webpack 内部去调用 esbuild 和 Rollup。

Vite 把活拆开：开发用 esbuild 收依赖，上线用 Rollup 打 `dist`。

Webpack 是 **同一套引擎干开发和上线**：

- 开发（`webpack serve` / Taro `dev:weapp`）：在内存里编译、热更新，本地能看。对应 Vite 的开发阶段，但编译仍是 Webpack 自己做。
- 上线（`webpack` / Taro `build:weapp`）：写出 `dist`。对应 Rollup 打 `dist`。

对照一下三件事分别「下手在哪」：

| 工具 | 主要下手对象 | 典型输出 |
| --- | --- | --- |
| esbuild（Vite 开发） | `node_modules` 里的库；顺带把你的 TS 译成 JS | `node_modules/.vite/deps/*`，不上传 |
| Rollup（Vite 生产） | 你的 `src/` + 用到的库 | `dist/index.html` + `dist/assets/*` |
| Webpack（小程序 / Umi） | 开发和上线都是整份应用 | 开发在内存里预览；上线是 `dist` 或 `dist/weapp` |

小程序这条线没有「esbuild 预编译依赖」这一层。Taro 可以开类似能力（`compiler.prebundle`），本仓是关掉的，开发和出包都是 Webpack 5。

## 五个零件（先记这些）

Webpack 自己的配置通常叫 `webpack.config.js`。核心就这几个字段：

### 1. entry（入口）

打包从哪几个文件开始走依赖图。网页项目常常是 `src/index.tsx` 配一个 `index.html`。多页或小程序会有多个入口。

### 2. output（出口）

打完放到哪、文件叫什么。例如 `dist/main.js`。

### 3. loader（加载器）

**按文件类型翻译。** 一条规则通常是：匹配哪些文件（`test`）+ 用哪个翻译器（`loader`）。

常见例子：

- `babel-loader` / `ts-loader`：TS、JSX → JS
- `css-loader` + `style-loader` 或 mini-css 插件：CSS
- `sass-loader`：SCSS → CSS
- `url-loader` / `asset`：图片变成 URL 或内联

记法：loader 管「**这一种文件怎么变成 JS 能理解的东西**」。

### 4. plugin（插件）

**插手整个打包过程**，不限于某一种文件。它是一个带 `apply(compiler)` 的类，在 Webpack 生命周期里执行（编译开始、每个模块、全部写盘之后……）。

常见例子：

- `HtmlWebpackPlugin`：生成 HTML 并自动引入打包后的 JS
- `DefinePlugin`：在编译期把代码里的某个标识符换成常量（例如把 `process.env.NODE_ENV` 换成 `"production"`）
- `MiniCssExtractPlugin`：把 CSS 抽成单独文件
- 项目自己写的插件：打包结束后改产物路径、删文件

记法：loader 对「一种文件」；plugin 对「整次构建」。

### 5. 拆包（code split）与 HMR

- **拆包**：不要打成一个巨大 JS。按路由、按「主包 / 分包」、或 `import()` 动态加载，生成多个 chunk。
- **HMR**：开发时改一个文件只换这一块，不必整页重来。

## 一份最小配置长什么样

不必背完整 API，能看懂这种结构即可：

```js
module.exports = {
  entry: './src/index.js',
  output: { filename: 'bundle.js', path: __dirname + '/dist' },
  module: {
    rules: [
      { test: /\.tsx?$/, use: 'babel-loader' },
    ],
  },
  plugins: [
    // new webpack.DefinePlugin({ ... })
  ],
}
```

- `module.rules` = loader 列表
- `plugins` = plugin 列表

读这份配置时容易理解成「把 `index.js` 一个文件拷进 `bundle.js`，再用 babel-loader 解析这个 ts」。更准确是：

- **entry 是起点，不是唯一被打包的文件。** Webpack 从 `./src/index.js` 出发，跟着所有 `import` 画出依赖图，整张图合成 `dist/bundle.js`（没拆包时常常就这一个出口文件）。
- **loader 按文件名匹配，不是「专门处理入口」。** `test: /\.tsx?$/` 表示图里的 `.ts` / `.tsx` 才走 babel-loader。入口若是 `.js`，它自己不一定走这条；被它引进来的 `.tsx` 才会走。
- **loader 是翻译（TS/JSX → JS），不是阅读一遍。** 上面 `plugins` 里是注释，这份最小例子等于还没启用 DefinePlugin。

### 依赖图箭头从入口指向被 import 的文件

若 `src/index.js` 里有 `import deadline from '@/abc.ts'`：

```text
index.js  ──依赖──►  abc.ts
   ▲
   └── 从 entry 这里开始走
```

是 `index.js` 依赖 `abc.ts`，不是 `abc.ts → index.js`。入口叫 `index.js` 就还是 `index.js`，不会变成 `index.ts`。

如果 `abc.ts` 再 `import './foo.ts'`，图继续往外长：`index.js → abc.ts → foo.ts`。

`@/` 只是路径别名（常常等于 `src/`），不改变箭头方向，只改变去哪找文件。

很多框架 **不让你直接写这份文件**，而是用自己的配置字段，最后翻译成上面这样。Umi 用 `chainWebpack`，Taro 用 `compiler` + `webpackChain`，Vite 则根本不走 Webpack。主配置落在哪，见下面「配置文件在哪」。

Vite / Rollup 画依赖图同样从入口的 **`import` 往外走**。`export default` 和 `export function foo` 都只是「这个文件交出去什么」；**连边的是 `import`**，不是只有 `export default` 才进图。`import App from './App'`、`import { x } from './a'`、`import './a.css'`、`import('./page')` 都会连。开发时尽量按 ESM 给浏览器；`node_modules` 里的 `require` 会先被 esbuild 收成 ESM。上线仍是 Rollup 把这张图收进 `dist`。

## 配置文件在哪（约定，不是随便起名）

文件名和默认位置是 **各工具自己的约定**。选了 Vite / Taro / Umi，就按它们的说明书放，不是仓库随便起的。可以改路径（例如 `vite --config ./foo.ts`），没人这么干时就按约定。

哪个工具打包，就找那个工具的主配置：

| 谁打包 | 主配置（约定入口） | 真正引擎 |
| --- | --- | --- |
| 运营端 Vite | 项目根 `vite.config.ts`（`defineConfig` from `vite`） | 开发 esbuild + 生产 Rollup，没有 Webpack |
| 小程序 Taro | `config/index.ts`，再 merge `config/dev.ts`、`config/prod.ts` | `compiler.type: 'webpack5'` 时是 Webpack 5 |
| 运营端 Umi 3（鄂尔多斯 / retail-admin 这类） | `config/config.ts`，再按 `UMI_ENV` merge `config/config.prod.ts` 等 | `webpack5: {}` 打开 Webpack 5 |

仓库里 **没有** 手写的 `webpack.config.js` 很正常：Taro / Umi 读自己的 `config/`，在内存里生成 Webpack 配置。Vite 则根本不生成 Webpack 配置。

同目录下其它 `*config*` **不是**打包主配置，读者不同：

| 文件 | 读者 | 是不是打包主配置 |
| --- | --- | --- |
| `babel.config.js` | Babel | 否，是翻译规则 |
| `postcss.config.js` | PostCSS（被 postcss-loader 或 Vite CSS 流水线去读） | 否 |
| `tailwind.config.js` | Tailwind | 否 |
| `vitest.config.ts` | Vitest 单测 | 否，`npm test` 才读，不生成小程序包 / 上线 `dist` |
| `eslint.config.js` | ESLint | 否 |
| `tsconfig.json` | TypeScript / 编辑器 | 否 |
| 小程序 `src/app.config.ts`、页面 `index.config.ts` | 微信：页面路径、导航栏 | 否 |
| `project.config.json` | 微信开发者工具 | 否 |
| `_config/*.yaml` | 部署 / 环境，不是前端框架 | 否 |

不要用「最外层有没有 `xxx.config.js`」当找 loader 的方法。loader 写在 **Webpack 的 `module.rules`** 里；框架项目里默认规则在 Taro / Umi 内部，项目多出来的、改过的，看 `webpackChain` / `chainWebpack`。

`index.html` 不是 config，但是 Vite 的 HTML 入口，打包起点之一。

## Babel 是什么

Babel 是 **翻译器**，不是打包器。它把新语法、TypeScript、JSX 收成目标环境能跑的 JS。`babel-loader` 才是 Webpack 的 loader；`babel.config.js` 是 Babel 自己的说明书。两者不是同一个文件。

关系是两层：

```text
Webpack 的 module.rules
  → 写着：.ts / .tsx 用 babel-loader
      → babel-loader 跑起来
          → 去读 babel.config.js（presets: taro）
              → 译成普通 JS
                  → Webpack 再继续打进依赖图 / dist
```

所以不会出现 `babel-loader.config.js`。loader 的名字写在 Webpack 规则里；翻译怎么做写在 Babel 配置里。

`presets` 是「翻译规则礼包」。Babel 的能力拆成很多小插件（译 JSX、译可选链、去掉 TS 类型……）。每次列几十个太烦，就打成一套，叫 preset。数组写法 `['名字', { 选项 }]`：第一个是 preset 名，第二个是传给它的选项。小程序里常见：

```js
module.exports = {
  presets: [
    ['taro', { framework: 'react', ts: true }],
  ],
}
```

表示用名为 `taro` 的那一套，打开 React（所以懂 JSX）和 TS。第一行若有 `eslint-disable no-undef`，是因为 `module` 是 Node 给的全局，ESLint 可能不认。

同一词 **plugin** 也容易混：

- Webpack 的 `plugins`：整次打包过程（DefinePlugin、改产物）
- Babel 的 `plugins`：某一种语法怎么译（规则常常包在 preset 里，文件里不一定再列）

`babel.config.js` 是 **Babel 约定的文件名**。Webpack 不会因为你写了 `module.exports` 就去读它，是 Babel 按文件名来找。

有了 Taro 的 `config/` **仍然需要** `babel.config.js`：`config/index.ts` 管怎么把整个小程序打出去，不写「TSX 里的 JSX 怎么译成 JS」。没有 Babel 这份，打包器知道「要编译这些文件」，但不知道语法怎么处理。

这不是构建时从 `config/` 再吐出一份 `babel.config.js`。`taro init` 脚手架会一次放好两套：`config/` 给 Taro，`babel.config.js` 给 Babel。之后每次打包两边各读各的。

为什么不能只留一个 `webpack.config.ts`：Taro 不是 Webpack，是一层壳（小程序/H5、设计稿宽度、分包、拷贝原生文件）。Webpack 只是可选编译器。主配置必须是 Taro 自己的格式。`config/` 用文件夹，是 Taro 约定：公共一份，开发和生产再各补一份。

为什么 Babel 不写进 `config/index.ts`：真正做翻译的是 Babel。`babel-loader`、有时还有 ESLint 解析 TSX，都会去找这个文件名。Webpack 允许把 Babel 选项写在 loader 的 `options` 里，Taro 没这么做——编译器可以换，Babel 规则仍希望各工具共用同一份。

Umi 3 常常 **没有** 根目录 `babel.config.js`：它默认自己管 Babel，要加插件一般写 `extraBabelPlugins`。Taro 则强制根目录那份，两边不完全对称。

小程序里的 `vitest.config.ts` 是第三条线：Vitest 跑 `src/**/__tests__`，把 `@tarojs/taro` 指到测试 stub，**不参与**微信产物。`npm run dev:weapp` 读 Taro；`npm test` 读 Vitest。

## `module.exports`、`export default` 和 ESM

`module.exports` 和 `export default` 都是「把本文件交出去」，**两套模块体系**，不是 React API。组件文件里常写 `export default function Page()`，只是 React 项目爱用 ESM，语法属于 JavaScript。

| | CommonJS | ESM（ES Modules） |
| --- | --- | --- |
| 交出去 | `module.exports = { ... }` | `export default ...` 或 `export function foo` |
| 拿进来 | `const x = require('./a')` | `import x from './a'` |
| 常见于 | `babel.config.js`、`webpack.config.js` | 业务 `.ts` / `.tsx`、Vite、浏览器 |

ESM 全称 ECMAScript Modules，是语言标准里的 `import` / `export`。一个项目里两套并存很常见：业务用 ESM，部分工具配置仍用 CommonJS。

**文件名**是给某个工具当配置入口的暗号（`babel.config.js` → Babel，`webpack.config.js` → Webpack CLI）。**`module.exports` / `export default`** 只是这个 JS 怎么把对象交出去。自写的 `utils/foo.js` 也可以 `module.exports`，Babel 不会自动读它。

## webpackChain / chainWebpack（改装口，不是另一份配置文件）

**同一件事，两个框架起的函数名不一样。** 都是：框架已经生成好一份 Webpack 配置，再把一个可链式改配置的对象交给你改一刀。底层是同一个库 `webpack-chain`。

| | 小程序 Taro | Umi 3 |
| --- | --- | --- |
| 回调名 | `webpackChain` | `chainWebpack` |
| 写在哪 | `config/index.ts` 的 `h5` / `mini` 里 | 常抽到 `config/webpack-build.config.ts`，再 merge 进各环境 |
| 干什么 | 改框架生成的 Webpack | 改框架生成的 Webpack |

不是 Webpack 官方有两个 API。看到 `.loader()` / `.plugin()` / `.alias.set()` 这种链式写法，就是在改 Webpack。

它和 `config/index.ts` 是 **包含关系**：`webpackChain` 就是写在这份 Taro 配置里面的一个函数，不是并列的另一个文件。

```text
你跑 npm run dev:weapp
    → Taro 读 config/index.ts
        → 看到 compiler.type = webpack5，在内存里拼出一份 Webpack 配置（纸上已经有字）
        → 调用你写的 webpackChain(chain)，把这份配置以 chain 的形式传进来
        → 你的函数当场改它（别名、再加一条 loader、加插件）
        → Webpack 按改完的配置，从入口跟着 import 画依赖图、产出 dist/weapp
```

纸 **不是空白的**。Taro 已经写好一大半：从哪进、打到哪、`src` 用 babel-loader、样式用 sass-loader。`webpackChain` 是在这张已有字的纸上补几笔，不是从白纸写一份完整 Webpack 配置。`webpackChain` 也不画依赖图，只改规则；画图打包的是 Webpack。

**不会在仓库里再吐出一份 `webpack.config.js`。** 改动发生在那一次 `taro` 进程的内存里，跑完就丢。你能直接看到的是：

1. 源码里这段 `webpackChain` 函数本身
2. 打包完成后的 `dist/weapp`

为什么不跟框架「一块写完整 Webpack」：那一大半是 Taro 替所有项目写的公共模板。每个项目抄一份几百行配置，框架一升级就要自己对 diff。故意拆成两层：

1. **Taro 字段能表达的，已经写在 `config/index.ts` 里了**（`compiler.type`、`outputRoot`、`defineConstants`、`copy`……）。Taro 会翻译进那张纸。这是用 Taro 语言「一块写」。
2. **没提供开关、或要改它已经写上的那一行，才进 `webpackChain`。** 例如默认只编译 `sourceRoot`（`src`），包在外面的源码要补一条 babel-loader；sass-loader 是 Taro 先挂上的，只想改它的 options，不必重写整条 CSS 流水线。

可以想成：Taro 交房，你们装修几个房间，不会为了换插座把整栋楼图纸重画。Umi 的 `chainWebpack` 同一类改装口。`webpack5: {}` 相当于 Taro 的 `compiler.type: 'webpack5'`：告诉框架用哪台打包机。

所以它是 **项目自己的 Webpack 定制**，而且更窄：框架字段能表达的仍写字段；只有必须改 Webpack 时才进 chain。

## 拆包、主包、分包、chunk

**拆包（code split）是打包器手法：不要打成一份过大的 JS，切成多份，按需再加载。**  
**主包 / 分包是微信小程序的落地方式**，不是拆包的唯一定义。Web 上没有主包这个词，拆的是多个 chunk。

| 词 | 是谁的 | 含义 |
| --- | --- | --- |
| 拆包 | Webpack / Vite / Rollup | 输出多个 JS（CSS）文件，而不是一个巨大 bundle |
| chunk | 打包器 | 拆出来的 **每一份** 产物文件，就是一个 chunk |
| 主包 | 微信 | 打开小程序 **立刻下载** 的那一份，有体积上限（常见约 1.5～2MB） |
| 分包 | 微信 | `subPackages` 里的目录，**进对应页才下载**（也可预下载） |

关系：拆包是目的和方法；小程序用主包/分包实现这个目的；Web 用 `import()`、`splitChunks` 实现。Taro 内部仍可能产出 Webpack chunk，再落到 `dist/weapp` 的主包 JS、分包 JS。

Web 没有 `subPackages`。运营端用 `React.lazy(() => import('@/pages/...'))`：先下入口和公共代码，进某个后台页再下那一页的 JS。`lazy` 必须配 React 官方 `Suspense`（chunk 未到时显示 fallback）。动态 `import()` 返回 Promise，见 [Promise](./promise.md)。

## publicPath 和图片出包

`output.path` / Taro 的 `outputRoot` 是文件写到磁盘的哪。**`publicPath` 是写进 JS/CSS 的 URL 前缀**，运行时去哪拉这些文件。

小程序主包有体积上限，图进包最容易超。生产把 `src/assets` 打到 `dist/weapp` **外面**（例如 `dist/assets-cdn/`），`publicPath` 写成 COS 地址，JS 里只留完整 URL；CI 再把目录传到 CDN。开发走本地静态服务，不必每次传 COS。文件名带 hash，内容变了地址才变，缓存才能更新。

wxss 里避免把图 base64 进样式（等于又进包）。少量必须跟包的资源（如直播控件）仍可用 `copy` 拷进分包。运营端/H5 没有微信主包限制，但仍有 `publicPath`（相对路径或 CDN）。

## 容易错的地方

1. **把框架配置名当成 Webpack 原名。** 例如 Taro 的 `defineConstants`、`compiler.type` 是 Taro 的表单，真正干活的是 Webpack 的 `DefinePlugin` 和 Webpack 5 引擎。
2. **loader 和 plugin 不分。** 问「谁把 TS 编译成 JS」→ loader；问「谁在打包结束改文件、谁注入环境变量」→ plugin。
3. **以为没有 `webpack.config.js` 就没用 Webpack。** 被 Taro / Umi 包起来时，配置分散在 `config/*.ts` 里，依赖里仍有 `webpack`。
4. **环境变量以为运行时还有 `process`。** 浏览器和小程序里常常没有 Node 的 `process`。`DefinePlugin` 是编译期替换字符串，漏配会运行报错。
5. **把 esbuild 预编译依赖当成上线打包。** 那一步不进 `dist`，也不给微信。上线打包才出 `dist`。
6. **以为 entry 只打包那一个文件。** 打的是从入口出发的整张依赖图。箭头从入口指向被 import 的模块，不是反过来。
7. **以为 `export default` 是 React 的写法。** 它是 ESM 的导出，Vue、工具库也能用。React 组件只是常用它。
8. **以为 `module.exports` 决定了文件必须叫 babel.config.js。** 文件名是工具的约定；`module.exports` 只是 CommonJS 导出。
9. **以为 Vite 只认 `export default`。** 依赖图跟的是各种 `import`，包括具名导入、副作用导入、动态导入。
10. **以为 `babel.config.js` 就是 babel-loader。** loader 在 Webpack 规则里；这份文件是 Babel 的 presets / plugins。
11. **以为根目录每个 `*config*` 都是打包配置。** 主配置只有打包工具那一份；PostCSS / Tailwind / Vitest / ESLint / 微信 app.config 各管各的。
12. **以为 Taro 每次构建会从 `config/` 生成 `babel.config.js`。** 脚手架一次放好，之后各读各的。
13. **以为 `webpackChain` 会在磁盘写出 Webpack 配置。** 只改内存里的对象；产物是 `dist`，不是 `webpack.config.js`。
14. **把 `_config/` 当成 Umi 的打包目录。** Umi 认的是 `config/config.ts`；`_config/*.yaml` 往往是部署。
15. **把拆包说成「就是小程序主包分包」。** 拆包是通用手法；主包/分包是微信的落地。Web 拆的是 chunk。

## 和 Vite、Umi 怎么对应

| 概念 | Webpack | Vite | Umi 3 / Taro（常见） |
| --- | --- | --- | --- |
| 你敲的命令 | `webpack` / `webpack serve` | `vite` / `vite build` | `umi dev` / `umi build`；`taro build` |
| 开发时 | Webpack 自己编译 | esbuild 预编译依赖 + 业务走 ESM | 一般仍是 Webpack |
| 上线出 `dist` | Webpack | Rollup | 内部仍是 Webpack 5 |
| 改配置 | `webpack.config.js` | `vite.config.ts` | 主文件在 `config/`；改装用 `chainWebpack` / `webpackChain` |
| 编译期替换变量 | `DefinePlugin` | `define` | Umi `define` / Taro `defineConstants` |

学 Webpack 心智：把任意项目里的配置 **翻译回这五个零件**。不必先背完官方文档。刷题用 [Webpack 面试提问](./webpack-interview.md)。
