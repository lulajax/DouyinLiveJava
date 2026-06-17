package com.douyinlive.event;

/**
 * 房间统计。displayValue 为数值，total 为累计值，displayLong 为完整展示串（如 "xxx 人看过"）。
 */
public final class RoomStatsEvent extends BaseEvent {

    private final long displayValue;
    private final long total;
    private final String displayLong;

    public RoomStatsEvent(String roomId, long msgId, long createTime, String logId,
                          long displayValue, long total, String displayLong) {
        super(roomId, msgId, createTime, logId);
        this.displayValue = displayValue;
        this.total = total;
        this.displayLong = displayLong;
    }

    /** 展示数值。 */
    public long displayValue() {
        return displayValue;
    }

    /** 累计值。 */
    public long total() {
        return total;
    }

    /** 完整展示串。 */
    public String displayLong() {
        return displayLong;
    }
}
