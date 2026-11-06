# CSS 选择器怎么读（class、标签、空格、伪元素）

CSS 选择器是在问：**选哪一个节点、选它的哪一层**。点、没点、空格、逗号、`::` 各自改的是这个问题，不是装饰。

来源：`agentic-mall` 小程序「添加收货地址」H5 适配。粘贴区占位字要钉浅色、两行，生产页这样写：

```scss
.address-edit-page__smart-input .taro-textarea::placeholder,
taro-textarea-core.address-edit-page__smart-input .taro-textarea::placeholder {
  color: #CCCCCC;
  font-size: 24rpx;
  line-height: 32rpx;
  opacity: 1;
  word-break: break-all;
  overflow-wrap: anywhere;
}
```

文件：`frontend-group/vibe-miniprogram-fe/src/subpackages/address/pages/address-edit/index.scss`

## 符号表

| 写法 | 找的是 | 例子 |
| --- | --- | --- |
| `.xxx` | `class="xxx"` | `.taro-textarea` |
| `xxx`（没点） | `<xxx>` 这种**标签** | `div`、`textarea`、`taro-textarea-core` |
| `#xxx` | `id="xxx"` | `#app` |
| `[attr]` | 只要带了这个属性 | `[placeholder]` |
| `[attr='v']` | 带这个属性且值匹配 | `[data-highlighted='true']` |
| `::placeholder` | 占位字，不是子节点 | `textarea::placeholder` |

`[placeholder]` 和 `[data-highlighted='true']` 是同一类方括号，一个不比值、一个比值。细节见 [CSS 属性选择器](./css-attribute-selector.md)。

## Taro H5 Textarea 有两层

`<Textarea className='address-edit-page__smart-input' />` 在 H5 不是一个原生 textarea，大致编成：

```html
<taro-textarea-core class="address-edit-page__smart-input">
  <textarea class="taro-textarea" placeholder="请输入或粘贴..."></textarea>
</taro-textarea-core>
```

| 层 | 是什么 | 怎么选 |
| --- | --- | --- |
| 外层 | 自定义标签 `taro-textarea-core`，class 落在这里 | `taro-textarea-core` 或 `.address-edit-page__smart-input` |
| 内层 | 真正画字的原生 `<textarea>` | `.taro-textarea` |
| 占位字 | 内层 textarea 的伪元素 | `.taro-textarea::placeholder` |

Elements 里点外层，改的是宿主。字是内层画的，所以占位样式必须打到内层 `::placeholder`。

## 为什么 `taro-textarea-core` 前面不加 `.`

因为它是**标签名**，不是 class。

H5 允许带连字符的自定义标签，写法跟 `div`、`span` 一样：没有点，匹配的是 `<taro-textarea-core>`。

```html
<taro-textarea-core class="address-edit-page__smart-input">
```

| 写法 | 实际在找 |
| --- | --- |
| `taro-textarea-core` | 标签 `<taro-textarea-core>` |
| `.taro-textarea-core` | `class="taro-textarea-core"`，页面上没有，打空 |
| `.taro-textarea` | 内层 `class="taro-textarea"`，这才是原生 textarea |
| `taro-textarea-core.address-edit-page__smart-input` | **同一个节点**：既是这个标签，又带这个 class |

`taro-textarea-core.class` 中间没有空格，读成 AND，不是后代。

## 空格、没空格、逗号

```scss
.address-edit-page__smart-input .taro-textarea::placeholder,
taro-textarea-core.address-edit-page__smart-input .taro-textarea::placeholder
```

从左往右读：

**第一行** `.address-edit-page__smart-input .taro-textarea::placeholder`

1. 先找带 `address-edit-page__smart-input` 的节点（外层宿主）
2. **有空格** = 它**里面的**后代
3. 再找 `.taro-textarea`（内层 textarea）
4. `::placeholder` = 这个 textarea 的占位字

**第二行** `taro-textarea-core.address-edit-page__smart-input .taro-textarea::placeholder`

1. `taro-textarea-core.address-edit-page__smart-input`：**没空格** = 同一节点，标签 + class
2. 后面有空格，再找里面的 `.taro-textarea::placeholder`

**逗号**：两个选择器共用后面那份声明。命中任意一个就套用。不是加权重。

| 写法 | 含义 |
| --- | --- |
| `A B`（有空格） | B 在 A 里面 |
| `AB` / `tag.class`（没空格） | 同一节点同时满足 |
| `A, B`（逗号） | A 或 B，声明只写一次 |

第二行比第一行多锁了标签名，选择器更窄、优先级略高。这个页面 `__smart-input` 已经是独有 BEM，**第一条就够**。第二条是防别处也有同名 class 包着 `.taro-textarea`。

## `::placeholder` 在 Elements 里看不到

`::placeholder` 是**伪元素**，不是 DOM 子节点。Elements 树里不会出现一个叫 placeholder 的孩子。

要看它：

1. 展开 `taro-textarea-core`
2. 点内层原生 `<textarea class="taro-textarea">`
3. 右侧 Styles 搜 `placeholder`

点外层自定义标签一定看不到：自定义标签没有 placeholder。

小程序里 `placeholderClass='address-edit-page__smart-placeholder'` 会作用到占位字。H5 的 `placeholderClass` 落在宿主上，**进不了** `::placeholder`。所以颜色、字号、换行要直接写到内层 `::placeholder`。

## 这段声明在干什么

这些属性打在占位字上，不是输入后的正文。

| 属性 | 作用 | 不写会怎样 |
| --- | --- | --- |
| `color: #CCCCCC` | 浅灰 | H5 UA 默认更深 |
| `font-size: 24rpx` | 对齐小程序 12 号 | UA 字号更大，一行装不下变三行 |
| `line-height: 32rpx` | 每行行高 | 字号改了行距不跟着走 |
| `opacity: 1` | 关掉 Chrome 给 placeholder 的半透明（常见 0.54） | `#CCCCCC` 实际发灰，对不齐 |
| `word-break: break-all` | 任意字符都可断开 | `18700880065` 被当一个词，换行比小程序早 |
| `overflow-wrap: anywhere` | 没有空格/逗号也强制断行 | 和 `break-all` 接近，双保险 |

外层宿主再抄一遍 `width/height/font-size` 往往没用：那些 `&__smart-input` 已经有了，而且字不是宿主画的。Elements 里把宿主那坨删掉画面不变，是正常的。

## 几个坑

**把标签当成 class**

`.taro-textarea-core` 和 `taro-textarea-core` 不是一回事。前者找 class，后者找标签。

**改外层以为改了字**

宿主上的 `word-break`、`padding` 不自动等于内层 textarea。占位字看内层 `::placeholder`。

**逗号不是提高优先级**

`A, B { ... }` 只是两套选择器共用声明。要加重，靠更具体的选择器（多写 class、标签、祖先），见 [CSS 属性选择器](./css-attribute-selector.md) 里的 specificity。

**空格会改成后代**

`taro-textarea-core.address-edit-page__smart-input` 是自己。`taro-textarea-core .address-edit-page__smart-input` 变成「这个标签**里面的**带这个 class 的节点」，这页对不上。

**伪元素不是节点**

不能在 Elements 里「选中 ::placeholder」。只能选中产生它的 textarea，再看 Styles。

## 面试可以怎么讲

CSS 选择器里，点是 class，没点是标签名，空格是后代，没空格是同一节点同时满足，逗号是并集共用声明。Taro H5 把 Textarea 编成自定义标签 `taro-textarea-core` 包原生 textarea，className 在宿主上，字在内层。`::placeholder` 是伪元素，Elements 树里看不到；H5 的 placeholderClass 也到不了它，占位样式要写到内层 textarea 的 `::placeholder`。

## 回到添加地址那一句

```scss
.address-edit-page__smart-input .taro-textarea::placeholder,
taro-textarea-core.address-edit-page__smart-input .taro-textarea::placeholder
```

拆开就是：

1. 第一行：宿主里面的原生 textarea 的占位字
2. 第二行：宿主必须是 `taro-textarea-core` 且带这个 class，再找里面的占位字（更窄，通常可省略）
3. 不加 `.` 是因为 `taro-textarea-core` 是标签
4. 写 `::placeholder` 是因为 H5 的 placeholderClass 打不中占位字

姓名 / 手机号那行是同一思路，改用属性选内层，避免 `input` 标签被 plugin-html 改写：

```scss
.address-edit-page__field-input [placeholder]::placeholder
```

`[placeholder]` 怎么读见 [CSS 属性选择器](./css-attribute-selector.md)。

## 相关

`[placeholder]`、`[data-highlighted='true']` 见 [CSS 属性选择器](./css-attribute-selector.md)。
