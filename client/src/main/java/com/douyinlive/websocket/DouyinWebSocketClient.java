package com.douyinlive.websocket;

import com.douyinlive.mapper.MessageRouter;
import com.douyinlive.protocol.Message;
import com.douyinlive.protocol.PushFrame;
import com.douyinlive.protocol.Response;
import com.douyinlive.util.GzipUtil;
import com.google.protobuf.ByteString;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 抖音弹幕 WSS 客户端：连接 + 心跳 + ack + 帧解码 + 分发。
 * 接收循环：PushFrame -> gzip 解压 -> Response -> (needAck 时回 ack) -> 遍历 messagesList 路由。
 */
public class DouyinWebSocketClient extends WebSocketClient {

    /** 心跳帧：固定字节 3a 02 68 62 */
    private static final byte[] HEARTBEAT = {0x3a, 0x02, 0x68, 0x62};

    private final MessageRouter router;
    private final long heartbeatIntervalMs;
    private ScheduledExecutorService heartbeatExecutor;

    public DouyinWebSocketClient(URI uri, Map<String, String> headers, long heartbeatIntervalMs, MessageRouter router) {
        super(uri, new Draft_6455(), headers, 0);
        this.router = router;
        this.heartbeatIntervalMs = heartbeatIntervalMs > 0 ? heartbeatIntervalMs : 10000;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        router.onConnect();
        send(HEARTBEAT);
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "douyin-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                try {
                    send(HEARTBEAT);
                } catch (Exception ignore) {
                    // 关闭中发送失败可忽略
                }
            }
        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onMessage(String message) {
        // 抖音只下发二进制帧，文本忽略
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);

            PushFrame frame = PushFrame.parseFrom(data);
            byte[] payload = GzipUtil.tryGunzip(frame.getPayload().toByteArray());
            Response response = Response.parseFrom(payload);

            if (response.getNeedAck()) {
                PushFrame ack = PushFrame.newBuilder()
                        .setLogId(frame.getLogId())
                        .setPayloadType("ack")
                        .setPayload(ByteString.copyFromUtf8(response.getInternalExt()))
                        .build();
                send(ack.toByteArray());
            }

            for (Message m : response.getMessagesListList()) {
                router.route(m);
            }
        } catch (Exception e) {
            router.onError(e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        router.onDisconnect(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        router.onError(ex);
    }
}
