package com.hyphenate.easecallkit.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.hyphenate.easecallkit.base.EaseCallFloatWindow;

/**
 * As the base call activity, the common code can be put in
 */
public class EaseBaseCallActivity extends AppCompatActivity {
    protected final int REQUEST_CODE_OVERLAY_PERMISSION = 1002;
    //用于防止多次打开请求悬浮框页面
    protected boolean requestOverlayPermission;

    /**
     * Check whether float window is showing
     * @return
     */
    public boolean isFloatWindowShowing() {
        return EaseCallFloatWindow.getInstance().isShowing();
    }

    /**
     * Check permission and show float window
     */
    public void showFloatWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                doShowFloatWindow();
            } else { // To reqire the window permission.
                if(!requestOverlayPermission) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        // Add this to open the management GUI specific to this app.
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
                        requestOverlayPermission = true;
                        // Handle the permission require result in #onActivityResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            doShowFloatWindow();
        }
    }

    public void doShowFloatWindow() {}
}
