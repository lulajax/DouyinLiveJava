package com.douyinlive.event;

/** 弹幕/聊天消息事件。 */
public record ChatEvent(String roomId, DouyinUser user, String content) {
}
