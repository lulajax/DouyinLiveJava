package com.douyinlive.mapper;

import com.douyinlive.event.ChatEvent;
import com.douyinlive.event.ControlEvent;
import com.douyinlive.event.DouyinLiveListener;
import com.douyinlive.event.DouyinUser;
import com.douyinlive.event.EmojiChatEvent;
import com.douyinlive.event.FansclubEvent;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;
import com.douyinlive.event.RoomMessageEvent;
import com.douyinlive.event.RoomRankEvent;
import com.douyinlive.event.RoomStatsEvent;
import com.douyinlive.event.RoomUserSeqEvent;
import com.douyinlive.event.SocialEvent;
import com.douyinlive.protocol.ChatMessage;
import com.douyinlive.protocol.ControlMessage;
import com.douyinlive.protocol.EmojiChatMessage;
import com.douyinlive.protocol.FansclubMessage;
import com.douyinlive.protocol.GiftMessage;
import com.douyinlive.protocol.LikeMessage;
import com.douyinlive.protocol.MemberMessage;
import com.douyinlive.protocol.Message;
import com.douyinlive.protocol.RoomMessage;
import com.douyinlive.protocol.RoomRankMessage;
import com.douyinlive.protocol.RoomStatsMessage;
import com.douyinlive.protocol.RoomUserSeqMessage;
import com.douyinlive.protocol.SocialMessage;
import com.douyinlive.protocol.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 把抖音 protobuf 消息按 method 解析并分发给监听器。
 * 新增消息类型时在 switch 里加一个分支即可（其余走 onUnknown 兜底）。
 */
public class MessageRouter {

    private final String roomId;
    private final String title;
    private final List<DouyinLiveListener> listeners;

    public MessageRouter(String roomId, String title, List<DouyinLiveListener> listeners) {
        this.roomId = roomId;
        this.title = title;
        this.listeners = listeners;
    }

    public void onConnect() {
        for (DouyinLiveListener l : listeners) {
            safe(() -> l.onConnect(roomId, title));
        }
    }

    public void onDisconnect(int code, String reason, boolean remote) {
        for (DouyinLiveListener l : listeners) {
            safe(() -> l.onDisconnect(code, reason, remote));
        }
    }

    public void onError(Throwable t) {
        for (DouyinLiveListener l : listeners) {
            safe(() -> l.onError(t));
        }
    }

    public void route(Message m) {
        String method = m.getMethod();
        try {
            switch (method) {
                case "WebcastChatMessage" -> {
                    ChatMessage c = ChatMessage.parseFrom(m.getPayload());
                    ChatEvent e = new ChatEvent(roomId, user(c.getUser()), c.getContent());
                    dispatch(l -> l.onChat(e));
                }
                case "WebcastGiftMessage" -> {
                    GiftMessage g = GiftMessage.parseFrom(m.getPayload());
                    String giftName = g.hasGift() ? g.getGift().getName() : "";
                    GiftEvent e = new GiftEvent(roomId, user(g.getUser()), g.getGiftId(), giftName,
                            g.getRepeatCount(), g.getComboCount());
                    dispatch(l -> l.onGift(e));
                }
                case "WebcastMemberMessage" -> {
                    MemberMessage mm = MemberMessage.parseFrom(m.getPayload());
                    MemberEvent e = new MemberEvent(roomId, user(mm.getUser()), mm.getMemberCount());
                    dispatch(l -> l.onMember(e));
                }
                case "WebcastLikeMessage" -> {
                    LikeMessage lk = LikeMessage.parseFrom(m.getPayload());
                    LikeEvent e = new LikeEvent(roomId, user(lk.getUser()), lk.getCount(), lk.getTotal());
                    dispatch(l -> l.onLike(e));
                }
                case "WebcastSocialMessage" -> {
                    SocialMessage s = SocialMessage.parseFrom(m.getPayload());
                    SocialEvent e = new SocialEvent(roomId, user(s.getUser()), s.getAction(),
                            s.getShareType(), s.getFollowCount());
                    dispatch(l -> l.onSocial(e));
                }
                case "WebcastFansclubMessage" -> {
                    FansclubMessage f = FansclubMessage.parseFrom(m.getPayload());
                    FansclubEvent e = new FansclubEvent(roomId, user(f.getUser()), f.getType(), f.getContent());
                    dispatch(l -> l.onFansclub(e));
                }
                case "WebcastRoomUserSeqMessage" -> {
                    RoomUserSeqMessage r = RoomUserSeqMessage.parseFrom(m.getPayload());
                    RoomUserSeqEvent e = new RoomUserSeqEvent(roomId, r.getTotal(), r.getTotalUser(),
                            r.getTotalUserStr());
                    dispatch(l -> l.onRoomUserSeq(e));
                }
                case "WebcastRoomStatsMessage" -> {
                    RoomStatsMessage r = RoomStatsMessage.parseFrom(m.getPayload());
                    RoomStatsEvent e = new RoomStatsEvent(roomId, r.getDisplayValue(), r.getTotal(),
                            r.getDisplayLong());
                    dispatch(l -> l.onRoomStats(e));
                }
                case "WebcastControlMessage" -> {
                    ControlMessage c = ControlMessage.parseFrom(m.getPayload());
                    ControlEvent e = new ControlEvent(roomId, c.getStatus());
                    dispatch(l -> l.onControl(e));
                }
                case "WebcastRoomRankMessage" -> {
                    RoomRankMessage r = RoomRankMessage.parseFrom(m.getPayload());
                    List<RoomRankEvent.Rank> ranks = new ArrayList<>();
                    for (RoomRankMessage.RoomRank rk : r.getRanksListList()) {
                        ranks.add(new RoomRankEvent.Rank(user(rk.getUser()), rk.getScoreStr()));
                    }
                    RoomRankEvent e = new RoomRankEvent(roomId, ranks);
                    dispatch(l -> l.onRoomRank(e));
                }
                case "WebcastEmojiChatMessage" -> {
                    EmojiChatMessage em = EmojiChatMessage.parseFrom(m.getPayload());
                    EmojiChatEvent e = new EmojiChatEvent(roomId, user(em.getUser()), em.getEmojiId(),
                            em.getDefaultContent());
                    dispatch(l -> l.onEmojiChat(e));
                }
                case "WebcastRoomMessage" -> {
                    RoomMessage r = RoomMessage.parseFrom(m.getPayload());
                    RoomMessageEvent e = new RoomMessageEvent(roomId, r.getContent());
                    dispatch(l -> l.onRoomMessage(e));
                }
                default -> dispatch(l -> l.onUnknown(method, m.getPayload()));
            }
        } catch (Exception ex) {
            onError(ex);
        }
    }

    private void dispatch(java.util.function.Consumer<DouyinLiveListener> action) {
        for (DouyinLiveListener l : listeners) {
            safe(() -> action.accept(l));
        }
    }

    private static DouyinUser user(User u) {
        return new DouyinUser(u.getId(), u.getNickName());
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Exception ignore) {
            // 监听器内部异常不影响其它监听器与接收循环
        }
    }
}
