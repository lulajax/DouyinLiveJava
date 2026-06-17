package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/**
 * 关注/分享消息。action：1=关注，3=分享（抖音约定）；followCount 为主播当前粉丝数。
 */
public final class SocialEvent extends UserEvent {

    private final long action;
    private final long shareType;
    private final long followCount;

    public SocialEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user,
                       long action, long shareType, long followCount) {
        super(roomId, msgId, createTime, logId, user);
        this.action = action;
        this.shareType = shareType;
        this.followCount = followCount;
    }

    /** 行为类型：1=关注，3=分享。 */
    public long action() {
        return action;
    }

    /** 分享渠道类型。 */
    public long shareType() {
        return shareType;
    }

    /** 主播当前粉丝数。 */
    public long followCount() {
        return followCount;
    }

    public boolean isFollow() {
        return action == 1;
    }

    public boolean isShare() {
        return action == 3;
    }
}
