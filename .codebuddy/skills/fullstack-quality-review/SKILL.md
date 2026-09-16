---
name: fullstack-quality-review
description: This skill should be used when implementing, reviewing, debugging, refactoring, or preparing to submit changes in this React, Spring Boot, and MySQL project, especially when the task spans frontend, API, backend, database, quality checks, or security review.
---

# 全栈质量审查

## 目标

将功能开发与可验证的质量检查结合起来。覆盖 React 前端、Spring Boot API、MySQL 数据模型和前后端接口契约，并在每次任务结束时给出简短的“可完善点”。

## 执行流程

### 1. 明确改动范围

- 定位受影响的前端组件、`notes-web/src/api.ts`、`notes-web/src/types.ts`、Controller、DTO、Service、Mapper、SQL 与 `docs/api.md`。
- 追踪一次完整数据流：用户操作 → 前端状态 → HTTP 请求 → Controller → Service → 数据库 → API 响应 → 页面状态。
- 若变更涉及命名代码符号，优先使用语义代码导航；若涉及日志、配置或文案，使用文本搜索。

### 2. 审查契约与安全边界

检查以下项目：

- API 是否始终使用 `{ code, message, data }` 响应结构；HTTP 状态是否与业务码一致。
- 前端是否同时处理网络失败、非 JSON 响应、HTTP 错误和业务错误。
- 后端是否校验请求内容、分页、状态枚举和关联资源；数据库操作是否使用参数绑定。
- 是否存在 XSS、敏感信息日志、硬编码密钥、无授权的远程访问、越权或不安全反序列化风险。
- 是否将认证、授权、数据迁移和删除策略等未明确的业务决策错误地自动实现。

对于本地免登录模式，保持服务默认仅绑定回环地址；任何远程暴露方案都必须先设计认证、授权与资源归属校验。

### 3. 实施改进

直接修复可验证的技术问题，例如类型错误、构建失败、统一错误映射、输入校验、请求层异常处理、索引缺失和文档漂移。

不要自动改变以下事项：用户角色、资源所有权、数据保留策略、不可逆数据库迁移、产品级长度限制或复习业务规则。将它们列为“待确认”。

### 4. 运行验证

在仓库根目录运行：

```bash
bash scripts/quality-check.sh
```

如改动需要数据库、浏览器或接口行为验证，再运行与改动范围相匹配的集成测试或手动冒烟检查。不要把构建成功误报为业务功能已验证。

### 5. 输出审查结果

以以下结构收尾：

1. 已完成：列出实现或修复的内容及验证结果；
2. 必须修复：仅列出会阻断正确性、安全性或发布的问题；
3. 建议优化：列出性能、测试、可维护性改进；
4. 待确认：列出需要产品或部署决策的问题；
5. 可完善点：用不超过三项的简短建议提醒当前最值得推进的后续工作。

除非用户明确要求，不创建或提交 Git commit；不执行不可逆的数据操作。
