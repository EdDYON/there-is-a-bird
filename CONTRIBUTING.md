# 贡献指南

报告 Bug 时，请附上游戏、加载器和模组版本，以及复现步骤和日志。较大的改动先在 Issue 中讨论；安全问题按 [安全策略](SECURITY.md) 私密报告。

## 开发环境

- Minecraft 1.20.1
- Forge 47.2.x
- Java 17
- GeckoLib 4.4.x

## 构建

```bash
./gradlew build
```

Windows 使用 `gradlew.bat build`。Gradle JVM 或 `JAVA_HOME` 需设置为 Java 17。

## 验证

`build` 会运行独立回归测试，并检查发行 JAR 的版本、Mixin 配置及映射。只运行回归测试可用 `./gradlew regressionTest`，Windows 使用 `gradlew.bat regressionTest`。

`src/test/java` 下的 `*Test.java` 通过 `public static void main(String[] args)` 运行，测试失败时抛出异常或返回非零退出码。

修改玩法后，还需在游戏中检查对应行为；涉及联机的改动应使用独立服务端测试。PR 中说明测试环境、结果和未测试的部分。

## 提交修改

- 1.20.1 的 PR 提交到 `main`，1.21.1 的 PR 提交到 `port/1.21.1`。
- 每个 PR 集中处理一个问题，说明功能变化，并附上相关 Issue。
- 游戏逻辑在服务端处理，渲染和界面放在客户端；联机状态需要同步。
- 资源路径应与注册 ID 一致，修改过的 JSON 应能正常解析。
- 不提交构建产物、游戏存档、IDE 缓存、日志或私密信息。

## 素材与许可

新增素材请注明来源和授权。修改模型、动画或配套纹理前，先获得作者同意；不要从官方 JAR 解包素材。

提交代码即表示你有权提供该贡献，并同意代码部分按项目的 [MIT License](LICENSE) 发布。模型、动画、模型配套纹理和其他受限资产仍受 [模型资产限制许可协议](LICENSE-MODELS.md) 约束，不因代码贡献而变更许可。

为提交代码贡献而创建 Fork、本地编译和测试是允许的；不得向公众分发包含受限资产的测试 JAR。详情见 [官方发行与二次分发政策](DISTRIBUTION-POLICY.md)。
