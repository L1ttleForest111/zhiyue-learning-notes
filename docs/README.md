# 知页-学习笔记：开发文档

这里保存会随项目提交到 GitHub 的学习笔记和项目文档。日常临时记录仍可通过网页保存到 MySQL；经过整理、值得长期保留的内容再归档到这里。

## 文档分类

| 分类 | 内容 |
| --- | --- |
| [学习计划](./plans/README.md) | 阶段目标、任务编号、完成标准和产出链接 |
| [前端](./frontend/README.md) | React、TypeScript、网络、工程化和浏览器知识 |
| [后端](./backend/README.md) | Java、Spring Boot、接口设计、服务发现和基础设施 |
| [数据库](./database/README.md) | MySQL、SQL、表结构、关联关系和索引 |
| [项目案例](./cases/README.md) | 来自 `agentic-mall` 等真实项目的实践和验证结果 |
| [项目文档](./project/README.md) | 当前笔记项目的结构、流程、质量检查和开发决策 |
| [文档模板](./templates/README.md) | 主题笔记和项目案例的统一结构 |
| [接口文档](./api.md) | 知页-学习笔记 REST API |

## 使用约定

1. 一篇主题笔记使用一个独立的 `.md` 文件，文件名使用小写英文和连字符。
2. 新增前先搜索已有标题和核心术语；同主题优先合并，不创建 `-2`、`-new`、`-final` 文件。
3. 通用概念归入前端、后端或数据库；阶段任务归入计划；真实改动和验证归入案例。
4. 跨项目内容记录来源项目、仓库相对路径和可确认的 commit，不写本机绝对路径。
5. 新增或移动文档后，更新对应目录的 `README.md`。
6. 归档步骤和完成标准见[跨项目学习笔记归档工作流](./project/learning-note-archive-workflow.md)。
7. 需要在网页阅读时，明确调用 `move-learning-notes` 的本地数据库发布模式；仅归档不会写库，详见 [Skill 入库流程](./project/markdown-to-database-workflow.md)。

## 网页笔记分类

以下映射作为 Skill 入库指引。AI 先阅读正文和各目录的 `README.md` 确认归类，再通过后端 API 按分类名称查编号，不能假定分类 ID 固定。

| 归档目录 | 网页分类 | 默认处理 |
| --- | --- | --- |
| `docs/frontend/` | 前端 | 用户调用入库后发布正式主题文档 |
| `docs/backend/` | 后端 | 用户调用入库后发布正式主题文档 |
| `docs/database/` | 数据库 | 用户调用入库后发布正式主题文档 |
| `docs/cases/<project>/` | 项目案例 | 保留来源项目，发布案例 |
| `docs/project/` | 项目文档 | 只发布用户选定的当前项目文档 |
| `docs/plans/` | 学习计划 | 只发布用户选定的计划 |
| `docs/templates/`、各级 `README.md`、`docs/api.md` | 不适用 | 默认跳过 |

导入是单向发布：保留 Markdown 原件，不自动覆盖网页修改。重复文件先核查来源记录，禁止按标题猜测匹配。
