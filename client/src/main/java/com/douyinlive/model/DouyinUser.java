package com.douyinlive.model;

/** 直播间用户的精简信息。secUid/displayId(抖音号) 在弹幕消息里可能为空字符串（抖音不一定下发）。 */
public record DouyinUser(long id, String secUid, String displayId, String nickname, String avatarUrl,
                         long followerCount, long followingCount) {
}
