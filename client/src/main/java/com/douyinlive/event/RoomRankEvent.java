package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;
import java.util.List;

/**
 * 在线观众榜单（小时榜/贡献榜等）。
 */
public final class RoomRankEvent extends BaseEvent {

    private final List<Rank> ranks;

    public RoomRankEvent(String roomId, long msgId, long createTime, String logId, List<Rank> ranks) {
        super(roomId, msgId, createTime, logId);
        this.ranks = ranks;
    }

    /** 榜单列表。 */
    public List<Rank> ranks() {
        return ranks;
    }

    /** 榜单单项：用户 + 分值展示串。 */
    public record Rank(DouyinUser user, String scoreStr) {
    }
}
