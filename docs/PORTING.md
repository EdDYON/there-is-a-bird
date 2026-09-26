# NeoForge 1.21.1 迁移指南

本分支为 `port/1.21.1`，对应 Forge 1.20.1 的 **3.5.0** 内容版本；`main` 独立维护 Forge 1.20.1。正式发行包以作者发布的版本为准。

## 环境与构建

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Java | 21 |
| GeckoLib | NeoForge 1.21.1 / 4.6.6 |
| ModDevGradle / Gradle | 2.0.144 / 8.14.5 |

将 IDEA 的 Gradle JVM 或 `JAVA_HOME` 设置为 Java 21。使用仓库自带的 Gradle wrapper：

```sh
./gradlew build
./gradlew runGameTestServer
```

Windows 使用 `gradlew.bat`；`build-mod.bat` 与 `run-client.bat` 使用当前 `JAVA_HOME` 或 PATH 中的 Java。发行包路径为 `build/libs/guaniao-neoforge-1.21.1-3.5.0.jar`。本地 Maven 发布任务为 `publishMavenJavaPublicationToMavenRepository`，目标目录为 `mcmodsrepo`。生成的数据资源 `src/generated/resources` 已加入主资源。

## 保留的内容与平台适配

- 保留 60 个注册物品、16 种鸟、20 个配方、37 个进度、15 个战利品表，以及各物种原有 AI、互动、图鉴、相机照片和风翎扇实现。
- 原版 604 个主资源均有对应；134 张贴图、31 个几何模型、22 份动画及 116 个声音文件保持原内容。资源路径、NeoForge 加载器名称、配方和进度格式按 1.21.1 适配。
- 物品数据改用组件，旧版嵌套物品 NBT 经 DataFixer 转换；三个附魔改为数据驱动注册，唱片使用 jukebox_song。13 个网络数据包迁移为 NeoForge 自定义载荷。
- 同步 Forge 3.5.0 的起飞首帧、近垂直起飞、无重力上升识别和地面动画过渡；迁入对应的两个回归测试。
- 修复鸟粪投射物构造和变体同步、压力板支撑块命中与残留计重、普通成书及旧作者笔记兼容、天空鸟影视角与空区块高度采样。
- 保留原有鸟类独立名额及 5 种飞越入口，补充覆盖全部 16 种鸟的独立栖息地生成。此入口只抽取鸟类，牛羊不占其名额；遵守群系、生成开关、碰撞、玩家距离及种群上限。不承诺与原版同种子的自然密度完全相同。
- 修复压力测试的 Pre/Post tick 计时和注册前实体类型查询的默认值缓存。金鸟奖励使用新版原版战利品同样的附魔查询，并经过 NeoForge 附魔等级扩展事件。

## 测试与兼容性

仓库 CI 执行 `build` 和 `runGameTestServer`。自动检查范围如下：

- 20 个独立回归：模型起飞首帧、扑翼推进、落地、垂直起飞与普通小跳区分，以及天空视角和动画帧等。
- 14 项服务端 GameTest：鸟类数据、独立生成与名额限制、鸟粪变体和压力板、恶作剧食物、蛋糕污染、排便、旧笔记成就、物品及方块实体数据、配方、进度、附魔、照片摆放与旋转等。
- 构建检查：发行 JAR 的版本、NeoForge 元数据、Mixin 注册及 JSON 资源；开发测试模组不进入发行包。

`runClientSmoke` 提供图鉴模型、设置界面、物品显示与相机流程的客户端检查。自然鸟类和天空鸟群的长期表现仍需在实际游戏中观察。

`src/gameTest` 是独立开发测试模组，不进入发行包。客户端场景需图形环境；`runClientSmoke` 会操作专用测试玩家，使用前将 GameTest 世界复制到 `run-client-smoke/saves/port-test`。不要将正式存档放入该目录。普通 `runClient` 不会自动运行这些场景。

自动测试不覆盖所有使用场景。多人联机、长期生态及性能、完整鸟类生活周期、全部附魔战斗、第三方模组和光影兼容、旧世界整体升级仍需专项实机验证。

## 辅助工具

三个根目录 Python 工具从自身所在项目解析路径。`_gen_notes.py` 生成 138 本笔记的语言文本和 1.21.1 成书代码，保留创造物品栏入口；`_measure_model_height.py` 测量模型站姿高度；`_gen_egg_umbrella.py` 保留原版历史图标生成算法，依赖 Pillow。该算法输出与仓库当前成品图标不同，不能用于原样重建当前贴图。

## 分支与发布

- `main` 维护 Forge 1.20.1；本分支维护 NeoForge 1.21.1，无需将整套迁移代码合并回 1.20.1。
- 修复分支或 Pull Request 以对应平台分支为目标；保留现有授权与分发政策。
- 内容版本与对应 Forge 内容版本保持一致；平台使用分支、发行文件名和 Tag 区分，例如 `mc1.21.1-neoforge-v3.5.0`。
- 正式发行使用对应平台的 Tag 和 Release，并明确标注 Minecraft 与加载器版本。
