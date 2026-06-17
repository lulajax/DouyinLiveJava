package com.douyinlive.event;

/**
 * 礼物连击状态。抖音 {@code repeatEnd} 仅有两态：
 * <ul>
 *   <li>{@link #ONGOING} —— 连击进行中（repeatEnd=0），repeatCount 递增；</li>
 *   <li>{@link #FINISHED} —— 连击结算（repeatEnd=1），repeatCount 为本次连击总数。</li>
 * </ul>
 * 不细分 TikTok 的 Begin/Active：那需要客户端维护「是否首条」状态，抖音 repeatEnd 已足够。
 */
public enum GiftComboState {
    ONGOING,
    FINISHED
}
