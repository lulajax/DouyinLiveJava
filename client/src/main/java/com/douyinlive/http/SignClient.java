package com.douyinlive.http;

import com.google.gson.Gson;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 签名服务客户端。通过 HTTP 调用签名服务，把 liveId / roomId 换成可连接的 WSS URL，
 * 或查询用户直播状态。本类只做 HTTP 契约调用，不含任何抖音逆向逻辑。
 * 签名 / ttwid 有时效，断线重连应重新调用。
 */
public class SignClient {

    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public SignClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 用 liveId(web_rid) 签名（经进房接口取 room_id 与元数据）。 */
    public SignResult sign(String liveId) throws Exception {
        return sign(liveId, null);
    }

    public SignResult sign(String liveId, String cookie) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("liveId", liveId);
        if (cookie != null && !cookie.isBlank()) payload.put("cookie", cookie);
        return postSign(payload);
    }

    /** 用 room_id 直连签名（跳过进房接口，配合 status 拿到的 roomId 使用，无需 web_rid）。 */
    public SignResult signByRoomId(String roomId) throws Exception {
        return signByRoomId(roomId, null);
    }

    public SignResult signByRoomId(String roomId, String cookie) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("roomId", roomId);
        if (cookie != null && !cookie.isBlank()) payload.put("cookie", cookie);
        return postSign(payload);
    }

    private SignResult postSign(Map<String, String> payload) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/sign"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("签名服务返回 " + resp.statusCode() + ": " + resp.body());
        }
        SignResult result = gson.fromJson(resp.body(), SignResult.class);
        if (result == null || result.wssUrl == null) {
            throw new RuntimeException("签名服务返回异常: " + resp.body());
        }
        return result;
    }

    /** 用数字 uid 查询直播状态与 room_id。 */
    public StatusResult statusByUid(String uid) throws Exception {
        return queryStatus("uid", uid);
    }

    /** 用 sec_uid 查询直播状态与 room_id。 */
    public StatusResult statusBySecUid(String secUid) throws Exception {
        return queryStatus("secUid", secUid);
    }

    private StatusResult queryStatus(String key, String value) throws Exception {
        String url = baseUrl + "/status?" + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("状态查询返回 " + resp.statusCode() + ": " + resp.body());
        }
        StatusResult result = gson.fromJson(resp.body(), StatusResult.class);
        if (result == null) {
            throw new RuntimeException("状态查询返回异常: " + resp.body());
        }
        return result;
    }
}
