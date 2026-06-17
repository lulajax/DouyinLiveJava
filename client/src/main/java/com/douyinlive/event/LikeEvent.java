package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/** 点赞消息事件。count 为本次点赞数，total 为直播间累计点赞数。 */
public final class LikeEvent extends UserEvent {

    private final long count;
    private final long total;

    public LikeEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user, long count, long total) {
        super(roomId, msgId, createTime, logId, user);
        this.count = count;
        this.total = total;
    }

    /** 本次点赞数。 */
    public long count() {
        return count;
    }

    /** 直播间累计点赞数。 */
    public long total() {
        return total;
    }
}
