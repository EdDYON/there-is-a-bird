<div align="center">
  <img src="src/main/resources/guaniao_logo.png" width="160" alt="Bird Watching logo">

# Bird Watching / 哪来的鸟？

一个为 Minecraft Forge 1.20.1 制作的鸟类生态、观鸟摄影与羽扇战斗模组。

[![Build](https://github.com/EdDYON/there-is-a-bird/actions/workflows/build.yml/badge.svg)](https://github.com/EdDYON/there-is-a-bird/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)
![Forge](https://img.shields.io/badge/Forge-47.2.x-E04E14)
![Java](https://img.shields.io/badge/Java-17-ED8B00)

</div>

## 模组特色

- 12 种具有独立模型、动画、声音与生态行为的鸟类。
- 栖息地、迁徙、觅食、睡眠、种群与个体性格等动态生态系统。
- 可变焦相机、离屏世界渲染、照片保存与服务端照片管理。
- 八哥学舌、几维鸟领地争斗等物种专属行为。
- 风翎扇及葬翎、裂翎、猎归三种专属形态与技能。
- 面向多人服务器的照片、临时实体和自然掉落保护。

## 当前支持

| 项目 | 当前状态 |
| --- | --- |
| 模组版本 | 2.1.4 |
| Minecraft | 1.20.1 |
| Forge | 47.2.x |
| Java | 17 |
| GeckoLib | 4.4.x |
| `main` | 当前维护的 1.20.1 版本 |
| `port/1.21.1` | 计划中的迁移工作分支，不代表已经兼容 |

## 下载与安装

正式版本只会发布在本仓库的 [Releases](https://github.com/EdDYON/there-is-a-bird/releases) 或作者明确标注的官方渠道。Releases 页面为空时，表示当前还没有可公开下载的正式构建。

1. 安装 Minecraft 1.20.1 对应的 Forge 47.2.x。
2. 安装与 1.20.1 兼容的 GeckoLib 4.4.x。
3. 将官方发布的模组 JAR 放入游戏的 `mods` 目录。
4. 启动前备份重要世界；服务端与客户端应使用相同版本。

## 从源码构建

```powershell
# Windows
.\gradlew.bat build
```

```bash
# Linux / macOS
./gradlew build
```

构建结果位于 `build/libs/`。本地构建仅用于开发、测试和提交贡献；包含受限模型资产的测试 JAR 不得对外分发。

## 文档与贡献

- [贡献指南](CONTRIBUTING.md)：开发环境、分支目标、验证与素材规则。
- [版本迁移指南](docs/PORTING.md)：1.21.1 迁移模块、认领方式与完成标准。
- [服务端管理](docs/SERVER_ADMIN.md)：照片存储、维护命令与问题报告。
- [更新记录](CHANGELOG.md)：版本功能与修复历史。
- [安全策略](SECURITY.md)：私密报告漏洞的方式。
- [社区行为准则](CODE_OF_CONDUCT.md)：协作边界与执行方式。

欢迎玩家提交可复现的 Bug、生态建议和版本迁移贡献。开始编码前请先创建或认领 Issue，避免多人重复处理同一模块。

## 分支约定

| 分支 | 用途 |
| --- | --- |
| `main` | 当前稳定维护的 Minecraft 1.20.1 源码 |
| `port/<版本>` | 大版本迁移，例如 `port/1.21.1` |
| `feature/<主题>` | 新功能开发 |
| `fix/<主题>` | 缺陷修复 |

模组版本使用 Git tag 和 GitHub Release 标记，不为每一个小版本长期保留独立分支。

## 许可与官方版本

本项目采用代码与模型资产分离授权：

| 内容 | 协议 |
| --- | --- |
| 源代码及代码相关构建、配置文件 | [MIT License](LICENSE) |
| 模型、几何、骨骼、动画及模型配套纹理 | [模型资产限制许可协议](LICENSE-MODELS.md) |
| 官方 JAR、项目名称与 Logo | [官方发行与二次分发政策](DISTRIBUTION-POLICY.md) |
| 未被单独许可的其他非代码媒体资产 | 保留所有权利 |

模型资产不属于 MIT 授权范围。未经 EdDYON 书面许可，禁止为取得或二次利用而解包官方 JAR，禁止将模型资产商用、复用、移植、修改或再分发。正常下载安装并进行个人、非商业游戏不受影响。

官方仓库：[github.com/EdDYON/there-is-a-bird](https://github.com/EdDYON/there-is-a-bird)。任何非官方构建都不得冒充「Bird Watching / 哪来的鸟？」正式版本。
