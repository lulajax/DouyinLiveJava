package com.douyinlive.event;

/**
 * 直播间系统提示消息（公告/系统文案等）。
 */
public final class RoomMessageEvent extends BaseEvent {

    private final String content;

    public RoomMessageEvent(String roomId, long msgId, long createTime, String logId, String content) {
        super(roomId, msgId, createTime, logId);
        this.content = content;
    }

    /** 系统提示文本。 */
    public String content() {
        return content;
    }
}
