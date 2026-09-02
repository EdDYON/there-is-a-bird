# 「哪来的鸟？」官方发行与二次分发政策 v1.0

版权所有 © 2026 EdDYON。保留所有未明确授予的权利。

本政策用于区分源代码贡献、官方发行包和未经授权的修改版。它不缩减根目录 MIT 协议已经授予的源代码权利，但 MIT 协议不适用于模型资产、其他受限媒体资产、项目 Logo 或作者身份标识。

## 1. 官方渠道

本项目当前官方源地址为：

- https://github.com/EdDYON/there-is-a-bird
- https://github.com/EdDYON/there-is-a-bird/releases

只有 EdDYON 在上述仓库或其他明确声明的官方渠道发布的构建，才属于官方版本。正式 Release 提供校验值时，玩家应使用对应 SHA-256 校验下载文件。

## 2. 官方 JAR 是混合授权作品

官方 JAR 同时包含：

- 按 MIT 协议提供的源代码或其编译结果；
- 按 `LICENSE-MODELS.md` 提供的受限模型、动画及配套纹理；
- 未被单独开放授权的其他媒体资产；
- 不属于 MIT 授权范围的项目名称、Logo 和作者身份标识。

获得官方 JAR 不代表获得其中全部内容的修改权或再分发权。

## 3. 禁止的改包和传播行为

未经 EdDYON 事先书面许可，不得：

1. 上传、分享、出售、赠予或发布包含受限资产的修改版 JAR；
2. 从官方 JAR 中提取模型、动画、配套纹理或其他受限资产用于修改、复用、移植或传播；
3. 修改受限模型资产后重新打包，无论是否免费、是否注明作者；
4. 使用「Bird Watching / 哪来的鸟？」、原项目 Logo、EdDYON 的名称或相似标识，使非官方构建看起来像正式版本；
5. 删除或伪造版本号、来源链接、作者信息、版权声明、许可信息或完整性标识；
6. 将未经授权的修改版描述为修复版、增强版、移植版、汉化版或其他可能让玩家误认为得到作者认可的版本。

注明原作者、免费分享或附带源链接，都不会自动取得上述许可。

## 4. 允许的代码贡献流程

欢迎玩家通过以下方式帮助修复问题和迁移 Minecraft 版本：

1. Fork 官方 GitHub 仓库；
2. 在独立分支修改 MIT 授权的代码；
3. 在本地编译和测试临时 JAR；
4. 向官方仓库提交 Issue 或 Pull Request。

贡献过程中不得修改、提取或复用受限模型资产，也不得向其他玩家分发本地测试 JAR。临时构建只用于贡献者本人测试或经 EdDYON 明确许可的协作测试。

## 5. 独立代码 Fork

MIT 协议允许对代码进行 Fork、修改和再分发。如果要在官方项目之外发布独立代码 Fork，发布者必须：

- 移除全部受限模型、动画、纹理、Logo 和其他未开放资产；
- 遵守 MIT 协议的版权和许可声明保留要求；
- 使用不同的项目名称、Logo、模组 ID 和发行标识；
- 清楚声明该 Fork 与 EdDYON 及官方项目无关，且未获官方认可。

## 6. 举报非官方改包

如果发现有人传播疑似修改版、提取模型或冒充官方版本，请保留下载地址、文件、截图、发布时间和发布者信息，并通过官方 GitHub 仓库联系作者。不要继续转发可疑文件。

---

# Official Release and Redistribution Policy v1.0

The source code is licensed under MIT, but official JAR files also contain restricted models, animations, textures, branding, and other non-code assets that are not covered by MIT.

Only builds released by EdDYON through the official repository or another expressly announced channel are official. You may not distribute modified JAR files containing restricted assets, extract or alter those assets, or use the project name, logo, or author identity to present an unofficial build as official.

Contributors may fork the repository and create temporary local builds solely to submit code fixes or version-porting work back to the official project. Such test JAR files may not be distributed. Independent MIT code forks must remove all restricted assets and branding, use a different name and mod ID, preserve required MIT notices, and clearly state that they are unofficial and unaffiliated with EdDYON.
