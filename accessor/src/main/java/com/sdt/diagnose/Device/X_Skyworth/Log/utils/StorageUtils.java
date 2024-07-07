package com.sdt.diagnose.Device.X_Skyworth.Log.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.sdt.diagnose.Device.X_Skyworth.Log.bean.Property;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ClassName: StorageUtils
 *
 * <p>ClassDescription: StorageUtils
 *
 * <p>Author: ZHX Date: 2022/10/28
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class StorageUtils {
    private static final String TAG = "StorageUtils";

    /**
     * write property
     *
     * @param file:file   absolute path
     * @param key:key
     * @param value:value
     */
    public static void writeProperty(String file, String key, String value) {
        try {
            Set<Property> properties = new HashSet<>(readInternal(file));
            Property property = new Property(key, value);
            properties.remove(property);
            properties.add(property);
            writeInternal(file, properties);
        } catch (Exception e) {
            LogUtils.e(TAG, "writeProperty error: " + e.getMessage());
        }
    }

    /**
     * read property
     *
     * @param file:file        absolute path
     * @param key:key
     * @param defValue:default value
     * @return value
     */
    public static String readProperty(String file, String key, String defValue) {
        Set<Property> properties = readInternal(file);
        Optional<String> optional =
                properties.stream()
                        .filter(s -> TextUtils.equals(s.getKey(), key))
                        .map(Property::getValue)
                        .findFirst();
        return optional.orElse(defValue);
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private static void writeInternal(String file, Set<Property> properties) {
        try {
            File f = new File(file);
            if (!f.exists()) {
                f.createNewFile();
                f.setReadable(true, false);
                f.setWritable(true, false);
                f.setExecutable(true, false);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "writeInternal file error: " + e.getMessage());
        }
        try (FileOutputStream outStream = new FileOutputStream(file)) {
            for (Property property : properties) {
                outStream.write((property.formatString() + "\n").getBytes());
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "writeInternal FileOutputStream error: " + e.getMessage());
        }
    }

    private static Set<Property> readInternal(String file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(file))))) {
            return reader.lines()
                    .map(Property::fromString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LogUtils.e(TAG, "readInternal error: " + e.getMessage());
            return Collections.emptySet();
        }
    }
}
