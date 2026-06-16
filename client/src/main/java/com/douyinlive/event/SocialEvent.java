package com.douyinlive.event;

/**
 * 关注/分享消息。action：1=关注，3=分享（抖音约定）；followCount 为主播当前粉丝数。
 */
public record SocialEvent(String roomId, DouyinUser user, long action, long shareType, long followCount) {

    public boolean isFollow() {
        return action == 1;
    }

    public boolean isShare() {
        return action == 3;
    }
}
