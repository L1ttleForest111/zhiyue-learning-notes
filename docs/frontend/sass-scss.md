# Sass 和 SCSS 是什么

Sass 是 **CSS 预处理器**：你写带变量、嵌套、`@mixin` 的样式，构建时再编译成浏览器能认的普通 CSS。浏览器不懂 `@include`、`$is-h5` 这些。

它不是订阅制那个 **SaaS**（Software as a Service）。拼写少一个 `a`，读音差不多。

## Sass 到底是编译器还是语法

这个词被用乱了。

**Sass 首先是编译器 / 这套工具的名字。** npm 包叫 `sass`，webpack 用 `sass-loader`，Taro 配置键是 `sass:`。这些都在说工具。

它支持两种语法：

| | **SCSS**（现在常用） | **缩进语法**（老写法） |
|---|---|---|
| 文件 | `.scss` | `.sass` |
| 样子 | 几乎就是 CSS，有 `{}` 和 `;` | 靠缩进，没有 `{}` 和 `;` |

SCSS：

```scss
.category-page {
  &__top-cat-list {
    @include flex-gap(32rpx);
  }
}
```

老 Sass：

```sass
.category-page
  &__top-cat-list
    @include flex-gap(32rpx)
```

口语里 “Sass” 有时指工具，有时特指那种缩进语法。本仓库：

- 工具 = Sass
- 语法 = SCSS
- 文件 = `.scss`

没有 `.sass` 缩进文件。

## Taro 配置为什么写 `sass` 而不是 `scss`

Taro 规定的键名就是 `sass`，没有 `scss:` 这个配置项。改成 `scss: { ... }` 它不认。

这 **不是**「理论上该写 `.sass` 文件，写 `.scss` 只是兼容」。Sass 编译器两种语法都是一等公民，**正确姿势就是写 `.scss`**。配置里的 `sass` 配的是编译器，源码里的 `.scss` 是语法，两者对得上。

## `@include flex-gap` 为什么页面里没 import

`@include` 是 Sass mixin 调用：把公共片段展开进当前选择器。`flex-gap` 定义在 `src/styles/mixins.scss`。

页面 scss 里看不见 `@import`，是因为 **编译器替你注入了**。Taro 配置：

```ts
sass: {
  resource: [
    'src/styles/mixins.scss',
  ],
  data: `$is-h5: ${process.env.TARO_ENV === 'h5'};`,
}
```

`sass.resource` 的意思：编译 **每一个** `.scss` 之前，先把 `mixins.scss` 拼到文件头顶。所以能直接 `@include`。

页面里禁止再 `@import '@/styles/mixins.scss'`，重复导入会报重复定义。切过分支后旧 webpack 进程可能还拿着上一份注入，会报 `Undefined mixin flex-gap`，这时要重启 H5。

## `resource` 和 `data` 分别干什么

两项都会在编译每一个 `.scss` 之前插到文件最上面，形态不同。

| | 塞什么 | 干什么 |
|---|---|---|
| `resource` | 文件路径 | 公共 mixin 定义（`flex-gap`、`grid-cols` 等） |
| `data` | 一段 SCSS 源码字符串 | 这次编译的开关变量 |

**H5 和微信都会注入** `mixins.scss`。`resource` 跟是不是 H5 无关。

跟端有关的只有 `data` 里的 `$is-h5`：

- `npm run dev:h5` → `$is-h5: true;`
- `npm run dev:weapp` → `$is-h5: false;`

编译一个页面 scss 时，实际顺序是：

```scss
$is-h5: true;              // data 插进来的变量
/* mixins.scss 全文 */     // resource 插进来的文件
.category-page { ... }     // 你写的页面样式
```

`data` 适合塞全局变量；`resource` 适合塞一整份公共文件。两者都只存在于编译期，浏览器看不到。

## 为什么必须有 `data`

`resource` 给的是同一份 mixin 代码。`data` 给的是这次编译的开关：你现在是在编 H5 还是编微信。

`$is-h5` 不是 CSS 变量，浏览器运行时看不到。Sass 在打包时就要决定走哪个分支。mixin 里类似：

```scss
@if $is-h5 {
  & > *:not(:last-child) { margin-bottom: $row; }
} @else {
  & > view:not(:last-child), & > text:not(:last-child), ... { margin-bottom: $row; }
}
```

- H5 可以输出 `> *`
- 微信 WXSS 不认 `*` 通配符，必须枚举 `view, text, image...`

没有 `data`，`$is-h5` 就是未定义，编译会报错。

一句话：`resource` 负责「有哪些 mixin」；`data` 负责「这次编哪一端」。

## `$row` 是什么

不是页面上的某一行商品。只是 mixin **第一个参数的名字**，表示间距值。

```scss
@mixin flex-gap($row, $col: $row, $dir: row, ...) {
  gap: $row $col;
}
```

你 `@include` 时传进去的第一个数，在 mixin 里面就叫 `$row`。没传第二个时，`$col` 默认等于 `$row`。对照 CSS：`gap: 行间距 列间距`，这里就是 `gap: $row $col`。

### 例子 1：只传一个数

```scss
@include flex-gap(32rpx);
```

- `$row` = `32rpx`
- `$col` = `32rpx`（没传，跟 `$row` 一样）
- `$dir` = `row`（默认）
- 编出来：`gap: 32rpx 32rpx`

### 例子 2：传 CSS 变量 + 多行宫格

```scss
@include flex-gap(var(--semantic-spacing-card-y), 0, $dir: both);
```

- `$row` = `var(--semantic-spacing-card-y)`（行与行）
- `$col` = `0`（列与列不加 gap，宽度已用百分比自己撑）
- `$dir` = `both`

### 例子 3：纵向列表

```scss
@include flex-gap(4rpx, $dir: column);
```

- `$row` = `4rpx`（上下间距）
- `$dir` = `column`

## `gap: 32rpx` 和 `gap: 32rpx 32rpx`

视觉上一样。

- 一个值：`gap: 32rpx` → 行间距、列间距都是 32
- 两个值：`gap: 32rpx 32rpx` → 显式写出「行 32、列 32」

mixin 内部写死了 `gap: $row $col`。你只传 `32rpx` 时 `$col` 默认等于 `$row`，所以编出来永远是两个值。不是间距翻倍，只是写法展开了。

## 「老内核降级」是什么

不是变模糊、不是变低清。意思是：**新浏览器走 A 方案，不支持的老浏览器走 B 方案，间距尽量还在。**

- 新内核（Chrome 84+、Safari 14.1+）：认 `gap`，图标之间空 32rpx
- 老内核（安卓 7 的 WebView、iOS 13）：不认 flex 的 `gap`，这条声明被丢掉，间距会变成 0，挤在一起

所以 mixin 额外准备了 B：用 `margin-bottom` / `margin-right` 冒充 gap。页面根上有 `.no-flex-gap` 时才启用（JS 测过发现不支持 gap 才加这个 class）。新浏览器没有这个 class，B 不生效，只走 `gap`。

「降级」= 能力不够时换土办法，不是把效果故意做差。

这次改公共文件，只改了 `$dir: both` 时负 margin 的写法（`-$row` → `calc($row * -1)`）。现代浏览器走原生 `gap`，这套降级根本不跑。普通 `32rpx` 时 `-32rpx` 和 `calc(32rpx * -1)` 算出来一样。

## `$dir` 是什么；wrap 不是「锁进」

`$dir` 是 mixin 参数，告诉降级 **往哪个方向补 margin**：

| `$dir` | 对应布局 | 老内核补什么 |
|---|---|---|
| `row`（默认） | 横排，不换行 | 主要补右边距 |
| `column` | 竖排 | 补下边距 |
| `both` | `flex-wrap: wrap` 多行宫格 | 行、列都可能要补，还要用负 margin 抵消最后一行多出来的空隙 |

`wrap` 是 CSS 的 `flex-wrap: wrap`：**这一行摆不下就折到下一行**，像打字换行。不是锁定、也不是锁进容器。

分类页一级图标横滑：一行滑，用默认 `row`。展开后的宫格：一行 5 个，多了折下一行，才用 `$dir: both`。

## 变量前面加负号为什么不行（不是 Taro 规定的）

**不是 Taro 的规则，是 Sass 编译器自己的算术规则。** Taro 只是把文件交给 Sass 去编。

分两个时间点：

- **编译时（Sass）**：电脑打包，还没打开浏览器
- **运行时（浏览器）**：页面已经出来，CSS 变量才变成具体像素

`$row: 32rpx` 时，Sass 看得见数字 32，`-$row` 在编译时就能算出 `-32rpx`。

`$row: var(--semantic-spacing-card-y)` 时，Sass 看见的只是一串字 `var(...)`，不知道里面是 16 还是 20。主题要等浏览器读 CSS 变量才知道。Sass 对「一串不是数字的东西」做一元减号，就算不了，报 `Undefined operation`。

`calc(#{$row} * -1)` 的意思是：Sass 不要自己算，把「乘 -1」原样交给浏览器。展开就是：

```css
margin-bottom: calc(var(--semantic-spacing-card-y) * -1);
```

浏览器运行时已经知道变量是多少 px，就能算出负数。

所以：不是 Taro 禁止加负号，是 **编译期还没有变量的值，Sass 没法取负；`calc` 把取负推迟到浏览器。**

## `@include flex-gap` 编出来是什么

原生会写成 `gap: 32rpx;`。这里写成 `@include flex-gap(32rpx);`，编译结果里先输出 `gap`，再给不支持 `gap` 的老 WebView 补一套 `margin` 降级（挂在 `.no-flex-gap` 下面）。

常用参数：

| 写法 | 含义 |
|---|---|
| `@include flex-gap(32rpx)` | 横向间距 |
| `@include flex-gap(4rpx, $dir: column)` | 纵向 flex 的行间距 |
| `@include flex-gap(..., $dir: both)` | `flex-wrap` 多行网格 |

## 容易错

1. 把 Sass 听成 SaaS。这里是样式预处理，和商业订阅无关。
2. 以为 Taro 配了 `sass` 就要写 `.sass` 文件。本仓库写 `.scss`。
3. 在页面 scss 里再 `@import mixins.scss`。已经全局注入，再导会重复定义。
4. 以为 `resource` 只给 H5 注入。两端都注入；只有 `$is-h5` 的值不同。
5. 把 `$is-h5` 当成运行时 CSS 变量。它只在编译期存在。
6. 把 `gap: 32rpx 32rpx` 看成间距翻倍。两个值和一个值一样大。
7. 把 `flex-wrap` 理解成锁定。它是折行。
8. 以为给 CSS 变量加负号失败是 Taro 的限制。那是 Sass 编译期算不了 `var()`。
9. 改了 `mixins.scss` 就等于改了所有页面的布局。这次只改 both 方向负 margin 的写法；新浏览器不走降级分支。

## 相关

`&[data-highlighted='true']` 这种「父选择器 + 属性」见 [CSS 属性选择器](./css-attribute-selector.md)。

class / 标签 / `::placeholder` 见 [CSS 选择器怎么读](./css-selector.md)。
