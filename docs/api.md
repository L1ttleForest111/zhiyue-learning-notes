# 学习笔记本地 API

- 基础地址：`http://localhost:8080/api`
- 数据格式：`application/json`
- 认证：不需要登录，仅限本机开发环境使用。

## 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

非 `0` 的 `code` 表示请求失败；`message` 为可直接展示的错误原因。

## 笔记

### 获取笔记列表

`GET /notes?keyword=java&categoryId=1&tagId=2&status=LEARNING&page=1&pageSize=20`

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `keyword` | 否 | 搜索标题与正文 |
| `categoryId` / `tagId` | 否 | 分类或标签筛选 |
| `status` | 否 | `TODO`、`LEARNING`、`MASTERED`、`REVIEW` |
| `page` / `pageSize` | 否 | 从 `1` 开始；单页最多 `100` 条 |

### 新建笔记

`POST /notes`

```json
{
  "title": "Spring Boot 参数校验",
  "content": "# 学习内容\n\n- 使用 @Valid 校验请求体",
  "categoryId": 1,
  "status": "LEARNING",
  "tagIds": [1, 2],
  "nextReviewAt": "2026-09-10T20:00:00"
}
```

`title` 必填，最长 200 个字符；`content` 必填。`categoryId`、`tagIds` 传入时必须对应已有数据。

### 查看、更新与删除

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/notes/{id}` | 查询详情 |
| `PUT` | `/notes/{id}` | 请求体同“新建笔记” |
| `DELETE` | `/notes/{id}` | 逻辑删除 |

### 更新笔记状态

`PATCH /notes/{id}/status`

```json
{ "status": "REVIEW", "nextReviewAt": "2026-09-12T20:00:00" }
```

### 设置置顶或收藏

| 方法 | 地址 | 请求体 |
| --- | --- | --- |
| `PATCH` | `/notes/{id}/pin` | `{ "value": true }` |
| `PATCH` | `/notes/{id}/favorite` | `{ "value": true }` |

## 分类

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/categories` | 分类列表 |
| `POST` | `/categories` | `{ "name": "后端" }` |
| `PUT` | `/categories/{id}` | 修改名称 |
| `DELETE` | `/categories/{id}` | 分类无关联笔记时才可删除 |

## 标签

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/tags` | 标签列表 |
| `POST` | `/tags` | `{ "name": "Java" }` |
| `DELETE` | `/tags/{id}` | 删除标签及其笔记关联 |

## 学习概览

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/dashboard` | 总笔记、学习中、已掌握、待复习与最近编辑 |
| `GET` | `/reviews/today` | `nextReviewAt` 早于当前时间的笔记 |

## 旅行笔记（独立于学习笔记）

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `GET` | `/travel-notes?keyword=阳朔&page=1&pageSize=10` | 摘要分页，搜索标题、目的地和摘要；不包含 HTML 或心得正文 |
| `POST` | `/travel-notes` | 创建旅行笔记 |
| `GET` | `/travel-notes/{id}` | 完整详情，含 `htmlContent`、`journalContent`、`htmlSha256`、`importKey` |
| `PUT` | `/travel-notes/{id}` | 更新（可替换 HTML、清空日期），保留相同 `importKey` |
| `DELETE` | `/travel-notes/{id}` | 逻辑删除；之后详情和预览返回 404 |
| `GET` | `/travel-notes/capabilities` | `{ "mapConfigured": false, "maxHtmlBytes": 2097152 }` |
| `GET` | `/travel-notes/{id}/map-preview` | `{ "previewUrl": "http://127.0.0.1:8082/preview/随机UUID", "mapConfigured": false }`，十分钟有效的独立地图预览地址 |
| `GET` | `/travel-notes/{id}/preview-document` | 兼容 JSON 包装的 `{ "htmlContent": "服务端清理后的 HTML" }`，用于不透明来源文字预览 |
| `GET` | `/travel-notes/{id}/preview` | 兼容直接访问的 `text/html;charset=UTF-8`；服务端清理副本并设置 CSP sandbox |

创建及更新请求：

```json
{
  "title": "阳朔三日旅行攻略",
  "destination": "阳朔",
  "startDate": null,
  "endDate": null,
  "summary": "三天两晚：十里画廊、遇龙河、兴坪",
  "journalContent": "# 我的心得\n",
  "htmlContent": "<!doctype html><html><body><h1>攻略</h1></body></html>",
  "originalFilename": "攻略.html",
  "importKey": "12345678-1234-1234-1234-123456789012"
}
```

`title` 非空、最多 200 字；目的地最多 100 字，摘要最多 1000 字，心得最多 100000 字。HTML 非空且 UTF-8 编码最多 2 MiB；文件名必须是 `.html/.htm` 基础名称，不能传电脑文件路径。日期格式 `YYYY-MM-DD`，结束日期不得早于开始日期。

`importKey` 由客户端生成并在重试时保持不变；数据库唯一约束防止重复创建。相同标识、相同 HTML 返回已有记录；不同 HTML 返回 409；已逻辑删除的标识不自动重建。重复导入另一份旅行时应明确新建，使用新 UUID。HTML 哈希只检测内容，不充当旅行身份。

地图弹窗通过 Axios 获取 `map-preview`，再导航到独立回环地址预览源。预览票据十分钟有效、只绑定一篇当前可读攻略，重启后失效，失效后重新打开即可；服务最多保留 128 个未过期票据。独立预览端口默认 8082（`TRAVEL_PREVIEW_PORT`），只有 GET `/preview/{token}`，无数据库 CRUD 路由或 CORS；校验 Host 为精确回环地址和端口防止 DNS 重绑定。

独立源采用 HTTP CSP `sandbox allow-scripts allow-same-origin`，允许 SDK 访问预览源自身存储，但与 5173 的 React 和 8080 的 API 都不同源。禁止与主页面同源部署，不允许顶层导航、弹窗、表单或嵌套页面。资源 CSP 在响应头和服务端生成的 meta 中限制为已确认的腾讯地图公共资源，拒绝本机网络请求。GL SDK 动态样式计算需要 `unsafe-eval`，JSONP 鉴权需要腾讯地图子域脚本，因此仅独立地图源的 MAP_RESOURCE_CSP 开放这些权限；主应用和兼容文字预览保持无 `unsafe-eval` 策略。只导入可信攻略，该隔离不宣称可以安全执行任意恶意网页。加载失败和超时提供提示及重试。

兼容的直接 `/preview` 与 JSON `/preview-document` 仍采用不透明来源 iframe 模式，不能通过给主页面同源 iframe 添加 `allow-same-origin` 来修复地图。HTML 原文留存，预览移除 WorkBuddy 代理、未知外部脚本、相对附件及原文件 Key。未配置 `TENCENT_MAP_KEY` 时仍尝试官方 SDK，底图与固定点位可能加载，但存在产品鉴权报错；不承诺路线服务或正式授权可用。配置个人 Key 后仍需检查域名授权和配额。不要仅凭 SDK 初始化或 HTTP 200 认定地图产品已授权。

迁移：`notes-api/src/main/resources/db/migrations/001_create_travel_notes.sql`，手工执行，不会因文件存在自动迁移。
