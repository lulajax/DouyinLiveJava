package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/**
 * 表情弹幕。emojiId 为表情 ID，defaultContent 为兜底文本。
 */
public final class EmojiChatEvent extends UserEvent {

    private final long emojiId;
    private final String defaultContent;

    public EmojiChatEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user, long emojiId, String defaultContent) {
        super(roomId, msgId, createTime, logId, user);
        this.emojiId = emojiId;
        this.defaultContent = defaultContent;
    }

    /** 表情 ID。 */
    public long emojiId() {
        return emojiId;
    }

    /** 兜底文本。 */
    public String defaultContent() {
        return defaultContent;
    }
}
