---
title: 小程序和 H5 怎么拿定位
type: topic
source_project: agentic-mall
source_files:
  - frontend-group/vibe-miniprogram-fe/src/utils/getLocationCompat.ts
  - frontend-group/vibe-miniprogram-fe/src/utils/openLocationCompat.ts
  - frontend-group/vibe-miniprogram-fe/src/subpackages/store/pages/list/hooks.ts
source_commit: ""
plan_tasks: []
status: reviewed
tags: [weapp, h5, geolocation, gcj02, tencent-map]
created_at: 2026-11-06
updated_at: 2026-11-06
---

# 小程序和 H5 怎么拿定位

定位是问**手机 / 系统**要当前经纬度，不是问业务后端。后端只负责：拿到点之后算附近门店、配不配送。

两端问的对象不一样：

| | 小程序 | 普通 H5 |
|---|---|---|
| 问谁 | 微信（微信再问系统定位） | 浏览器 |
| 官方 API | `wx.getLocation` | `navigator.geolocation.getCurrentPosition` |
| 坐标系 | 国内用 `gcj02`（国测局） | 一般是 `WGS84`（GPS 原始） |
| 授权弹窗 | 微信位置权限；2023 年后还要先过隐私协议 | 浏览器「允许获取位置」 |
| 安全限制 | 要在小程序后台配位置权限 | 必须是 https 或 localhost |

Taro 的 `Taro.getLocation` 只是包了一层：小程序里等于 `wx.getLocation`；H5 里只有页面注入了微信 JS-SDK 的 `wx.getLocation` 才可用，**普通浏览器会直接失败**。跨端项目不要默认「调 Taro 两端都能拿到点」。

官方文档：

- 小程序：[wx.getLocation](https://developers.weixin.qq.com/miniprogram/dev/api/location/wx.getLocation.html)
- 小程序权限：[地理位置](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/authorize.html)
- 浏览器：[Geolocation](https://developer.mozilla.org/zh-CN/docs/Web/API/Geolocation_API)
- 微信网页：[JS-SDK getLocation](https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#64)

---

## 0. 给其他项目的 AI：能不能直接用

**不能把 vibe-mall 那两个文件整份复制进别的仓库。** 能直接用的是本篇的判定、坐标系、URL 形态和下面的可移植片段。工程拆分方式、请求头缓存、referer 名字、空态文案都要换成目标项目自己的。

本文件在「知页-学习笔记」仓库。其他项目的 AI **默认读不到**。要用时至少做一件：

1. 对话里 `@` 本文件；或
2. 把本文件拷到目标仓库 `docs/`；或
3. 在目标仓库的 AGENTS.md / Skill 里写：涉及定位、附近门店、`getLocation`、`openLocation`、H5 打不开地图时，先读这篇。

### 何时按这篇做

用户或代码出现这些就加载本篇：定位、附近门店、距离、`getLocation`、`openLocation`、H5 没有位置、请在小程序设置中开启、导航跑到错误城市、腾讯地图 URI。

先分清两件独立的事：

| 要做什么 | 用哪套 |
|---|---|
| 拿「我在哪」 | 小程序 `wx.getLocation`；普通 H5 `navigator.geolocation` |
| 打开地图看店 / 导航 | 小程序 `wx.openLocation`；H5 腾讯 **地点标注** URI（不要路线规划） |

不要做成同一个函数硬撑两端。

### 落地顺序（按目标仓库改，不要照抄路径）

1. 用编译期常量拆小程序 / H5（Taro 用 `TARO_ENV`，别的栈用自己的）。小程序分支保持原来的 `getLocation({ type: 'gcj02' })`，不要改行为。
2. H5 不要调用会找 `window.wx.getLocation` 的封装（含 Taro H5 默认 `getLocation`）。先看当前页有没有这个函数；没有或调用失败，走浏览器定位。
3. 浏览器点是 WGS84，给国内门店 / 国内地图前转 gcj02。小程序已经要了 gcj02，不要再转。
4. 每次进入强依赖位置的页面再问一次。内存点可以写进请求头，**不要**用来短路下一次定位。
5. 失败文案按端写：小程序去设置里开位置；H5 允许浏览器获取位置。
6. H5 看店用 `apis.map.qq.com/uri/v1/marker`，`coord` 为 `纬度,经度`。不要用 `routeplan`。
7. 只改调用点。首页定店、地址栏、订单详情若仍裸调原 API，不会自动变好。

### `window.wx.getLocation` 怎么看（不是缓存）

这是**当前这个标签页内存里的全局对象**，不是 localStorage，也不是接口返回。每次定位当场看一眼：

```js
typeof window !== 'undefined' && typeof window.wx?.getLocation === 'function'
```

开发者工具 Console 敲 `window.wx` 和 `typeof window.wx?.getLocation`。Chrome / Safari 普通页一般是 `undefined`。有这个函数只说明脚本挂上了，**不等于** `wx.config` 已鉴权；调用失败必须退回浏览器定位。

---

## 1. 小程序：微信给点

流程可以记成：

1. 用户打开小程序
2. 你的代码调用 `wx.getLocation`（或 Taro 同名 API）
3. 微信检查：隐私协议是否同意、位置权限是否开
4. 系统把当前坐标交给微信，微信再交给你

**不是微信自己推过来。** 不调用就不会更新。授权过之后再调，通常不再弹窗，只是再要一次当前 GPS。

### 项目里要配的（官方要求，不是业务接口）

1. 小程序后台 → 开发管理 → 接口设置，开通「获取当前地理位置」。
2. `app.json`（或等价配置）声明位置权限，例如：

```json
{
  "permission": {
    "scope.userLocation": {
      "desc": "你的位置将用于查找附近门店"
    }
  },
  "requiredPrivateInfos": ["getLocation"]
}
```

3. 基础库 2.32+：用户未同意《隐私保护指引》前，**不能**调 `getLocation`，否则会被判定违规。先走 `wx.requirePrivacyAuthorize`（或封装好的隐私门），再定位。
4. 国内业务把 `type` 设成 `'gcj02'`。默认 `wgs84` 和国内地图、国内门店坐标对不齐。

```js
wx.getLocation({
  type: 'gcj02',
  success(res) {
    // res.latitude / res.longitude
  },
  fail() {
    // 用户拒绝、系统定位关了、超时，都走这里
  },
})
```

失败时不要假装成功。附近列表、配送范围这类强依赖坐标的页面，应停在「请开启位置权限」；定店可以再调一次**不带经纬度**的业务接口，让后端给默认门店。

---

## 2. H5：浏览器给点

普通网页没有 `wx.getLocation`。正确路径是浏览器定位：

```js
if (!navigator.geolocation) {
  // 环境不支持，当失败处理
} else {
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude, longitude } = pos.coords // WGS84
    },
    (err) => {
      // 拒绝、超时、非安全源
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
  )
}
```

`maximumAge: 0` 表示不要用浏览器里过期的旧点，每次重新要。若你希望「退出页面再进也更新」，不要在自己的内存里先短路返回上一次的点。

### 使用条件

- **安全源**：https，或本机 `localhost` / `127.0.0.1`。
- 用户点允许。拒绝后要自己做空态，文案写「请允许浏览器获取位置」，不要写「请在小程序设置中开启」。
- iOS Safari 对精度和权限更严，失败很常见，要有兜底。

### localhost、局域网 IP、正式 https 分别是什么

| 你打开的地址 | 是什么 | 浏览器定位 | 微信 JS-SDK `wx.getLocation` |
|---|---|---|---|
| `http://localhost:10087` / `http://127.0.0.1:10087` | **这台电脑自己**，不经过网线/Wi-Fi。浏览器破例把它当安全源 | 可以 | 不行（不是备案域名） |
| `http://10.7.135.61:10087` | 局域网里这台机器的 IP，别人连你的热点/公司网才能打开 | **不行** | 不行 |
| `https://10.7.135.61` | 局域网 IP 套了 https。证书往往是自签的 | 证书被信任时，浏览器定位可以 | 不行（SDK 要的是域名，不要 IP、不要端口） |
| `https://h5.xxx.com` | **公网**（互联网上的正式域名，任何人有网就能打开） | 可以 | **可以**（还要满足下面一节，且必须用微信打开） |

三种地址不是一类东西：

- localhost：这台电脑自己访问自己
- `10.7.135.61`：同一 Wi-Fi / 公司网里的机器互访
- `h5.xxx.com`：公网域名，走互联网，一般还要备案和 https 证书

### Chrome / Safari / Windows 浏览器能不能定位？

**能。** 它们用的是浏览器自己的 `navigator.geolocation`，和微信无关。Chrome、Safari、Edge、Firefox，Windows 和 Mac 都一样。

条件只有两条：

1. 地址是 https，或 localhost / 127.0.0.1
2. 用户在弹窗里点允许

这些浏览器里 **没有** `wx.getLocation`。就算 H5 域名是 `https://h5.xxx.com`，用 Chrome 打开也是浏览器定位，不是微信定位。只有用 **微信 App 内置浏览器** 打开这个域名，并且 JS-SDK 鉴权成功，才是 `wx.getLocation`。

`http://10.7.135.61:10087/#/pages/index/index` 按官方规定拿不到浏览器定位。本地测用 `http://localhost:端口`，或给 H5 配真正的 https 域名。开发机用 IP 测，只能把该源加进 Chrome `unsafely-treat-insecure-origin-as-secure`（不要用于线上）。

---

## 2.1 微信 JS-SDK：网页里调用和微信类似的定位

这说的是 **微信里打开的 H5 网页**，不是小程序。小程序自带 `wx.getLocation`，网页要自己注入 SDK。

**正式 https 域名固定下来之后，可以在微信里用 `wx.getLocation`，** 效果接近小程序（都是微信给 gcj02）。但还要：这个域名配进公众号「JS 接口安全域名」、后端按当前页 URL 签名、`wx.config` 成功、用户用微信打开这个 H5。用 Chrome / Safari / Edge 打开同一个 https 域名没有 `wx`，仍然走浏览器定位。

Taro H5 的 `Taro.getLocation` 内部就是找 `window.wx.getLocation`。所以：

- 普通 Chrome / Safari：没有这套 `wx`，Taro 定位必失败，应走浏览器定位。
- 微信内置浏览器：即使有 `wx` 对象，**没做 `wx.config` 鉴权也会失败**。
- 鉴权成功后，才能像小程序那样调 `wx.getLocation({ type: 'gcj02' })`。

官方：[JS-SDK 使用步骤](https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#1)、[getLocation](https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/JS-SDK.html#64)

### 注入要具备什么

1. 一个认证过的 **微信公众号（服务号）** 或开放平台移动应用，不是小程序 AppID 直接拿来用。
2. 公众号后台 → 设置与开发 → 公众号设置 → 功能设置 → **JS 接口安全域名**。
   - 只填域名，**不能带端口、不能填 IP**。
   - 所以 `10.7.135.61:10087` 这条开发地址 **做不了官方 JS-SDK**。要用已备案的 https 域名。
3. 后端能用 AppSecret 换 `access_token`，再换 `jsapi_ticket`（要缓存，每天有限额）。

### 注入步骤（各项目通用）

1. 页面加载官方脚本：`https://res.wx.qq.com/open/js/jweixin-1.6.0.js`
2. 用**当前页完整 URL**（去掉 hash `#` 后面）向自己的后端要签名。签名算法官方规定：`jsapi_ticket`、`noncestr`、`timestamp`、`url` 字典序拼接后 SHA-1。
3. 前端：

```js
wx.config({
  appId,      // 公众号 AppID
  timestamp,
  nonceStr,
  signature,
  jsApiList: ['getLocation'],
})
wx.ready(() => {
  wx.getLocation({
    type: 'gcj02',
    success(res) { /* res.latitude, res.longitude */ },
    fail() { /* 再退到浏览器定位 */ },
  })
})
wx.error(() => {
  // 签名失败：域名没配、url 和签名用的不一致、ticket 过期
})
```

4. `wx.config` 的 url 必须和地址栏去掉 hash 后**逐字符一致**（含 https、路径、query）。SPA 改路由后要按新 URL 重新签名。

### 和「小程序一模一样」吗？

能力接近（都是微信给 gcj02），**接入完全不是同一套**：

| | 小程序 | 微信内 H5 + JS-SDK |
|---|---|---|
| AppID | 小程序 | 公众号 / 开放平台 |
| 鉴权 | 用户授权位置 + 隐私协议 | 服务端签名 + `wx.config` |
| 域名 | 小程序合法域名 | JS 接口安全域名（无端口） |
| 本机 `http://IP:端口` | 开发者工具能测 | 官方不支持 |

没有公众号签名链路时，H5 就老实用浏览器定位，不要指望「注入一下就和小程序一样」。

---

## 3. 坐标系：两端数字不能直接当同一个点

| 来源 | 坐标系 |
|---|---|
| 手机 GPS / 浏览器定位 | WGS84 |
| 微信 `wx.getLocation({ type: 'gcj02' })` | GCJ-02 |
| 国内地图、国内门店库（高德/腾讯/微信） | 一般是 GCJ-02 |

浏览器拿到的点，给国内门店或腾讯/高德地图用之前，要做 **WGS84 → GCJ-02**。境外坐标不要转。偏移大约几十到几百米，不转会出现「店在马路对面」。

小程序已经要了 `gcj02`，就不要再转一次。

可移植转换（系数不要改）。北京附近自测：WGS84 `(39.915, 116.404)` 转完大约 `(39.916404, 116.410244)`。

```js
const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

function outOfChina(lat, lng) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}

function transformLat(lng, lat) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lat * PI) + 40.0 * Math.sin((lat / 3.0) * PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((lat / 12.0) * PI) + 320 * Math.sin((lat * PI) / 30.0)) * 2.0) / 3.0
  return ret
}

function transformLng(lng, lat) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lng * PI) + 40.0 * Math.sin((lng / 3.0) * PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((lng / 12.0) * PI) + 300.0 * Math.sin((lng / 30.0) * PI)) * 2.0) / 3.0
  return ret
}

function wgs84ToGcj02(lat, lng) {
  if (outOfChina(lat, lng)) return { latitude: lat, longitude: lng }
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * PI
  let magic = Math.sin(radLat)
  magic = 1 - EE * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI)
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI)
  return { latitude: lat + dLat, longitude: lng + dLng }
}
```

---

## 4. 什么时候会变成新位置

定位不会在你走路时自动改页面上的距离。只有**再次调用**定位 API，才会拿到新点。

常见时机（按产品自己选，不是微信自动做的）：

- 小程序冷启动 / 杀掉再开
- 进入某个强依赖位置的页面（如附近门店）
- 用户点「重新定位」
- H5 整页刷新、关掉标签再开

只在当前页下拉刷新、搜索、改筛选，如果代码复用了本次已拿到的点，距离不会变。

收货地址是另一套坐标（地址库里的经纬度）。改收货地址会改「配哪家店」，**不会**自动改「离我多远」。附近距离请继续用设备定位。

---

## 5. 打开地图 ≠ 获取定位

| 能力 | 小程序 | H5 |
|---|---|---|
| 拿我在哪 | `wx.getLocation` | `navigator.geolocation` 或 JS-SDK |
| 打开地图看店 / 导航 | `wx.openLocation` | 没有对等实现，打开腾讯/高德网页 URI |

H5 调 `Taro.openLocation` / `wx.openLocation` 在普通浏览器会失败。不要和「拿当前 GPS」用同一个封装。

### 5.1 腾讯地图：地点标注 vs 路线规划

官方文档：[地点标注](https://lbs.qq.com/webApi/uriV1/uriGuide/uriWebMarker)、[路线规划](https://lbs.qq.com/webApi/uriV1/uriGuide/uriWebRoute)。坐标都是 **纬度在前、经度在后**（gcj02，和微信 `openLocation` 相同，门店坐标不用再转）。

**地点标注（推荐，对齐小程序先看到店）**

```text
https://apis.map.qq.com/uri/v1/marker
  ?marker=coord:22.609581,114.029225;title:某门店;addr:某门店
  &referer=你的应用名
```

- 路径：`/uri/v1/marker`
- `coord`：`纬度,经度`
- `title` / `addr` 里不要出现 `;` `|` `:`，会截断参数
- `referer` 是应用名，不是 key。不要抄 `vibe-mall`，换成当前应用名。

可移植拼接：

```js
function sanitizeMarkerValue(value, fallback) {
  const text = String(value || '').replace(/[;|:]+/g, ' ').trim()
  return text || fallback
}

function buildTencentMapMarkerUrl({ latitude, longitude, name, address, referer }) {
  const title = sanitizeMarkerValue(name, '门店')
  const addr = sanitizeMarkerValue(address || name, title)
  const params = new URLSearchParams()
  params.set('marker', `coord:${latitude},${longitude};title:${title};addr:${addr}`)
  params.set('referer', referer)
  return `https://apis.map.qq.com/uri/v1/marker?${params.toString()}`
}

function openMapInH5(url) {
  const opened = window.open(url, '_blank')
  if (!opened) window.location.href = url
}
```

小程序继续 `wx.openLocation`。H5 调上面两条。Vitest 里的店名、坐标只是用例入参，换成「朝阳门店」或任意字符串都不影响线上。

**路线规划（电脑端容易翻车，不要用来「看这家店」）**

```text
https://apis.map.qq.com/uri/v1/routeplan
  ?type=drive
  &to=某门店
  &tocoord=22.609581,114.029225
  &policy=1
  &referer=你的应用名
```

- 路径：`/uri/v1/routeplan`
- 含义：从「用户当前位置」开到店。电脑上经常没有可靠起点
- 会跳到 `map.qq.com/?type=nav&tocoord=经度,纬度`，一对数字的顺序和官方 `lat,lng` 对不上。经度 114 被当成纬度 → 非法点 → 地图落到你所在城市（例如重庆），并不是漏传了门店坐标

小程序 `wx.openLocation` 是钉点，不是网页规划。H5 要对齐，用标注，不要用规划。

### 5.2 地址栏里的 URL 为什么和拼的不一样

你打开的是 `apis.map.qq.com/uri/v1/...`，腾讯再 **302 跳到** 真正的网页地图。地址栏是最终页，不是你代码里那条。

标注成功时，电脑端常见最终地址：

```text
https://map.qq.com/?type=marker
  &name=某门店
  &pointx=114.029225    ← 经度
  &pointy=22.609581     ← 纬度
  &ref=你的应用名
```

`pointx` / `pointy` 分开写，电脑端不会把经纬度揉成一对搞反。规划失败时常见 `type=nav` + `tocoord=114,22`，就是对调之后的形态。

弹窗被拦（部分 App WebView）时，`window.open` 返回空，改在当前页跳同一条 URI，保证点了有反应。

---

## 6. H5 取点骨架（按目标栈替换「小程序分支」）

```js
function isValidCoordinate(longitude, latitude) {
  return (
    typeof longitude === 'number' &&
    typeof latitude === 'number' &&
    Number.isFinite(longitude) &&
    Number.isFinite(latitude) &&
    !(longitude === 0 && latitude === 0)
  )
}

function browserGeolocation() {
  return new Promise((resolve, reject) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      reject(new Error('getLocation:fail geolocation unsupported'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords
        if (!isValidCoordinate(longitude, latitude)) {
          reject(new Error('getLocation:fail invalid coordinates'))
          return
        }
        resolve(wgs84ToGcj02(latitude, longitude))
      },
      (err) => {
        reject(new Error(err?.message ? `getLocation:fail ${err.message}` : 'getLocation:fail'))
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    )
  })
}

async function getLocationCompat() {
  // 小程序：保持原 API，已是 gcj02。不要走下面。
  if (!IS_H5) return originalGetLocation({ type: 'gcj02' })

  if (typeof window !== 'undefined' && typeof window.wx?.getLocation === 'function') {
    try {
      const loc = await originalGetLocation({ type: 'gcj02' })
      if (isValidCoordinate(loc?.longitude, loc?.latitude)) return loc
    } catch {
      // 有 wx 但没鉴权，落到浏览器
    }
  }

  return browserGeolocation()
}
```

`IS_H5` / `originalGetLocation` 换成目标项目的编译常量和 API。Taro 里分别是 `process.env.TARO_ENV === 'h5'` 和 `Taro.getLocation`。

### 不要从 vibe-mall 原样带走

| 那份实现里的东西 | 别的项目怎么处理 |
|---|---|
| `setGpsCoords` / `clientLocation` | 那是该项目请求头用的内存点，不是定位结果。没有这套 Header 就不要抄 |
| `MAP_REFERER = 'vibe-mall'` | 换成当前应用名 |
| 门店列表空态文案、骨架 class | 按当前页 UI 改 |
| 只给 `store/pages/list` 接线 | 新项目要自己搜所有 `getLocation` / `openLocation` 调用点，逐个接 |

---

## 7. 容易踩的坑

- 把 Taro/微信 API 当成 H5 也能用，空态一直是「请在小程序设置中开启」。
- 用 http 局域网 IP 测 H5 定位，浏览器直接失败。
- 浏览器 WGS84 和门店 gcj02 混用，距离和导航都偏。
- 第一次定位成功后把点存内存，以后再进页面直接读缓存，人已经到另一个城市距离也不变。
- 把收货地址经纬度当成「我在哪」。
- 隐私协议没过就调小程序 `getLocation`，被微信判定违规。
- 打开地图和获取定位用同一个封装，H5 两个都挂。
- H5 导航用腾讯路线规划，电脑端经纬度被对调，店跑到错误城市；应改地点标注。
- 拿地址栏最终的 `map.qq.com` 当自己拼的 URL。真正入口是 `apis.map.qq.com/uri/v1/marker`，后面是腾讯跳转。
- 以为 `window.wx` 在缓存或接口里。它只在当前页全局对象上，Console 才能看到。
- 把 vibe-mall 的 `getLocationCompat.ts` 整文件丢进非 Taro 项目，却不改 `TARO_ENV` / `setGpsCoords`。
