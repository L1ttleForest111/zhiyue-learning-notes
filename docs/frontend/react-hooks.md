# React 常用 Hook 说明

Hook 就是函数组件里以 `use` 开头的函数，用来让组件「记住数据、做副作用、缓存计算结果」。下面用大白话说，不绑某个项目里的变量名。

可以先记四句话：

- `useState`：记住一份要展示的数据
- `useReducer`：也是记住数据，但按你定的规则去改
- `useMemo`：记住一次算出来的结果，别每次渲染都重算
- `useCallback`：记住一个函数，别每次渲染都换新函数

---

## 1. useState：组件自己的记事本

组件刷新时，普通变量会丢。`useState` 把值存在 React 里，改它，画面就更新。

```js
const [name, setName] = useState('张三');

setName('李四');           // 直接给新值
setName((old) => old + '！'); // 按旧值算出新值
```

返回两个东西：当前值，以及改这个值的函数。改完一定会触发一次重渲染。

适合：输入框内容、开关、列表、加载中，这些你确实要画在界面上的数据。

---

## 2. useReducer：带规则的记事本

也是记住一份数据。差别是：你不是每次把新值塞进去，而是喊一声「按这条规则改」。

```js
function reducer(state, action) {
  if (action.type === 'add') return { n: state.n + 1 };
  if (action.type === 'reset') return { n: 0 };
  return state;
}

const [state, dispatch] = useReducer(reducer, { n: 0 });

dispatch({ type: 'add' });
dispatch({ type: 'reset' });
```

字段很多、改法很多时，比一堆 `setXxx` 更清楚。

还有一种极简用法：根本不关心这个数字是多少，只想让组件重画一次（比如真正的数据在组件外面）：

```js
const [, forceTick] = useReducer((x) => x + 1, 0);
forceTick();
```

`,` 表示第一个值不用，只要第二个函数。用 `useState` 当铃铛也能做到同样的事：

```js
const [, setN] = useState(0);
const forceTick = () => setN((n) => n + 1);
```

---

## 3. useMemo：把算过的结果记住

组件每次渲染，函数体会从头跑一遍。下面这种每次都会 `filter` 一次：

```js
const result = list.filter((x) => x.ok);
```

列表很大、而且 `list` 没变时，白算。`useMemo` 说：依赖没变，就用上次的结果。

```js
const result = useMemo(() => {
  return list.filter((x) => x.ok);
}, [list]);
```

第二个参数是依赖数组：里面的值变了才重算。空数组 `[]` 表示只算一次。

适合：过滤、排序、拼复杂对象。不要把所有变量都包一层，简单加减没必要。

---

## 4. useCallback：把函数记住

每次渲染，组件里新写的函数都是一个新函数（内容看起来一样，对 React 来说是新的）。

```js
const onClick = () => {
  console.log(name);
};
```

如果把这个函数传给子组件，子组件会觉得「props 变了」，可能跟着白渲染。`useCallback` 在依赖不变时，给你同一个函数引用。

```js
const onClick = useCallback(() => {
  console.log(name);
}, [name]);
```

`name` 变了，才会换新函数，这样才能读到新的 `name`。

和 `useMemo` 的关系：`useCallback(fn, deps)` 约等于 `useMemo(() => fn, deps)`。一个缓存函数，一个缓存值。

---

## 5. useEffect：渲染之后再做事

界面画完，再去拉接口、加监听、开定时器。这些叫副作用，写在 `useEffect` 里。

```js
useEffect(() => {
  const timer = setInterval(() => {
    console.log('滴答');
  }, 1000);

  return () => clearInterval(timer); // 组件卸掉或下次重跑前，先清理
}, []);
```

依赖数组的含义：

| 写法 | 何时跑 |
|---|---|
| `useEffect(fn)` 不写第二参数 | 每次渲染后都跑 |
| `useEffect(fn, [])` | 只在第一次进来时跑 |
| `useEffect(fn, [id])` | `id` 变了才跑 |

不要在里面直接 `setState` 造成死循环；依赖变一次，就只处理这一次。

---

## 6. useRef：一个盒子，改它不重画

`useRef` 也记住一份数据，但改 `.current` **不会**触发渲染。

```js
const inputRef = useRef(null);
<input ref={inputRef} />
inputRef.current.focus(); // 拿到真实 DOM

const countRef = useRef(0);
countRef.current += 1;    // 记住次数，屏幕不动
```

适合：

- 抓 DOM（输入框聚焦、滚动）
- 记住「最新的值」给定时器、事件用，又不想为此刷新界面
- 跨渲染保住同一个对象（比如某条连接）

对比：`useState` 改了要重画；`useRef` 改了不重画。

---

## 7. useContext：跨很多层传一份数据

不想一层层把 `theme` 当 props 往下递，就把数据放进 Context，下面谁要用谁取。

### 7.1 最短例子

```jsx
const ThemeContext = createContext('light');

function App() {
  return (
    <ThemeContext.Provider value="dark">
      <ThemedButton />
    </ThemeContext.Provider>
  );
}

function ThemedButton() {
  const theme = useContext(ThemeContext);
  const isDark = theme === 'dark';

  return (
    <button
      style={{
        background: isDark ? '#333' : '#eee',
        color: isDark ? '#fff' : '#000',
      }}
    >
      {isDark ? '深色' : '浅色'}
    </button>
  );
}
```

按钮颜色只看 `theme === 'dark'` 是否为真：

- `theme` 是 `'dark'` → 背景 `#333`（深灰），白字，文案「深色」
- `theme` 是 `'light'` → 背景 `#eee`（浅灰），黑字，文案「浅色」

这份例子包了 `value="dark"`，所以一打开是深灰，不是浅灰。

### 7.2 `'light'` 和 `'dark'` 到底听谁的

`createContext('light')` 只是**没人提供时的兜底**。`Provider` 的 `value` 才是树上实际给出去的值。两套不会同时生效。

有 Provider 时，`useContext` 往上找到它，拿到 `'dark'`：

```js
const isDark = theme === 'dark';  // true
```

`createContext('light')` 这轮用不上，不会先让 `isDark` 为 false，再被 App「盖成黑色」。

对照：默认值像「房间没人调灯时用日光灯」；`value="dark"` 是「这间房已经把灯调成深色了」。有人调过，就听调过的那个。

### 7.3 Provider 是不是必须写

不是语法上必须成对。下面这样也能跑：

```jsx
const ThemeContext = createContext('light');

function App() {
  return <ThemedButton />;
}
```

这时树上没有 Provider，`useContext` 落到创建时的 `'light'`，按钮是浅灰。

想**改**这份值（比如改成 `'dark'`，或跟着开关变），才需要包：

```jsx
<ThemeContext.Provider value="dark">
  <ThemedButton />
</ThemeContext.Provider>
```

### 7.4 疑问对照

| 疑问 | 结论 |
|---|---|
| 创建写了 `'light'`，下面又 `value="dark"`，到底是哪个？ | 有 Provider 就听 `value`，这里是 `'dark'`。`'light'` 只是没 Provider 时的默认值。 |
| `isDark = theme === 'dark'` 是不是必然 false？因为创建时是 `'light'` | 不是。`theme` 来自 `useContext`，这份例子里是 `'dark'`，`isDark` 为 true，显示深色。 |
| 不写 Provider 可以吗？ | 可以，会一直用默认 `'light'`，按钮浅灰。没有 Provider 就没法在上面改成 `'dark'`。 |
| 深灰 / 浅灰分别何时出现？ | `theme === 'dark'` → `#333` 深灰；否则 → `#eee` 浅灰。 |

---

## 8. 其他常见的

**useLayoutEffect**  
和 `useEffect` 很像，但它在浏览器真正画到屏幕之前同步跑。用来读 DOM 尺寸、避免闪一下。一般用 `useEffect` 就够。

**useId**  
生成稳定的唯一 id，给输入框和 label 配对，服务端渲染也不会对不齐。

```js
const id = useId();
<label htmlFor={id}>姓名</label>
<input id={id} />
```

**useImperativeHandle**  
配 `forwardRef` 用，让父组件能调子组件暴露出来的方法（比如 `focus()`）。不常用。

**自定义 Hook**  
自己写的 `useXxx`，内部还是组合上面这些。约定：用了 Hook 的函数，名字以 `use` 开头。

```js
function useToggle(initial = false) {
  const [on, setOn] = useState(initial);
  const toggle = useCallback(() => setOn((v) => !v), []);
  return [on, toggle];
}
```

---

## 9. 对照表

| Hook | 记住什么 | 改了会重画吗 | 一句话 |
|---|---|---|---|
| `useState` | 一份值 | 会 | 界面上要变的数据 |
| `useReducer` | 一份值 + 改法 | 会 | 复杂更新，或只当刷新铃铛 |
| `useMemo` | 算出来的结果 | 依赖变了才重算（然后可能重画） | 少做重复计算 |
| `useCallback` | 一个函数 | 依赖变了才换新函数 | 少把新函数传给子组件 |
| `useEffect` | 不记住展示数据 | 本身不存数据 | 渲染后拉接口、加监听 |
| `useRef` | 一个盒子 `.current` | 不会 | 抓 DOM，或存不想触发刷新的值 |
| `useContext` | 树上共享的那份值 | Provider 变了会 | 跨层传数据 |
| `useLayoutEffect` | 同 useEffect | 同左 | 画出来之前先动 DOM |
| `useId` | 一个稳定 id | 基本不变 | 给表单控件对 id |

---

## 10. 怎么选（最短）

- 屏幕上要变 → `useState`（或字段多时 `useReducer`）
- 数据在组件外，只想刷新一下 → `useReducer` / `useState` 当铃铛
- 每次渲染都算得很重 → `useMemo`
- 函数要传给子组件，且子组件怕白渲染 → `useCallback`
- 进来后请求、订阅、定时器 → `useEffect`
- 要 DOM 或要存值但不刷新 → `useRef`
- 很多层都要同一份配置 → `useContext`

规则：Hook 只能写在组件（或自定义 Hook）的顶层，不能写在 `if` / 循环里。每次渲染调用的个数和顺序必须一样，React 才能对得上上一轮记住的数据。
