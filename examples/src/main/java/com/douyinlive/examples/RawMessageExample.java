package com.douyinlive.examples;

import com.douyinlive.DouyinLiveClient;
import com.douyinlive.http.SignProvider;
import com.douyinlive.event.ChatEvent;
import com.douyinlive.listener.DouyinLiveListener;
import com.douyinlive.event.GiftEvent;
import com.douyinlive.event.LikeEvent;
import com.douyinlive.event.MemberEvent;
import com.google.protobuf.ByteString;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 原始消息示例：统计直播间各类消息出现次数（含未单独映射的 onUnknown 消息），
 * 每 5 秒打印一次。用于了解某直播间都推送哪些 method，便于按需扩展映射。
 *
 *   java -cp examples/target/douyin-live-examples.jar \
 *        com.douyinlive.examples.RawMessageExample 640801847218
 */
public class RawMessageExample {

    public static void main(String[] args) throws Exception {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String liveId = args.length > 0 ? args[0] : env.get("LIVE_ID", "640801847218");
        SignProvider provider = SignProvider.fromConfig(env::get);   // 按配置自动选 RapidAPI / 自建
        String cookie = env.get("DOUYIN_COOKIE", "");

        Map<String, AtomicLong> tally = new ConcurrentHashMap<>();

        DouyinLiveClient client = new DouyinLiveClient(liveId, provider, cookie);
        client.addListener(new DouyinLiveListener() {
            @Override
            public void onConnect(String roomId, String title) {
                System.out.println("✅ 已连接：" + title + "，开始统计消息类型...\n");
            }

            @Override
            public void onChat(ChatEvent e) {
                count("WebcastChatMessage");
            }

            @Override
            public void onGift(GiftEvent e) {
                count("WebcastGiftMessage");
            }

            @Override
            public void onMember(MemberEvent e) {
                count("WebcastMemberMessage");
            }

            @Override
            public void onLike(LikeEvent e) {
                count("WebcastLikeMessage");
            }

            @Override
            public void onUnknown(String method, ByteString payload) {
                // 未映射的消息：只统计 method，payload 可按需自行 parseFrom 对应类型
                count(method);
            }

            private void count(String method) {
                tally.computeIfAbsent(method, k -> new AtomicLong()).incrementAndGet();
            }
        });

        client.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));

        // 每 5 秒打印一次统计
        while (client.isOpen()) {
            Thread.sleep(5000);
            System.out.println("---- 消息类型统计 ----");
            tally.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                    .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
            System.out.println();
        }
    }
}
