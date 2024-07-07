package com.sdt.diagnose.Device.X_Skyworth.Log.bean;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * ClassName: Status
 *
 * <p>ClassDescription: Status
 *
 * <p>Author: ZHX Date: 2022/10/26
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
public class Status {
    public static final int TERMINATE = 1;
    public static final int PREPARE = 2;
    public static final int RUNNING = 3;
    public static final int SUCCESS = 4;
    public static final int FAILED = 5;

    @IntDef(value = {TERMINATE, PREPARE, RUNNING, SUCCESS, FAILED})
    public @interface Code {
    }

    private final @Code int code;
    private final String msg;

    private Status(@Code int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public @Code int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    @NonNull
    public String toString() {
        return "Result{" + "code=" + code + ", msg='" + msg + '\'' + '}';
    }

    public static Status terminate() {
        return new Status(TERMINATE, "");
    }

    public static Status prepare() {
        return new Status(PREPARE, "");
    }

    public static Status running() {
        return new Status(RUNNING, "");
    }

    public static Status success() {
        return new Status(SUCCESS, "");
    }

    public static Status failure(String msg) {
        return new Status(FAILED, msg);
    }
}
