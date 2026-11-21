# No More Die

Minecraft **26.3** 的 Fabric **客户端**模组：当你自己的不死图腾被触发时，立即退出当前服务器。

## 行为

- 只监听**你自己**的不死图腾，别人触发图腾不会影响你。
- 触发后走的是原版“保存并退出到标题界面”的同一套代码路径：
  `Minecraft#disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE)`
  → `ClientLevel#disconnect`
  → `Connection#disconnect(Component)` 关闭 Netty 连接。
- Minecraft 的 play 协议里没有专门的“主动退出”数据包；正常退出就是客户端干净地关闭连接。
  因此服务端收到的结果与玩家手动点“断开连接”完全一致（不是 kick，也不会发送任何自定义包）。
- 用于单人/局域网时，效果等同于退出世界。

## 原理

原版在 `ClientPacketListener#handleEntityEvent` 中处理实体事件 ID `35`（不死图腾保护）：
只有当受影响的实体就是本地玩家时，才会调用 `LocalPlayer#displayItemActivation`。
模组把 Mixin 注入点精确放在这次调用上，因此：

1. 不需要自己判断事件 ID 或实体身份，天然只对自己的图腾生效；
2. 注入点位于 `PacketUtils.ensureRunningOnSameThread` 之后，一定运行在客户端线程；
3. 注入后取消原方法剩余逻辑，直接安排退出。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 任意能启动 26.3 的版本（不声明最低版本，0.19.x 起可用） |
| Java | 随 26.3 的要求（25+） |
| Fabric API | 不需要 |

`fabric.mod.json` 只校验 Minecraft 版本，不再声明 `fabricloader` / `java` 依赖；
代码也没有调用任何 Fabric Loader API，只使用原版 Minecraft 类 + Mixin。

## 构建

```bash
./gradlew build
```

产物：`build/libs/nomoredie-1.0.1.jar`

> `gradle.properties` 末尾预留了 `127.0.0.1:7897` 代理配置（默认注释）。
> 如果 Gradle 拉取依赖超时，取消这四行 `systemProp.*` 的注释即可。

## 安装

1. 安装任意可启动 Minecraft 26.3 的 Fabric Loader（0.19.x 起）并选择 26.3；
2. 把 `nomoredie-1.0.1.jar` 放进 `.minecraft/mods/`；
3. 启动游戏。模组是纯客户端的，服务器端无需安装。

## 注意

- 如果服务器插件/反作弊**不发送**实体事件 35（极少数会屏蔽图腾动画事件），客户端就收不到触发信号，模组无法生效。
- 模组不绕过任何服务器规则，只是把“手动退出”提前到图腾触发的那一刻。
