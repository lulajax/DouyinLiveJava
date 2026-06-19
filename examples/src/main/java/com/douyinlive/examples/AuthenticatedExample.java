package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.http.SignProvider;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.listener.DouyinLiveListener;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * 登录态示例：带 Cookie 连接，可收到更完整的弹幕/事件。
 *
 * 配置从 .env 读取（缺失则回退到系统环境变量）：
 *   LIVE_ID、SIGN_URL、DOUYIN_COOKIE
 * 不配置 DOUYIN_COOKIE 时自动退化为匿名连接。
 */
public class AuthenticatedExample {

    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String liveId = args.length > 0 ? args[0] : env.get("LIVE_ID", "640801847218");
        SignProvider provider = SignProvider.fromConfig(env::get);   // 按配置自动选 RapidAPI / 自建
        String cookie = env.get("DOUYIN_COOKIE", "");

        boolean authed = cookie != null && !cookie.isBlank();
        if (!authed) {
            System.out.println("未配置 DOUYIN_COOKIE，将以匿名方式连接（配置后可收到更完整的弹幕/事件）。");
        }

        DouyinLiveClient client = new DouyinLiveClient(liveId, provider, cookie);
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
