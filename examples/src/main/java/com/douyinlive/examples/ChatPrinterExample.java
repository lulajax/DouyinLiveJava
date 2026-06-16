package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.event.DouyinLiveListener;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;

/**
 * 基础示例：连接直播间并打印弹幕/礼物/进房/点赞。
 *
 *   1. 先启动签名服务：cd ../sign-service && node server.js
 *   2. 运行：
 *        mvn -q -pl examples -am package
 *        java -jar examples/target/douyin-live-examples.jar 640801847218
 *
 * 环境变量 SIGN_URL 覆盖签名服务地址（默认 http://localhost:18080）。
 */
public class ChatPrinterExample {

    public static void main(String[] args) throws Exception {
        String liveId = args.length > 0 ? args[0] : "640801847218";
        String signUrl = System.getenv().getOrDefault("SIGN_URL", "http://localhost:18080");

        System.out.println("连接直播间 liveId=" + liveId + " 经签名服务 " + signUrl);

        DouyinLiveClient client = new DouyinLiveClient(liveId, signUrl);
        client.addListener(new DouyinLiveListener() {
            @Override
            public void onConnect(String roomId, String title) {
                System.out.println("✅ 已连接：" + title + "  (roomId=" + roomId + ")\n");
            }

            @Override
            public void onChat(ChatEvent e) {
                System.out.println("💬 " + e.user().nickname() + ": " + e.content());
            }

            @Override
            public void onGift(GiftEvent e) {
                System.out.println("🎁 " + e.user().nickname() + " 送出 " + e.giftName() + " x" + e.repeatCount());
            }

            @Override
            public void onMember(MemberEvent e) {
                System.out.println("👤 " + e.user().nickname() + " 来了 (累计 " + e.memberCount() + ")");
            }

            @Override
            public void onLike(LikeEvent e) {
                System.out.println("👍 " + e.user().nickname() + " 点赞 +" + e.count() + " (累计 " + e.total() + ")");
            }

            @Override
            public void onDisconnect(int code, String reason, boolean remote) {
                System.out.println("❌ 连接关闭 code=" + code + " reason=" + reason);
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("⚠️ " + error.getMessage());
            }
        });

        client.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        Thread.currentThread().join();
    }
}
