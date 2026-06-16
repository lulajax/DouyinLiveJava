package com.douyinlive.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public final class GzipUtil {

    private GzipUtil() {
    }

    /**
     * 尝试 gzip 解压；若数据不是 gzip（极少数控制帧）则原样返回。
     * 抖音 WSS 帧 compress=gzip，PushFrame.payload 一般是 gzip 压缩的 Response。
     */
    public static byte[] tryGunzip(byte[] data) {
        if (data == null || data.length < 2) {
            return data;
        }
        // gzip 魔数 0x1f 0x8b
        if ((data[0] & 0xff) != 0x1f || (data[1] & 0xff) != 0x8b) {
            return data;
        }
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gis.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return data;
        }
    }
}
