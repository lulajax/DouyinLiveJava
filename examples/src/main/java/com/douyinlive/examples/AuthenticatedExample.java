package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.event.DouyinLiveListener;

/**
 * 登录态示例：带 Cookie 连接，可收到更完整的弹幕/事件。
 *
 *   DOUYIN_COOKIE="sessionid=xxx; ..." \
 *   java -cp examples/target/douyin-live-examples.jar \
 *        com.douyinlive.examples.AuthenticatedExample 640801847218
 *
 * 不设置 DOUYIN_COOKIE 时自动退化为匿名连接。
 */
public class AuthenticatedExample {

    public static void main(String[] args) throws Exception {
        String liveId = args.length > 0 ? args[0] : "640801847218";
        String signUrl = System.getenv().getOrDefault("SIGN_URL", "http://localhost:18080");
        String cookie = System.getenv("DOUYIN_COOKIE");

        boolean authed = cookie != null && !cookie.isBlank();
        if (!authed) {
            System.out.println("未设置 DOUYIN_COOKIE，将以匿名方式连接（设置后可收到更完整的弹幕/事件）。");
        }

        DouyinLiveClient client = new DouyinLiveClient(liveId, signUrl, cookie);
        client.addListener(new DouyinLiveListener() {
            @Override
            public void onConnect(String roomId, String title) {
                System.out.println("✅ 已连接：" + title + (authed ? "  [登录态]" : "  [匿名]"));
            }

            @Override
            public void onChat(ChatEvent e) {
                System.out.println("💬 " + e.user().nickname() + ": " + e.content());
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
