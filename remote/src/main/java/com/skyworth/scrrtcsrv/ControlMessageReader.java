package com.skyworth.scrrtcsrv;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


public final class ControlMessageReader {
    private static final String TAG = "TR369 ControlMessageReader";

    private ControlMessageReader() {
    }

    public static ControlMessage parse(String message) {
        try {
            JSONObject obj = new JSONObject(message);
            return parse(obj);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parse error, " + e.getMessage());
        }
        return null;
    }

    public static ControlMessage parse(JSONObject obj) {
        ControlMessage msg;
        int type = obj.optInt("type");
        switch (type) {
            case ControlMessage.TYPE_INJECT_KEYCODE:
                msg = parseInjectKeycode(obj);
                break;
            case ControlMessage.TYPE_INJECT_TEXT:
                msg = parseInjectText(obj);
                break;
            case ControlMessage.TYPE_INJECT_TOUCH_EVENT:
                msg = parseInjectTouchEvent(obj);
                break;
            case ControlMessage.TYPE_INJECT_SCROLL_EVENT:
                msg = parseInjectScrollEvent(obj);
                break;
            case ControlMessage.TYPE_BACK_OR_SCREEN_ON:
                msg = parseBackOrScreenOnEvent(obj);
                break;
            case ControlMessage.TYPE_GET_CLIPBOARD:
                msg = parseGetClipboard(obj);
                break;
            case ControlMessage.TYPE_SET_CLIPBOARD:
                msg = parseSetClipboard(obj);
                break;
            case ControlMessage.TYPE_SET_SCREEN_POWER_MODE:
                msg = parseSetScreenPowerMode(obj);
                break;
            case ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL:
            case ControlMessage.TYPE_EXPAND_SETTINGS_PANEL:
            case ControlMessage.TYPE_COLLAPSE_PANELS:
            case ControlMessage.TYPE_ROTATE_DEVICE:
                msg = ControlMessage.createEmpty(type);
                break;
            default:
                Ln.w("Unknown event type: " + type);
                msg = null;
                break;
        }

        return msg;
    }

    private static ControlMessage parseInjectKeycode(JSONObject obj) {
        try {
            int action = obj.getInt("action");
            int keycode = obj.getInt("keycode");
            int repeat = obj.getInt("repeat");
            int metaState = obj.getInt("metaState");
            return ControlMessage.createInjectKeycode(action, keycode, repeat, metaState);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseInjectKeycode error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseInjectText(JSONObject obj) {
        try {
            String text = obj.getString("text");
            if (text.isEmpty()) {
                return null;
            }
            return ControlMessage.createInjectText(text);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseInjectText error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseInjectTouchEvent(JSONObject obj) {
        try {
            int action = obj.getInt("action");
            long pointerId = obj.getLong("pointerId");
            Position position = readPosition(obj.getJSONObject("position"));
            float pressure = (float) obj.getDouble("pressure");
            int buttons = obj.getInt("buttons");
            return ControlMessage.createInjectTouchEvent(action, pointerId, position, pressure, buttons);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseInjectTouchEvent error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseInjectScrollEvent(JSONObject obj) {
        try {
            Position position = readPosition(obj.getJSONObject("position"));
            int hScroll = obj.getInt("hScroll");
            int vScroll = obj.getInt("vScroll");
            int buttons = obj.getInt("buttons");
            return ControlMessage.createInjectScrollEvent(position, hScroll, vScroll, buttons);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseInjectScrollEvent error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseBackOrScreenOnEvent(JSONObject obj) {
        try {
            int action = obj.getInt("action");
            return ControlMessage.createBackOrScreenOn(action);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseBackOrScreenOnEvent error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseGetClipboard(JSONObject obj) {
        try {
            int copyKey = obj.getInt("copyKey");
            return ControlMessage.createGetClipboard(copyKey);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseGetClipboard error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseSetClipboard(JSONObject obj) {
        try {
            long sequence = obj.getLong("sequence");
            boolean paste = obj.getBoolean("paste");
            String text = obj.getString("text");
            if (text.isEmpty()) {
                return null;
            }
            return ControlMessage.createSetClipboard(sequence, text, paste);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseSetClipboard error, " + e.getMessage());
        }
        return null;
    }

    private static ControlMessage parseSetScreenPowerMode(JSONObject obj) {
        try {
            int mode = obj.getInt("mode");
            return ControlMessage.createSetScreenPowerMode(mode);
        } catch (Exception e) {
            Log.e(TAG, "ControlMessage parseSetScreenPowerMode error, " + e.getMessage());
        }
        return null;
    }

    private static Position readPosition(JSONObject obj) throws JSONException {
        int x = obj.getInt("x");
        int y = obj.getInt("y");
        int screenWidth = obj.getInt("sw");
        int screenHeight = obj.getInt("sh");
        return new Position(x, y, screenWidth, screenHeight);
    }
}
