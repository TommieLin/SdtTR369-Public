package com.sdt.diagnose.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.WindowManager;

public class ScreenShot {
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;

    public ScreenShot() {
    }

    public ScreenShot(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
    public Bitmap takeScreenshot() {
        mDisplay.getRealMetrics(mDisplayMetrics);
        Rect crop = new Rect(0, 0, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        int rot = mDisplay.getRotation();
        int width = crop.width();
        int height = crop.height();

        // Take the screenshot
        Bitmap mScreenBitmap = SurfaceControl.screenshot(crop, width, height, rot);
        if (mScreenBitmap == null) {
            return null;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();
        dealWithBitmap(mScreenBitmap);
        return mScreenBitmap;
    }

    //TODO save to storage or transfer to server?
    private void dealWithBitmap(Bitmap bitmap) {
    }

}
