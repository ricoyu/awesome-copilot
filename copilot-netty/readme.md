# copilot-netty

Netty 学习/验证模块（包名 `com.awesomecopilot.netty`）。生产代码目前只有一个工具类，价值集中在 `src/test` 下 50+ 个可运行的示例与验证用例。

# 一 能力总览

| 分类 | 核心类 | 提供什么 |
|------|--------|----------|
| 工具 | `netty.utils.ByteBufUtils` | `toBytes` / `toString`（含 charset、索引区间重载）——处理过 ByteBuf 读指针踩坑的安全读法 |
| 示例 | `src/test/.../netty/echo` | Echo 服务：TCP 半包/粘包处理演示 |
| 示例 | `.../chat` | 群聊服务（ChannelGroup 广播） |
| 示例 | `.../heartbeat` | IdleStateHandler 心跳检测 |
| 示例 | `.../http` | Netty 实现 HTTP 服务 |
| 示例 | `.../barrage` | 弹幕（WebSocket 广播） |
| 示例 | `.../codec` | 自定义编码器/解码器、protobuf + protostuff 编解码 |
| 示例 | `.../bytebuf` | ByteBuf 分配、引用计数、CompositeByteBuf 实验 |
| 示例 | `.../base` | Channel/EventLoop/Pipeline 基础行为验证 |

# 二 ByteBufUtils 为什么存在

`ByteBuf.toString(Charset)` 之前有 `readBytes`/`skipBytes` 移动读指针后结果错误等隐患，`ByteBufUtils` 统一走"不动 readerIndex 的绝对读取"，排查问题时优先用它。

```java
String body = ByteBufUtils.toString(buf, StandardCharsets.UTF_8);
byte[] raw  = ByteBufUtils.toBytes(buf);
```
