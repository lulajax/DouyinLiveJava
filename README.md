# DouyinLiveJava

抖音直播弹幕采集 Java 客户端：连接抖音直播间 WebSocket、解析 protobuf 消息，以事件方式分发（弹幕 / 礼物 / 进房 / 点赞 / 榜单 / 在线人数 等）。设计参考 [TikTokLiveJava](https://github.com/jwdeveloper/TikTok-Live-Java)。

> **本仓库不含任何平台签名算法实现。** 连接抖音 WSS 需要一个「签名服务」产出带签名的 URL，本客户端通过 HTTP 调用它（接口契约见下），签名服务需你自行提供。

## 架构

```
                          ┌── 你的签名服务 (HTTP) ──┐
liveId ──> DouyinLiveClient ──> POST /sign ─────────> 返回 wssUrl + headers
                │                                       │
                └──< 连接 WSS（Java-WebSocket）<─────────┘
                     心跳 → gzip 解压 → protobuf 解析 → 事件分发
```

## 目录

```
DouyinLiveJava/
├── pom.xml          # 父 pom（聚合 client + examples，统一管理依赖版本）
├── client/          # 客户端库：连接 / 解析 / 事件
│   └── src/main/proto/douyin.proto   # 抖音 webcast protobuf 定义
└── examples/        # 使用示例（独立模块，依赖 client）
    └── .../examples/{ChatPrinter,Authenticated,RawMessage}Example
```

## 签名服务接口契约

客户端通过 HTTP 把 `liveId` 换成可连接的 WSS URL。你需要提供一个实现以下契约的服务：

```
请求：POST /sign   Body: {"liveId":"<web_rid>", "cookie":"<可选登录态>"}
      （或 GET /sign?liveId=<web_rid>）
```
```json
响应：
{
  "roomId": "...",
  "title": "...",
  "wssUrl": "wss://.../webcast/im/push/v2/?...&signature=...",
  "headers": { "User-Agent": "...", "Cookie": "ttwid=..." },
  "heartbeat": { "intervalMs": 10000, "payloadHex": "3a026862" }
}
```
> `liveId` 是 `live.douyin.com/{liveId}` 里的数字（web_rid）。签名 URL 有时效，断线重连应重新请求。

## 快速开始

```bash
mvn clean package        # 构建 client 库 + examples（protoc 构建期生成 protobuf 类）

# 运行前确保签名服务在运行（默认 http://localhost:18080，用 SIGN_URL 覆盖）
java -jar examples/target/douyin-live-examples.jar 640801847218

# 其它示例用 -cp 指定类名
java -cp examples/target/douyin-live-examples.jar com.douyinlive.examples.RawMessageExample 640801847218
```

## 作为库使用

```java
DouyinLiveClient client = new DouyinLiveClient("640801847218", "http://localhost:18080");
client.addListener(new DouyinLiveListener() {
    @Override public void onChat(ChatEvent e)     { System.out.println(e.user().nickname() + ": " + e.content()); }
    @Override public void onGift(GiftEvent e)     { /* ... */ }
    @Override public void onRoomRank(RoomRankEvent e) { /* 在线榜单 */ }
    @Override public void onUnknown(String method, ByteString payload) { /* 自行解析其它消息 */ }
});
client.connect();
```

## 支持的事件

| 回调 | 消息 |
|---|---|
| `onChat` | 弹幕 |
| `onGift` | 礼物 |
| `onMember` | 进房 |
| `onLike` | 点赞 |
| `onSocial` | 关注 / 分享 |
| `onFansclub` | 粉丝团 |
| `onRoomUserSeq` | 在线 / 累计观看人数 |
| `onRoomStats` | 房间统计 |
| `onControl` | 直播控制（status=3 即直播结束）|
| `onRoomRank` | 在线榜单 |
| `onEmojiChat` | 表情弹幕 |
| `onRoomMessage` | 系统提示 |
| `onUnknown` | 其它未单独映射的消息（method + 原始 payload）|

新增消息类型：在 `MessageRouter` 的 switch 里按 `method` 加分支并补一个回调即可。

## 登录态（可选）

匿名连接能收到大部分弹幕；通过签名服务传入登录态 Cookie（尤其 `sessionid`）可收到更完整的事件。客户端侧：

```java
new DouyinLiveClient(liveId, signUrl, "sessionid=xxx; ...");   // 或环境变量 DOUYIN_COOKIE
```
> Cookie 是敏感登录凭证，请勿提交进代码库。

## 协议说明

`douyin.proto` 为抖音 webcast 协议的 protobuf 定义，构建期由 `protoc`（protoc-jar-maven-plugin）生成 Java 类。

## 免责声明

本项目仅供学习与技术研究使用。使用者须遵守抖音相关服务条款及适用法律法规，不得用于侵犯他人权益或任何商业滥用；因使用本项目产生的后果由使用者自行承担。

**本仓库不包含、不提供任何平台签名算法实现**；连接所需的签名服务由使用者自行提供。

## License

[MIT](LICENSE)
