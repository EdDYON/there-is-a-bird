# Bird Watching

一个为 Minecraft Forge 1.20.1 制作的鸟类模组。

## 开发环境

- Minecraft 1.20.1
- Forge 47.2.0
- Java 17
- GeckoLib 4.4.9


## 许可协议

本项目采用代码与模型资产分离授权：

| 内容 | 协议 |
| --- | --- |
| 源代码及代码相关构建、配置文件 | [MIT License](LICENSE) |
| 模型、几何、骨骼、动画及模型配套纹理 | [模型资产限制许可协议](LICENSE-MODELS.md) |
| 官方 JAR、项目名称与 Logo | [官方发行与二次分发政策](DISTRIBUTION-POLICY.md) |
| 未被单独许可的其他非代码媒体资产 | 保留所有权利 |

模型资产不属于 MIT 授权范围。未经 EdDYON 书面许可，禁止将模型资产用于商业用途，禁止为取得或二次利用而解包、提取，禁止在其他项目中复用、移植、修改或再分发。正常下载安装并进行个人、非商业游戏不受影响。

## 官方版本与二次分发声明

请仅从 [本 GitHub 仓库](https://github.com/EdDYON/there-is-a-bird) 及作者明确声明的官方渠道下载模组。源代码采用 MIT 协议，但官方 JAR 同时包含不属于 MIT 的模型、动画、纹理及其他受限资产。

未经许可，禁止提取或修改受限资产，禁止上传、分享或出售包含受限资产的修改版 JAR，禁止使用「Bird Watching / 哪来的鸟？」、原 Logo 或作者 EdDYON 的身份将非官方版本冒充为正式版本。供提交代码贡献使用的仓库 Fork、本地编译和测试不受影响，但不得对外分发测试 JAR，具体规则见 [官方发行与二次分发政策](DISTRIBUTION-POLICY.md)。

## 服务端照片管理

照片索引保存在世界 SavedData 中，JPEG 文件保存在：

```text
<世界>/data/guaniao/photos
```

备份世界时必须同时备份这个目录。管理员可使用：

```text
/guaniao photo stats
/guaniao photo stats <玩家>
/guaniao photo list <玩家>
/guaniao photo delete <PhotoId>
/guaniao photo restore <PhotoId>
/guaniao photo verify
/guaniao photo prune dry_run
/guaniao photo prune confirm
```

删除会先进入回收站；达到配置的保留天数后，`prune confirm` 才会永久删除文件。


- `main`：当前 1.20.1 源码。
- `1.21.1`：与 `main` 相同的 1.20.1 源码快照，作为后续移植到 1.21.1 的起点；目前不代表已兼容 1.21.1。
