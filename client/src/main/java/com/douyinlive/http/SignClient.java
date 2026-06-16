package com.douyinlive.http;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Node 签名服务，输入 liveId（及可选登录态 cookie）取回可连接的 WSS URL + headers。
 * 由于签名 / ttwid 有时效，每次（含断线重连）都应重新调用本接口取新 URL。
 * 用 POST 传 cookie，避免登录态进入 URL / 访问日志。
 */
public class SignClient {

    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public SignClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 匿名签名。 */
    public SignResult sign(String liveId) throws Exception {
        return sign(liveId, null);
    }

    /**
     * @param cookie 可选登录态 Cookie 串（"sessionid=...; ..."），传 null/空则匿名。
     */
    public SignResult sign(String liveId, String cookie) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("liveId", liveId);
        if (cookie != null && !cookie.isBlank()) {
            payload.put("cookie", cookie);
        }
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
}
