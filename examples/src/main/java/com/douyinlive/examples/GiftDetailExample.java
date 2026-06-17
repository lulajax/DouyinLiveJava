package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.model.DouyinUser;
import com.douyinlive.event.GiftComboEvent;
import com.douyinlive.event.GiftEvent;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * 礼物消息详情示例：带 Cookie 连接，打印礼物消息的完整字段，便于排查/了解直播间都推送了哪些内容。
 *
 *   java -cp examples/target/douyin-live-examples.jar \
 *        com.douyinlive.examples.GiftDetailExample 640801847218
 *
 * 配置从项目根目录的 .env 读取（缺失则回退到系统环境变量）：
 *   LIVE_ID、SIGN_URL、DOUYIN_COOKIE
 * 复制模板开始：cp .env.example .env
 */
public class GiftDetailExample {

    public static void main(String[] args) throws Exception {
        // 找不到 .env 时不报错，自动回退到系统环境变量
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String liveId = args.length > 0 ? args[0] : env.get("LIVE_ID", "640801847218");
        String signUrl = env.get("SIGN_URL", "http://localhost:18080");
        String cookie = env.get("DOUYIN_COOKIE", "");

        boolean authed = cookie != null && !cookie.isBlank();
        if (!authed) {
            System.out.println("未配置 DOUYIN_COOKIE，将以匿名方式连接（在 .env 里设置可收到更完整的事件）。");
        }
        System.out.println("连接直播间 liveId=" + liveId + " 经签名服务 " + signUrl
                + (authed ? "  [登录态]" : "  [匿名]"));

        DouyinLiveClient client = new DouyinLiveClient(liveId, signUrl, cookie);
        client.addListener(new DouyinLiveListener() {
            @Override
            public void onConnect(String roomId, String title) {
                System.out.println("✅ 已连接：" + title + "  (roomId=" + roomId + ")\n");
            }

            @Override
            public void onGift(GiftEvent e) {
                System.out.println("🎁 [礼物] " + user(e.user())
                        + " giftId=" + e.giftId()
                        + " giftName=\"" + e.giftName() + "\""
                        + " diamondCount=" + e.diamondCount()
                        + " repeatCount=" + e.repeatCount()
                        + " comboCount=" + e.comboCount()
                        + " repeatEnd=" + e.repeatEnd()
                        + " groupId=" + e.groupId()
                        + " msgId=" + e.msgId());
            }

            // @Override
            // public void onGiftCombo(GiftComboEvent e) {
            //     // 连击进度：每条都回调（ONGOING 进行中 / FINISHED 结算）
            //     System.out.println("✨ [礼物·连击 " + e.state() + "] " + user(e.user())
            //             + " giftName=\"" + e.giftName() + "\""
            //             + " repeatCount=" + e.repeatCount()
            //             + " comboCount=" + e.comboCount()
            //             + " groupId=" + e.groupId()
            //             + " msgId=" + e.msgId());
            // }

            @Override
            public void onError(Throwable error) {
                System.err.println("⚠️ " + error.getMessage());
            }
        });

        client.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        Thread.currentThread().join();
    }

    /** 把用户信息格式化为一行详情。 */
    private static String user(DouyinUser u) {
        return "user{id=" + u.id()
                + ", nickname=\"" + u.nickname() + "\""
                + ", followerCount=" + u.followerCount()
                + ", followingCount=" + u.followingCount()
                + ", avatar=" + u.avatarUrl() + "}";
    }
}
