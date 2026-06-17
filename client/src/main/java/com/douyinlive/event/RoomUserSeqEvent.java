package com.douyinlive.event;

/**
 * 在线/累计观看人数。total=当前在线热度，totalUser=累计观看人数，displayStr=展示串。
 */
public final class RoomUserSeqEvent extends BaseEvent {

    private final long total;
    private final long totalUser;
    private final String displayStr;

    public RoomUserSeqEvent(String roomId, long msgId, long createTime, String logId,
                            long total, long totalUser, String displayStr) {
        super(roomId, msgId, createTime, logId);
        this.total = total;
        this.totalUser = totalUser;
        this.displayStr = displayStr;
    }

    /** 当前在线热度。 */
    public long total() {
        return total;
    }

    /** 累计观看人数。 */
    public long totalUser() {
        return totalUser;
    }

    /** 展示串。 */
    public String displayStr() {
        return displayStr;
    }
}
