package com.douyinlive.event;

/**
 * 直播控制消息。status=3 通常表示直播已结束。
 */
public record ControlEvent(String roomId, int status) {

    public boolean isLiveEnded() {
        return status == 3;
    }
}
