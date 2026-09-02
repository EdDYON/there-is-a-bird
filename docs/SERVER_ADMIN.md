# 服务端管理

## 照片存储

照片索引保存在世界 SavedData 中，JPEG 文件保存在：

```text
<世界>/data/guaniao/photos
```

备份世界时必须同时备份这个目录。恢复备份时也应保持世界存档和照片目录来自同一时间点。

## 管理命令

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

删除会先进入回收站；达到配置的保留天数后，`prune confirm` 才会永久删除文件。执行永久清理前先运行 `dry_run` 并完成备份。

## 性能问题报告

报告服务器性能问题时，请附上：

- 模组、Forge、Java 和服务端版本。
- 在线人数、加载区块数量和问题持续时间。
- Spark 报告或对应分析数据。
- `/forge entity list` 等实体数量信息及异常集中区块。
- 是否在无人在线时仍会持续增长。

日志与报告中请先删除公网 IP、玩家令牌、系统用户名和不必要的绝对路径。
