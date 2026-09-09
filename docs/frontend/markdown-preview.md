# React 中实现完整 Markdown 实时预览

## 目标

笔记正文以 Markdown 原文保存；前端通过 React 在编辑时实时解析并展示预览。这样既便于数据存储，也能让用户用简单的文本格式写标题、表格、代码和任务清单。

## 依赖组合

```text
react-markdown     # Markdown 转 React 组件
remark-gfm         # GitHub 风格 Markdown：表格、任务列表、删除线
rehype-highlight   # 代码块语法高亮
highlight.js       # 高亮规则和样式主题
```

安装命令：

```bash
npm install react-markdown remark-gfm rehype-highlight highlight.js
```

## 核心实现

在组件中导入解析器和插件：

```tsx
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import remarkGfm from 'remark-gfm';
```

封装预览组件：

```tsx
function MarkdownPreview({ content }: { content: string }) {
  if (!content.trim()) {
    return <span>在左侧写下你的学习收获…</span>;
  }

  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      rehypePlugins={[rehypeHighlight]}
    >
      {content}
    </ReactMarkdown>
  );
}
```

编辑器中的使用方式：

```tsx
<TextArea
  value={draft.content}
  onChange={(event) => setDraft((current) => ({
    ...current,
    content: event.target.value,
  }))}
/>

<MarkdownPreview content={draft.content} />
```

## 工作流程

1. 用户在 `TextArea` 输入 Markdown 文本。
2. `onChange` 更新 `draft.content` 状态。
3. React 检测到 State 变化并重新渲染。
4. `react-markdown` 将 Markdown 解析为 React 组件树。
5. `remark-gfm` 增加表格、任务列表、删除线等 GFM 能力。
6. `rehype-highlight` 为带语言标记的代码块添加语法高亮。

## 当前支持的 Markdown

````md
# 一级标题

**加粗**、*斜体*、~~删除线~~、`行内代码`

- 普通列表
- [x] 已完成任务
- [ ] 待完成任务

| 技术 | 用途 |
| --- | --- |
| React | 前端界面 |
| Spring Boot | 后端接口 |

```ts
const title = '知页-学习笔记';
```

[React 官网](https://react.dev)
````

## 样式重点

Markdown 解析后会生成 `table`、`pre`、`code`、`blockquote` 等元素，需要增加专门样式：

- 表格：边框和横向滚动；
- 代码块：背景、内边距、滚动条；
- 行内代码：浅色背景和圆角；
- 引用：左侧强调色边框；
- 任务列表：复选框间距。

代码高亮主题在前端入口文件中引入：

```tsx
import 'highlight.js/styles/github.css';
```

## 安全注意事项

不要直接使用下面的方式渲染用户输入：

```tsx
<div dangerouslySetInnerHTML={{ __html: content }} />
```

这会产生 XSS 风险。当前项目使用 `react-markdown`，并且未启用 `rehype-raw`，所以 Markdown 内嵌的原始 HTML 不会被直接执行，这是更安全的默认配置。

如果以后明确需要支持 HTML，必须同时引入 `rehype-sanitize` 过滤危险标签、属性和脚本，不能只使用 `rehype-raw`。

## 后续可扩展方向

- Markdown 文件导入和导出；
- 图片上传；
- Mermaid 流程图；
- 数学公式；
- 代码块复制按钮；
- 编辑器工具栏；
- 自动保存草稿。
