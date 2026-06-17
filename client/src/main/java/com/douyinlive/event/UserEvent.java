package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/**
 * 带用户信息的消息事件基类（弹幕、礼物、进房、点赞、关注、粉丝团、表情弹幕等）。
 * 在 {@link BaseEvent} 公共字段之上增加消息关联用户。
 */
public abstract class UserEvent extends BaseEvent {

    private final DouyinUser user;

    protected UserEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user) {
        super(roomId, msgId, createTime, logId);
        this.user = user;
    }

    /** 消息关联用户（发送者/进房者等）。 */
    public DouyinUser user() {
        return user;
    }
}
