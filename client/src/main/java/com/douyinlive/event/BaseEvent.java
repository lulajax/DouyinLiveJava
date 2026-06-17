package com.douyinlive.event;

/**
 * 所有直播消息事件的基类，收敛各消息共有的字段（来自 protobuf 的 Common）：
 * 直播间号、消息 ID、发送时间、服务端日志 ID。
 *
 * <p>访问器沿用 record 风格（{@code roomId()} 等），便于业务统一处理与扩展；
 * 子类只声明各自特有字段。
 */
public abstract class BaseEvent {

    private final String roomId;
    private final long msgId;
    private final long createTime;
    private final String logId;

    protected BaseEvent(String roomId, long msgId, long createTime, String logId) {
        this.roomId = roomId;
        this.msgId = msgId;
        this.createTime = createTime;
        this.logId = logId;
    }

    /** 直播间 room_id。 */
    public String roomId() {
        return roomId;
    }

    /** 消息业务唯一 ID（抖音 Common.msgId，可用于去重）。 */
    public long msgId() {
        return msgId;
    }

    /**
     * 消息发送时间（抖音 Common.createTime）。
     * 注意：抖音对部分消息会下发 0 或秒级时间戳（不统一），业务侧需校验，
     * 不可靠时应以接收时间兜底。
     */
    public long createTime() {
        return createTime;
    }

    /** 服务端日志 ID（Common.logId），排查链路用。 */
    public String logId() {
        return logId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{roomId=" + roomId + ", msgId=" + msgId + ", createTime=" + createTime + "}";
    }
}
