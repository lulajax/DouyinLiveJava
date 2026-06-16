package com.douyinlive.event;

/**
 * 直播间系统提示消息（公告/系统文案等）。
 */
public record RoomMessageEvent(String roomId, String content) {
}
