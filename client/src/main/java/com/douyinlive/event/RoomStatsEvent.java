package com.douyinlive.event;

/**
 * 房间统计。displayValue 为数值，total 为累计值，displayLong 为完整展示串（如 "xxx 人看过"）。
 */
public record RoomStatsEvent(String roomId, long displayValue, long total, String displayLong) {
}
