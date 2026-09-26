# 版本维护与迁移指南

Forge 1.20.1 与 NeoForge 1.21.1 使用独立分支维护，当前内容版本均为 **3.5.0**。

## 当前分支

| 分支 | Minecraft | 加载器 | Java | GeckoLib |
| --- | --- | --- | --- | --- |
| `main` | 1.20.1 | Forge 47.2.x | 17 | 4.4.x |
| `port/1.21.1` | 1.21.1 | NeoForge 21.1.248 | 21 | 4.6.6 |

1.21.1 的迁移源码与自动构建已就绪。构建、测试和兼容性说明见 [NeoForge 分支指南](https://github.com/EdDYON/there-is-a-bird/blob/port/1.21.1/docs/PORTING.md)。正式发行包以作者发布的版本为准，源码分支不等同于正式发行。

## 开发与构建

切换分支后，将 Gradle JVM 或 `JAVA_HOME` 设置为上表对应的 Java 版本，再使用仓库自带的 Gradle wrapper：

```sh
./gradlew build
```

Windows 使用 `gradlew.bat build`。NeoForge 分支另外提供 `runGameTestServer` 服务端集成测试。

## 跨版本修复

1. Forge 1.20.1 的修改提交到 `main`，NeoForge 1.21.1 的修改提交到 `port/1.21.1`。
2. 共通功能修复应在两个分支分别适配；保留各自的加载器、映射、资源格式和构建配置，不将整套迁移代码合并回 `main`。
3. 先保证玩家行为与内容一致，再考虑使用新版本 API 重构。内容版本保持对应，平台通过分支和发行文件名区分。
4. 每个 Pull Request 处理一个可验证的主题。涉及多个模块的大改动先通过 Issue 明确范围。
5. 素材和发行遵循现有授权及分发政策。

## 验证要求

- 对应分支的构建及自动回归通过。
- 客户端与独立服务端能够启动，服务端不加载客户端专用类。
- 修改涉及的玩法有对应实机验证；共通修复在两个版本分别检查。
- 新增或修改的 JSON 可解析，注册 ID 与资源路径一致。
- Pull Request 列出测试范围、兼容性影响及尚未验证的场景。
