package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/** 进房/成员消息事件。memberCount 为当前直播间累计观看人数。 */
public final class MemberEvent extends UserEvent {

    private final long memberCount;

    public MemberEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user, long memberCount) {
        super(roomId, msgId, createTime, logId, user);
        this.memberCount = memberCount;
    }

    /** 当前直播间累计观看人数。 */
    public long memberCount() {
        return memberCount;
    }
}
