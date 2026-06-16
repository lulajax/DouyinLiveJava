package com.douyinlive.http;

import java.util.Map;

/** 签名服务 /sign 接口的返回结构（由 gson 反序列化）。 */
public class SignResult {
    public String liveId;
    public String roomId;
    public String title;
    public int status;       // 2 = 直播中
    public Long userCount;
    public String wssUrl;
    public Map<String, String> headers;   // 连接 WSS 需要的请求头：User-Agent / Cookie
    public Heartbeat heartbeat;

    public static class Heartbeat {
        public int intervalMs;
        public String payloadHex;
    }
}
