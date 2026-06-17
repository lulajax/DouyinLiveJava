package com.douyinlive.event;

/**
 * 直播控制消息。status=3 通常表示直播已结束。
 */
public final class ControlEvent extends BaseEvent {

    private final int status;

    public ControlEvent(String roomId, long msgId, long createTime, String logId, int status) {
        super(roomId, msgId, createTime, logId);
        this.status = status;
    }

    /** 控制状态码。 */
    public int status() {
        return status;
    }

    public boolean isLiveEnded() {
        return status == 3;
    }
}
