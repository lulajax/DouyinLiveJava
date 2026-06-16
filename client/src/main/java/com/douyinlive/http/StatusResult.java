package com.douyinlive.http;

/** 签名服务 /status 接口的返回结构（由 gson 反序列化）。 */
public class StatusResult {
    public String uid;
    public String secUid;
    public String nickname;
    public boolean live;       // 是否在播
    public String roomId;      // 在播时为字符串 room_id（可直接用于连接），未开播为 null
}
