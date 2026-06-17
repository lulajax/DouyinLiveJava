package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/** 弹幕/聊天消息事件。 */
public final class ChatEvent extends UserEvent {

    private final String content;

    public ChatEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user, String content) {
        super(roomId, msgId, createTime, logId, user);
        this.content = content;
    }

    /** 弹幕文本。 */
    public String content() {
        return content;
    }
}
