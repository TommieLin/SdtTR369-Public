package com.sdt.diagnose.common;

import com.sdt.diagnose.common.log.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static String readFileToStr(String filePath) {
        BufferedReader reader = null;
        int readChar;
        StringBuilder fileStrBuffer = new StringBuilder("");
        if (null == filePath) {
            return null;
        }

        try {
            reader = new BufferedReader(new FileReader(filePath));
            while ((readChar = reader.read()) != -1) {
                fileStrBuffer.append((char) readChar);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "readFileToStr error, " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LogUtils.e(TAG, "readFileToStr finally error, " + e.getMessage());
                }
            }
        }
        return fileStrBuffer.toString();
    }

    public static byte[] fileToByte(File img) {
        byte[] bytes = new byte[0];
        try (InputStream in = Files.newInputStream(img.toPath())) {
            bytes = new byte[in.available()];
            in.read(bytes);
        } catch (IOException e) {
            LogUtils.e(TAG, "fileToByte error, " + e.getMessage());
        }
        return bytes;
    }
}
