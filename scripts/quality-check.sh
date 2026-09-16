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
