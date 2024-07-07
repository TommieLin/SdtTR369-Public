package com.sdt.android.tr369.Utils;

import android.content.Context;
import android.os.SystemProperties;

import com.sdt.diagnose.common.log.LogUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FileUtils {
    private static final String TAG = "FileUtils";
    public static final String PLATFORM_TMS_TR369_MODEL_DEFAULT = "sdt_tms_tr369_model.default";
    public static final String PLATFORM_TMS_TR369_MODEL_XML = "sdt_tms_tr369_model.xml";
    public static final String SYS_PROP_TR369_SOFTWARE_VERSION = "persist.sys.tr369.software.version";

    public static void copyTr369AssetsToFile(Context context) {
        File modelFile = new File(context.getFilesDir(), PLATFORM_TMS_TR369_MODEL_XML);
        File defaultFile = new File(context.getFilesDir(), PLATFORM_TMS_TR369_MODEL_DEFAULT);

        String version = SystemProperties.get(SYS_PROP_TR369_SOFTWARE_VERSION, "");
        String curSoftwareVersion = getSoftwareVersion();

        /* 判断model是否需要更新 */
        if (!curSoftwareVersion.equals(version)
                || !modelFile.exists()
                || !defaultFile.exists()) {
            LogUtils.i(TAG, "It is detected that the Model file needs to be created.");
            copyAssetFile(context, PLATFORM_TMS_TR369_MODEL_XML, modelFile);
            copyAssetFile(context, PLATFORM_TMS_TR369_MODEL_DEFAULT, defaultFile);
            SystemProperties.set(SYS_PROP_TR369_SOFTWARE_VERSION, curSoftwareVersion);
        }
    }

    public static String getSoftwareVersion() {
        long utc = SystemProperties.getLong("ro.build.date.utc", 1631947123L);
        return String.valueOf(utc * 1000L);
    }

    private static void copyAssetFile(Context context, String inFileName, File outFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getAssets().open(inFileName);
            outputStream = Files.newOutputStream(outFile.toPath());
            int c;
            while ((c = inputStream.read()) != -1) {
                outputStream.write(c);
            }
            outputStream.flush();
            chmod(outFile);
        } catch (IOException e) {
            LogUtils.e(TAG, "copyAssetFile error, " + e.getMessage());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                LogUtils.e(TAG, "copyAssetFile finally error, " + e.getMessage());
            }
        }
    }

    private static void chmod(File outFileName) throws IOException {
        Path path = Paths.get(outFileName.getPath());
        Set perms = Files.readAttributes(path, PosixFileAttributes.class).permissions();
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(path, perms);
    }

}
