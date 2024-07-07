package com.sdt.diagnose.command;

import static com.sdt.diagnose.command.ShortMessageUtils.KEY_CONTENT;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_ID;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_IMAGE_URL;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_POSITION;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_TITLE;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_TYPE;
import static com.sdt.diagnose.command.ShortMessageUtils.KEY_URL;
import static com.sdt.diagnose.command.ShortMessageUtils.SHOW_TIME;
import static com.sdt.diagnose.command.ShortMessageUtils.TYPE_IMAGE;
import static com.sdt.diagnose.command.ShortMessageUtils.TYPE_TEXT;
import static com.sdt.diagnose.command.ShortMessageUtils.TYPE_URL;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.sdt.accessor.R;
import com.sdt.diagnose.common.GlobalContext;
import com.squareup.picasso.Picasso;

public class ShortMessageActivity extends Activity {
    public TextView title;
    public TextView noContent;
    public WebView noWeb;
    public ImageView noImage;
    public String messageId;
    public static final int dismissDialog = 1;
    AlertDialog alertDialog;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case dismissDialog:
                    dismissDialog();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice);
        android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
        p.height = 1;
        p.width = 1;
        p.alpha = 0.0f;
        getWindow().setAttributes(p);
        ShortMessageUtils.hookWebView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showLauncherNotification();
    }

    public void showLauncherNotification() {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_notification_view, null);
        title = view.findViewById(R.id.no_title);
        noContent = view.findViewById(R.id.no_content);
        noWeb = view.findViewById(R.id.no_webview);
        noImage = view.findViewById(R.id.no_imageview);
        final Bundle bundle = getIntent().getExtras();
        messageId = bundle.getString(KEY_ID);
        title.setText(bundle.getString(KEY_TITLE));
        title.setTextColor(Color.WHITE);

        if (bundle.getString(KEY_TYPE).equals(TYPE_TEXT)) {
            noContent.setVisibility(View.VISIBLE);
            noWeb.setVisibility(View.GONE);
            noImage.setVisibility(View.GONE);
            noContent.setText(bundle.getString(KEY_CONTENT));
            noContent.setTextColor(Color.WHITE);
        } else if (bundle.getString(KEY_TYPE).equals(TYPE_URL)) {
            noWeb.setVisibility(View.VISIBLE);
            noContent.setVisibility(View.GONE);
            noImage.setVisibility(View.GONE);
            noWeb.loadUrl(bundle.getString(KEY_URL));
            // 设置WebView属性，能够执行JavaScript脚本
            noWeb.getSettings().setJavaScriptEnabled(true);
            // 设置可以支持缩放
            noWeb.getSettings().setSupportZoom(true);
            // 设置出现缩放工具
            noWeb.getSettings().setBuiltInZoomControls(true);
            // 为图片添加放大缩小功能
            noWeb.getSettings().setUseWideViewPort(true);
            noWeb.setInitialScale(50);  //100代表不缩放
            noWeb.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    noWeb.loadUrl(url);
                    return true;
                }
            });
        } else if (bundle.getString(KEY_TYPE).equals(TYPE_IMAGE)) {
            noContent.setVisibility(View.GONE);
            noWeb.setVisibility(View.GONE);
            noImage.setVisibility(View.VISIBLE);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            int displayWidth = (int) (screenWidth * 0.5);
            int displayHeight = (int) (screenHeight * 0.5);
            noImage.setLayoutParams(new LinearLayout.LayoutParams(displayWidth, displayHeight));
            // 图片保持原长宽比自适应控件大小
            Picasso.get().load(bundle.getString(KEY_IMAGE_URL)).fit().centerInside().into(noImage);
        }
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(this, R.style.AlertDialogCustom);
        alertDialog = builder.setView(view)
                .setPositiveButton(
                        GlobalContext.getContext().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissDialog();
                            }
                        }).create();

        // 根据前端设置dialog的位置
        alertDialog.getWindow().setGravity(covertDialogPosition(bundle.getString(KEY_POSITION)));
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // 设置自动消失的时长
                handler.sendEmptyMessageDelayed(dismissDialog, bundle.getInt(SHOW_TIME) > 0
                        ? (bundle.getInt(SHOW_TIME) * 1000L) : 10000);
                Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setTextSize(10);
                button.setTextColor(Color.WHITE);
                //button.setGravity(Gravity.BOTTOM);
                LinearLayout.LayoutParams buttonLP =
                        (LinearLayout.LayoutParams) button.getLayoutParams();
                // 设置按钮的大小
                buttonLP.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                buttonLP.width = LinearLayout.LayoutParams.MATCH_PARENT;
                // 设置文字居中
                buttonLP.gravity = Gravity.CENTER_HORIZONTAL;
                button.setLayoutParams(buttonLP);
                //button.setTextColor(Color.GREEN);
                button.setBackgroundResource(R.drawable.button_background_shortmsg);
                //button.requestFocus();
                ShortMessageUtils.responseServer(messageId);
            }
        });

        alertDialog.show();
        // 设置dialog的大小
        setLayoutParams(alertDialog);
    }

    public void dismissDialog() {
        alertDialog.dismiss();
        finish();
    }

    private void setLayoutParams(Dialog dialog) {
        WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
        WindowManager m = getWindowManager();
        // 获取屏幕宽、高
        Display d = m.getDefaultDisplay();
        int lastWidth = attrs.width;
        int lastHeight = attrs.height;
        int maxWidth = (int) (d.getWidth() * 0.5);
        int maxHeight = (int) (d.getHeight() * 0.5);
        if (lastHeight > maxHeight || lastWidth > maxWidth) {
            attrs.width = maxWidth;
            attrs.height = maxHeight;
        }
        dialog.getWindow().setAttributes(attrs);
    }

    public int covertDialogPosition(String position) {
        if (TextUtils.isEmpty(position)) {
            return Gravity.CENTER;
        }
        int location;
        switch (position) {
            case "top":
                location = Gravity.TOP;
                break;
            case "bottom":
                location = Gravity.BOTTOM;
                break;
            case "left":
                location = Gravity.LEFT;
                break;
            case "right":
                location = Gravity.RIGHT;
                break;
            default:
                location = Gravity.CENTER;
                break;
        }
        return location;
    }
}
