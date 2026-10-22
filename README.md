# 知页-学习笔记

一个免登录的本地学习笔记项目。前端使用 React，后端使用 Java Spring Boot，数据存储在 MySQL。

## 项目结构

```text
.
├── notes-web/       # React + TypeScript + Vite
├── notes-api/       # Java 17 + Spring Boot + MyBatis-Plus
├── docs/            # 长期学习笔记、计划、案例和项目文档
├── scripts/         # 本地质量检查脚本
└── .codebuddy/      # 随项目维护的 CodeBuddy Skills
```

## 环境要求

- Node.js 20 或以上
- Java 17
- Maven 3.9 或以上
- MySQL 8

## 本地启动

### 1. 初始化 MySQL

在 MySQL 客户端执行：

```bash
mysql -u root -p < notes-api/src/main/resources/db/init.sql
```

默认数据库名为 `learning_notes`。如果本地 MySQL 用户名、密码或端口不同，请通过环境变量配置：

```bash
export MYSQL_URL='jdbc:mysql://localhost:3306/learning_notes?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export MYSQL_USERNAME='root'
export MYSQL_PASSWORD='你的本地密码'
```

不要把密码写进 `application.yml` 或提交到版本库。

### 2. 启动 Java 后端

```bash
cd notes-api
mvn spring-boot:run
```

服务启动后访问 `http://localhost:8080/api/dashboard` 可检查服务状态。

### 3. 启动 React 前端

新开一个终端：

```bash
cd notes-web
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`。

## 旅行笔记

左侧“旅行笔记”入口支持选择 UTF-8 的 `.html/.htm` 攻略（最多 2 MiB），填写目的地、日期、摘要与 Markdown 心得后保存到独立 `travel_notes` 表，不计入学习统计。列表支持分页和搜索；已保存攻略可隔离预览或下载原文件。

已有数据库首次启用旅行功能，在项目根目录执行只新增旅行表的迁移（有密码时加 `-p`）：

```bash
mysql -u root < notes-api/src/main/resources/db/migrations/001_create_travel_notes.sql
```

HTML 原文存入 MySQL；地图攻略使用独立本机预览源 `http://127.0.0.1:8082`，端口由 `TRAVEL_PREVIEW_PORT` 配置，随 Spring Boot 启停。主页面向 API 获取有效十分钟的随机预览地址，再以 iframe 打开。该端口只提供受控 HTML，不提供数据库接口、CORS 或写入操作。iframe 的 `allow-same-origin` 只保留预览自身来源以便地图 SDK 使用存储，预览和知页主页面仍是不同来源；不得把预览接口反向代理到主页面同源。资源 CSP 白名单、禁止主页面导航/表单/嵌套 iframe 的 sandbox 与浏览器权限限制保持生效。腾讯 GL SDK 的样式计算使用动态 JavaScript、鉴权使用 JSONP，因此只有独立地图源允许 `unsafe-eval` 和腾讯地图子域脚本，主页面及兼容文字预览不会增加这些权限。此兼容策略只适合可信攻略，不是通用恶意 HTML 执行平台。加载失败提示可重试。

WorkBuddy 的 `_TMapSecurityConfig` 本机代理被移除，原文件 Key 不继承，地图脚本标准化为官方 GL SDK。未配置 Key 时不再主动禁用 SDK，可以尝试加载底图和固定点位；实测存在鉴权报错，因此初始化或加载瓦片不代表正式授权、配额及道路路线可用。正式使用请在启动后端前通过环境变量 `TENCENT_MAP_KEY` 配置自己的 Web Key。浏览器 Key 可见，应限制授权域和配额。外部链接、相对 PDF、表单和嵌入页面默认移除；只允许已确认的腾讯地图公共资源，不提供 WorkBuddy 内网代理，不自动抓取分享 URL。请只导入可信攻略，不把旅行隐私数据提交 Git。

兼容的 `/preview-document` 和 `/preview` 仍保留不透明来源沙箱方案，用于文字预览，但地图 SDK 在该模式可能因无法访问浏览器存储而失败。独立预览端口被占用时会导致启动失败，请修改 `TRAVEL_PREVIEW_PORT` 而不是关闭安全隔离。

原始 HTML 下载后在浏览器独立打开会执行原脚本，下载只适用于可信文件。PDF、相对图片等附件不会自动随 HTML 导入。

## 文档导航

- [开发文档总导航](./docs/README.md)
- [小米高级前端：云 Mall 对齐计划](./docs/plans/xiaomi-senior-frontend-plan.md)
- [`agentic-mall` 项目实践案例](./docs/cases/agentic-mall/README.md)
- [跨项目学习笔记归档工作流](./docs/project/learning-note-archive-workflow.md)
- [REST API 文档](./docs/api.md)

## 质量检查

每次完成一个功能或修复后，在仓库根目录运行：

```bash
bash scripts/quality-check.sh
```

该命令会执行前端请求回归测试、类型检查与生产构建、生产依赖漏洞检查，以及后端测试与打包验证。完整审查规则见 [全栈质量检查工作流](./docs/project/quality-workflow.md)。

## 学习路径

1. 阅读 `entity`、`mapper`、`service`、`controller` 四层代码，理解 Spring Boot 分层。
2. 参考 `docs/api.md` 用 Apifox、Postman 或 curl 调接口。
3. 从笔记 CRUD 开始，观察 MyBatis-Plus 如何将 Java 实体转换为 SQL。
4. 继续扩展图片附件、笔记版本、导出 Markdown、复习提醒等功能。

## 说明

这是单机学习项目，按你的要求不包含登录系统。若将来部署到局域网或公网，请先补充身份认证、访问控制和 CORS 白名单。
