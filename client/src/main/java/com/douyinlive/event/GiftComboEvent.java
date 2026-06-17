package com.douyinlive.event;

import com.douyinlive.model.DouyinUser;

/**
 * 礼物连击进度事件：每条 GiftMessage 都会触发，{@link #state()} 标识连击进行中还是已结算。
 *
 * <p>用途区分（对标 TikTok 的 GiftComboEvent / GiftEvent）：
 * <ul>
 *   <li>本事件（{@code onGiftCombo}）——每条都发，做实时连击进度/动画展示；</li>
 *   <li>{@link GiftEvent}（{@code onGift}）——仅在结算态发一次，做入账/落库统计。</li>
 * </ul>
 */
public final class GiftComboEvent extends UserEvent {

    private final long giftId;
    private final String giftName;
    private final long diamondCount;
    private final long repeatCount;
    private final long comboCount;
    private final long groupId;
    private final GiftComboState state;

    public GiftComboEvent(String roomId, long msgId, long createTime, String logId, DouyinUser user,
                          long giftId, String giftName, long diamondCount, long repeatCount, long comboCount,
                          long groupId, GiftComboState state) {
        super(roomId, msgId, createTime, logId, user);
        this.giftId = giftId;
        this.giftName = giftName;
        this.diamondCount = diamondCount;
        this.repeatCount = repeatCount;
        this.comboCount = comboCount;
        this.groupId = groupId;
        this.state = state;
    }

    /** 礼物 ID。 */
    public long giftId() {
        return giftId;
    }

    /** 礼物名称。 */
    public String giftName() {
        return giftName;
    }

    /** 单个礼物钻石价值。 */
    public long diamondCount() {
        return diamondCount;
    }

    /** 连击累计次数（结算态为本次连击总数）。 */
    public long repeatCount() {
        return repeatCount;
    }

    /** 连击数。 */
    public long comboCount() {
        return comboCount;
    }

    /** 连击组 ID（抖音为秒级时间戳，跨主播会撞，需配合 secUid 区分）。 */
    public long groupId() {
        return groupId;
    }

    /** 连击状态：进行中 / 已结算。 */
    public GiftComboState state() {
        return state;
    }

    /** 连击是否已结算（state==FINISHED，等价于 repeatEnd==1）。 */
    public boolean isEnd() {
        return state == GiftComboState.FINISHED;
    }
}
