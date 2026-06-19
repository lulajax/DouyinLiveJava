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

    private final SignProvider provider;
    private final HttpClient http;
    private final Gson gson = new Gson();

    /** 以接入方式构造，推荐用 {@link SignProvider#fromConfig} / {@link SignProvider#rapidApi} 等工厂获取 provider。 */
    public SignClient(SignProvider provider) {
        this.provider = provider;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 向后兼容：直接以 baseUrl 构造，等价于自建/本地无鉴权接入。 */
    public SignClient(String baseUrl) {
        this(SignProvider.selfHosted(baseUrl));
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
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(provider.baseUrl() + provider.signPath()))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8));
        provider.authHeaders().forEach(builder::header);
        HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("签名服务返回 " + resp.statusCode() + ": " + resp.body() + authHint(resp.statusCode()));
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
        String url = provider.baseUrl() + provider.statusPath()
                + "?" + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET();
        provider.authHeaders().forEach(builder::header);
        HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("状态查询返回 " + resp.statusCode() + ": " + resp.body() + authHint(resp.statusCode()));
        }
        StatusResult result = gson.fromJson(resp.body(), StatusResult.class);
        if (result == null) {
            throw new RuntimeException("状态查询返回异常: " + resp.body());
        }
        return result;
    }

    /** 鉴权失败（401/403）时附加接入方式的引导文案。 */
    private String authHint(int statusCode) {
        if ((statusCode == 401 || statusCode == 403) && provider.authErrorHint() != null) {
            return "\n" + provider.authErrorHint();
        }
        return "";
    }
}
