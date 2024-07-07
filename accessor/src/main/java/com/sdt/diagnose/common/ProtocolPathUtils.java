package com.sdt.diagnose.common;

import java.util.List;

public class ProtocolPathUtils {
    private final static String REGEX = "\\.";

    public static String[] parse(String prefix, String path) {
        if (!path.startsWith(prefix)) {
            return null;
        }
        return path.replace(prefix, "").split(REGEX);
    }

    public static String getInfoFromArray(String prefix, String path, IProtocolArray array) {
        if (!path.startsWith(prefix)) {
            return null;
        }

        String[] paramsArr = path.replace(prefix, "").split(REGEX);
        if (paramsArr.length < 1) {
            //Todo report error.
            return null;
        }
        int index = 0;
        try {
            index = Integer.parseInt(paramsArr[0]);
            if (index < 1) {
                //Todo report error.
                return null;
            }
        } catch (NumberFormatException e) {
            //Todo report error.
            return null;
        }
        List list = array.getArray();
        if (list == null || list.size() < 1)
            return null;
        // param 是从1开始计数
        if (index > list.size()) {
            //Todo report error.
            return null;
        }
        if (list.get(index - 1) == null)
            return null;

        return array.getValue(list.get(index - 1), paramsArr);
    }
}
