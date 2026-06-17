package com.douyinlive.mapper;

import com.douyinlive.event.GiftEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 礼物连击聚合器：把抖音「连击进行态多条 repeatCount 递增 + 结算态一条 repeatEnd==1」
 * 收敛成恰好一次入账事件（{@link GiftEvent}，经 {@code onGift} 下发），并对未正常结算的连击做超时兜底，
 * 保证使用者只听 {@code onGift} 即可拿到「不漏不重」的入账事件、无需自己缓存或加定时器。
 *
 * <p>由 {@link MessageRouter} 持有（每连接一个实例），按 {@code groupId} 聚合（单连接=单主播，
 * groupId 在直播间内唯一）。超时检查由 WS 心跳定时驱动（{@code onTick}），连接关闭时 {@code flushAll} 兜底。
 */
public class GiftComboAggregator {

    private record Entry(GiftEvent event, long ts) {
    }

    private final long timeoutMs;
    private final Consumer<GiftEvent> giftSink;
    private final Map<Long, Entry> pending = new ConcurrentHashMap<>();

    public GiftComboAggregator(long timeoutMs, Consumer<GiftEvent> giftSink) {
        this.timeoutMs = timeoutMs;
        this.giftSink = giftSink;
    }

    /**
     * 处理一条礼物消息。
     *
     * @param gift 本条礼物对应的入账事件（结算态 repeatCount 为本次连击总数）
     * @param end  是否结算态（repeatEnd==1）
     * @param now  当前时间（毫秒）
     */
    public void handle(GiftEvent gift, boolean end, long now) {
        if (end) {
            // 结算/单次：清掉进行态缓存，立即入账
            pending.remove(gift.groupId());
            giftSink.accept(gift);
        } else {
            // 连击进行中：按 groupId 覆盖缓存最新态（repeatCount 递增），等结算或超时兜底
            pending.put(gift.groupId(), new Entry(gift, now));
        }
    }

    /** 定时检查：把超时未结算的连击按缓存的最新进行态（repeatCount 当前最大）补发入账。 */
    public void checkTimeout(long now) {
        pending.forEach((groupId, en) -> {
            if (now - en.ts() > timeoutMs && pending.remove(groupId, en)) {
                giftSink.accept(en.event());
            }
        });
    }

    /** 连接关闭：把所有未结算的连击补发入账，避免断流/直播结束漏单。 */
    public void flushAll() {
        pending.forEach((groupId, en) -> {
            if (pending.remove(groupId, en)) {
                giftSink.accept(en.event());
            }
        });
    }
}
