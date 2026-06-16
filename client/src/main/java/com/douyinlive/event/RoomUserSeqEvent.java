package com.douyinlive.event;

/**
 * 在线/累计观看人数。total=当前在线热度，totalUser=累计观看人数，displayStr=展示串。
 */
public record RoomUserSeqEvent(String roomId, long total, long totalUser, String displayStr) {
}
