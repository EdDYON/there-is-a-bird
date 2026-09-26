# 贡献指南

感谢你愿意帮助完善 Bird Watching / 哪来的鸟？。本项目欢迎可复现的 Bug 报告、生态设计建议、代码修复、文档完善和版本迁移贡献。

## 开始之前

1. 阅读 [README](README.md)、[行为准则](CODE_OF_CONDUCT.md) 和本指南。
2. 搜索现有 Issue，确认问题尚未被报告或认领。
3. 较大的功能、物种或迁移工作先创建 Issue，说明范围后再编码。
4. 安全漏洞不要公开提交 Issue，请按 [安全策略](SECURITY.md) 私密报告。

## 开发环境

- Minecraft 1.21.1
- NeoForge 21.1.248 / ModDevGradle 2.0.144
- Java 21
- GeckoLib NeoForge 4.6.6

构建命令：

```powershell
# Windows
.\gradlew.bat build
```

```bash
# Linux / macOS
./gradlew build
```

## 分支与提交目标

- 1.20.1 的修复和功能提交到 `main`。
- 1.21.1 的修复和功能提交到 `port/1.21.1`。
- 功能分支使用 `feature/<主题>`，修复分支使用 `fix/<主题>`。
- 一个 Pull Request 只处理一个清晰主题，不混入无关格式化或生成文件。

## 开发要求

### Java 与端侧

- 优先复用已有 Bird、AI、注册、网络和客户端渲染结构。
- 客户端类不得从通用或服务端路径直接加载。
- 服务端负责游戏逻辑，客户端只负责表现；多人状态必须正确同步。
- 不吞掉异常，不以占位逻辑或不可验证的 API 冒充完成。

### 资源与数据

- JSON、纹理、声音、模型和动画路径必须与注册 ID 一致。
- 提交素材时必须说明来源和授权；禁止提交来源不明、抓取或未经许可的资产。
- 不要从官方 JAR 解包、修改或复用受限模型资产。
- 修改 `geo`、`animations`、模型配套纹理前必须先获得作者明确同意。

### 验证

`build` 会通过 `check` 在独立 JVM 中运行 `src/test/java` 下的所有 `*Test.java` 主方法测试，并检查正式 JAR 的版本信息、NeoForge 元数据、Mixin 注册和 JSON 资源。新增这类测试时须提供 `public static void main(String[] args)`；失败时抛出异常或返回非零退出码。仅运行这些回归测试可使用 `gradlew.bat regressionTest`（Linux / macOS 使用 `./gradlew regressionTest`）。真实世界的服务端集成检查使用 `gradlew.bat runGameTestServer`。`src/gameTest` 是隔离测试模组，不会进入发行 JAR。客户端渲染及相机流程检查使用 `runClientSmoke`，需要先将测试世界放入 `run-client-smoke/saves/port-test`。这些自动检查不能替代长时间多人实机验证。

Pull Request 至少应包含：

- `./gradlew build` 或 `gradlew.bat build` 通过。
- 新增/修改 JSON 可被解析，资源路径存在。
- 对应功能的单人或多人实机测试说明。
- 若无法执行某项验证，清楚说明原因与未验证风险。

## 许可与贡献授权

提交代码即表示你有权提供该贡献，并同意代码部分按项目的 [MIT License](LICENSE) 发布。模型、动画、模型配套纹理和其他受限资产仍受 [模型资产限制许可协议](LICENSE-MODELS.md) 约束，不因代码贡献而变更许可。

为提交代码贡献而创建 Fork、本地编译和测试是允许的；不得向公众分发包含受限资产的测试 JAR。详情见 [官方发行与二次分发政策](DISTRIBUTION-POLICY.md)。

## Pull Request 清单

- 关联一个 Issue，或解释为什么不需要。
- 描述行为变化及兼容性影响。
- 只提交本次任务所需文件。
- 不包含 `build/`、`run/`、IDE 缓存、日志或私密信息。
- 更新必要的语言、配置、数据和文档。
- 列出实际执行的构建和实机测试。
