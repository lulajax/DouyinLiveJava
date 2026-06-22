package com.douyinlive.http;

/** 签名服务 /status 接口的返回结构（由 gson 反序列化）。 */
public class StatusResult {
    public String uid;
    public String secUid;
    public String nickname;
    public boolean live;       // 是否在播（签名服务已用 room_ids_display 判真实在播，过滤空直播间）
    public String roomId;      // 在播时为字符串 room_id（可直接用于连接），未开播为 null

    public Integer roomIdsDisplay; // own_room.room_ids_display[0]：1=真实公开直播，0=空/不公开房间
    public String avatarUrl;       // 主播头像
    public String displayId;       // 抖音号
    public String signature;       // 个人简介
    public Integer gender;         // 性别（1男 2女）
    public Boolean verified;       // 是否认证
    public long followerCount;     // 粉丝数
    public long followingCount;    // 关注数
    public Integer fansClubLevel;  // 粉丝团等级
    public String fansClubName;    // 粉丝团名称
    public String webRid;          // 直播间短号（拼 live.douyin.com/{webRid}），来自 profile/other 的 room_data.owner.web_rid
}
