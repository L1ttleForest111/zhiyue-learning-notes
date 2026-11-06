# Object.assign 是什么

`Object.assign` 做一件事：**把后面对象上的属性，贴到第一个对象上，然后把第一个对象原样返回。**

来源：`agentic-mall` 小程序「我的收藏」左滑删除。`VSwipeAction` 把删除按钮挂到组件上：

```ts
export default Object.assign(VSwipeAction, { Delete: DeleteAction })
```

文件：`frontend-group/vibe-miniprogram-fe/src/components/ui/VSwipeAction/index.tsx`

## 语法

```js
Object.assign(目标, 来源1, 来源2, ...)
```

- 第一个参数是**目标**，会被改掉。
- 后面都是**来源**，只提供要贴的键。
- 返回值就是第一个参数本身（同一个引用，不是新对象）。

```js
const a = { x: 1 }
const b = { y: 2 }
const r = Object.assign(a, b)

r === a   // true，还是 a
a         // { x: 1, y: 2 }
b         // { y: 2 }，没被改
```

多个来源从左到右贴，**后面的同名键覆盖前面的**：

```js
Object.assign({ a: 1 }, { a: 2, b: 3 }, { b: 4 })
// { a: 2, b: 4 }
```

## 函数也是对象，所以能往组件上贴

JavaScript 里函数是对象，可以当普通对象往上挂字段。React 组件就是函数：

```ts
const DeleteAction = ({ onDelete }) => (
  <View onClick={onDelete}>删除</View>
)

const VSwipeAction = (props) => (
  <Swipe rightAction={props.rightAction}>{props.children}</Swipe>
)

export default Object.assign(VSwipeAction, { Delete: DeleteAction })
```

等价于：

```ts
VSwipeAction.Delete = DeleteAction
export default VSwipeAction
```

贴完之后：

```ts
typeof VSwipeAction     // 'function'，还能 <VSwipeAction />
VSwipeAction.Delete     // 就是 DeleteAction
```

页面这样用：

```tsx
<VSwipeAction rightAction={<VSwipeAction.Delete onDelete={fn} />}>
  <ProductCard />
</VSwipeAction>
```

`assign` **不会**让滑动条自动带删除。`.Delete` 只是个组件引用，还是要写进 `rightAction` 才塞进 Swipe 右侧槽。不写 `rightAction`，左滑什么都没有。

这是 React 常见的**复合组件**写法，类似 `Select.Option`、`Tabs.TabPane`：一个入口，子件挂在父组件上，不用再单独 import 删除按钮。

## 浅拷贝：只贴第一层

嵌套对象拷的是**引用**，不是深克隆：

```js
const src = { user: { name: '豪' } }
const dst = Object.assign({}, src)

dst.user === src.user     // true，还是同一个对象
dst.user.name = '改了'
src.user.name             // '改了'，源也被改了
```

只合并第一层键时用 `assign` 没问题。要深拷贝用 `structuredClone`，别指望 `assign`。

## 常见用法

### 合并到新对象（不改源）

第一个参数传 `{}`，源对象都不会被改：

```js
const merged = Object.assign({}, defaults, userConfig)
```

现在更常见的是展开：

```js
const merged = { ...defaults, ...userConfig }
```

展开每次都是新对象；`assign` 默认改第一个参数。不想改原对象，第一个一定要传 `{}`。

### 填默认值

```js
function createUser(opts) {
  return Object.assign({ role: 'guest', active: true }, opts)
}

createUser({ name: '豪' })
// { role: 'guest', active: true, name: '豪' }

createUser({ name: '豪', role: 'admin' })
// { role: 'admin', active: true, name: '豪' }
```

后面覆盖前面，用户传了 `role` 就用用户的。

### 拷贝可枚举自有属性

```js
const copy = Object.assign({}, original)
```

只拷：自有属性（不在原型上的）、可枚举的。拷不了不可枚举键、原型上的方法。getter 会执行，贴过去的是**取值结果**，不是 getter 本身。

### 给函数挂静态方法（和组件挂 Delete 同一套路）

```js
function request(url) { /* ... */ }
Object.assign(request, {
  get: (url) => request(url, { method: 'GET' }),
  post: (url, data) => request(url, { method: 'POST', data }),
})

request('/a')
request.get('/a')
request.post('/a', { x: 1 })
```

`request` 既能当函数调，又能 `.get` / `.post`。

## 和展开运算怎么选

| 写法 | 改不改原对象 | 典型场景 |
| --- | --- | --- |
| `Object.assign(a, b)` | 改 `a` | 就地补属性、往函数上挂字段 |
| `Object.assign({}, a, b)` | 不改 | 合并出新对象 |
| `{ ...a, ...b }` | 不改 | 同上，现在更常用 |
| `structuredClone(a)` | 不改 | 深拷贝 |
| `{ ...a, Delete: X }` | 不改，且得到**新对象** | 不能用来给同一个函数组件挂静态属性 |

给组件挂 `.Delete` 必须改**那个函数对象本身**，所以这里用 `assign`（或 `VSwipeAction.Delete = ...`），不能写成：

```ts
export default { ...VSwipeAction, Delete: DeleteAction }
// 错：变成普通对象，不能当 <VSwipeAction /> 用
```

展开一个函数得不到可调用的组件。

## 几个坑

**`null` / `undefined` 当目标会抛错**

```js
Object.assign(null, { a: 1 })         // TypeError
Object.assign(undefined, { a: 1 })    // TypeError
```

来源是 `null` / `undefined` 会跳过，不当错误：

```js
Object.assign({ a: 1 }, null, { b: 2 })  // { a: 1, b: 2 }
```

**同名键后面覆盖前面**，包括 `undefined` 也会盖掉（展开也一样）：

```js
Object.assign({ a: 1 }, { a: undefined })  // { a: undefined }
```

**浅拷贝**，嵌套对象共享引用。

## 和 Object 其它常用 API

对象就是「键 → 值」的袋子。`assign` 负责贴属性；旁边这几个经常一起用：

```js
const obj = { a: 1, b: 2 }

Object.keys(obj)     // ['a', 'b']     只要键
Object.values(obj)   // [1, 2]         只要值
Object.entries(obj)  // [['a',1],['b',2]]  键值对，方便 for...of

Object.hasOwn(obj, 'a')   // true，是不是自己的键（不是原型上的）
```

从 entries 再拼回去：

```js
Object.fromEntries([['a', 1], ['b', 2]])  // { a: 1, b: 2 }
```

面试里如果被问「拷贝对象」，先分清：

- 只要第一层 → `Object.assign({}, x)` 或 `{ ...x }`
- 要连嵌套一起拷、互不影响 → `structuredClone(x)`
- 往已有对象/函数上补字段 → `Object.assign(目标, 来源)`

## 面试可以怎么讲

`Object.assign` 把来源对象的可枚举自有属性拷到目标对象上，返回目标本身，所以是就地修改加浅拷贝。React 里常见用法是给函数组件挂静态子组件，比如 `VSwipeAction.Delete`：组件还是那个函数，多一个 `.Delete` 属性指向删除按钮。这和 `{ ...component }` 不是一回事，展开函数会丢掉可调用性。

## 回到收藏页那一句

```ts
export default Object.assign(VSwipeAction, { Delete: DeleteAction })
```

拆开就是：

1. 目标：`VSwipeAction` 这个函数组件
2. 来源：`{ Delete: DeleteAction }`
3. 效果：`VSwipeAction.Delete === DeleteAction`
4. 导出：还是这个函数，所以 `<VSwipeAction>` 能用，`<VSwipeAction.Delete>` 也能用

删除按钮会不会出现，跟 `assign` 无关，只跟页面有没有传 `rightAction` 有关。
