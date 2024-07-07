package com.skyworth.scrrtcsrv;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

public final class Controller {
    private static final int MSG_CONTROL = 1;

    private static final int DEFAULT_DEVICE_ID = 0;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final Object mHandlerFence = new Object();
    private MessageHandler mHandler;
    private HandlerThread mThread;
    private final Device device;

    // private final DeviceMessageSender sender;
    private final boolean clipboardAutosync;
    private final boolean powerOn;

    private final KeyCharacterMap charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    private long lastTouchDown;
    private final PointersState pointersState = new PointersState();
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];

    private boolean keepPowerModeOff;

    public Controller(Device device, boolean clipboardAutosync, boolean powerOn) {
        this.device = device;
        this.clipboardAutosync = clipboardAutosync;
        this.powerOn = powerOn;
        initPointers();
        // sender = new DeviceMessageSender();
    }

    private void initPointers() {
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    public void start() {
        synchronized (mHandlerFence) {
            if (mThread != null) {
                return;
            }
            mThread = new HandlerThread("Controller");
            mThread.start();
            mHandler = new MessageHandler(mThread.getLooper());
        }
    }

    public void stop() {
        synchronized (mHandlerFence) {
            if (mThread == null) {
                return;
            }
            mThread.quit();
            mThread = null;
            mHandler = null;
        }
    }

    public void handleEvent(ControlMessage msg) {
        mHandler.obtainMessage(MSG_CONTROL, msg).sendToTarget();
    }

    private class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what != MSG_CONTROL) {
                super.handleMessage(msg);
                return;
            }
            ControlMessage ctrl = (ControlMessage) msg.obj;
            int type = ctrl.getType();
            Ln.d("received control message: type=" + ControlMessage.getTypeStr(type));
            switch (type) {
                case ControlMessage.TYPE_INJECT_KEYCODE:
                    if (device.supportsInputEvents()) {
                        injectKeycode(ctrl.getAction(), ctrl.getKeycode(),
                                ctrl.getRepeat(), ctrl.getMetaState());
                    }
                    break;
                case ControlMessage.TYPE_INJECT_TEXT:
                    if (device.supportsInputEvents()) {
                        injectText(ctrl.getText());
                    }
                    break;
                case ControlMessage.TYPE_INJECT_TOUCH_EVENT:
                    if (device.supportsInputEvents()) {
                        injectTouch(ctrl.getAction(), ctrl.getPointerId(),
                                ctrl.getPosition(), ctrl.getPressure(), ctrl.getButtons());
                    }
                    break;
                case ControlMessage.TYPE_INJECT_SCROLL_EVENT:
                    if (device.supportsInputEvents()) {
                        injectScroll(ctrl.getPosition(), ctrl.getHScroll(),
                                ctrl.getVScroll(), ctrl.getButtons());
                    }
                    break;
                case ControlMessage.TYPE_BACK_OR_SCREEN_ON:
                    if (device.supportsInputEvents()) {
                        pressBackOrTurnScreenOn(ctrl.getAction());
                    }
                    break;
                case ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL:
                    Device.expandNotificationPanel();
                    break;
                case ControlMessage.TYPE_EXPAND_SETTINGS_PANEL:
                    Device.expandSettingsPanel();
                    break;
                case ControlMessage.TYPE_COLLAPSE_PANELS:
                    Device.collapsePanels();
                    break;
                case ControlMessage.TYPE_GET_CLIPBOARD:
                    getClipboard(ctrl.getCopyKey());
                    break;
                case ControlMessage.TYPE_SET_CLIPBOARD:
                    setClipboard(ctrl.getText(), ctrl.getPaste(), ctrl.getSequence());
                    break;
                case ControlMessage.TYPE_SET_SCREEN_POWER_MODE:
                    if (device.supportsInputEvents()) {
                        int mode = ctrl.getAction();
                        boolean setPowerModeOk = Device.setScreenPowerMode(mode);
                        if (setPowerModeOk) {
                            keepPowerModeOff = mode == Device.POWER_MODE_OFF;
                            Ln.i("Device screen turned " + (mode == Device.POWER_MODE_OFF ? "off" : "on"));
                        }
                    }
                    break;
                case ControlMessage.TYPE_ROTATE_DEVICE:
                    Device.rotateDevice();
                    break;
            }
        }
    }

    private boolean injectKeycode(int action, int keycode, int repeat, int metaState) {
        if (keepPowerModeOff && action == KeyEvent.ACTION_UP && (keycode == KeyEvent.KEYCODE_POWER || keycode == KeyEvent.KEYCODE_WAKEUP)) {
            schedulePowerModeOff();
        }
        return device.injectKeyEvent(action, keycode, repeat, metaState, Device.INJECT_MODE_ASYNC);
    }

    private int injectText(String text) {
        int successCount = 0;
        for (char c : text.toCharArray()) {
            if (!injectChar(c)) {
                Ln.w("Could not inject char u+" + String.format("%04x", (int) c));
                continue;
            }
            successCount++;
        }
        return successCount;
    }

    private boolean injectChar(char c) {
        String decomposed = KeyComposition.decompose(c);
        char[] chars = decomposed != null ? decomposed.toCharArray() : new char[]{c};
        KeyEvent[] events = charMap.getEvents(chars);
        if (events == null) {
            return false;
        }
        for (KeyEvent event : events) {
            if (!device.injectEvent(event, Device.INJECT_MODE_ASYNC)) {
                return false;
            }
        }
        return true;
    }

    private boolean injectTouch(int action, long pointerId, Position position, float pressure, int buttons) {
        long now = SystemClock.uptimeMillis();

        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            Ln.w("Ignore touch event, it was generated for a different device size");
            return false;
        }

        int pointerIndex = pointersState.getPointerIndex(pointerId);
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return false;
        }
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(pressure);
        pointer.setUp(action == MotionEvent.ACTION_UP);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        // Right-click and middle-click only work if the source is a mouse
        boolean nonPrimaryButtonPressed = (buttons & ~MotionEvent.BUTTON_PRIMARY) != 0;
        int source = nonPrimaryButtonPressed ? InputDevice.SOURCE_MOUSE : InputDevice.SOURCE_TOUCHSCREEN;
        if (source != InputDevice.SOURCE_MOUSE) {
            // Buttons must not be set for touch events
            buttons = 0;
        }

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEFAULT_DEVICE_ID, 0, source,
                        0);
        return device.injectEvent(event, Device.INJECT_MODE_ASYNC);
    }

    private boolean injectScroll(Position position, int hScroll, int vScroll, int buttons) {
        long now = SystemClock.uptimeMillis();
        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            // ignore event
            return false;
        }

        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = point.getX();
        coords.y = point.getY();
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, MotionEvent.ACTION_SCROLL, 1,
                        pointerProperties, pointerCoords, 0, buttons,
                        1f, 1f, DEFAULT_DEVICE_ID, 0,
                        InputDevice.SOURCE_MOUSE, 0);
        return device.injectEvent(event, Device.INJECT_MODE_ASYNC);
    }

    private boolean pressBackOrTurnScreenOn(int action) {
        if (Device.isScreenOn()) {
            return device.injectKeyEvent(action, KeyEvent.KEYCODE_BACK, 0, 0, Device.INJECT_MODE_ASYNC);
        }

        // Screen is off
        // Only press POWER on ACTION_DOWN
        if (action != KeyEvent.ACTION_DOWN) {
            // do nothing,
            return true;
        }

        if (keepPowerModeOff) {
            schedulePowerModeOff();
        }
        return device.pressReleaseKeycode(KeyEvent.KEYCODE_POWER, Device.INJECT_MODE_ASYNC);
    }

    private void getClipboard(int copyKey) {
        // On Android >= 7, press the COPY or CUT key if requested
        if (copyKey != ControlMessage.COPY_KEY_NONE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && device.supportsInputEvents()) {
            int key = copyKey == ControlMessage.COPY_KEY_COPY ? KeyEvent.KEYCODE_COPY : KeyEvent.KEYCODE_CUT;
            // Wait until the event is finished, to ensure that the clipboard text we read just after is the correct one
            device.pressReleaseKeycode(key, Device.INJECT_MODE_WAIT_FOR_FINISH);
        }

        // If clipboard autosync is enabled, then the device clipboard is synchronized to the computer clipboard whenever it changes, in
        // particular when COPY or CUT are injected, so it should not be synchronized twice. On Android < 7, do not synchronize at all rather than
        // copying an old clipboard content.
        if (!clipboardAutosync) {
            String clipboardText = Device.getClipboardText();
            if (clipboardText != null) {
                // TODO: please implement sendClipboardText
                // sender.pushClipboardText(clipboardText);
                Ln.e("TODO: please implement sendClipboardText to send:" + clipboardText);
            }
        }
    }

    private boolean setClipboard(String text, boolean paste, long sequence) {
        boolean ok = device.setClipboardText(text);
        if (ok) {
            Ln.i("Device clipboard set");
        }

        // On Android >= 7, also press the PASTE key if requested
        if (paste && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && device.supportsInputEvents()) {
            device.pressReleaseKeycode(KeyEvent.KEYCODE_PASTE, Device.INJECT_MODE_ASYNC);
        }

        if (sequence != ControlMessage.SEQUENCE_INVALID) {
            // Acknowledgement requested
            // TODO: please implement sendAckClipboard
            // sender.pushAckClipboard(sequence);
            Ln.e("TODO: please implement sendAckClipboard");
        }

        return ok;
    }

    /**
     * Schedule a call to set power mode to off after a small delay.
     */
    private static void schedulePowerModeOff() {
        EXECUTOR.schedule(new Runnable() {
            @Override
            public void run() {
                Ln.i("Forcing screen off");
                Device.setScreenPowerMode(Device.POWER_MODE_OFF);
            }
        }, 200, TimeUnit.MILLISECONDS);
    }
}
