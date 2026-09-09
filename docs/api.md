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
