package com.sdt.diagnose.common;

import android.net.NetworkUtils;

import com.sdt.diagnose.common.log.LogUtils;

import java.net.Inet4Address;
import java.util.regex.Pattern;

public class NetUtils {
    private static final String TAG = "NetUtils";

    /*
     * convert subMask string to prefix length
     */
    public static int maskStr2InetMask(String maskStr) {
        StringBuffer sb;
        String str;
        int inetmask = 0;
        int count = 0;
        /*
         * check the subMask format
         */
        Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
        if (!pattern.matcher(maskStr).matches()) {
            LogUtils.e(TAG, "subMask is error");
            return 0;
        }

        String[] ipSegment = maskStr.split("\\.");
        for (String s : ipSegment) {
            sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(s)));
            str = sb.reverse().toString();
            count = 0;
            for (int i = 0; i < str.length(); i++) {
                i = str.indexOf("1", i);
                if (i == -1)
                    break;
                count++;
            }
            inetmask += count;
        }
        return inetmask;
    }


    public static Inet4Address getInet4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            LogUtils.e(TAG, "getInet4Address error, " + e.getMessage());
            return null;
        }
    }

}
