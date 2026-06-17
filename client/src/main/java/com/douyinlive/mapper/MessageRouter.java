package com.douyinlive.mapper;

import com.douyinlive.event.ChatEvent;
import com.douyinlive.event.ControlEvent;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.model.DouyinUser;
import com.douyinlive.event.EmojiChatEvent;
import com.douyinlive.event.FansclubEvent;
import com.douyinlive.event.GiftComboEvent;
import com.douyinlive.event.GiftComboState;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;
import com.douyinlive.event.RoomMessageEvent;
import com.douyinlive.event.RoomRankEvent;
import com.douyinlive.event.RoomStatsEvent;
import com.douyinlive.event.RoomUserSeqEvent;
import com.douyinlive.event.SocialEvent;
import com.douyinlive.protocol.ChatMessage;
import com.douyinlive.protocol.Common;
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
 * 公共字段（msgId/createTime/logId）统一取自每条消息内嵌的 Common 并传入事件基类。
 * 新增消息类型时在 switch 里加一个分支即可（其余走 onUnknown 兜底）。
 */
public class MessageRouter {

    /** 连击超时阈值：超过此时长未结算则按当前最大 repeatCount 补发入账事件（断流/直播结束兜底）。 */
    private static final long COMBO_TIMEOUT_MS = 10_000;

    private final String roomId;
    private final String title;
    private final List<DouyinLiveListener> listeners;
    private final GiftComboAggregator giftAggregator;

    public MessageRouter(String roomId, String title, List<DouyinLiveListener> listeners) {
        this.roomId = roomId;
        this.title = title;
        this.listeners = listeners;
        // 连击聚合 + 超时兜底：结算/超时时通过 onGift 下发恰好一次入账事件
        this.giftAggregator = new GiftComboAggregator(COMBO_TIMEOUT_MS, gift -> dispatch(l -> l.onGift(gift)));
    }

    public void onConnect() {
        for (DouyinLiveListener l : listeners) {
            safe(() -> l.onConnect(roomId, title));
        }
    }

    public void onDisconnect(int code, String reason, boolean remote) {
        // 断开前把未结算的连击补发入账，避免漏单
        giftAggregator.flushAll();
        for (DouyinLiveListener l : listeners) {
            safe(() -> l.onDisconnect(code, reason, remote));
        }
    }

    /** 由 WS 心跳定时驱动：检查并补发超时未结算的连击（复用心跳调度，不额外加线程）。 */
    public void onTick() {
        giftAggregator.checkTimeout(System.currentTimeMillis());
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
                    Common cm = c.getCommon();
                    ChatEvent e = new ChatEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(c.getUser()), c.getContent());
                    dispatch(l -> l.onChat(e));
                }
                case "WebcastGiftMessage" -> {
                    GiftMessage g = GiftMessage.parseFrom(m.getPayload());
                    Common cm = g.getCommon();
                    String giftName = g.hasGift() ? g.getGift().getName() : "";
                    long diamondCount = g.hasGift() ? g.getGift().getDiamondCount() : 0;
                    boolean end = g.getRepeatEnd() == 1;
                    DouyinUser u = user(g.getUser());
                    // 连击进度：每条礼物消息都发（含进行中与结算态），供实时连击 UI
                    GiftComboEvent combo = new GiftComboEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            u, g.getGiftId(), giftName, diamondCount, g.getRepeatCount(), g.getComboCount(),
                            g.getGroupId(), end ? GiftComboState.FINISHED : GiftComboState.ONGOING);
                    dispatch(l -> l.onGiftCombo(combo));
                    // 入账：交给聚合器——结算态(repeatEnd==1)立即发 onGift；进行态缓存，等结算或超时兜底
                    GiftEvent gift = new GiftEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            u, g.getGiftId(), giftName, diamondCount,
                            g.getRepeatCount(), g.getComboCount(), g.getRepeatEnd(), g.getGroupId());
                    giftAggregator.handle(gift, end, System.currentTimeMillis());
                }
                case "WebcastMemberMessage" -> {
                    MemberMessage mm = MemberMessage.parseFrom(m.getPayload());
                    Common cm = mm.getCommon();
                    MemberEvent e = new MemberEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(mm.getUser()), mm.getMemberCount());
                    dispatch(l -> l.onMember(e));
                }
                case "WebcastLikeMessage" -> {
                    LikeMessage lk = LikeMessage.parseFrom(m.getPayload());
                    Common cm = lk.getCommon();
                    LikeEvent e = new LikeEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(lk.getUser()), lk.getCount(), lk.getTotal());
                    dispatch(l -> l.onLike(e));
                }
                case "WebcastSocialMessage" -> {
                    SocialMessage s = SocialMessage.parseFrom(m.getPayload());
                    Common cm = s.getCommon();
                    SocialEvent e = new SocialEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(s.getUser()), s.getAction(), s.getShareType(), s.getFollowCount());
                    dispatch(l -> l.onSocial(e));
                }
                case "WebcastFansclubMessage" -> {
                    FansclubMessage f = FansclubMessage.parseFrom(m.getPayload());
                    Common cm = f.getCommonInfo();
                    FansclubEvent e = new FansclubEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(f.getUser()), f.getType(), f.getContent());
                    dispatch(l -> l.onFansclub(e));
                }
                case "WebcastRoomUserSeqMessage" -> {
                    RoomUserSeqMessage r = RoomUserSeqMessage.parseFrom(m.getPayload());
                    Common cm = r.getCommon();
                    RoomUserSeqEvent e = new RoomUserSeqEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            r.getTotal(), r.getTotalUser(), r.getTotalUserStr());
                    dispatch(l -> l.onRoomUserSeq(e));
                }
                case "WebcastRoomStatsMessage" -> {
                    RoomStatsMessage r = RoomStatsMessage.parseFrom(m.getPayload());
                    Common cm = r.getCommon();
                    RoomStatsEvent e = new RoomStatsEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            r.getDisplayValue(), r.getTotal(), r.getDisplayLong());
                    dispatch(l -> l.onRoomStats(e));
                }
                case "WebcastControlMessage" -> {
                    ControlMessage c = ControlMessage.parseFrom(m.getPayload());
                    Common cm = c.getCommon();
                    ControlEvent e = new ControlEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            c.getStatus());
                    dispatch(l -> l.onControl(e));
                }
                case "WebcastRoomRankMessage" -> {
                    RoomRankMessage r = RoomRankMessage.parseFrom(m.getPayload());
                    Common cm = r.getCommon();
                    List<RoomRankEvent.Rank> ranks = new ArrayList<>();
                    for (RoomRankMessage.RoomRank rk : r.getRanksListList()) {
                        ranks.add(new RoomRankEvent.Rank(user(rk.getUser()), rk.getScoreStr()));
                    }
                    RoomRankEvent e = new RoomRankEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(), ranks);
                    dispatch(l -> l.onRoomRank(e));
                }
                case "WebcastEmojiChatMessage" -> {
                    EmojiChatMessage em = EmojiChatMessage.parseFrom(m.getPayload());
                    Common cm = em.getCommon();
                    EmojiChatEvent e = new EmojiChatEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            user(em.getUser()), em.getEmojiId(), em.getDefaultContent());
                    dispatch(l -> l.onEmojiChat(e));
                }
                case "WebcastRoomMessage" -> {
                    RoomMessage r = RoomMessage.parseFrom(m.getPayload());
                    Common cm = r.getCommon();
                    RoomMessageEvent e = new RoomMessageEvent(roomId, cm.getMsgId(), cm.getCreateTime(), cm.getLogId(),
                            r.getContent());
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
        String avatarUrl = "";
        if (u.hasAvatarThumb() && u.getAvatarThumb().getUrlListListCount() > 0) {
            avatarUrl = u.getAvatarThumb().getUrlListList(0);
        }
        long followerCount = 0;
        long followingCount = 0;
        if (u.hasFollowInfo()) {
            followerCount = u.getFollowInfo().getFollowerCount();
            followingCount = u.getFollowInfo().getFollowingCount();
        }
        return new DouyinUser(u.getId(), u.getSecUid(), u.getDisplayId(), u.getNickName(), avatarUrl, followerCount, followingCount);
    }

    private void safe(Runnable r) {
        try {
            r.run();
        } catch (Exception ignore) {
            // 监听器内部异常不影响其它监听器与接收循环
        }
    }
}
