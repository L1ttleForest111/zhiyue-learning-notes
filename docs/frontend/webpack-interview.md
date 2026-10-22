# Webpack 面试提问

原理在 [Webpack 是什么](./webpack.md)。本篇只留：问 → 口述 → 可能追问 → 仓库对照。不要在这里展开字段说明书。

未讲完的问先列在文末，讲完再补口述。

---

## 1. 主包、分包、拆包、chunk

**问：** 拆包是什么？小程序超体积怎么办？chunk 又是什么？

**口述：** 全量打成一份会又大又慢，所以要拆包：先只下必须的，其余按需下。拆包是打包器手法。微信里落实成主包和分包：主包是第一次进来立刻下载的（`pages`，启动和 Tab）；分包是进到对应页面才下载的（`subPackages`）。大 SDK 可以放无页面的异步分包。只被分包用的公共模块不要误进主包 `common.js`。拆出来的每一份 JS 叫 chunk。运营端没有主包这个词，用路由懒加载拆 chunk。

**可能追问：** 拆包是不是就是主包分包？运营端怎么拆？

**答：** 不是。拆包通用；主包/分包是微信落地。H5/运营端说 `import()`、`lazy`、`splitChunks`。运营端清单在 `src/router/`，`React.lazy` + `Suspense`；官方 `lazy` 外包一层失败刷新（发版后旧 chunk 404）。

**仓库对照：** 小程序 `src/app.config.ts` 的 `pages` / `subPackages` / `async-libs`；`config/index.ts` 的 `optimizeMainPackage`、`copy`。运营端 `src/router/index.tsx`、`lazyWithRetry.ts`。原理：[拆包、主包、分包、chunk](./webpack.md#拆包主包分包chunk)。

---

## 2. 图片为什么不进微信包；publicPath 是什么

**问：** 主包还是超，图怎么办？`publicPath` 和输出目录有什么区别？

**口述：** 图进微信包最容易撑体积。生产把 `src/assets` 打到 `dist/weapp` 外面，JS 里写成 COS 地址，CI 再上传 CDN。`publicPath` 是打包器写入产物的访问前缀，不是又一个 output 目录。开发走本地图床。分包减的是代码按页再下；CDN 减的是静态资源根本不进包。

**可能追问：** 是不是所有图都上 CDN？运营端也这样吗？

**答：** 不是。直播控件等可用 `copy` 跟包。运营端没有微信主包限制，但仍有 `publicPath`（相对路径或 CDN）。

**仓库对照：** `config/prod.ts` 的 `imageUrlLoaderOption`（`outputPath`、`publicPath`、hash 文件名）；`config/utils/cdn.js`；开发时 `config/index.ts` 本地静态服务。原理：[publicPath 和图片出包](./webpack.md#publicpath-和图片出包)。

---

## 待讲

3. 编译期常量 vs 运行时环境变量（`defineConstants` / 小程序没有 `process`）
4. Tree-shaking
5. 开发为什么 Vite 快、Webpack 慢
6. loader vs plugin vs 框架插件
7. source map / hash 文件名 / HMR / `sideEffects` / ANALYZE（能说清即可）
