package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * 基础示例：连接直播间并打印弹幕/礼物/进房/点赞。
 *
 *   1. 先启动签名服务：cd ../sign-service && node server.js
 *   2. 运行：
 *        mvn -q -pl examples -am package
 *        java -jar examples/target/douyin-live-examples.jar 640801847218
 *
 * 配置（LIVE_ID / SIGN_URL / DOUYIN_COOKIE）从 .env 读取，缺失则回退到系统环境变量。
 */
public class ChatPrinterExample {

    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String liveId = args.length > 0 ? args[0] : env.get("LIVE_ID", "640801847218");
        String signUrl = env.get("SIGN_URL", "http://localhost:18080");
        String cookie = env.get("DOUYIN_COOKIE", "");

        boolean authed = cookie != null && !cookie.isBlank();
        System.out.println("连接直播间 liveId=" + liveId + " 经签名服务 " + signUrl
                + (authed ? "  [登录态]" : "  [匿名]"));

        DouyinLiveClient client = new DouyinLiveClient(liveId, signUrl, cookie);
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
