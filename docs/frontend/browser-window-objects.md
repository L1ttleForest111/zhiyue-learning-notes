---
title: 浏览器页面上的 window
type: topic
source_project: agentic-mall
source_files:
  - frontend-group/vibe-miniprogram-fe/src/utils/getLocationCompat.ts
source_commit: de5eafbcbb
plan_tasks: []
status: reviewed
tags: [browser, window, document, navigator, location, console, geolocation]
created_at: 2026-11-11
updated_at: 2026-11-11
---

# 浏览器页面上的 window

## 一句话结论

一页网页最外层有一个对象叫 `window`。浏览器事先往上面放了四样东西：`document`（正在显示的标签树）、`navigator`（浏览器信息，定位在这里）、`location`（地址栏）、`console`（日志）。代码里写 `navigator`，等于写 `window.navigator`，前面的 `window.` 可以省掉。

这些都不用 `npm install`。代码调用的是浏览器自己的对象，比如 `navigator.geolocation`。

MDN（`developer.mozilla.org`）是一个说明网站，不是浏览器自带的工具，也不是要安装或调用的库。Mozilla 在维护它，Chrome、Safari、Edge 的接口也写在上面。页面底部有一张兼容表，写着哪个浏览器、从哪个版本开始有这个接口。表上没有的，就不能当所有浏览器都有。

## 和开发者工具怎么对上

| 你在工具里看到的 | 对应的对象 | 它是什么 |
| --- | --- | --- |
| Elements 面板那棵标签树 | `document` | 页面上正在显示的 HTML |
| 地址栏 | `location` | 当前网址 |
| Console 里输入 `navigator` 能展开 | `navigator` | 浏览器自己的信息 |
| Console 面板里的打印 | `console` | 浏览器提供的日志对象 |

Elements 里是标签。`navigator`、`location`、`console` 不在那棵树上，所以 Elements 里找不到它们。打开 Console，把名字打进去回车，看的就是 `window` 上的这些对象。

Console 有两层，不要混成一件事：

- **`console` 对象**：浏览器提供的，页面代码可以调用，比如 `console.log`。
- **Console 面板**：浏览器给你看输出、顺便当命令行用的窗口。你在里面输入 `navigator` 能看到结果，是因为这个面板在执行当前页面的 JavaScript。

## document：看得见的那一块

`document` 不是你写的源码。源码是仓库里的 `.tsx`、`.html`，存在文件里。浏览器读完源码，在页面上搭出标题、按钮、列表。这棵正在显示的标签树才叫 `document`。Elements 面板看的就是它，所以它最直观。

源码改了要重新编译、刷新，`document` 才会变。运行中用脚本改 `document`，Elements 会立刻跟着变，源码文件不会改。

常用的就这几个：

```js
document.title              // 标签页标题
document.querySelector('.store-list-page__item')  // 找第一个匹配的标签
document.querySelectorAll('button')               // 找全部
document.getElementById('app')
```

`querySelector` 用的就是 CSS 选择器。选不中时返回 `null`，不是空数组。

## navigator：浏览器信息，定位在这里

`navigator` 描述的是**这个浏览器**，不是当前这一个页面。换个网址，`navigator.userAgent` 还是同一串。

和这次定位直接相关的是：

```js
navigator.geolocation.getCurrentPosition(
  (pos) => console.log(pos.coords.latitude, pos.coords.longitude),
  (err) => console.log(err.message),
)
```

这就是 `getLocationCompat.ts` 里 H5 分支在调的接口。浏览器自带，不用装包。它还有两道限制：页面得是安全来源（https、localhost、127.0.0.1），并且用户得点允许。局域网 `http://10.x` 会被直接拒绝。安全来源见 [HTTP / HTTPS 面试考点](./http-https-interview.md)，两端定位差异见 [小程序和 H5 怎么拿定位](./weapp-h5-geolocation.md)。

`navigator` 是一个袋子，里面装的是**这个浏览器的能力和环境**，不是当前页面的数据。换一个网址，`userAgent` 还是同一串。Chrome、Safari、微信内置浏览器各自带的字段不完全一样。Console 里输入 `navigator` 回车，展开看到的就是**你现在这个浏览器**实际有的全部。下面是日常会碰到的。

### 打开就能读的

不用弹授权框。

| 字段 | 看什么 | 注意 |
| --- | --- | --- |
| `userAgent` | 一长串自报：系统、浏览器名 | 只适合打日志。不要靠拆这串字符串决定功能开不开，字段会过时，也会被改 |
| `language` | 首选语言，如 `zh-CN` | 做文案语言时用它 |
| `languages` | 语言列表，按用户排序 | 比单个 `language` 全 |
| `onLine` | 浏览器觉得网卡是否连着 | `true` 只表示「好像有网」。接口仍可能超时，不能拿它代替请求失败处理 |
| `cookieEnabled` | 浏览器是否允许 cookie | 用户在设置里关掉时是 `false` |
| `hardwareConcurrency` | CPU 逻辑核心数 | 用来估并发，不是精确型号 |
| `maxTouchPoints` | 触点数量 | `0` 多半是桌面鼠标环境，大于 `0` 说明有触摸屏 |

`platform` 也能读到系统名，已经不推荐继续拿它做判断。

### 定位

都在 `navigator.geolocation` 上。这个对象不存在，就是当前环境没有定位。

| 方法 | 干什么 |
| --- | --- |
| `getCurrentPosition(成功, 失败, 选项)` | 要一次当前位置。门店列表用的就是这个 |
| `watchPosition(成功, 失败, 选项)` | 位置变了再回调，返回一个 id |
| `clearWatch(id)` | 停掉上面的持续监听 |

选项里常见三个：`enableHighAccuracy`（尽量用 GPS）、`timeout`（最多等多少毫秒）、`maximumAge`（允许多久以内的缓存，`0` 表示要新的）。

### 要用之前先问用户

这些能力浏览器有，但调用时会要权限，并且多数要求安全来源。

| 对象 | 常用方法 | 干什么 |
| --- | --- | --- |
| `clipboard` | `writeText` / `readText` | 写进剪贴板、从剪贴板读文字。写通常要点一下按钮才允许 |
| `mediaDevices` | `getUserMedia({ video: true })` | 要摄像头或麦克风 |
| `permissions` | `query({ name: 'geolocation' })` | 问某项权限现在是允许、拒绝，还是还没问过。查询本身不弹框 |
| `share` | `share({ title, url })` | 调起系统分享。手机浏览器多见，没有这个函数就不要调用 |

调用前先判断函数在不在：

```js
if (navigator.share) {
  navigator.share({ title: document.title, url: location.href })
}
```

### 和页面走完有关的

| 字段 | 常用方法 | 干什么 |
| --- | --- | --- |
| `serviceWorker` | `register('/sw.js')` | 注册 Service Worker，做离线缓存、消息推送。普通业务页不碰它 |
| `sendBeacon` | `sendBeacon(url, data)` | 页面要关掉时补发一条统计。浏览器会尽量送出，不像普通请求那样容易被取消 |

小程序逻辑层没有这个 `navigator`。Node 脚本里也没有。所以这段只能跑在浏览器页面里。

## location：地址栏

`location` 就是地址栏的拆开版。

假设现在是 `http://localhost:10086/pages/store/list?from=appointment`：

| 字段 | 值 |
| --- | --- |
| `location.href` | 整段地址 |
| `location.protocol` | `http:` |
| `location.host` | `localhost:10086` |
| `location.pathname` | `/pages/store/list` |
| `location.search` | `?from=appointment` |

给 `location.href` 赋一个新地址，页面会跳走。只是想读，就读字段，不要赋值。

## console：浏览器提供的日志

`console` 是浏览器挂上的。代码里调用它，输出出现在 Console 面板。用户正常使用页面时看不见这些输出，只有打开开发者工具才看得到。

级别从轻到重，面板可以按级别过滤：

| 方法 | 面板里的样子 | 用来干什么 |
| --- | --- | --- |
| `console.debug` | 调试，默认常被折叠 | 临时细日志 |
| `console.log` | 普通 | 看一个值 |
| `console.info` | 信息 | 和 log 接近，语义是「通知」 |
| `console.warn` | 黄色 | 不正常，但程序还在走 |
| `console.error` | 红色 | 失败。也用来打印捕获到的异常 |

第一个参数是说明，后面可以继续跟值。对象不要拼进字符串，否则会变成 `[object Object]`。

```js
console.log('门店', store)
console.warn('没有坐标，不算距离', store.storeName)
console.error('定位失败', err)
```

### 看清楚一个对象或一张表

`console.log` 打印对象时，面板里往往要再点开。这两下更直接：

```js
console.dir(navigator.geolocation)   // 按属性展开对象
console.table([                       // 数组套对象时排成表
  { name: '广州店', km: 998.5 },
  { name: '石家庄店', km: 1188.1 },
])
```

`console.table` 适合「多条结构一样的数据」。单个对象用 `console.dir`。

### 把一段日志收成一组

一次定位会打好几行。用分组折起来，面板不会刷成一长条：

```js
console.group('getLocation')
console.log('secure', location.protocol)
console.log('coords', latitude, longitude)
console.groupEnd()
```

`console.groupCollapsed('getLocation')` 是同一件事，只是默认折着。

### 看一段代码花了多久

```js
console.time('locate')
navigator.geolocation.getCurrentPosition(() => {
  console.timeEnd('locate')   // 面板打印 locate: 320.1ms 这种
})
```

名字要成对。`time` 和 `timeEnd` 的字符串不一致，就对不上。

### 数一件事发生了几次

```js
console.count('render')      // render: 1
console.count('render')      // render: 2
console.countReset('render') // 清零
```

用来确认一个函数是不是被调用得比预想多，比如列表多渲染了一轮。

### 条件不成立才喊

```js
console.assert(store.latitude != null, '这家店没有纬度', store.storeName)
```

条件为真时什么都不印。条件为假时按错误打出来。断言失败**不会**把程序停住，后面的代码继续走。

### 想知道是谁调用到这里

```js
console.trace('进了 formatDistance')
```

会打印调用栈：哪个函数调用了哪个函数，最后进到这一行。比只打一个值更容易看出重复调用是从哪条链路来的。

### 清屏

```js
console.clear()
```

只清面板里已经打印的内容，不重启页面，也不撤销程序。面板左上角的清空按钮做的是同一件事。

### 占位符

不想字符串拼接时可以用：

| 占位符 | 塞进去的类型 |
| --- | --- |
| `%s` | 字符串 |
| `%d` | 数字 |
| `%o` | 对象，可点开 |
| `%c` | 后面一段文字用 CSS 上色，调试时少用 |

```js
console.log('%s 距离 %d m', store.storeName, store.distance)
```

## 在 Console 里可以直接试的几句

下面这些打进 Console 回车即可，不用改代码：

```js
navigator
navigator.userAgent
navigator.language
navigator.onLine
location.href
document.title
```

Elements 里点中一个标签后，Console 里的 `$0` 就是这个标签，属于面板的快捷方式，不是 JS 标准里的变量。

## 和这次代码的关系

`getLocationCompat.ts` 的 H5 分支先看 `navigator.geolocation` 在不在，再调用 `getCurrentPosition`。成功时从 `pos.coords` 拿经纬度。这整段依赖的是浏览器挂在 `window` 上的对象，不是第三方定位 SDK。
