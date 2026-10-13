# HTTP Referer（来源地址）知识点

对应项目配置：`src/pages/document.ejs`

```html
<meta name="referrer" content="no-referrer-when-downgrade">
```

---

## 1. Referer 是什么

从 A 页面跳到 B（点链接、加载图片/JS、发接口）时，浏览器常会带一个 HTTP **请求头** `Referer`，告诉对方：「这个请求是从哪个网址过来的」。

示例：当前页是：

```text
https://qa.xxx.com/h5/promotionDetail?promotionId=123
```

页内加载腾讯云验证码脚本时，对方服务器可能收到：

```http
Referer: https://qa.xxx.com/h5/promotionDetail?promotionId=123
```

注意：

- 单词历史拼写是 `Referer`（少一个 `r`），请求头就叫这个；
- HTML 中 meta 的属性名是正确拼写 `referrer`（两个 `r`）；
- 这不是页面上显示的文字，普通用户在网页上看不到。

---

## 2. 在哪里能看到

网页正文、Elements 里的 HTML **都没有** Referer。

查看步骤：

1. 打开页面，按 F12；
2. 切到 **Network（网络）**；
3. 刷新页面，点击一条请求，例如外链 JS、图片或接口；
4. 在右侧进入 **Headers → Request Headers（请求标头）**；
5. 找 `Referer`。

对方服务器的访问日志里也能记录 Referer。

不是所有请求都有 Referer：策略禁止时不会带；某些同源请求也可能不带。跨站加载的脚本和图片通常更容易观察到。

---

## 3. 谁控制“带不带、带多少”

本项目写在全站 HTML 模板 `src/pages/document.ejs` 的 `<head>` 中，**不是 Umi 自动生成的**。

修改这一行会影响登录页、后台和所有 H5 页面。

```html
<meta name="referrer" content="no-referrer-when-downgrade">
```

`content` 的常见取值：

| 值 | 行为 |
| --- | --- |
| `no-referrer` | 去哪都不说来源 |
| `origin` | 只带域名，例如 `https://qa.xxx.com`，不含路径和 query |
| `strict-origin-when-cross-origin` | 现代浏览器常见默认：同站带完整 URL，跨站只带域名；HTTPS 跳 HTTP 时不带 |
| `no-referrer-when-downgrade` | 当前项目的值：平时带完整 URL；仅 HTTPS 跳 HTTP 时不带 |
| `unsafe-url` | 总是带完整 URL，query 中的敏感参数也可能泄漏，不推荐 |

“降级（downgrade）”指从加密的 **HTTPS** 跳到明文的 **HTTP**。

因此 `no-referrer-when-downgrade` 的含义是：

> 仅在 HTTPS 跳转到 HTTP 时不带 Referer，不是永远不带。

本站和验证码都使用 HTTPS 时，实际效果接近“正常携带完整来源 URL”。

---

## 4. 暴露与不暴露的优缺点

这里的“暴露”指对方能看到完整来源 URL，可能包含路径和 query 参数。

### 带完整 Referer

**优势：**

- 统计：对方知道流量来自活动页、分享页或搜索页；
- 防盗链：图床/CDN 可校验 Referer，拒绝外站盗用图片；
- 联调和排障：验证码、监控等第三方能对应到具体页面；
- 部分登录或风控策略会使用来源页信息。

**劣势：**

- 路径和 query 可能被第三方看到，例如 `promotionId`、内部路径 `/h5/promotionDetail`；
- 如果 URL 错误地携带 token、手机号等敏感信息，可能通过 Referer 泄漏；
- 嵌在 BPM iframe 中时，外层和内层页面也可能互相暴露地址。

### 完全不带 Referer

例如使用：

```html
<meta name="referrer" content="no-referrer">
```

**优势：**

- 第三方拿不到具体页面地址和参数，隐私更好；
- 降低 URL 中意外携带敏感 query 参数时的泄漏风险。

**劣势：**

- 统计、防盗链和部分第三方排障能力可能变差或失效；
- 极少数依赖 Referer 的接口可能异常。

### 折中策略

使用 `origin` 或 `strict-origin-when-cross-origin`：

- 对方只知道“请求来自哪个域名”，不知道具体功能页；
- 同站跳转仍可带完整 URL，方便站内统计；
- 跨站时不会把 `promotionId` 等细节发送给外部；
- 通常比“全部暴露”和“完全不带”更均衡。

---

## 5. 和本项目的关系

- 审批 H5、置顶滚动、商品包逻辑 **都不依赖** 这条 meta；
- 它只影响浏览器向外发送请求时，是否把当前页面网址告诉对方；
- 如果需要收紧策略，可以改为 `origin` 或 `strict-origin-when-cross-origin`；
- 这是全站配置，修改前需要评估统计、防盗链和验证码是否依赖完整 Referer；
- 不要使用 `unsafe-url`。

## 6. 一句话记住

> Referer 是 HTTP 请求头中的“我从哪来”，在 Network 中查看，不在页面上显示。带完整地址便于统计和防盗链，但可能泄露路径和参数；不带更安全，但第三方能力会变弱。当前 `no-referrer-when-downgrade` 表示：平时带完整来源，只有 HTTPS 跳 HTTP 时隐藏来源。
