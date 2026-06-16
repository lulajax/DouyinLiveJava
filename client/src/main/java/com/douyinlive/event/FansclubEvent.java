package com.douyinlive.event;

/**
 * 粉丝团消息。type：粉丝团等级变动/升级等类型；content 为展示文案。
 */
public record FansclubEvent(String roomId, DouyinUser user, int type, String content) {
}
