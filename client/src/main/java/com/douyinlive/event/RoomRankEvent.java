package com.douyinlive.event;

import java.util.List;

/**
 * 在线观众榜单（小时榜/贡献榜等）。
 */
public record RoomRankEvent(String roomId, List<Rank> ranks) {

    /** 榜单单项：用户 + 分值展示串。 */
    public record Rank(DouyinUser user, String scoreStr) {
    }
}
