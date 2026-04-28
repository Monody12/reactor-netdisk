---
name: graceful-shutdown
description: 停止进程的正确方式
type: feedback
---

## 规则

**停止Spring Boot应用时，禁止使用 `taskkill //F //PID` 强制结束进程。**

必须使用以下方式之一：
1. `Ctrl+C` — 前台运行时直接按，优雅关闭
2. `pkill -f "reactor-netdisk"` — 后台运行时优雅停止
3. `kill %1` — 后台jobs任务用kill

**Why:** 使用taskkill强制结束会导致H2数据库文件被锁，需要手动删除`.mv.db`文件才能重启。

**How to apply:** 任何需要停止应用的时候，都用pkill或Ctrl+C，不使用taskkill。
