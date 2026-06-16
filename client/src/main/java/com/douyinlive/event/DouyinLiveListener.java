package com.douyinlive.event;

import com.google.protobuf.ByteString;

/**
 * 直播间事件监听器。所有方法都是 default 空实现，按需覆盖。
 */
public interface DouyinLiveListener {

    /** 连接成功（已拿到房间信息）。 */
    default void onConnect(String roomId, String title) {
    }

    /** 弹幕。 */
    default void onChat(ChatEvent event) {
    }

    /** 礼物。 */
    default void onGift(GiftEvent event) {
    }

    /** 进房/成员。 */
    default void onMember(MemberEvent event) {
    }

    /** 点赞。 */
    default void onLike(LikeEvent event) {
    }

    /** 关注/分享。 */
    default void onSocial(SocialEvent event) {
    }

    /** 粉丝团。 */
    default void onFansclub(FansclubEvent event) {
    }

    /** 在线/累计观看人数。 */
    default void onRoomUserSeq(RoomUserSeqEvent event) {
    }

    /** 房间统计（如观看数展示）。 */
    default void onRoomStats(RoomStatsEvent event) {
    }

    /** 直播控制消息（如直播结束 status=3）。 */
    default void onControl(ControlEvent event) {
    }

    /** 在线观众榜单。 */
    default void onRoomRank(RoomRankEvent event) {
    }

    /** 表情弹幕。 */
    default void onEmojiChat(EmojiChatEvent event) {
    }

    /** 直播间系统提示消息。 */
    default void onRoomMessage(RoomMessageEvent event) {
    }

    /** 未单独映射的消息（method + 原始 payload），供高级用法自行解析。 */
    default void onUnknown(String method, ByteString payload) {
    }

    /** 连接关闭。 */
    default void onDisconnect(int code, String reason, boolean remote) {
    }

    /** 处理过程中的异常。 */
    default void onError(Throwable error) {
    }
}
