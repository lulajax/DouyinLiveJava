package com.douyinlive.event;

/** 进房/成员消息事件。memberCount 为当前直播间累计观看人数。 */
public record MemberEvent(String roomId, DouyinUser user, long memberCount) {
}
