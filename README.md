# Bird Watching

一个为 Minecraft Forge 1.20.1 制作的鸟类模组。

## 开发环境

- Minecraft 1.20.1
- Forge 47.2.0
- Java 17
- GeckoLib 4.4.9

## 构建

在项目根目录运行：

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs`。

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

## 管理员压测

压测会分批生成带标记的测试鸟、采集 tick 耗时，并在结束后自动清理：

```text
/guaniao stress start <鸟数量 1-400> <秒数 10-900> [半径 8-64]
/guaniao stress status
/guaniao stress stop
/guaniao stress cleanup
/guaniao perf
/guaniao perf reset
```

建议依次测试 50、100、200 只鸟，每档至少运行 600 秒，并保存平均、P95、最大 tick 耗时及估算 TPS。不要在正式存档首次执行 200～400 只测试；先复制存档或使用专门测试世界。

## 分支说明

- `main`：当前 1.20.1 源码。
- `1.21.1`：与 `main` 相同的 1.20.1 源码快照，作为后续移植到 1.21.1 的起点；目前不代表已兼容 1.21.1。
