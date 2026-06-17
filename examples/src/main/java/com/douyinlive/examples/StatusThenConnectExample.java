package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.MemberEvent;
import com.douyinlive.http.SignClient;
import com.douyinlive.http.StatusResult;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * 状态查询 + room_id 直连示例：
 *   输入 secUid → 查直播状态 → 在播则用 room_id 直连弹幕（无需 web_rid）。
 *
 *   java -cp examples/target/douyin-live-examples.jar \
 *        com.douyinlive.examples.StatusThenConnectExample <secUid>
 */
public class StatusThenConnectExample {

    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String signUrl = env.get("SIGN_URL", "http://localhost:18080");
        String cookie = env.get("DOUYIN_COOKIE", "");
        String secUid = args.length > 0 ? args[0]
                : env.get("SEC_UID", "MS4wLjABAAAA7rOkPwkReCsi5xr42lOi3d8s7hs_4WadleWvQkEwLjw");

        // 1. 查直播状态（uid 也可用 sign.statusByUid(uid)）
        SignClient sign = new SignClient(signUrl);
        StatusResult st = sign.statusBySecUid(secUid);
        System.out.println("主播 " + st.nickname + " | 在播=" + st.live + " | roomId=" + st.roomId);
        if (!st.live) {
            System.out.println("未开播，退出。");
            return;
        }

        // 2. 用 status 拿到的 room_id 直连（无需 web_rid）
        DouyinLiveClient client = DouyinLiveClient.byRoomId(st.roomId, signUrl, cookie);
        client.addListener(new DouyinLiveListener() {
            @Override
            public void onConnect(String roomId, String title) {
                System.out.println("✅ 已连接 room=" + roomId + "\n");
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
                System.out.println("👤 " + e.user().nickname() + " 来了");
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("⚠️ " + t.getMessage());
            }
        });

        client.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        Thread.currentThread().join();
    }
}
