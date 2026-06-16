package com.douyinlive.event;

/**
 * 表情弹幕。emojiId 为表情 ID，defaultContent 为兜底文本。
 */
public record EmojiChatEvent(String roomId, DouyinUser user, long emojiId, String defaultContent) {
}
