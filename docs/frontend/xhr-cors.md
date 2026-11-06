# XHR、同源与跨域（CORS）

来源：`agentic-mall` 小程序发票详情「下载 PDF」。H5 用 `Taro.downloadFile` 拉 COS 上的文件会失败，改成 `window.open(pdfUrl)` 后能预览。

文件：

- `frontend-group/vibe-miniprogram-fe/src/subpackages/invoice/pages/detail/index.tsx`（`handleDownloadPdf`）
- `frontend-group/vibe-miniprogram-fe/src/services/invoice/api.ts`（`getInvoicePdf`）
- `frontend-group/vibe-miniprogram-fe/src/services/request.ts`（`Taro.request`）
- `frontend-group/vibe-miniprogram-fe/config/dev.ts`（H5 `devServer`：`port: 10086`，`host: '0.0.0.0'`）

Network 头怎么看，见 [Chrome Network 中的 HTTP 请求与响应头](./http-network-headers.md)。`fetch` / Axios 怎么选，见 [Fetch 与 Axios](./fetch-vs-axios.md)。

## 先记三句

1. XHR / `fetch` / H5 的 `downloadFile` = 页面里的 **JS 要读别人家的响应** → 要过 CORS。
2. 打开链接、换地址、`<a href>`、`<img src>` = **浏览器自己去** → 通常不要 CORS。
3. 小程序 `wx.downloadFile` 是客户端能力，不受网页同源策略管。

---

## 发票下载其实是两步

点「下载 PDF」不是一次请求。

| 步骤 | 干什么 | 请求打到哪 | H5 现在用什么 |
| --- | --- | --- | --- |
| 第一步 | 拿地址 | 自己的 `GET /app/trade/invoice/pdf` | `getInvoicePdf` → `Taro.request`（底层是 XHR） |
| 第二步 | 拿文件 | COS：`https://xxx.cos....myqcloud.com/invoice.pdf` | **`window.open(url)`，不再用 `downloadFile`** |

第一步同源（H5 走 `localhost:10086` 的 devServer 代理），没问题。第二步文件在 COS 上，和页面不是一个源。跨域卡在第二步。

`getInvoicePdf(id)` **不是读 PDF 文件**。它只调自己的接口，JS 读到的是 JSON 里的 `pdfUrl` 字符串。这一步不要 COS 的 CORS。

| 代码 | JS 读到什么 | 要不要 COS 的 CORS |
| --- | --- | --- |
| `getInvoicePdf(id)` | JSON 里的 `pdfUrl` | 不要 |
| H5 `Taro.downloadFile({ url })` | PDF 文件内容 | **要**（会被拦） |
| H5 `window.open(url)` | 什么都不读，浏览器自己打开 | 不要 |
| 小程序 `Taro.downloadFile({ url })` | 微信客户端拉文件 | 不要 |

小程序第二步仍是 `Taro.downloadFile` + `Taro.openDocument`。不加 `window.open` 那段，H5 会继续往下走 `Taro.downloadFile`，被编成 XHR 去读 COS 上的字节，就会失败。

---

## 源（origin）= 协议 + 域名 + 端口

三个有一个不同，就是跨源。比的是 **当前页面的 URL** 和 **这条请求的 URL**，不是整个地址都要一样。**路径、query 不算源**，`/pages/invoice/detail` 和 `/app/trade/invoice/pdf` 路径不同，只要上面三个一样，就是同源。

```text
http://localhost:10086/pages/invoice/detail
│     │          │
协议   域名        端口
```

```text
https://xxx.cos.ap-xxx.myqcloud.com/invoice.pdf
│      │                             │
协议    域名                          路径（不算源）
```

### 没写端口不等于没有端口

地址栏不写端口时，浏览器用默认端口：

| 协议 | 没写端口时实际用的 |
| --- | ---: |
| `http://` | **80** |
| `https://` | **443** |

所以 COS 那条实际是：`https` + `xxx.cos....myqcloud.com` + **443**。

`http://localhost:10086` 拆开：

- 协议：`http`
- 域名：`localhost`
- 端口：**写出来了，就是 10086**

和 COS 比：协议、域名、端口三个全不同，所以跨源。哪怕以后做成 `https://localhost:10086` 去下 `https://xxx.cos..../invoice.pdf`，域名还是不同，照样跨源。

---

## `localhost:10086` 和 `10.7.135.61:10086` 不是两个页面

本地 H5 配置：

```ts
devServer: {
  port: 10086,
  host: '0.0.0.0',
}
```

`host: '0.0.0.0'` 表示 devServer 绑在**本机所有网卡**上。同一套服务，两个入口：

| 你输入的 | 实际指谁 | 谁能打开 |
| --- | --- | --- |
| `http://localhost:10086/` | 本机回环（`127.0.0.1`） | 只有这台电脑的浏览器 |
| `http://10.7.135.61:10086/` | 这台电脑当前的局域网 IP | 同一 Wi-Fi 下的手机、别的电脑也能开 |

`10.7.135.61` **没有写在项目里**，是电脑此刻的内网 IP，换网络会变。`localhost` 永远指「我自己」。

本机 Chrome 平时用的就是 **`http://localhost:10086/`**。地址栏写哪个，页面 origin 就是哪个。

这两条**不是同一个源**：协议、端口一样，**域名不同**。Cookie、localStorage、CORS 的 `Origin` 都按地址栏算。在 `localhost` 登录过，换 IP 打开等于换了一个站点。

---

## XHR 是什么，和 fetch / downloadFile 什么关系

**XHR** = `XMLHttpRequest`：页面里的 JS 发 HTTP，把响应读进内存。`fetch` 是同一类事的新写法。

业务接口一直在用。`/app/member-info/assets`、`getInvoicePdf` 都是 `Taro.request`，H5 落到浏览器里就是 XHR 或 fetch。Network 里 Type 常见是 `xhr` / `fetch`，`Content-Type: application/json` 说明返回的是 JSON。

封装：

```ts
const res = await Taro.request({
  url: fullUrl,   // 例如 /app/member-info/assets
  method: 'GET',
  data,
  header,
})
```

H5 等价于：

```js
const xhr = new XMLHttpRequest()
xhr.open('GET', 'http://localhost:10086/app/member-info/assets')
xhr.setRequestHeader('Authorization', '...')
xhr.onload = () => {
  const json = JSON.parse(xhr.responseText)
}
xhr.send()
```

`fetch` 同一件事：

```js
const res = await fetch('http://localhost:10086/app/member-info/assets')
const json = await res.json()
```

`fetch` 不是「新版就可以跨域」的通行证，只是写法新。CORS 也不看正文是 XML 还是 JSON：两种 API、两种格式，跨源要读 body 都得对方回 `Access-Control-Allow-Origin`。

### `Taro.downloadFile` 一端一名，两端实现不同

`Taro.downloadFile` 不是微信独有的。Taro 只是套了一层，编译到不同端换成不同实现：

| 运行环境 | `Taro.downloadFile` 实际变成 | 谁去拉文件 | 要不要 CORS |
| --- | --- | --- | --- |
| 小程序 `weapp` | `wx.downloadFile` | 微信客户端 | **不要** |
| H5 | `@tarojs/taro-h5` 里的 `XMLHttpRequest` | 页面里的 JS | **要** |

所以「小程序 `wx.downloadFile` 不要 CORS」和「H5 `Taro.downloadFile` 用 XHR、要 CORS」说的是**同一个方法名的两条实现**。发票详情现在用 `TARO_ENV === 'h5'` 拆开，H5 走 `window.open`，小程序才进 `Taro.downloadFile`。

**H5 的 `downloadFile` 才是 XHR**，把响应当文件，不要 JSON：

```js
xhr.open('GET', 'https://xxx.cos..../invoice.pdf')
xhr.withCredentials = true
xhr.responseType = 'blob'   // 当文件，不要当文本
xhr.send()
```

成功后 Taro H5 会 `URL.createObjectURL(blob)`，变成一个本地预览地址。只要「JS 要读这份响应」，CORS 就要过。

Taro 默认 `withCredentials: true`。跨域还要带 Cookie 时更严：`Access-Control-Allow-Origin` 不能是 `*`，还要 `Access-Control-Allow-Credentials: true`。跨域 COS 更容易死。

### 现在到底有没有用

| 场景 | 用没用 |
| --- | --- |
| 调自己的 `/app/...`（详情、会员资产、拿 `pdfUrl`） | **一直在用 XHR**（`Taro.request`） |
| H5 去拉 COS 上的 PDF 文件 | **现在不用 `downloadFile` 了**，改 `window.open` |
| 小程序拉 PDF | 还在用 `Taro.downloadFile` |

---

## XML 是什么，和 XHR 名字的关系

**XML** 是一种用标签包数据的文本格式（Extensible Markup Language）。长得像 HTML，但不是给页面看的，是给程序传结构化数据的。HTML 标签规定好了（`div`、`p`）；XML 标签可以自己起名。

早期接口经常直接返回 XML。同一张发票，XML 可能是：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<response>
  <code>0</code>
  <data>
    <pdfUrl>https://xxx.cos.ap-xxx.myqcloud.com/invoice.pdf</pdfUrl>
    <ofdUrl></ofdUrl>
  </data>
</response>
```

现在同一份数据是 JSON：

```json
{
  "code": 0,
  "data": {
    "pdfUrl": "https://xxx.cos.ap-xxx.myqcloud.com/invoice.pdf",
    "ofdUrl": ""
  }
}
```

XHR 两种都能拉：

```js
xhr.responseType = 'text'
xhr.onload = () => {
  // JSON：JSON.parse(xhr.responseText)
  // XML：xhr.responseXML 会得到一棵标签树
}
```

`XMLHttpRequest` 名字来自「当年用它拉 XML」。后来什么都能拉：JSON、PDF、图片、纯文本。发票接口 `Content-Type: application/json`，**没有在用 XML**。今天说 XHR，通常就是「页面里的 JS 发 HTTP」。

---

## CORS：Origin 和 Access-Control-Allow-Origin 不是同一个东西

浏览器默认：**页面上的 JS 不能随便读另一个源的响应**。这叫同源策略。防的是 A 站脚本去读 B 站（网银、Cookie）的数据。

跨域本身**一直可以发生**。浏览器拦的不是「能不能发请求」，而是 **「页面里的 JS 能不能读这份响应」**。Network 里可能仍是 200，控制台是 `blocked by CORS policy`。

| 谁在用这份数据 | 跨源行不行 | 要不要服务器开 CORS |
| --- | --- | --- |
| 地址栏、`window.open`、点 `<a href>` 打开 | 行 | 不要 |
| `<img src>` 显示图片、`<script src>` 加载脚本 | 行（只显示/执行，JS 读不了内容） | 不要 |
| 页面 JS：`Taro.request` / `fetch` / XHR / H5 `downloadFile` | 请求能发出去 | **要读 body 就必须 CORS 过** |
| 小程序 `wx.downloadFile` | 行 | 不要（不是浏览器那套） |

**CORS** 是服务器给浏览器的通行证。一对问答：

| 头 | 谁加的 | 意思 |
| --- | --- | --- |
| `Origin` | **浏览器自动加**，页面改不了 | 「我是谁」：当前页面的源 |
| `Access-Control-Allow-Origin` | **服务器返回** | 「我允许谁读这份响应」 |

`Origin` 是一个**真的请求头**，在 **Request Headers** 里，不在 Response。同一条请求里配对看：

1. Network 点开那条请求
2. Headers → **Request Headers** 找 `Origin`
3. **Response Headers** 找 `Access-Control-Allow-Origin`

页面改不了 `Origin`。代码里 `setRequestHeader('Origin', ...)` 浏览器会忽略或丢掉。

跨源的 XHR / fetch，浏览器会带：

```http
Origin: http://localhost:10086
```

**同源 GET 常常没有这一行。** Chrome 觉得没必要声明「我是谁」。发票详情里 `GET /app/trade/invoice/pdf` 就是这样：`Host`、`Referer` 都是 `localhost:10086`，`Sec-Fetch-Site: same-origin`，Request Headers 里找不到 `Origin` 是正常的。

`Sec-Fetch-Mode: cors` 只表示这是 XHR/fetch 这种「要读响应」的请求，**不等于跨域**。跨不跨看 `Sec-Fetch-Site`：

| `Sec-Fetch-Site` | 会不会有 `Origin` |
| --- | --- |
| `same-origin` | 常常没有 |
| `cross-site` | **一定有** `Origin: http://localhost:10086` |

现在 H5 改成 `window.open` 后，去拉 COS 的那条 XHR 根本不会发，Network 里也就没有带 `Origin` 的 COS 请求。新标签是导航，常见是 `Referer`，同样常常没有 `Origin`。

服务器如果同意，就回：

```http
Access-Control-Allow-Origin: http://localhost:10086
```

或：

```http
Access-Control-Allow-Origin: *
```

`*` 表示「任何网站的 JS 都可以读这份响应」。

浏览器拿这两个比对：允许的值是当前 `Origin`，或 `*`，JS 才能读 body。对不上就拦。Network 里可能仍是 200，控制台是 `blocked by CORS policy`。

本地 `/app/member-info/assets` 常见也能看到 `Access-Control-Allow-Origin: *`（网关如 TencentEdgeOne 顺手带的）。这条 Request URL 已经是 `localhost:10086`，和页面**同源**，CORS 本来就不限制，这个 `*` 不是它能成功的原因。

真正会卡的是第二步：页面还是 `localhost:10086`，去 XHR `https://xxx.cos..../invoice.pdf`。那时 `Origin` 仍是 `http://localhost:10086`，COS 响应里经常**没有** `Access-Control-Allow-Origin`，就会被拦。

业务接口看起来也跨域（交易中心 8104），但 `config/dev.ts` 把 `/app` 代理到后端，浏览器以为还是 `localhost:10086`，所以第一步不跨域。代理只挡自己的 API，**挡不到 COS 上的文件 URL**。

复杂请求（自定义头、`Content-Type: application/json`、非 GET/POST）会先发一个 **OPTIONS 预检**。预检失败，真正的 GET 都不会发。`downloadFile` 这个 GET blob 一般是「简单请求」，不一定有预检，但响应头不对照样读不到 body。

---

## 「JS 读文件」读的不是 URL

这里的「JS」就是发票详情页上那段代码。URL 早就拿到了：

```js
pdfUrl = 'https://xxx.cos..../invoice.pdf'
```

这只是「文件在哪」的门牌号。字符串本身很小，也不跨域（是你们自己的接口给的）。

以前 H5 还想再做一步：按这个地址把 **PDF 的字节下载进页面内存**：

```ts
const dl = await Taro.downloadFile({ url: pdfUrl })
await Taro.openDocument({ filePath: dl.tempFilePath, fileType: 'pdf' })
```

这时 JS 要读的不是 URL，是 **URL 背后那份文件内容**。CORS 拦的就是这一步。

现在 H5：不搬文件，只把地址交给浏览器。

```ts
if (process.env.TARO_ENV === 'h5') {
  const opened = window.open(url, '_blank')
  if (!opened) window.location.href = url
  return
}
```

脚本只告诉浏览器「去打开这个链接」。新标签里由浏览器自己请求 PDF、自己预览或下载。页面里的 JS **既不下载、也不读取文件内容**。

| 做法 | 手里有什么 | 还要不要文件内容 |
| --- | --- | --- |
| 以前 `downloadFile` | 有 URL | 要，JS 把 PDF 拉进内存 |
| 现在 `window.open` | 有 URL | 不要，浏览器自己打开这个 URL |

不是「JS 读 URL」，是「JS 曾经想按这个 URL 把文件读进来」。现在绕开的是读文件，不是绕开 URL。URL 一直都有，现在直接拿去打开 / 下载。

---

## 谁在读文件，要不要 CORS

| 做法 | 谁在读文件 | 要不要 CORS |
| --- | --- | --- |
| `downloadFile` / XHR / `fetch` | 页面里的 JS | 要 |
| `window.open` / `<a href>` / 地址栏 | 浏览器自己 | 不要 |
| 小程序 `wx.downloadFile` | 微信客户端 | 不要 |

小程序下载不是浏览器 XHR，是客户端自己去拉文件，下完给临时路径，`openDocument` 用微信自带阅读器打开。H5 没有这个能力，`openDocument` 也不支持。所以同一段 `downloadFile` + `openDocument`：小程序成功；H5 在 `downloadFile` 就被 CORS 打进 `catch`，于是出现「链接已复制，请在浏览器打开」。

---

## 怎么在 DevTools 里看

1. Network 里点那条请求。看的是**页面 URL** 和 **这条 Request URL** 的协议 / 域名 / 端口，不是整段路径。
2. 先看 `Sec-Fetch-Site`：`same-origin` 就是同源；`cross-site` 才是跨源。
3. 跨源 XHR / fetch：Request Headers 会有 `Origin: http://localhost:10086`。同源 GET 常常没有，别在 `pdf?id=...` 这种自己的接口上死找。
4. Response Headers 看有没有 `Access-Control-Allow-Origin`。没有，或对不上当前页面源 → JS 读响应会被拦。
5. 同源请求（页面和 Request URL 都是 `localhost:10086`）本来就不受 CORS 限制，即使看到 `*` 也不说明这条跨过了跨域检查。
6. 交易中心真实地址可能是 `:8104`，但 H5 开发时 Network 里看到的是 `localhost:10086/app/...`。浏览器只认最终出现的 URL，代理把接口伪装成和页面同源了。代理挡不到 COS 上的文件 URL。

---

## 当时问过的问题

**`http://localhost:10086` 域名是 localhost、端口是 10086 吧？COS 那条是不是没有端口只有域名？**

对，localhost + 10086。COS 地址栏没写端口，实际端口是 HTTPS 默认 **443**。路径 `/invoice.pdf` 不算源。

**`http://10.7.135.61:10086/` 和 `http://localhost:10086/` 区别？本地启动 H5 到底是哪一个？**

同一套服务，不是两个页面。本机浏览器一般开 localhost。局域网 IP 给手机预览用。域名不同，所以是两个 origin。

**XML 是什么？接口返回数据是 XML 吗？**

XML 是一种标签格式的数据，早期接口常用。你们现在返回 JSON。XHR 名字里的 XML 是历史原因。

**downloadFile / XHR / fetch 目前没有用到吧？能举个例子吗？**

调自己的 `/app/...` 一直在用（`Taro.request` → XHR）。H5 拉 COS PDF 现在不用 `downloadFile`。例子见上文 `Taro.request` / `XMLHttpRequest` / `fetch` / `downloadFile` 四段。

**Origin 是什么，和 Access-Control-Allow-Origin 是一个东西吗？本地看到 `*` 怎么理解？**

不是同一个东西。`Origin` 是请求头，浏览器说「我是谁」；`Access-Control-Allow-Origin` 是响应头，服务器说「允许谁读」。本地那条 `/app/member-info/assets` 的 `*` 是网关带的，请求已经同源，不是靠这个 `*` 才成功。

**JS 是什么？要读文件读的是 URL 吗？现在是绕过 JS 读文件、直接打开 URL？**

JS = 发票页那段脚本。读的不是 URL 字符串（URL 早就有），是 URL 指向的 PDF 字节。现在 `window.open` 把地址交给浏览器，脚本不再把文件拉进内存。

**`getInvoicePdf` 是不是 JS 读文件、需要 CORS？**

不是。它只拿 JSON 里的地址，打的是自己的 `/app/...`。要 CORS 的是后面 H5 的 `Taro.downloadFile` 去拉 COS 文件。

**`Taro.downloadFile` 不是小程序原生吗？小程序不是不要 CORS 吗？**

同一个 Taro 方法名，两端实现不同。小程序才是 `wx.downloadFile`；H5 是 XHR。现在代码里 H5 已经 `return` 掉了，后面那行 `Taro.downloadFile` 只跑在小程序。

**不加 `window.open`，H5 是不是就会走 `Taro.downloadFile`，跨域读不到？跨域什么时候可以、什么时候不可以？**

对。不加就会编成 XHR 去读 COS 字节。跨域不是一律禁止：浏览器自己打开 / 显示可以；页面 JS 要把结果拿回来用，必须对方回 `Access-Control-Allow-Origin`。

**XHR 是旧版读 body，新版是 JSON，那 JSON 跨域可以吗？**

不是这个关系。旧的是 XHR 这个 API，新的是 `fetch`；JSON 只是 body 的形状。`fetch` 跨域拉 JSON，没 CORS 头照样读不到。

**Origin 真的能在 Request Headers 里看到吗？我这条 `pdf?id=...` 为什么没有？**

能看到，但只在跨源的 XHR / fetch 上常出现。你这条是同源 GET，`Sec-Fetch-Site: same-origin`，Chrome 经常不带 `Origin`。

**同源跨域是页面 URL 和接口 URL 对不上吧？**

对，但只对协议 / 域名 / 端口。路径不同仍可同源。Network 里 `/app/...` 已经是代理后的 `localhost:10086`，所以第一步算同源。
