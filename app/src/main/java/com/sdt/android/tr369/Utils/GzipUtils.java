package com.sdt.android.tr369.Utils;

import android.text.TextUtils;

import com.sdt.diagnose.common.log.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipUtils {
    private static final String TAG = "GzipUtils";

    /**
     * 对数据进行GZIP压缩处理
     * 压缩条件为长度大于512byte
     *
     * @param data
     * @return
     */
    public static String dataHandleByGzip(String data) {
        if (TextUtils.isEmpty(data)) {
            return data;
        }

        byte[] inputData = data.getBytes(StandardCharsets.ISO_8859_1);
        // 打印压缩前的数据大小
        LogUtils.d(TAG, "Data size before gzip compression: " + inputData.length + " bytes");

        if (inputData.length < 512) {
            LogUtils.d(TAG, "Data size is less than 512, no compression is required");
            return data;
        }

        // 创建输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 创建 GZIPOutputStream
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            // 将数据写入 GZIPOutputStream
            gzipOutputStream.write(inputData);
        } catch (Exception e) {
            LogUtils.e(TAG, "dataHandleByGzip error, " + e.getMessage());
            return "";
        }

        // 获取压缩后的数据
        byte[] compressedData = outputStream.toByteArray();

        // 打印压缩后的数据大小
        LogUtils.d(TAG, "Data size after gzip compression: " + compressedData.length + " bytes");

        return new String(compressedData, StandardCharsets.ISO_8859_1);
    }

    /**
     * 对数据进行GZIP解压处理
     *
     * @param data
     * @return
     */
    public static String dataHandleByUnGzip(String data) {
        if (TextUtils.isEmpty(data)) {
            return data;
        }
        // 打印解压前的数据大小
        LogUtils.d(TAG, "Data size before decompression: " + data.length());

        // 压缩的数据
        byte[] compressedData = data.getBytes(StandardCharsets.ISO_8859_1);

        // 创建输入流
        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);

        // 创建 GZIPInputStream
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            // 创建输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // 解压缩数据
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 获取解压缩后的数据
//            byte[] decompressedData = outputStream.toByteArray();
//            String str = new String(decompressedData);
            String str = outputStream.toString();
            // 打印解压后的数据大小
            LogUtils.d(TAG, "Data size after decompression: " + str.length());

            return str;
        } catch (Exception e) {
            LogUtils.e(TAG, "dataHandleByUnGzip error, " + e.getMessage());
            return "";
        }
    }
}
