# DouyinLiveJava — Douyin Live Danmaku (Bullet-Chat) Java SDK

[![Stars](https://img.shields.io/github/stars/lulajax/DouyinLiveJava?style=social)](https://github.com/lulajax/DouyinLiveJava/stargazers)
[![License](https://img.shields.io/github/license/lulajax/DouyinLiveJava)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/)
[![Last commit](https://img.shields.io/github/last-commit/lulajax/DouyinLiveJava)](https://github.com/lulajax/DouyinLiveJava/commits)

[简体中文](README.md) | **English**

A Java client for collecting Douyin (TikTok China) live-stream danmaku: it connects to a live room's WebSocket, parses the protobuf messages, and dispatches them as events (chat / gift / join / like / rank / viewer count, and more). Design inspired by [TikTokLiveJava](https://github.com/jwdeveloper/TikTok-Live-Java).

> **Keywords**: douyin danmaku, douyin live, live danmaku, bullet-screen / barrage, live chat crawler, douyin live room messages, webcast, 抖音弹幕, 抖音直播, 弹幕采集, Java SDK.

> **This repository contains NO platform signing-algorithm implementation.** Connecting to the Douyin WSS requires a "sign service" that produces a signed URL; this client calls it over HTTP (contract below). You can subscribe to the hosted [RapidAPI gateway](#how-to-get-a-sign-service-subscription) (a few minutes to set up) or provide your own.

## Architecture

```
                          ┌── your sign service (HTTP) ──┐
liveId ──> DouyinLiveClient ──> POST /sign ──────────────> returns wssUrl + headers
                │                                            │
                └──< connect WSS (Java-WebSocket) <──────────┘
                     heartbeat → gzip inflate → protobuf parse → event dispatch
```

## Layout

```
DouyinLiveJava/
├── pom.xml          # parent pom (aggregates client + examples, manages dependency versions)
├── client/          # client library: connect / parse / events
│   └── src/main/proto/douyin.proto   # Douyin webcast protobuf definitions
└── examples/        # usage examples (standalone module, depends on client)
    └── .../examples/{ChatPrinter,Authenticated,RawMessage,StatusThenConnect}Example
```

## Sign-service contract

The client turns a `liveId` into a connectable WSS URL over HTTP. You need a service that implements the following contract:

```
Request: POST /sign   Body: {"liveId":"<web_rid>", "cookie":"<optional login state>"}
         (or GET /sign?liveId=<web_rid>)
```
```json
Response:
{
  "roomId": "...",
  "title": "...",
  "wssUrl": "wss://.../webcast/im/push/v2/?...&signature=...",
  "headers": { "User-Agent": "...", "Cookie": "ttwid=..." },
  "heartbeat": { "intervalMs": 10000, "payloadHex": "3a026862" }
}
```
> `liveId` is the number in `live.douyin.com/{liveId}` (the web_rid). Signed URLs expire — request a fresh one when reconnecting.

Two more endpoints:

```
GET /status?uid=<numeric uid> or ?secUid=<sec_uid>
  -> {"uid","secUid","nickname","live":true,"roomId":"..."}   // live status; roomId is null when live=false

GET /sign?roomId=<room_id>  or POST /sign {"roomId":"..."}
  -> same as the /sign response (connect directly by room_id, skipping room entry, no web_rid needed)
```
> Typical flow: `/status` to get `roomId` → `/sign?roomId=` to get `wssUrl` → connect (no web_rid required).

## How to get a sign-service subscription

Don't want to host your own sign service? Just subscribe to the hosted RapidAPI gateway — **ready in minutes**:

1. Open the listing → [douyin-sign-api on RapidAPI](https://rapidapi.com/tikhub-team-tikhub-team-default/api/douyin-sign-api/playground/apiendpoint_4a3adc62-7f1a-4bed-9df2-cf14b34e2837) and pick a plan;
2. Copy your **X-RapidAPI-Key** from the console;
3. Put it into `.env` at the project root (see `.env.example`): `RAPIDAPI_KEY=your_key`.

The access mode is **auto-detected** from the keys in `.env` — switch by editing config, no code changes needed:

| key in `.env` | access mode | baseUrl | auth headers |
|---|---|---|---|
| `RAPIDAPI_KEY` set | **RapidAPI gateway** (recommended · paid) | `https://douyin-sign-api.p.rapidapi.com` | `x-rapidapi-key` / `x-rapidapi-host` |
| empty | self-hosted / local | `SIGN_URL` (default `http://localhost:18080`) | none |

> Endpoint-path differences are handled internally by the SDK: RapidAPI uses `/api/v1/douyin/sign` and `/api/v1/douyin/sign/status`; self-hosted uses `/sign` and `/status`.

You can also pick the access mode explicitly in code: `SignProvider.rapidApi("your_key")`, `SignProvider.selfHosted("http://localhost:18080")`.

## Quick start

```bash
mvn clean package        # build the client lib + examples (protobuf classes generated at build time by protoc)

# make sure the sign service is running first (default http://localhost:18080, override with SIGN_URL)
java -jar examples/target/douyin-live-examples.jar 640801847218

# run other examples by passing the class name with -cp
java -cp examples/target/douyin-live-examples.jar com.douyinlive.examples.RawMessageExample 640801847218
```

## Add the dependency via JitPack

[![JitPack](https://jitpack.io/v/lulajax/DouyinLiveJava.svg)](https://jitpack.io/#lulajax/DouyinLiveJava)

No manual build required — pull `douyin-live-client` directly via [JitPack](https://jitpack.io/#lulajax/DouyinLiveJava):

**Maven** — add the repository first, then the dependency (note the multi-module groupId form `com.github.user.repo`):

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.lulajax.DouyinLiveJava</groupId>
  <artifactId>douyin-live-client</artifactId>
  <version>v1.0.0</version>
</dependency>
```

**Gradle**:

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.lulajax.DouyinLiveJava:douyin-live-client:v1.0.0' }
```

> Use a [Release](https://github.com/lulajax/DouyinLiveJava/releases) tag for the version (e.g. `v1.0.0`), or `main-SNAPSHOT` for the latest build off the main branch.

## Use as a library

```java
// auto-select the access mode from .env (RapidAPI / self-hosted); or use SignProvider.rapidApi("your_key")
SignProvider provider = SignProvider.fromConfig(System::getenv);
DouyinLiveClient client = new DouyinLiveClient("640801847218", provider);
client.addListener(new DouyinLiveListener() {
    @Override public void onChat(ChatEvent e)     { System.out.println(e.user().nickname() + ": " + e.content()); }
    @Override public void onGift(GiftEvent e)     { /* ... */ }
    @Override public void onRoomRank(RoomRankEvent e) { /* online rank */ }
    @Override public void onUnknown(String method, ByteString payload) { /* parse other messages yourself */ }
});
client.connect();
```

### Check live status first, then connect directly by room_id

```java
SignProvider provider = SignProvider.fromConfig(System::getenv);
SignClient sign = new SignClient(provider);
StatusResult st = sign.statusBySecUid("MS4w...");   // or sign.statusByUid("<numeric uid>")
if (st.live) {
    DouyinLiveClient client = DouyinLiveClient.byRoomId(st.roomId, provider);
    client.addListener(/* ... */);
    client.connect();   // connect directly by room_id, no web_rid needed
}
```

## Supported events

| callback | message |
|---|---|
| `onChat` | chat / danmaku |
| `onGift` | gift |
| `onMember` | join |
| `onLike` | like |
| `onSocial` | follow / share |
| `onFansclub` | fans club |
| `onRoomUserSeq` | current / cumulative viewers |
| `onRoomStats` | room stats |
| `onControl` | live control (status=3 means the stream ended) |
| `onRoomRank` | online rank |
| `onEmojiChat` | emoji danmaku |
| `onRoomMessage` | system notice |
| `onUnknown` | other un-mapped messages (method + raw payload) |

To add a new message type: add a branch by `method` in the `MessageRouter` switch and provide a matching callback.

## Login state (optional)

An anonymous connection receives most danmaku; passing a login-state Cookie (especially `sessionid`) through the sign service yields more complete events. On the client side:

```java
new DouyinLiveClient(liveId, provider, "sessionid=xxx; ...");   // provider as above; the Cookie can also come from the DOUYIN_COOKIE env var
```
> The Cookie is a sensitive login credential — do not commit it to the repository.

## Protocol notes

`douyin.proto` is the protobuf definition of the Douyin webcast protocol; Java classes are generated at build time by `protoc` (protoc-jar-maven-plugin).

## Disclaimer

This project is for learning and technical research only. Users must comply with Douyin's terms of service and applicable laws and regulations; it must not be used to infringe others' rights or for any commercial abuse. You bear all consequences arising from its use.

**This repository neither contains nor provides any platform signing-algorithm implementation**; the sign service required for connecting must be provided by the user.

## License

[MIT](LICENSE)
