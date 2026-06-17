package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.event.ControlEvent;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.model.DouyinUser;
import com.douyinlive.event.EmojiChatEvent;
import com.douyinlive.event.FansclubEvent;
import com.douyinlive.event.GiftComboEvent;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;
import com.douyinlive.event.RoomMessageEvent;
import com.douyinlive.event.RoomRankEvent;
import com.douyinlive.event.RoomStatsEvent;
import com.douyinlive.event.RoomUserSeqEvent;
import com.douyinlive.event.SocialEvent;
import com.google.protobuf.ByteString;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * 消息详情示例：带 Cookie 连接，覆盖所有事件类型并打印每条消息的完整字段，
 * 便于排查/了解直播间都推送了哪些内容。
 *
 *   java -cp examples/target/douyin-live-examples.jar \
 *        com.douyinlive.examples.MessageDetailExample 640801847218
 *
 * 配置从项目根目录的 .env 读取（缺失则回退到系统环境变量）：
 *   LIVE_ID、SIGN_URL、DOUYIN_COOKIE
 * 复制模板开始：cp .env.example .env
 */
public class MessageDetailExample {

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
            public void onChat(ChatEvent e) {
                System.out.println("💬 [弹幕] " + user(e.user()) + " content=\"" + e.content() + "\"");
            }

            @Override
            public void onGift(GiftEvent e) {
                // 入账：仅连击结算/单次时一次，repeatCount=本次连击总数
                System.out.println("🎁 [礼物·入账] " + user(e.user())
                        + " giftId=" + e.giftId()
                        + " giftName=\"" + e.giftName() + "\""
                        + " diamondCount=" + e.diamondCount()
                        + " repeatCount=" + e.repeatCount()
                        + " comboCount=" + e.comboCount()
                        + " groupId=" + e.groupId());
            }

            @Override
            public void onGiftCombo(GiftComboEvent e) {
                // 连击进度：每条都回调（ONGOING 进行中 / FINISHED 结算）
                System.out.println("✨ [礼物·连击 " + e.state() + "] " + user(e.user())
                        + " giftName=\"" + e.giftName() + "\""
                        + " repeatCount=" + e.repeatCount()
                        + " comboCount=" + e.comboCount()
                        + " groupId=" + e.groupId());
            }

            @Override
            public void onMember(MemberEvent e) {
                System.out.println("👤 [进房] " + user(e.user()) + " memberCount=" + e.memberCount());
            }

            @Override
            public void onLike(LikeEvent e) {
                System.out.println("👍 [点赞] " + user(e.user()) + " count=" + e.count() + " total=" + e.total());
            }

            @Override
            public void onSocial(SocialEvent e) {
                String act = e.isFollow() ? "关注" : e.isShare() ? "分享" : "其它";
                System.out.println("🔗 [社交] " + user(e.user())
                        + " action=" + e.action() + "(" + act + ")"
                        + " shareType=" + e.shareType()
                        + " followCount=" + e.followCount());
            }

            @Override
            public void onFansclub(FansclubEvent e) {
                System.out.println("🎫 [粉丝团] " + user(e.user())
                        + " type=" + e.type() + " content=\"" + e.content() + "\"");
            }

            @Override
            public void onRoomUserSeq(RoomUserSeqEvent e) {
                System.out.println("📈 [在线人数] total(热度)=" + e.total()
                        + " totalUser(累计观看)=" + e.totalUser()
                        + " displayStr=\"" + e.displayStr() + "\"");
            }

            @Override
            public void onRoomStats(RoomStatsEvent e) {
                System.out.println("📊 [房间统计] displayValue=" + e.displayValue()
                        + " total=" + e.total()
                        + " displayLong=\"" + e.displayLong() + "\"");
            }

            @Override
            public void onControl(ControlEvent e) {
                System.out.println("🎬 [控制] status=" + e.status()
                        + (e.isLiveEnded() ? " (直播已结束)" : ""));
            }

            @Override
            public void onRoomRank(RoomRankEvent e) {
                System.out.println("🏆 [榜单] 共 " + e.ranks().size() + " 项");
                int i = 1;
                for (RoomRankEvent.Rank r : e.ranks()) {
                    System.out.println("    #" + (i++) + " " + user(r.user()) + " score=\"" + r.scoreStr() + "\"");
                }
            }

            @Override
            public void onEmojiChat(EmojiChatEvent e) {
                System.out.println("😀 [表情弹幕] " + user(e.user())
                        + " emojiId=" + e.emojiId()
                        + " defaultContent=\"" + e.defaultContent() + "\"");
            }

            @Override
            public void onRoomMessage(RoomMessageEvent e) {
                System.out.println("📢 [系统提示] content=\"" + e.content() + "\"");
            }

            @Override
            public void onUnknown(String method, ByteString payload) {
                System.out.println("❓ [未映射] method=" + method + " payloadBytes=" + payload.size());
            }

            @Override
            public void onDisconnect(int code, String reason, boolean remote) {
                System.out.println("❌ 连接关闭 code=" + code + " reason=" + reason + " remote=" + remote);
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

    /** 把用户信息格式化为一行详情。 */
    private static String user(DouyinUser u) {
        return "user{id=" + u.id()
                + ", secUid=" + u.secUid()
                + ", displayId=" + u.displayId()
                + ", nickname=\"" + u.nickname() + "\""
                + ", followerCount=" + u.followerCount()
                + ", followingCount=" + u.followingCount()
                + ", avatar=" + u.avatarUrl() + "}";
    }
}
