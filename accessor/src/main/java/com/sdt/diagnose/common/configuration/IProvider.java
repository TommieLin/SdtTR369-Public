package com.sdt.diagnose.common.configuration;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * 与configdb下的IProvider一致
 */
public interface IProvider {
    /**
     * 表格名
     */
    String CONFIG_TABLE_NAME = "config";

    /**
     * authority
     */
    String AUTOHORITY = "com.skyworth.config.ConfigProvider";
    Uri AUTOHORITY_URI = Uri.parse("content://" + AUTOHORITY + "/config");

    String CALL_METHOD_USER_KEY = "user_key";
    String CALL_METHOD_GET_CONFIG = "GET_config";
    String CALL_METHOD_PUT_CONFIG = "PUT_config";

    /**
     * config表格的基本信息的字符串
     */
    final class ConfigColumns implements BaseColumns {
        public static final String NAME = "configName";
        public static final String VALUE = "configValue";
        public static final String DEFAULT_SORT_ORDER = BaseColumns._ID + " desc";
    }
}
