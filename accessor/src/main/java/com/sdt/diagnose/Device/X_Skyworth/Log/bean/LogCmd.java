package com.sdt.diagnose.Device.X_Skyworth.Log.bean;

/**
 * ClassName: LogCmd
 *
 * <p>ClassDescription: LogCmd
 *
 * <p>Author: ZHX Date: 2022/10/31
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
public enum LogCmd {
    DoNothing("do_nothing"),
    CatchLog("catch_log"),
    ExportLog("export_log"),
    ClearLog("clear_log");
    String cmdName;

    LogCmd(String cmdName) {
        this.cmdName = cmdName;
    }

    public String getCmdName() {
        return cmdName;
    }
}
