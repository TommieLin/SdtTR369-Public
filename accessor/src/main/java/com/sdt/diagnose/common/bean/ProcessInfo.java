package com.sdt.diagnose.common.bean;

import android.app.ActivityManager;

public class ProcessInfo {
    private int mPid;
    private String mCommand;
    private int mSize;
    private int mPriority;
    private int mCpuTime;
    private String mState;

    public ProcessInfo(ActivityManager.RunningAppProcessInfo mProcess) {
        if (mProcess != null) {
            mPid = mProcess.pid;
            mCommand = mProcess.processName;
            mPriority = mProcess.importance;
        }
    }

    public int getPid() {
        return mPid;
    }

    public String getCommand() {
        return mCommand;
    }

    public int getSize() {
        return mSize;
    }

    public void setSize(int size) {
        mSize = size;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setCpuTime(int cpuTime) {
        mCpuTime = cpuTime;
    }

    public int getCpuTime() {
        return mCpuTime;
    }

    public String getState() {
        return mState;
    }

    public String toString() {
        return "process info:{mPid:" + mPid
                + ", mCommand:" + mCommand
                + ", mSize:" + mSize
                + ", mPriority:" + mPriority
                + ", mCpuTime:" + mCpuTime
                + ", mState:" + mState
                + "}";
    }
}
