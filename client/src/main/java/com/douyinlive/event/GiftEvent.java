package com.douyinlive.event;

/** 礼物消息事件。 */
public record GiftEvent(String roomId, DouyinUser user, long giftId, String giftName,
                        long repeatCount, long comboCount) {
}
