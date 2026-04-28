---
name: project-reactor-netdisk
description: 个人云端网盘项目，WebFlux流式传输文件
type: project
---

# 项目背景

**reactor-netdisk** 是一个个人云端网盘项目，核心目标是 **使用 WebFlux 实现文件流式传输与非阻塞 IO**。

# 技术栈

- **框架**: SpringBoot 3 + WebFlux
- **语言**: Kotlin
- **数据库**: MySQL

# 项目定位

**Why:** 选用 WebFlux 是为了探索响应式在文件传输场景下的优势（验证差异是附带收获，非主要目的）。

**How to apply:** 项目的技术选型围绕"非阻塞流式传输"展开，理解设计决策时应聚焦于此。
