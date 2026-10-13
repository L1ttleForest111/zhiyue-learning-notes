# 前端、后端、MySQL、Redis、Nacos 与 Feign 的关系

## 一、先记住结论

不同技术负责不同事情，不是所有项目都必须使用全部技术。

当前“知页-学习笔记”使用的是：

```text
React + Vite + Ant Design
→ Spring Boot
→ MyBatis-Plus
→ MySQL
```

当前项目**没有使用** Redis、Nacos、Feign。它是一个适合学习前后端完整流程的单体项目。

## 二、当前项目的真实请求流程

当用户在网页填写笔记并点击“保存笔记”时，流程如下：

```text
浏览器中的 React 页面
  ↓
fetch 发送 HTTP 请求
  ↓
POST http://localhost:8080/api/notes
  ↓
Spring Boot 的 Controller 接收请求
  ↓
Service 校验并处理业务规则
  ↓
MyBatis-Plus 的 Mapper 生成并执行 SQL
  ↓
MySQL 的 learning_notes.notes 表保存数据
  ↓
后端返回 JSON
  ↓
React 更新页面并提示保存成功
```

## 三、当前项目中每个技术的职责

| 技术 | 属于哪一层 | 作用 | 当前项目中的位置 |
| --- | --- | --- | --- |
| React | 前端 | 构建页面、处理用户交互和状态 | `notes-web/src/` |
| Vite | 前端工具 | 启动开发服务器、构建前端代码 | `notes-web/vite.config.ts` |
| Ant Design | 前端组件库 | 提供按钮、输入框、表单、卡片等组件 | `notes-web/src/App.tsx` |
| fetch | 浏览器能力 | 从前端向后端发送 HTTP 请求 | `notes-web/src/api.ts` |
| Spring Boot | 后端框架 | 启动 Java 服务、接收接口、组织业务代码 | `notes-api/` |
| Controller | 后端接口层 | 接收 HTTP 请求、返回 JSON | `controller/` |
| Service | 后端业务层 | 校验、默认值、业务流程和事务 | `service/` |
| MyBatis-Plus | 数据访问框架 | 将 Java 对象转换为 SQL 并执行 | `mapper/` |
| MySQL | 数据库 | 长期保存笔记、分类、标签等正式数据 | `learning_notes` |

## 四、前端部分

### 1. React

React 用于构建用户看到的页面，例如：

- 笔记列表；
- 新建和编辑笔记；
- 分类和标签选择；
- Markdown 编辑与预览；
- 保存成功或失败提示。

React 本身不保存长期数据。用户点击保存后，React 需要请求后端，由后端写入 MySQL。

### 2. Vite

Vite 是前端开发工具，不负责页面业务。

它负责：

```text
npm run dev 时启动本地开发服务器
监听前端代码变化
自动刷新浏览器
把 TypeScript 和 TSX 编译为浏览器可运行的 JavaScript
```

当前前端默认运行在：

```text
http://localhost:5173
```

### 3. Ant Design

Ant Design 是 React 的 UI 组件库。

它提供现成的：

```text
Button、Input、Select、Card、List、Form、Modal、message
```

React 负责“数据和交互逻辑”，Ant Design 负责“通用组件和视觉样式”。

### 4. fetch

`fetch` 是浏览器自带的 HTTP 请求能力。

当前项目通过 `fetch` 请求后端，例如：

```text
POST http://localhost:8080/api/notes
```

前端发送的数据一般是 JSON：

```json
{
  "title": "学习 Spring Boot",
  "content": "# 我的学习笔记",
  "categoryId": 3,
  "status": "LEARNING",
  "tagIds": [1, 2]
}
```

## 五、Spring Boot 后端部分

### 1. Java 和 Spring Boot 的区别

```text
Java = 编程语言，类似 JavaScript / TypeScript
Spring Boot = 在 Java 上开发 Web 后端的框架
```

Spring Boot 帮助后端完成通用工作：

```text
启动 HTTP 服务
监听端口
接收 GET、POST、PUT、DELETE 请求
解析请求中的 JSON
返回 JSON 响应
管理项目中的对象
连接数据库
处理常见错误
```

当前后端默认运行在：

```text
http://localhost:8080
```

### 2. Controller：接收前端接口请求

Controller 可以理解为后端的“接口接待处”。

例如：

```text
POST /api/notes
```

会由笔记 Controller 中标记了 `@PostMapping` 的方法接收。

Controller 的主要职责：

```text
接收前端请求
读取 JSON 参数
启动参数校验
调用 Service
返回 JSON
```

Controller 不应该直接写复杂业务逻辑或直接拼 SQL。

### 3. DTO / Request：描述前端传来的数据

例如 `NoteRequest` 描述“创建或更新笔记时前端允许传什么”。

它还能通过：

```text
@NotBlank
@Size
```

定义标题不能为空、标题长度不能超过 200 等规则。

Controller 参数上的 `@Valid` 表示：

```text
请按照 NoteRequest 字段上写的规则校验请求。
```

校验失败时，后端不会执行保存逻辑，也不会写入 MySQL。

### 4. Service：处理业务规则

Service 是业务逻辑层。

创建笔记时，Service 负责：

```text
检查分类 ID 是否存在
检查标签 ID 是否存在
将前端数据复制到 Java Note 对象
设置默认值，例如不置顶、不收藏
调用 Mapper 保存笔记
保存笔记和标签之间的关联
```

### 5. Mapper：数据库操作工具

Mapper 是 Java 后端访问数据库的专用层。

例如：

```java
@Mapper
public interface NoteMapper extends BaseMapper<Note> {}
```

可以这样理解：

```text
@Mapper = 告诉 MyBatis：这是数据库访问接口
BaseMapper<Note> = 提供 Note 的通用增删改查能力
```

真正访问数据库的代码一般长这样：

```java
noteMapper.insert(note);      // 新增
noteMapper.selectById(id);    // 查询
noteMapper.updateById(note);  // 更新
noteMapper.deleteById(id);    // 删除
```

### 6. MyBatis-Plus：Java 如何操作 MySQL

MyBatis-Plus 不是 Java 自带功能，而是第三方数据库框架。

它会根据：

```text
NoteMapper 操作 Note
Note 类上的 @TableName("notes")
Java 字段和数据库字段的映射规则
application.yml 中的数据库连接配置
```

自动生成并执行类似 SQL：

```sql
INSERT INTO notes (title, content, category_id, status)
VALUES (?, ?, ?, ?);
```

Java 中的：

```java
noteMapper.insert(note);
```

最终会向 MySQL 的 `notes` 表新增一行数据。

### 7. MySQL：长期保存正式业务数据

当前项目使用 MySQL 保存：

```text
notes       笔记主体
categories  分类
tags        标签
note_tags   笔记和标签的多对多关联
```

MySQL 是正式数据来源。笔记正文、分类、标签等核心数据必须保存到 MySQL，而不是只存浏览器或 Redis。

## 六、Redis：高速缓存和临时数据

Redis 是内存数据库，常用于：

```text
缓存
短信或邮箱验证码
登录状态
接口限流
计数器
热点数据
```

可以先这样区分：

```text
MySQL = 正式档案室，长期保存核心数据
Redis = 高速临时缓存盒子，快速读取短期数据
```

例如“学习概览”需要统计笔记总数、待复习数量等。访问量很大时，可以先从 Redis 读取统计结果，减少频繁查询 MySQL。

Redis 不适合单独作为笔记正文的唯一存储位置。

当前项目没有引入 Redis，因为本地单用户笔记应用的数据量很小，不需要缓存。

## 七、单体项目和微服务

### 1. 单体项目

当前项目是单体后端：

```text
一个 Spring Boot 服务
├── 笔记接口
├── 分类接口
├── 标签接口
└── 学习概览接口
```

它全部运行在：

```text
localhost:8080
```

单体项目的优点：

```text
结构简单
方便开发、调试和学习
部署成本低
适合中小项目
```

### 2. 微服务项目

当业务变得很大时，可能拆成多个独立后端服务：

```text
用户服务
商品服务
订单服务
支付服务
通知服务
```

每个服务都可能是独立的 Spring Boot 项目，并运行在不同端口、不同服务器甚至不同机器上。

这时才会出现 Nacos、Feign、网关、消息队列等技术。

## 八、Nacos：微服务的通讯录和配置中心

Nacos 常用于微服务场景，有两个主要作用。

### 1. 服务注册与发现

假设有：

```text
用户服务：10.0.0.10:8081
订单服务：10.0.0.11:8082
```

订单服务需要调用用户服务，但用户服务的地址可能变化，也可能启动多个实例。

Nacos 的工作方式：

```text
用户服务启动
→ 向 Nacos 登记：我叫 user-service，我的地址是 ...

订单服务需要调用用户服务
→ 向 Nacos 查询：user-service 在哪里？

Nacos 返回当前可用的服务地址
```

因此可以记为：

```text
Nacos = 微服务的通讯录
```

### 2. 配置中心

Nacos 还可以集中保存多个服务的配置，例如：

```text
数据库地址
Redis 地址
日志级别
第三方 API 配置
```

服务启动或运行时可以读取这些统一配置。

注意：

```text
Nacos 不保存笔记业务数据
Nacos 不是 MySQL
Nacos 不是 Redis
当前单体项目不需要 Nacos
```

## 九、Feign：后端服务之间的 HTTP 调用工具

Feign 通常指 Spring Cloud OpenFeign。

它解决的问题是：

```text
一个后端服务如何调用另一个后端服务？
```

前端调用后端时使用：

```ts
fetch('/api/notes')
```

微服务中，订单服务调用用户服务时，Feign 可以让代码看起来像普通 Java 方法：

```java
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/{id}")
    UserView getUser(@PathVariable Long id);
}
```

调用时：

```java
UserView user = userClient.getUser(1001L);
```

底层仍然是 HTTP 请求：

```text
订单服务
→ HTTP 请求
→ 用户服务
```

因此：

```text
Feign = 后端服务之间发 HTTP 请求的工具
```

## 十、Nacos 和 Feign 的关系

在微服务中常常一起使用：

```text
订单服务想调用用户服务
  ↓
Feign 负责发送 HTTP 请求
  ↓
Nacos 告诉 Feign：用户服务当前在哪里
  ↓
用户服务返回结果
```

可以记成：

| 技术 | 解决的问题 |
| --- | --- |
| Feign | 我怎样调用另一个后端服务？ |
| Nacos | 被调用服务的地址在哪里？ |

## 十一、未来典型的微服务关系图

```text
React 前端
  ↓ HTTP
API 网关
  ↓
用户服务、笔记服务、订单服务等多个 Spring Boot 服务
  ↓                 ↓
Feign             Redis
  ↓                 ↓
Nacos           缓存、验证码、登录状态
  ↓
MySQL
保存每个服务的正式业务数据
```

其中 API 网关是前端进入多个后端服务的统一入口；当前单体项目不需要网关。

## 十二、学习顺序建议

当前不要急着把 Redis、Nacos、Feign 加入笔记项目。

建议按以下顺序学习：

```text
1. React 如何通过 fetch 调用 Spring Boot 接口
2. Controller、DTO、@Valid、Service 的职责
3. Mapper 如何通过 MyBatis-Plus 操作 MySQL
4. 新增、查询、更新、删除的完整流程
5. Redis 的缓存场景
6. 单体项目和微服务的区别
7. Nacos、Feign、API 网关
```

当前最重要的主线始终是：

```text
React
→ fetch
→ Spring Boot Controller
→ Service
→ MyBatis-Plus Mapper
→ MySQL
```

## 十三、链路图 1：当前项目保存一篇笔记的完整过程

下面是一条真实的“点击保存笔记”链路。学习当前项目时，建议反复对照这张图和对应代码阅读。

```text
┌──────────────────────────────────────────────────────────────┐
│ 1. 用户在浏览器中填写笔记                                      │
│    标题、Markdown 正文、分类、标签、学习状态                    │
└───────────────────────────┬──────────────────────────────────┘
                            │ 点击“保存笔记”
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. React 页面                                                  │
│    save() 收集 draft 中的数据                                  │
│    文件：notes-web/src/App.tsx                                 │
└───────────────────────────┬──────────────────────────────────┘
                            │ 调用 api.createNote(payload)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. 前端 API 封装                                               │
│    fetch() 将 JavaScript 对象转换为 JSON                        │
│    文件：notes-web/src/api.ts                                  │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP POST
                            │ http://localhost:8080/api/notes
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 4. Spring Boot Controller                                      │
│    @PostMapping 接住 POST /api/notes                           │
│    @RequestBody：JSON → NoteRequest                            │
│    @Valid：执行标题、正文等参数校验                              │
│    文件：controller/NoteController.java                         │
└───────────────────────────┬──────────────────────────────────┘
                            │ noteService.create(request)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 5. Service                                                     │
│    检查分类和标签是否存在                                        │
│    创建 Note Java 对象                                          │
│    设置默认值：pinned=false、favorite=false                    │
│    文件：service/NoteService.java                              │
└───────────────────────────┬──────────────────────────────────┘
                            │ noteMapper.insert(note)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 6. MyBatis-Plus Mapper                                         │
│    Note → @TableName("notes") → notes 表                       │
│    自动生成 INSERT SQL，并通过 MySQL 驱动发送                    │
└───────────────────────────┬──────────────────────────────────┘
                            │ INSERT INTO notes (...)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 7. MySQL                                                       │
│    learning_notes 数据库                                       │
│    notes 表新增一行数据，生成笔记 id                             │
└───────────────────────────┬──────────────────────────────────┘
                            │ 新 id 返回给后端
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 8. Service 保存标签关联                                         │
│    向 note_tags 表插入：note_id + tag_id                        │
└───────────────────────────┬──────────────────────────────────┘
                            │ 返回 JSON 响应
                            ▼
┌──────────────────────────────────────────────────────────────┐
│ 9. React 更新界面                                               │
│    显示“已保存到本地数据库”，刷新笔记列表                         │
└──────────────────────────────────────────────────────────────┘
```

## 十四、链路图 2：后端内部如何从 Java 对象写入 MySQL

这一张图只看后端和数据库，帮助理解 `noteMapper.insert(note)` 为什么能写数据。

```text
application.yml
│
│ 配置：MySQL 地址、端口、数据库名、用户名、密码
│ jdbc:mysql://localhost:3306/learning_notes
▼
Spring Boot 启动
│
│ 创建并管理数据库连接能力
│ 扫描 @Mapper、@Service、@RestController
▼
NoteMapper
│
│ @Mapper：这是数据库访问接口
│ BaseMapper<Note>：拥有 insert / select / update / delete
▼
Note
│
│ @TableName("notes")：Note 对应 MySQL 的 notes 表
│ categoryId 自动映射为 category_id
▼
noteMapper.insert(note)
│
│ MyBatis-Plus 读取 Note 字段，生成带 ? 占位符的 INSERT SQL
▼
MySQL 驱动 mysql-connector-j
│
│ 按 MySQL 通信协议发送 SQL 和参数
▼
MySQL 的 learning_notes.notes 表
│
└── 新增一条笔记记录
```

## 十五、链路图 3：参数校验失败时为什么不会写入数据库

以“标题为空”为例：

```text
前端发送 POST /api/notes
{
  "title": "",
  "content": "学习内容"
}
        │
        ▼
Controller 接收请求
@RequestBody 将 JSON 转为 NoteRequest
        │
        ▼
@Valid 开始校验 NoteRequest
        │
        ▼
@NotBlank(message = "标题不能为空") 发现 title 是空字符串
        │
        ▼
校验失败，create() 方法不会继续调用 Service
        │
        ▼
GlobalExceptionHandler 捕获校验异常
        │
        ▼
返回 HTTP 400
{
  "code": 400,
  "message": "标题不能为空",
  "data": null
}
        │
        ▼
React 接到错误，显示红色提示

注意：没有进入 noteMapper.insert(note)，所以 MySQL 不会新增数据。
```

## 十六、链路图 4：Redis 在未来缓存中的位置

当前项目没有 Redis。未来访问量变大时，Redis 可以放在 Service 与 MySQL 之间，缓存常用查询结果。

```text
React 请求“学习概览”
        │
        ▼
Spring Boot Controller
        │
        ▼
Service
        │
        ├── 1. 先查 Redis：dashboard:overview 是否已有缓存？
        │       │
        │       ├── 有缓存
        │       │   └── 直接返回缓存结果，速度很快
        │       │
        │       └── 没有缓存
        │           │
        │           ▼
        │         Mapper 查询 MySQL
        │           │
        │           ▼
        │         将统计结果写入 Redis，并设置短暂过期时间
        │           │
        ▼           ▼
      返回给 React
```

这里的原则是：

```text
MySQL = 正式数据来源
Redis = 可删除、可重新生成的高速缓存
```

## 十七、链路图 5：微服务中 Nacos 和 Feign 如何配合

当前项目是一个 Spring Boot 后端，因此不需要这一套。未来多个后端服务拆分后，链路可能如下：

```text
用户服务启动
  │
  └── 向 Nacos 注册
      “我的服务名是 user-service，我目前运行在 10.0.0.10:8081”

订单服务启动
  │
  └── 向 Nacos 注册
      “我的服务名是 order-service，我目前运行在 10.0.0.11:8082”

──────────────────────────────────────────────────────────────

订单服务需要查询用户信息
  │
  ▼
Feign 调用 userClient.getUser(1001)
  │
  ▼
Feign 向 Nacos 询问：user-service 当前地址是什么？
  │
  ▼
Nacos 返回可用地址：10.0.0.10:8081
  │
  ▼
Feign 发送 HTTP GET /users/1001
  │
  ▼
用户服务返回用户 JSON
  │
  ▼
订单服务继续处理订单业务
```

一句话总结：

```text
Feign = 后端服务之间怎样发 HTTP 请求
Nacos = 被调用服务当前在哪里，以及统一配置放在哪里
```
