package com.sdt.diagnose.common;

import android.content.Context;

import java.util.ArrayList;

public abstract class AbstractCachedArray<T> {
    ArrayList<T> mList;

    AbstractCachedArray(Context context) {
        buildList(context);
    }

    public boolean isEmpty() {
        return mList == null || mList.isEmpty();
    }

    public void clear() {
        if (mList != null) {
            mList.clear();
            mList = null;
        }
    }

    public ArrayList<T> getList() {
        return mList;
    }

    void add(T t) {
        if (mList == null) {
            mList = new ArrayList<>();
        }
        mList.add(t);
    }

    abstract void buildList(Context context);
}
