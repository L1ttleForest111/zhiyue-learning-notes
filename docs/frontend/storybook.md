# Storybook：独立预览前端组件和页面

## Storybook 是什么

Storybook 是前端开发工具，用来在浏览器中独立展示、调试组件和页面。它不是浏览器，也不是面向最终用户的正式网站。

开发者在项目中启动 Storybook 后，它会在本机运行一个预览服务，并通过 Chrome 等普通浏览器打开。用户访问的正式网站或小程序中不会出现这个预览页面。

默认情况下，Storybook 只在开发者自己的电脑上运行，其他人无法通过 `localhost` 访问。

## 适合解决什么问题

使用 Storybook 查看界面时，不必先运行登录、后端接口和完整业务流程。开发者可以给组件准备固定的假数据，分别展示不同状态，例如：

- 正常状态；
- 加载中；
- 空数据；
- 请求失败；
- 不同尺寸、文案或权限下的状态。

它适合用于：

1. 对照设计稿检查一个组件、一块区域或一整页；
2. 查看平时不容易触发的空列表、接口失败等状态；
3. 修改组件后逐一检查相关状态，避免影响其他样式；
4. 集中展示可复用组件，便于开发者了解组件的用法。

Storybook 不适合用于：

- 调试真实后端接口；
- 查看正式环境中的数据；
- 替代测试环境或正式站点；
- 让客户验收完整业务功能。

客户验收功能时，应查看测试环境或正式环境中的真实页面。

## 预览画面从哪里来

Storybook 不会重新画一套界面。左侧目录中的每一项，都是将项目已有的组件引入后，配上指定参数或假数据进行展示。

每组故事通常对应一个 `*.stories.tsx` 或 `*.stories.js` 文件。该文件主要完成两件事：

1. 引入项目中的真实组件；
2. 为组件准备若干种预览状态，每种状态称为一个 story。

例如：

```tsx
import type { Meta, StoryObj } from '@storybook/react';
import { OrderDetail } from './OrderDetail';

const meta = {
  title: 'Pages/Order/Detail',
  component: OrderDetail,
} satisfies Meta<typeof OrderDetail>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    order: { id: '1001', status: '已完成' },
  },
};

export const Empty: Story = {
  args: {
    order: null,
  },
};
```

这里的对应关系是：

- `title: 'Pages/Order/Detail'` 决定左侧目录层级；
- `Default` 和 `Empty` 是两个可点击的预览状态；
- `args` 是传给组件的参数，也就是用于预览的假数据。

业务代码不会引用 `*.stories.*` 文件。正式页面和 Storybook 引用的是同一个业务组件，因此两边展示的是同一份组件代码。

## 安装与启动

Storybook 需要单独安装，浏览器、React 和 Vite 都不会自带。安装后，项目通常会在 `package.json` 的 `scripts` 中增加类似命令：

```json
{
  "scripts": {
    "storybook": "storybook dev -p 6006",
    "build-storybook": "storybook build"
  }
}
```

其中 `-p 6006` 表示使用 `6006` 端口。进入前端项目目录后运行：

```bash
npm run storybook
```

再用浏览器打开：

```text
http://localhost:6006
```

终端中的 Storybook 进程必须保持运行；停止进程后，本地预览页面也会关闭。实际端口以项目脚本配置和终端输出为准，未指定端口时通常使用 `6006`。

> 当前项目的 `notes-web/package.json` 尚未配置 Storybook 依赖和启动脚本，因此现在不能直接运行 `npm run storybook`。需要先完成 Storybook 初始化。

## 如何查看和操作

Storybook 页面通常分为以下区域：

- **左侧目录**：选择组件、页面或某一种状态；
- **中间画布**：显示当前组件或页面；
- **Controls 面板**：临时修改传给组件的参数，例如标题、数量、开关状态；
- **其他面板**：根据项目配置查看事件、文档或无障碍检查结果。

Controls 修改的是当前预览中的参数，不会直接修改组件源码。刷新或切换 story 后，参数通常会恢复为故事文件中的定义。

如果只想检查页面中的一小块区域，应选择对应组件的 story；如果要检查整体布局，则选择整页的 story。

## 与正式构建的关系

Storybook 有自己独立的开发和构建流程。正常配置下：

- Storybook 的预览应用不会成为正式网站的一部分；
- `*.stories.*` 文件不会被正式页面主动引用；
- Storybook 相关包通常放在 `devDependencies` 中；
- 正式页面引用的业务组件仍会进入正式构建，这是页面本身需要的代码，并不代表 Storybook 被打进了产物。

关键点是：Storybook 和正式页面共用组件，但使用不同的入口和构建流程。

有些项目会在正式构建前检查故事文件是否完整，检查失败可能导致构建停止。这只是质量检查，不表示 Storybook 会进入正式产物。

## 别人能否访问

`localhost` 指向当前运行服务的电脑，通常只有这台电脑自己能访问，因此不能把 `http://localhost:6006` 直接发给同事或客户。

如果需要让其他人查看，可以执行静态构建：

```bash
npm run build-storybook
```

该命令通常生成 `storybook-static` 目录。还需要把这个目录部署到服务器或静态托管平台，其他人才能通过公开地址访问。

是否需要部署 Storybook，取决于团队协作方式。对于客户的功能验收，仍应优先提供测试环境中的正式页面。

## 使用边界总结

| 需求 | 是否适合 Storybook |
| --- | --- |
| 独立查看组件 | 适合 |
| 展示加载、空数据、报错等状态 | 适合 |
| 对照设计稿检查样式 | 适合 |
| 调整组件参数并即时查看 | 适合 |
| 调试真实接口和完整业务流程 | 不适合 |
| 查看线上真实数据 | 不适合 |
| 客户验收正式功能 | 不适合 |

可以把 Storybook 理解为“组件和页面的开发预览目录”：它帮助开发者稳定、快速地检查界面，但不能替代真实应用和测试环境。
