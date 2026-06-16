package com.douyinlive.event;

/** 点赞消息事件。count 为本次点赞数，total 为直播间累计点赞数。 */
public record LikeEvent(String roomId, DouyinUser user, long count, long total) {
}
