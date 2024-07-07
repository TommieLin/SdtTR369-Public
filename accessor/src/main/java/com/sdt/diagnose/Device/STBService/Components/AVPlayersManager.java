package com.sdt.diagnose.Device.STBService.Components;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.sdt.diagnose.common.GlobalContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AVPlayersManager {
    private static AVPlayersManager mAVPlayers;
    private List<ResolveInfo> mList;
    private PackageManager mPm;

    public static AVPlayersManager getInstance() {
        synchronized (AVPlayersManager.class) {
            if (mAVPlayers == null) {
                mAVPlayers = new AVPlayersManager();
            }
        }
        return mAVPlayers;
    }

    private AVPlayersManager() {
        init();
    }

    public String getAVPlayerName1() {
        if (mList == null || mPm == null) return null;
        if (mList.size() > 1) {
            return mList.get(0).loadLabel(mPm).toString();
        }
        return null;
    }

    public String getAVPlayerName2() {
        if (mList == null || mPm == null) return null;
        if (mList.size() > 2) {
            return mList.get(1).loadLabel(mPm).toString();
        }
        return null;
    }

    public String getAVPlayerName3() {
        if (mList == null || mPm == null) return null;
        if (mList.size() > 3) {
            return mList.get(2).loadLabel(mPm).toString();
        }
        return null;
    }

    private void init() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.setDataAndType(Uri.fromFile(new File("")), "audio/*");// type:改成"video/*"表示获取视频的
        mPm = GlobalContext.getContext().getPackageManager();

        List<ResolveInfo> temp = mPm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (mList == null) {
            mList = new ArrayList<>();
        } else {
            mList.clear();
        }
        for (ResolveInfo resolveInfo : temp) {
            if (!include(mList, resolveInfo)) {
                mList.add(resolveInfo);
            }
        }
        intent.setDataAndType(Uri.fromFile(new File("")), "video/*");// type:改成"video/*"表示获取视频的
        temp.clear();
        temp = mPm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : temp) {
            if (!include(mList, resolveInfo)) {
                mList.add(resolveInfo);
            }
        }
    }

    private boolean include(List<ResolveInfo> list, ResolveInfo info) {
        if (info == null) {
            return false;
        }
        for (ResolveInfo resolveInfo : list) {
            if (resolveInfo.activityInfo.packageName.equals(info.activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }
}
