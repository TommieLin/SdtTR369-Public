package com.sdt.diagnose.common.configuration;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;

import com.sdt.diagnose.common.log.LogUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class Configs implements IProvider {
    private static final String TAG = "SkyConfigs";

    private static class NameValueCache {
        private final Uri mUri;
        private static final String[] SELECT_VALUE = new String[]{ConfigColumns.VALUE};
        private static final String NAME_EQ_PLACEHOLDER = "name=?";

        // Must synchronize on 'this' to access mValues and mValuesVersion.
        private final HashMap<String, String> mCaches = new HashMap<String, String>();

        // Initially null; set lazily and held forever. Synchronized on 'this'.
        private ContentProviderClient mContentProvider;

        // The method we'll call (or null, to not use) on the provider
        // for the fast path of retrieving settings.
        private final String mCallGetCommand;
        private final String mCallSetCommand;
        private boolean isDBChange;

        NameValueCache(Uri uri, String getCommand, String setCommand) {
            mUri = uri;
            mCallGetCommand = getCommand;
            mCallSetCommand = setCommand;
            isDBChange = false;
        }

        private ContentProviderClient getContentProviderClient(ContentResolver cr) {
            ContentProviderClient cp = null;
            synchronized (this) {
                cp = mContentProvider;
                if (cp == null) {
                    cp = mContentProvider = cr.acquireContentProviderClient(mUri);
                    ContentObserver observer =
                            new ContentObserver(null) {
                                @Override
                                public void onChange(boolean selfChange, Uri uri) {
                                    super.onChange(selfChange, uri);
                                    try {
                                        String last = uri.getLastPathSegment();
                                        if (last != null) { // last must not null
                                            synchronized (NameValueCache.this) {
                                                String remove = mCaches.remove(last); // removed
                                                if (remove != null) {
                                                    LogUtils.w(TAG, "onChange last: " + last);
                                                } else if (mCaches.size() >= 1) {
                                                    // 有超过1个才清除，如果都没有缓存无需处理
                                                    isDBChange = true;
                                                    LogUtils.w(TAG, "on db change uri: " + uri);
                                                }
                                            }
                                        } else { // last must not null
                                            throw new RuntimeException();
                                        }
                                    } catch (Exception e) {
                                        LogUtils.e(TAG, "onChange error, " + e.getMessage());
                                    }
                                }
                            };
                    cr.registerContentObserver(mUri, true, observer);
                }
            }
            return cp;
        }

        boolean putStringForUser(ContentResolver cr, String name, String value, int userHandle)
                throws RemoteException {
            Bundle arg = new Bundle();
            arg.putString(ConfigColumns.VALUE, value);
            arg.putInt(CALL_METHOD_USER_KEY, userHandle);
            ContentProviderClient cp = getContentProviderClient(cr);
            // cr.call(mCallSetCommand, name, arg);
            cp.call(mCallSetCommand, name, arg);
            return true;
        }

        String getStringForUser(ContentResolver cr, String name, int userHandle)
                throws RemoteException {
            // 本进程调用，就用缓存的方式给值，如果其他进程调用，就去查询值再缓存？ 可以修整为都从缓存中获取
            final boolean isSelf = true;
            if (isSelf) {
                // Our own user's settings data uses a client-side cache
                synchronized (this) {
                    if (isDBChange) {
                        LogUtils.v(TAG, "invalidate [" + mUri.getLastPathSegment() + "]: db Changed");
                        mCaches.clear();
                        isDBChange = false;
                    }

                    if (mCaches.containsKey(name)) {
                        String value = mCaches.get(name);
                        LogUtils.v(TAG, "from settings cache, name = " + name + ", value = " + value);
                        return value;
                    }
                }
            } else {
                LogUtils.v(TAG, "get setting for user " + userHandle + " by user " + Config.getMyUserId()
                        + " so skipping cache");
            }

            ContentProviderClient cp = getContentProviderClient(cr);

            // Try the fast path first, not using query(). If this
            // fails (alternate Settings provider that doesn't support
            // this interface?) then we fall back to the query/table
            // interface.
            if (mCallGetCommand != null && cp != null) {
                Bundle args = null;
                Bundle b = cp.call(mCallGetCommand, name, args);
                if (b != null) {
                    b.remove(CALL_METHOD_USER_KEY); // 放置错误返回值兼容
                    String value = b.getString(name);
                    // Don't update our cache for reads of other users' data
                    synchronized (this) {
                        mCaches.put(name, value);
                    }
                    return value;
                }
            }

            try {
                Cursor c = null;
                if (cp != null) {
                    c = cp.query(mUri, SELECT_VALUE, NAME_EQ_PLACEHOLDER,
                            new String[]{name}, null, null);
                }
                if (c == null) {
                    LogUtils.w(TAG, "Can't get key " + name + " from " + mUri);
                    return null;
                }
                // 因为明确查询，所以无需遍历
                String value = c.moveToNext() ? c.getString(0) : null;
                synchronized (this) {
                    mCaches.put(name, value);
                }
                return value;
            } catch (RemoteException e) {
                LogUtils.w(TAG, "Can't get key " + name + " from " + mUri, e);
                return null; // Return null, but don't cache it.
            }
        }

        Map cacheAllDataForUser(ContentResolver cr) {
            Cursor c = null;
            try {
                ContentProviderClient cp = getContentProviderClient(cr);
                if (cp != null) {
                    c = cp.query(mUri, null, null, null, null, null);
                }
                if (c == null) {
                    return new HashMap();
                }
                while (c.moveToNext()) {
                    String name = c.getString(0);
                    String value = c.getString(1);
                    LogUtils.i(TAG, "Get key " + name + "=" + value);
                    synchronized (this) {
                        mCaches.put(name, value);
                    }
                }
            } catch (RemoteException e) {
                LogUtils.e(TAG, "cacheAllDataForUser error, " + e.getMessage());
            } finally {
                if (c != null) c.close();
            }
            return mCaches;
        }
    }

    static final class Config {
        private static final NameValueCache sNameValueCache =
                new NameValueCache(AUTOHORITY_URI, CALL_METHOD_GET_CONFIG, CALL_METHOD_PUT_CONFIG);

        public static void registerContentObserver(
                ContentResolver resolver, boolean notifyForDescendents, ContentObserver observer) {
            resolver.registerContentObserver(AUTOHORITY_URI, notifyForDescendents, observer);
        }

        private static String getStringForUser(
                ContentResolver resolver, String name, int userHandle) throws Exception {
            return sNameValueCache.getStringForUser(resolver, name, userHandle);
        }

        private static boolean putStringForUser(
                ContentResolver resolver, String name, String value, int userHandle)
                throws RemoteException {
            return sNameValueCache.putStringForUser(resolver, name, value, userHandle);
        }

        private static String getStringForUser(ContentResolver resolver, String name)
                throws Exception {
            return getStringForUser(resolver, name, getMyUserId());
        }

        private static boolean putStringForUser(ContentResolver resolver, String name, String value)
                throws Exception {
            return putStringForUser(resolver, name, value, getMyUserId());
        }

        static String getString(ContentResolver resolver, String name) throws Exception {
            return getStringForUser(resolver, name);
        }

        static boolean putString(ContentResolver resolver, String name, String value)
                throws Exception {
            return putStringForUser(resolver, name, value);
        }

        public static boolean getBoolean(ContentResolver cr, String name, boolean def)
                throws Exception {
            String v = getString(cr, name);
            return v != null ? Boolean.parseBoolean(v) : def;
        }

        static boolean getBoolean(ContentResolver cr, String name) throws Exception {
            String v = getString(cr, name);
            return Boolean.parseBoolean(v);
        }

        public static void putBoolean(ContentResolver cr, String name, boolean value)
                throws Exception {
            putString(cr, name, Boolean.toString(value));
        }

        public static long getLong(ContentResolver cr, String name, long def) throws Exception {
            String v = getString(cr, name);
            return v != null ? Long.parseLong(v) : def;
        }

        public static void putLong(ContentResolver cr, String name, long value) throws Exception {
            putString(cr, name, Long.toString(value));
        }

        public static float getFloat(ContentResolver cr, String name, float def) throws Exception {
            String v = getString(cr, name);
            return v != null ? Float.parseFloat(v) : def;
        }

        public static void putFloat(ContentResolver cr, String name, float value) throws Exception {
            putString(cr, name, Float.toString(value));
        }

        static long getLong(ContentResolver cr, String name) throws Exception {
            String v = getString(cr, name);
            return Long.parseLong(v);
        }

        public static int getInt(ContentResolver cr, String name, int def) throws Exception {
            String v = getString(cr, name);
            return v != null ? Integer.parseInt(v) : def;
        }

        static int getInt(ContentResolver cr, String name) throws Exception {
            String v = getString(cr, name);
            return Integer.parseInt(v);
        }

        public static boolean putInt(ContentResolver cr, String name, int value) throws Exception {
            return putString(cr, name, Integer.toString(value));
        }

        private static int getMyUserId() {
            // int uid= UserHandle.myUserId();
            int uid = -101010;
            try {
                Class<?> threadClazz = Class.forName("android.os.UserHandle");
                Method method = threadClazz.getMethod("myUserId");
                uid = (int) method.invoke(null);
                LogUtils.i(TAG, "getMyUserId uid: " + uid);
            } catch (Exception e) {
                LogUtils.i(TAG, "getMyUserId error, " + e.getMessage());
            }
            return uid;
        }

        static Map cacheAllDataForUser(ContentResolver cr) throws Exception {
            return sNameValueCache.cacheAllDataForUser(cr);
        }

        static String[] getArray(ContentResolver cr, String name) throws Exception {
            String v = getString(cr, name);
            try {
                return v.split(";");
            } catch (NumberFormatException e) {
                throw new Settings.SettingNotFoundException(name);
            }
        }
    }
}
