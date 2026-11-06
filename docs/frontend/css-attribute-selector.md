# CSS 属性选择器是什么

属性选择器按 **HTML 属性** 选节点，不靠 class。方括号就是在问：这个元素有没有这个属性、值是不是某个字符串。

来源：`agentic-mall` 小程序「选择地址」H5 适配。位置条高亮态用 `data-highlighted`，生产页要盖住 Scene 的浅蓝底：

```scss
.address-selection-location,
.address-selection-location[data-highlighted='true'] {
  background: #FFFFFF;
}
```

文件：`frontend-group/vibe-miniprogram-fe/src/subpackages/address/pages/address-list/index.scss`

## 这一行在干什么

逗号表示 **两个选择器共用后面的声明**：

1. `.address-selection-location` — 普通位置条
2. `.address-selection-location[data-highlighted='true']` — 带了高亮标记的位置条

读成：class 是 `address-selection-location`，并且属性 `data-highlighted` 的值等于字符串 `"true"`。

组件是这样打标的：

```tsx
<View
  className='address-selection-location'
  data-highlighted={highlighted}
>
```

`highlighted` 为 `true` 时，H5 编出来大致是：

```html
<div class="address-selection-location" data-highlighted="true">
```

## `data-*` 是什么

HTML 规定，自定义数据属性必须以 `data-` 开头，浏览器不会当未知属性丢掉。页面状态不方便加 class 时，就挂在节点上给 CSS / JS 读。

这页 Scene 里不止高亮，门店当前态也是同一套路：

| JSX | 编出来的 HTML | CSS 怎么选 |
| --- | --- | --- |
| `data-highlighted={true}` | `data-highlighted="true"` | `[data-highlighted='true']` |
| `data-current={true}` | `data-current="true"` | `[data-current='true']` |
| `data-layout="drawer"` | `data-layout="drawer"` | `[data-layout='drawer']` |

React / Taro 把布尔写成属性时，**值一定是字符串**。DOM 里没有真正的布尔 `true`，只有 `"true"` / `"false"`，或者属性根本不出现。

```tsx
data-highlighted={true}        // data-highlighted="true"
data-highlighted={false}       // data-highlighted="false"（属性还在）
data-highlighted={undefined}   // 属性不出现
```

所以 CSS 必须拿字符串去比，写成 `'true'`，不要指望 `[data-highlighted=true]` 按 JS 布尔去匹配。

## 选择器对照

| 写法 | 含义 | 会选中 `false` 吗 |
| --- | --- | --- |
| `[data-highlighted]` | 只要有这个属性 | 会（`"false"` 也算有） |
| `[data-highlighted='true']` | 值必须是 `"true"` | 不会 |
| `[data-highlighted='false']` | 值必须是 `"false"` | 只选这个 |
| `.address-selection-location[data-highlighted='true']` | class **并且** 属性值匹配 | 只选高亮那条 |

单引号、双引号都行：`[data-highlighted='true']` 和 `[data-highlighted="true"]` 一样。

方括号可以记成两档：

| 写法 | 问的是 | 例子 |
| --- | --- | --- |
| `[attr]` | 有没有这个属性，值无所谓 | `[placeholder]`、`[data-highlighted]` |
| `[attr='值']` | 有，而且值刚好是这串字 | `[placeholder='姓名']`、`[data-highlighted='true']` |

属性值还可以做「开头 / 包含」匹配，这页没用到，面试常问：

| 写法 | 含义 | 例子 |
| --- | --- | --- |
| `[attr^='a']` | 以 `a` 开头 | `href^='https'` |
| `[attr$='.png']` | 以这段结尾 | `src$='.png'` |
| `[attr*='foo']` | 中间包含 | `class*='location'`（能用但不如直接写 class） |

优先用 class。`data-*` 适合 **状态**（高亮、当前、抽屉），不适合当 BEM 块名用。

## `[placeholder]`：只问有没有，不问值

添加收货地址的姓名 / 手机号 / 详细地址，H5 要把占位字钉成浅灰。`placeholderClass` 到不了内层，所以写成：

```scss
.address-edit-page__field-input [placeholder]::placeholder {
  color: var(--semantic-color-text-placeholder);
  opacity: 1;
}
```

文件还是 `address-edit/index.scss`。内层原生节点大概是：

```html
<input class="weui-input" placeholder="姓名">
```

整句从左往右读：

1. `.address-edit-page__field-input` — 先限定在这个字段输入框里
2. 空格 — 它**里面的**后代
3. `[placeholder]` — 带了 `placeholder` 属性的节点（不比值）
4. `::placeholder` — 这个节点的占位字

和选择地址那句是同一套语法，只是一个比值、一个不比：

| 写法 | 选中条件 |
| --- | --- |
| `[data-highlighted='true']` | 有 `data-highlighted`，并且值是 `"true"` |
| `[placeholder]` | 只要有 `placeholder`，值是「姓名」还是「收货人手机号」都算 |
| `[placeholder='姓名']` | 只有 placeholder 文案刚好是「姓名」的那一个 |

姓名、手机号、详细地址文案不同，不能写成 `[placeholder='姓名']`，否则后两个打不中。这里要的就是「凡是占位输入框」，所以用无值的 `[placeholder]`。

不写 `input::placeholder`，是因为项目开了 `@tarojs/plugin-html`：SCSS 里的标签选择器 `input` 会被改写成 `taro-input-core`，命中不到内层原生 `<input>`。`[placeholder]` 只认属性、不认标签名，改写伤不到它。

`[placeholder]` 和 `placeholder-class` 也不是一回事：前者是「节点有没有 placeholder 这个 HTML 属性」，后者是小程序给占位字用的 class 名，H5 只是挂在宿主上当摆设。选择器、伪元素怎么读见 [CSS 选择器怎么读](./css-selector.md)。

## SCSS 里的 `&[data-highlighted='true']`

`&` 代表当前嵌套的父选择器。Scene 这样写：

```scss
.address-selection-location {
  background: var(--semantic-color-bg-card);

  &[data-highlighted='true'] {
    background: #F5F9FF;
  }
}
```

编出来就是：

```css
.address-selection-location { background: var(--semantic-color-bg-card); }
.address-selection-location[data-highlighted='true'] { background: #F5F9FF; }
```

`&` 后面紧贴 `[...]`，中间没有空格。有空格含义就变了：

```scss
&[data-highlighted='true']   // 自己带这个属性
& [data-highlighted='true']  // 错：后代里带这个属性的节点
```

嵌套、`&` 的更多写法见 [Sass 和 SCSS](./sass-scss.md)。

## 为什么两个选择器都要写

CSS **更具体的规则赢**。粗算 specificity（id, class/属性, 标签）：

| 选择器 | 大约权重 |
| --- | --- |
| `.address-selection-location` | (0, 1, 0) |
| `.address-selection-location[data-highlighted='true']` | (0, 2, 0) |

高亮那条 **class + 属性**，比光 class 更重。只覆盖：

```scss
.address-selection-location {
  background: #FFFFFF;
}
```

普通态会变白，但 `highlighted === true` 时 Scene 的 `#F5F9FF` 仍然更具体，浅蓝还在。所以生产页把两个选择器写成一组，权重对齐后再盖颜色。

外面再套页面根，权重更高，也不会和别的页的同名类打架：

```scss
.address-list-h5 {
  .address-selection-location,
  .address-selection-location[data-highlighted='true'] {
    background: #FFFFFF;
  }
}
```

编出来是 `.address-list-h5 .address-selection-location[data-highlighted='true']`，(0, 3, 0)。这是生产页覆盖 Scene、又不改 `ui-design` 的常用手法。

## 和加 class 怎么选

两种都能表达「高亮」：

```tsx
className={`address-selection-location${highlighted ? ' is-highlighted' : ''}`}
```

```scss
.address-selection-location.is-highlighted { background: #F5F9FF; }
```

| | `data-*` | 额外 class |
| --- | --- | --- |
| JSX | 一个布尔 prop | 自己拼 class 字符串 |
| CSS | `[data-highlighted='true']` | `.is-highlighted` |
| 适合 | 设计态 / Scene 上已有的状态位 | 纯样式开关 |

这页 Scene 已经用 `data-highlighted`、`data-current`、`data-layout`，覆盖时跟它用同一套选择器，不要再发明一个 `is-highlighted`。

## 微信和小程序上的差别

同一套选择器，两端不一定都吃：

- **H5**：属性选择器正常，`highlighted: true` 会变成浅蓝 `#F5F9FF`
- **微信**：部分基础库对 `[data-xxx='true']` 匹配不稳定，位置条经常一直走默认白底

走查时小程序像白、H5 像灰/浅蓝，多半是这个，不是 token 丢了。H5 要对齐小程序视觉，就在生产页把高亮态也钉回白。

门店「当前」是同一个坑：Scene 写了

```scss
&__item[data-current='true'] &__distance {
  width: 62rpx;
}
```

小程序文字溢出还能看见；H5 父级 `overflow: hidden` 会把 `1068.5km` 裁掉。覆盖时同样要对着 `[data-current='true']` 写，不能只改 `__distance`。

## 几个坑

**`[data-highlighted]` 会误伤 `false`**

属性还在，值是 `"false"`，有属性就能匹配。要高亮才变色，必须比 `'true'`。

**JSX 布尔不是 CSS 布尔**

CSS 里没有 `true` 这个类型。不写引号的 `[data-highlighted=true]` 有的浏览器当标识符，有的当字符串，别省引号。

**空格会改成后代选择器**

`.foo[data-x='true']` 是同一个节点。`.foo [data-x='true']` 是 `.foo` **里面的** 后代。

**覆盖时权重要对齐**

Scene 用了 class+属性，你只用 class，高亮态盖不住。逗号把两种状态写成一组，声明只写一次。

**别用属性选择器代替 BEM**

`[class*='location']` 能跑，但会误伤别的 class。块、元素用 class；状态才用 `data-*`。

**`[placeholder]` 不要和 `input` 标签混用**

本仓库 SCSS 里的 `input` 会被 plugin-html 改写成 `taro-input-core`。要打内层原生 input，用 class（`.weui-input`）或属性（`[placeholder]`），不要写标签名。

## 面试可以怎么讲

属性选择器按 HTML 属性匹配。`[attr]` 只问有没有这个属性，`[attr='值']` 还要值相等。`[data-highlighted='true']` 就是选带了这个自定义数据且值等于字符串 true 的节点；`[placeholder]` 则是凡是写了 placeholder 的节点都算，不管文案是什么。React 布尔 prop 落到 DOM 上是字符串。带属性的选择器 specificity 比单 class 高一档，所以覆盖高亮态时必须把带属性的那条一起写上。Taro H5 上不要用 `input::placeholder` 打内层，plugin-html 会把标签改写成 `taro-input-core`，改用 `[placeholder]::placeholder`。

## 回到选择地址那一句

```scss
.address-selection-location,
.address-selection-location[data-highlighted='true'] {
  background: #FFFFFF;
}
```

拆开就是：

1. 逗号：普通态和高亮态共用白底
2. `[data-highlighted='true']`：对齐 Scene 里 `&[data-highlighted='true']` 的权重
3. 外层还有 `.address-list-h5`，避免和别的页同名类冲突
4. 颜色钉成白，是为了 H5 看起来和小程序一样，不是 Scene 设计稿的 `#F5F9FF`

高亮标记本身还在节点上，只是这块覆盖把背景改掉了。

## 相关

class、标签、空格、逗号、`::placeholder` 怎么读，见 [CSS 选择器怎么读](./css-selector.md)。
