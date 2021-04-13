package com.hyphenate.easecallkit.base;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.R;
import com.hyphenate.easecallkit.ui.EaseMultipleVideoActivity;
import com.hyphenate.easecallkit.ui.EaseVideoCallActivity;
import com.hyphenate.easecallkit.utils.EaseCallKitUtils;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import com.hyphenate.util.EMLog;

import java.lang.reflect.Method;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


/**
 * author lijian
 * email: Allenlee@easemob.com
 * date: 02/06/2021
 */
public class EaseCallFloatWindow {
    private static final String TAG = "EaseCallFloatWindow";

    private Context context;

    private static EaseCallFloatWindow instance;

    private WindowManager windowManager = null;
    private WindowManager.LayoutParams layoutParams = null;

    private View floatView;
    private ImageView avatarView;
    private SurfaceView surfaceView;

    private int screenWidth;
    private int floatViewWidth;
    private EaseCallType callType;
    private EaseCallMemberView memberView;
    private RtcEngine rtcEngine;
    private int uId;

    public EaseCallFloatWindow(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        screenWidth = point.x;
    }


    public static EaseCallFloatWindow getInstance(Context context) {
        if (instance == null) {
            instance = new EaseCallFloatWindow(context);
        }
        return instance;
    }

    public void setCallType(EaseCallType callType) {
        this.callType = callType;
    }

    public EaseCallMemberView getCallMemberView(){
        return memberView;
    }

    public void setRtcEngine(RtcEngine rtcEngine){
        this.rtcEngine = rtcEngine;
    }

    public EaseCallType getCallType() {
        return  callType;
    }

    /**
     * add float window
     */
    public void show() { // 0: voice call; 1: video call;
        if (floatView != null) {
            return;
        }
        layoutParams = new WindowManager.LayoutParams();
        layoutParams.gravity = Gravity.END | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.type = EaseCallKitUtils.getSupportedWindowType();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        floatView = LayoutInflater.from(context).inflate(R.layout.activity_float_window, null);
        floatView.setFocusableInTouchMode(true);

        windowManager.addView(floatView, layoutParams);
        floatView.post(new Runnable() {
            @Override
            public void run() {
                // Get the size of floatView;
                if(floatView != null) {
                    floatViewWidth = floatView.getWidth();
                }
            }
        });
        avatarView = (ImageView) floatView.findViewById(R.id.iv_avatar);

        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if(callType == EaseCallType.CONFERENCE_CALL){
                    intent = new Intent(EaseCallKit.getInstance().getAppContext(), EaseMultipleVideoActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                }else{
                    intent = new Intent(EaseCallKit.getInstance().getAppContext(), EaseVideoCallActivity.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("uId", uId);
                }
                intent.putExtra("isClickByFloat", true);
                EaseCallKit.getInstance().getAppContext().startActivity(intent);
                dismiss();
            }
        });

        floatView.setOnTouchListener(new View.OnTouchListener() {
            boolean result = false;

            int left;
            int top;
            float startX = 0;
            float startY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        result = false;
                        startX = event.getRawX();
                        startY = event.getRawY();

                        left = layoutParams.x;
                        top = layoutParams.y;

                        EMLog.i(TAG, "startX: " + startX + ", startY: " + startY + ", left: " + left + ", top: " + top);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 20 || Math.abs(event.getRawY() - startY) > 20) {
                            result = true;
                        }

                        int deltaX = (int) (startX - event.getRawX());

                        layoutParams.x = left + deltaX;
                        layoutParams.y = (int) (top + event.getRawY() - startY);
                        EMLog.i(TAG, "startX: " + (event.getRawX() - startX) + ", startY: " + (event.getRawY() - startY)
                                + ", left: " + left + ", top: " + top);
                        windowManager.updateViewLayout(floatView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        smoothScrollToBorder();
                        break;
                }
                return result;
            }
        });
    }



    public void update(EaseCallMemberView view) {
        if (floatView == null) {
            return;
        }
        memberView = view;
        uId = memberView.getUserId();
        if (memberView.isVideoOff()) { // 视频未开启
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.VISIBLE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.GONE);
        } else { // 视频已开启
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.GONE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.VISIBLE);

            String userAccount = memberView.getUserAccount();
            int uId = memberView.getUserId();
            boolean isSelf = (userAccount.equals(EMClient.getInstance().getCurrentUser())?true:false);
            prepareSurfaceView(isSelf,uId);
        }
    }
    
    public void updateCallWindow(boolean isSelf, int uId,boolean surface) {
        this.uId = uId;
        if(callType == EaseCallType.SINGLE_VIDEO_CALL && surface){
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.GONE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.VISIBLE);
            prepareSurfaceView(isSelf,uId);
        }else{
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.VISIBLE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.GONE);
        }
    }

    public boolean isShowing() {
        if(callType == EaseCallType.CONFERENCE_CALL){
            return memberView != null;
        }else{
            return floatView != null;
        }
    }

    public int getUid() {
        if(callType == EaseCallType.CONFERENCE_CALL && memberView != null) {
            return memberView.getUserId();
        }
        return -1;
    }

    /**
     * 停止悬浮窗
     */
    public void dismiss() {
        Log.i(TAG, "dismiss: ");
        if (windowManager != null && floatView != null) {
            windowManager.removeView(floatView);
            floatView = null;
        }
    }

    /**
     * set call surface view
     */
    private void prepareSurfaceView(boolean isSelf,int uid) {
        RelativeLayout surfaceLayout = (RelativeLayout) floatView.findViewById(R.id.layout_call_video);
        surfaceLayout.removeAllViews();
        surfaceView =
                RtcEngine.CreateRendererView(EaseCallKit.getInstance().getAppContext());
        surfaceLayout.addView(surfaceView);
        surfaceView.setZOrderOnTop(false);
        surfaceView.setZOrderMediaOverlay(false);
        if(isSelf){
            rtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN,0));
        }else{
            rtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        }
    }

    private void smoothScrollToBorder() {
        EMLog.i(TAG, "screenWidth: " + screenWidth + ", floatViewWidth: " + floatViewWidth);
        int splitLine = screenWidth / 2 - floatViewWidth / 2;
        final int left = layoutParams.x;
        final int top = layoutParams.y;
        int targetX;

        if (left < splitLine) {
            // 滑动到最左边
            targetX = 0;
        } else {
            // 滑动到最右边
            targetX = screenWidth - floatViewWidth;
        }

        ValueAnimator animator = ValueAnimator.ofInt(left, targetX);
        animator.setDuration(100)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (floatView == null) return;

                        int value = (int) animation.getAnimatedValue();
                        EMLog.i(TAG, "onAnimationUpdate, value: " + value);
                        layoutParams.x = value;
                        layoutParams.y = top;
                        windowManager.updateViewLayout(floatView, layoutParams);
                    }
                });
        animator.start();
    }
}
