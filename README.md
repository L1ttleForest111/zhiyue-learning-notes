# 知页-学习笔记

一个免登录的本地学习笔记项目。前端使用 React，后端使用 Java Spring Boot，数据存储在 MySQL。

## 项目结构

```text
.
├── notes-web/       # React + TypeScript + Vite
├── notes-api/       # Java 17 + Spring Boot + MyBatis-Plus
├── docs/api.md      # 本地 Markdown 接口文档
└── notes-api/src/main/resources/db/init.sql
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

## 学习路径

1. 阅读 `entity`、`mapper`、`service`、`controller` 四层代码，理解 Spring Boot 分层。
2. 参考 `docs/api.md` 用 Apifox、Postman 或 curl 调接口。
3. 从笔记 CRUD 开始，观察 MyBatis-Plus 如何将 Java 实体转换为 SQL。
4. 继续扩展图片附件、笔记版本、导出 Markdown、复习提醒等功能。

## 说明

这是单机学习项目，按你的要求不包含登录系统。若将来部署到局域网或公网，请先补充身份认证、访问控制和 CORS 白名单。
