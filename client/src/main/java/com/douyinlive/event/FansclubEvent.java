package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/**
 * 粉丝团消息。type：粉丝团等级变动/升级等类型；content 为展示文案。
 */
public final class FansclubEvent extends UserEvent {

    private final int type;
    private final String content;

    public FansclubEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user, int type, String content) {
        super(roomId, msgId, createTime, logId, user);
        this.type = type;
        this.content = content;
    }

    /** 粉丝团消息类型。 */
    public int type() {
        return type;
    }

    /** 展示文案。 */
    public String content() {
        return content;
    }
}
