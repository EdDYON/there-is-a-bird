# AGENTS.md

## Scope
This is a Minecraft Forge 1.20.1 Java mod project.

## Read scope
Prefer reading only:
- src/main/java
- src/main/resources
- build.gradle
- settings.gradle
- gradle.properties

Avoid reading or scanning:
- build/
- run/
- .gradle/
- .gradle-user/
- .tools/
- logs/
- out/
- crash-reports/

## Commands
Do not run Gradle commands unless explicitly requested.
When running Gradle, prefer:
- .\gradlew.bat --version
- .\gradlew.bat build
- .\gradlew.bat runClient

## 版本号规则

使用三段版本号：主版本.鸟类版本.修复版本。遵循作者约定，不套用自动清零次级版本的规则。

- 每完成一个新鸟种，第二段加 1。
- 每修复一个独立 bug，第三段加 1；同一个 bug 涉及多个文件或多次修改，只计一次。
- 每新增一个玩法，或一项大型、重大内容，第一段加 1。普通图标替换、文档整理等不单独计为新玩法或 bug 修复。
- 只增加对应段，其余段不主动清零。第二、三段满 10 清零并向前进 1；第一段按十进制正常增长。
- 示例：修复 bug，1.0.0 → 1.0.1；新增鸟种，1.0.0 → 1.1.0；末位进位，1.0.9 → 1.1.0；中间位进位，1.9.3 → 2.0.3。
- 同一批完成多个新鸟种或独立 bug 修复，按实际数量累计，不按文件数、提交数或测试次数计数。
- 以根目录 gradle.properties 的 mod_version 为实际版本来源；升级时同步 README.md 的当前版本和 CHANGELOG.md 的对应记录。
- 不追溯重算已有历史版本。

## 文档管理

- 不自动新增单独的功能实现说明、阶段记录、评估报告或临时优化方案 Markdown。
- 只有用户明确要求保存独立文档时，才新增这类文件；日常工作说明放在任务回复中。
- 保留项目介绍、更新记录、授权协议、实际使用的管理/迁移指南和 AGENTS.md；必要的长期说明优先更新已有文档。
