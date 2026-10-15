# `quality-check.sh`：当前项目 Shell 脚本逐行解读

> 对照脚本：[`scripts/quality-check.sh`](../../scripts/quality-check.sh)

## 1. 为什么不直接使用其他项目的 `launcher.sh` 笔记

其他项目的 `launcher.sh` 用于启动两个 Python 服务，因此会出现 `python3`、端口、`lsof`、`kill`、PID 文件、后台进程和 `open` 等内容。

当前“知页-学习笔记”项目没有 `launcher.sh`，实际使用的是：

```text
scripts/quality-check.sh
```

它的职责不是启动前后端，而是一次性检查：

```text
前端能否通过类型检查和生产构建
→ 前端生产依赖是否存在高危漏洞
→ 后端能否编译、测试和打包
```

因此本篇只解释当前项目真实存在的脚本，避免文档和代码脱节。

## 2. 如何运行

先在终端进入项目根目录，然后执行：

```bash
bash scripts/quality-check.sh
```

> `bash` 表示“用 Bash 解释器执行这个 `.sh` 文件”。

完整流程图：

```text
终端执行 bash scripts/quality-check.sh
        │
        ▼
Shell 从第 1 行开始执行脚本
        │
        ▼
定位项目根目录 ROOT_DIR
        │
        ├── 进入 notes-web → npm run build
        │
        ├── 进入 notes-web → npm audit --omit=dev --audit-level=high
        │
        └── 进入 notes-api → mvn verify
        │
        ▼
全部成功 → 输出“全栈质量检查通过”
任意一步失败 → 立刻停止并把错误显示在终端
```

## 3. 脚本原文

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf '\n==> 前端：类型检查与生产构建\n'
(
  cd "$ROOT_DIR/notes-web"
  npm run build
)

printf '\n==> 前端：生产依赖漏洞检查\n'
(
  cd "$ROOT_DIR/notes-web"
  npm audit --omit=dev --audit-level=high
)

printf '\n==> 后端：编译、测试与打包验证\n'
(
  cd "$ROOT_DIR/notes-api"
  mvn verify
)

printf '\n==> 全栈质量检查通过\n'
```

## 4. 第 1 行：Shebang

```bash
#!/usr/bin/env bash
```

这行叫 **shebang**。

含义：

> 如果给脚本添加可执行权限后直接运行它，系统应使用 `bash` 解释器执行。

其中：

| 部分 | 含义 |
| --- | --- |
| `#!` | shebang 标记 |
| `/usr/bin/env` | 在系统环境变量 `PATH` 中查找程序 |
| `bash` | 使用 Bash 执行脚本 |

当前我们运行的是：

```bash
bash scripts/quality-check.sh
```

因为已经明确写了 `bash`，所以即使没有 shebang 也能运行；保留 shebang 是 Shell 脚本的常见规范。

## 5. 第 2 行：严格模式

```bash
set -euo pipefail
```

这行会打开三个安全选项：

| 选项 | 全称 | 当前脚本中的作用 |
| --- | --- | --- |
| `-e` | errexit | 任意命令失败时，脚本立刻停止 |
| `-u` | nounset | 使用未定义变量时，立刻报错 |
| `-o pipefail` | pipefail | 管道中任意一个命令失败，整个管道都算失败 |

本脚本最重要的是 `-e`：

```text
npm run build 失败
→ 脚本立即停止
→ 不会错误地打印“全栈质量检查通过”
```

没有 `set -e` 时，前端构建即使失败，脚本仍可能继续执行后端检查，最后误导你以为全部成功。

## 6. 第 4 行：计算项目根目录

```bash
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
```

这行较长，但它只做一件事：

> 无论你当前在什么目录运行脚本，都找出项目根目录的绝对路径，并保存到 `ROOT_DIR`。

执行链路：

```text
${BASH_SOURCE[0]}
  ↓
当前脚本自身路径，例如 scripts/quality-check.sh
  ↓ dirname
脚本所在目录，例如 scripts
  ↓ /..
回到 scripts 的上一级，也就是项目根目录
  ↓ cd
进入项目根目录
  ↓ pwd
打印项目根目录的绝对路径
  ↓ $(...)
接住命令输出
  ↓ ROOT_DIR=...
保存为变量
```

### 6.1 `ROOT_DIR=...`：变量赋值

```bash
ROOT_DIR="..."
```

表示把右侧结果保存为变量 `ROOT_DIR`。

后续通过：

```bash
"$ROOT_DIR"
```

取出它的值。

等号两边不能有空格：

```bash
ROOT_DIR="/path"   # 正确
ROOT_DIR = "/path" # 错误，Shell 会把 ROOT_DIR 当命令名
```

### 6.2 `${BASH_SOURCE[0]}`：脚本自己的路径

```bash
${BASH_SOURCE[0]}
```

表示当前正在执行的 Shell 脚本文件路径。

在本项目中，它大致是：

```text
scripts/quality-check.sh
```

比 `$0` 更适合被其他脚本 `source` 的场景；这里使用它是更稳妥的 Bash 写法。

### 6.3 `dirname`：取父目录

```bash
dirname "scripts/quality-check.sh"
```

结果是：

```text
scripts
```

它会去掉路径最后一段文件名，只保留目录部分。

### 6.4 `..`：上一级目录

```bash
scripts/..
```

表示：

```text
项目根目录
```

因此：

```bash
"$(dirname "${BASH_SOURCE[0]}")/.."
```

就是从 `scripts` 回到项目根目录。

### 6.5 `cd`、`&&` 和 `pwd`

```bash
cd "目录" && pwd
```

| 写法 | 含义 |
| --- | --- |
| `cd` | 进入指定目录 |
| `&&` | 左边成功后，才执行右边 |
| `pwd` | 打印当前目录的绝对路径 |

因此：

```bash
cd "..." && pwd
```

表示：

> 先进入项目根目录；只有进入成功，才打印该目录的绝对路径。

### 6.6 `$(...)`：命令替换

```bash
$(命令)
```

表示先执行括号内命令，再用它的输出替换整段文本。

例如：

```bash
NAME="$(echo "知页")"
```

最终等价于：

```bash
NAME="知页"
```

本脚本中，`$(...)` 接住的是 `pwd` 输出的根目录路径。

### 6.7 双引号的作用

```bash
"$ROOT_DIR/notes-web"
```

双引号可以防止路径中有空格时，被 Shell 错误拆成多个参数。

这是路径和变量在 Shell 中的推荐写法。

## 7. `printf`：输出阶段提示

例如：

```bash
printf '\n==> 前端：类型检查与生产构建\n'
```

`printf` 是格式化输出命令。这里：

| 内容 | 含义 |
| --- | --- |
| `\n` | 换行 |
| `==>` | 仅用于让终端输出更容易阅读 |
| `前端：类型检查与生产构建` | 当前将要执行的步骤说明 |

它只是在终端输出提示，不会构建、不访问数据库、不启动服务。

本脚本使用 `printf` 而不是 `echo`，原因是 `printf` 对换行等格式控制更明确，适合脚本中的固定输出。

## 8. 圆括号 `(...)`：子 Shell

例如：

```bash
(
  cd "$ROOT_DIR/notes-web"
  npm run build
)
```

圆括号表示创建一个 **子 Shell**。可以先理解为临时打开一个“小终端环境”。

执行流程：

```text
主脚本当前目录
  ↓
进入子 Shell
  ↓
cd 到 notes-web
  ↓
执行 npm run build
  ↓
子 Shell 结束
  ↓
主脚本仍保持原来的目录
```

为什么要这样写？

如果直接写：

```bash
cd "$ROOT_DIR/notes-web"
npm run build
```

后面的命令会一直停留在 `notes-web` 目录。使用子 Shell 可以让每个检查步骤互不影响。

## 9. 前端检查步骤

```bash
(
  cd "$ROOT_DIR/notes-web"
  npm run build
)
```

### 9.1 `cd "$ROOT_DIR/notes-web"`

进入前端项目目录：

```text
notes-web/
```

因为 `package.json` 位于该目录，`npm run build` 必须在这里运行。

### 9.2 `npm run build`

`npm` 是 Node.js 的包管理和脚本运行工具。

`run build` 表示执行 `notes-web/package.json` 中的：

```json
{
  "scripts": {
    "build": "tsc -b && vite build"
  }
}
```

因此真正执行的链路是：

```text
npm run build
  ↓
tsc -b
  ↓
检查 TypeScript 类型
  ↓
vite build
  ↓
生成可部署的前端构建产物
```

如果 TypeScript 有类型错误，或者 Vite 无法构建，命令会失败，`set -e` 会让整个质量检查脚本停止。

## 10. 前端生产依赖漏洞检查

```bash
(
  cd "$ROOT_DIR/notes-web"
  npm audit --omit=dev --audit-level=high
)
```

`npm audit` 会检查已安装 npm 依赖是否存在公开披露的安全漏洞。

参数含义：

| 参数 | 含义 |
| --- | --- |
| `--omit=dev` | 不检查只在开发时使用的依赖，只关注生产运行需要的依赖 |
| `--audit-level=high` | 只有发现高危或严重漏洞时才让命令失败 |

它不检查你的业务代码，只检查依赖包的已知漏洞信息。

## 11. 后端验证步骤

```bash
(
  cd "$ROOT_DIR/notes-api"
  mvn verify
)
```

### 11.1 `mvn` 是什么

`mvn` 是 Maven 命令，Maven 是 Java 项目的依赖和构建工具。

它对应前端的：

```text
npm
```

类比：

| 前端 | Java 后端 |
| --- | --- |
| `package.json` | `pom.xml` |
| `npm install` | Maven 下载依赖 |
| `npm run build` | `mvn verify` 或 `mvn package` |

### 11.2 `mvn verify` 做什么

`verify` 是 Maven 生命周期中的一个阶段。它会依次完成前面的阶段，因此通常包含：

```text
编译 Java 代码
→ 执行测试
→ 打包
→ 校验构建结果
```

如果 Java 编译错误或测试失败，命令会返回失败，Shell 脚本随即停止。

## 12. 最后一行成功提示

```bash
printf '\n==> 全栈质量检查通过\n'
```

只有前面三项都成功时，代码才会运行到这一行。

因此看到：

```text
==> 全栈质量检查通过
```

表示当前脚本覆盖的前端构建、生产依赖漏洞检查和后端验证都已经通过。

它不表示：

```text
所有页面的每个交互都被人工验证
数据库中的所有业务数据都正确
项目已经适合部署公网
```

质量检查是基础保护，不是完整测试替代品。

## 13. 当前脚本不包含的内容

为了避免混淆，以下内容出现在其他项目启动脚本中，但**当前 `quality-check.sh` 没有使用**：

| 内容 | 为什么当前脚本不需要 |
| --- | --- |
| `python3` | 当前前端用 Vite，后端用 Spring Boot，不使用 Python 服务 |
| 端口和 `lsof` | 脚本不启动服务，不需要检查端口占用 |
| `kill`、PID 文件、`$!` | 脚本不创建后台服务，不需要记录或关闭进程 |
| `sleep` | 不需要等待服务启动 |
| `trap`、`wait` | 没有后台进程需要清理或等待 |
| `open`、`xdg-open` | 脚本不自动打开浏览器 |
| `>`、`2>`、`/dev/null` | 当前脚本不写 PID 文件，也没有主动丢弃输出 |
| `if`、`elif`、`[[ ]]` | 当前脚本依靠 `set -e` 控制失败，不需要分支判断 |

## 14. 什么时候应该使用质量检查

建议在以下时机执行：

```text
完成一个功能后
修复一个 bug 后
修改前端依赖或 Java 依赖后
准备 Git commit 前
准备 Git push 前
```

在项目根目录执行：

```bash
bash scripts/quality-check.sh
```

相关说明见：[全栈质量检查工作流](./quality-workflow.md)。
