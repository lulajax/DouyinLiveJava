package com.douyinlive;

import com.douyinlive.event.DouyinLiveListener;
import com.douyinlive.http.SignClient;
import com.douyinlive.http.SignResult;
import com.douyinlive.mapper.MessageRouter;
import com.douyinlive.websocket.DouyinWebSocketClient;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 抖音直播弹幕客户端：
 *   1. 调签名服务把 liveId 换成可连接的 WSS URL + headers；
 *   2. 用 Java-WebSocket 连接、解析 protobuf、分发事件。
 *
 * 用法：
 *   var client = new DouyinLiveClient("640801847218", "http://localhost:18080");
 *   client.addListener(new DouyinLiveListener(){ ... });
 *   client.connect();
 */
public class DouyinLiveClient {

    private final String liveId;   // liveId(web_rid) 模式，与 roomId 二选一
    private final String roomId;   // room_id 直连模式，与 liveId 二选一
    private final String cookie;
    private final SignClient signClient;
    private final List<DouyinLiveListener> listeners = new CopyOnWriteArrayList<>();

    private volatile DouyinWebSocketClient ws;
    private volatile SignResult lastSign;

    private DouyinLiveClient(String liveId, String roomId, String signServiceUrl, String cookie) {
        this.liveId = liveId;
        this.roomId = roomId;
        this.cookie = cookie;
        this.signClient = new SignClient(signServiceUrl);
    }

    public DouyinLiveClient(String liveId, String signServiceUrl) {
        this(liveId, null, signServiceUrl, null);
    }

    /** @param cookie 可选登录态 Cookie 串（"sessionid=...; ..."），传入可收到更完整的弹幕/事件。 */
    public DouyinLiveClient(String liveId, String signServiceUrl, String cookie) {
        this(liveId, null, signServiceUrl, cookie);
    }

    /** room_id 直连：配合 SignClient.statusBySecUid/statusByUid 拿到的 roomId 使用（无需 web_rid）。 */
    public static DouyinLiveClient byRoomId(String roomId, String signServiceUrl) {
        return new DouyinLiveClient(null, roomId, signServiceUrl, null);
    }

    public static DouyinLiveClient byRoomId(String roomId, String signServiceUrl, String cookie) {
        return new DouyinLiveClient(null, roomId, signServiceUrl, cookie);
    }

    public DouyinLiveClient addListener(DouyinLiveListener listener) {
        listeners.add(listener);
        return this;
    }

    /** 连接（阻塞直到握手完成或失败）。 */
    public void connect() throws Exception {
        SignResult sign = (roomId != null) ? signClient.signByRoomId(roomId, cookie) : signClient.sign(liveId, cookie);
        this.lastSign = sign;

        MessageRouter router = new MessageRouter(sign.roomId, sign.title, listeners);
        long interval = sign.heartbeat != null ? sign.heartbeat.intervalMs : 10000;
        DouyinWebSocketClient client = new DouyinWebSocketClient(URI.create(sign.wssUrl), sign.headers, interval, router);
        this.ws = client;

        boolean ok = client.connectBlocking();
        if (!ok) {
            throw new RuntimeException("WSS 连接失败（签名可能已过期，建议重试）");
        }
    }

    public void disconnect() {
        DouyinWebSocketClient client = this.ws;
        if (client != null) {
            client.close();
        }
    }

    public boolean isOpen() {
        DouyinWebSocketClient client = this.ws;
        return client != null && client.isOpen();
    }

    public SignResult lastSign() {
        return lastSign;
    }
}
