package com.hyphenate.easecallkit.base;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hyphenate.chat.EMClient;
import com.hyphenate.easecallkit.EaseCallKit;
import com.hyphenate.easecallkit.R;
import com.hyphenate.easecallkit.ui.EaseBaseCallActivity;
import com.hyphenate.easecallkit.utils.EaseCallKitUtils;

import com.hyphenate.easecallkit.widget.MyChronometer;
import com.hyphenate.util.EMLog;

import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;


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
    private long costSeconds;
    private ConferenceInfo conferenceInfo;
    private SingleCallInfo singleCallInfo;

    public EaseCallFloatWindow(Context context) {
        initFloatWindow(context);
    }

    private EaseCallFloatWindow() {
    }


    public static EaseCallFloatWindow getInstance(Context context) {
        if (instance == null) {
            instance = new EaseCallFloatWindow(context);
        }
        return instance;
    }

    public static EaseCallFloatWindow getInstance() {
        if(instance == null) {
            synchronized (EaseCallFloatWindow.class) {
                if(instance == null) {
                    instance = new EaseCallFloatWindow();
                }
            }
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

    public void setRtcEngine(Context context, RtcEngine rtcEngine){
        this.rtcEngine = rtcEngine;
        initFloatWindow(context);
    }

    private void initFloatWindow(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        screenWidth = point.x;
    }

    public EaseCallType getCallType() {
        return  callType;
    }

    private MyChronometer chronometer;

    public void setCostSeconds(long seconds) {
        this.costSeconds = seconds;
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

        if(floatView instanceof ViewGroup) {
            chronometer = new MyChronometer(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
            ((ViewGroup)floatView).addView(chronometer, params);
        }

        windowManager.addView(floatView, layoutParams);
        startCount();
        if(callType == EaseCallType.CONFERENCE_CALL) {
            conferenceInfo = new ConferenceInfo();
        }else {
            singleCallInfo = new SingleCallInfo();
        }
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
                Class<? extends EaseBaseCallActivity> callClass = EaseCallKit.getInstance().getCurrentCallClass();
                Log.e("TAG", "current call class: "+callClass);
                if(callClass != null) {
                    Intent intent = new Intent(context, callClass);
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    if(callType != EaseCallType.CONFERENCE_CALL) {
                        intent.putExtra("uId", singleCallInfo != null ? singleCallInfo.remoteUid : 0);
                    }
                    intent.putExtra("isClickByFloat", true);
                    EaseCallKit.getInstance().getAppContext().startActivity(intent);
                }else {
                   EMLog.e(TAG, "Current call class is null, please not call EaseCallKit.getInstance().releaseCall() before the call is finished");
                }
                //dismiss();
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

    private void startCount() {
        if(chronometer != null) {
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
        }
    }

    private void stopCount() {
        if(chronometer != null) {
            chronometer.stop();
        }
    }

    /**
     * Should call the method before call {@link #dismiss()}
     * @return Cost seconds in float window
     */
    public long getFloatCostSeconds() {
        if(chronometer != null) {
            return chronometer.getCostSeconds();
        }
        Log.e(TAG, "chronometer is null, can not get cost seconds");
        return 0;
    }

    /**
     * Should call the method before call {@link #dismiss()}
     * @return Total cost seconds
     */
    public long getTotalCostSeconds() {
        if(chronometer != null) {
            Log.e("activity", "costSeconds: "+chronometer.getCostSeconds());
        }
        if(chronometer != null) {
            return costSeconds + chronometer.getCostSeconds();
        }
        Log.e(TAG, "chronometer is null, can not get total cost seconds");
        return 0;
    }

    public void setConferenceInfo(ConferenceInfo info) {
        this.conferenceInfo = info;
    }

    public ConferenceInfo getConferenceInfo() {
        return conferenceInfo;
    }

    /**
     * Update conference call state
     * @param view
     */
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
            boolean isSelf = TextUtils.equals(userAccount, EMClient.getInstance().getCurrentUser());
            prepareSurfaceView(isSelf,uId);
        }
    }

    /**
     * Update the sing call state
     * @param isSelf
     * @param curUid
     * @param remoteUid
     * @param surface
     */
    public void update(boolean isSelf, int curUid, int remoteUid, boolean surface) {
        if(singleCallInfo == null) {
            singleCallInfo = new SingleCallInfo();
        }
        singleCallInfo.curUid = curUid;
        singleCallInfo.remoteUid = remoteUid;
        if(callType == EaseCallType.SINGLE_VIDEO_CALL && surface){
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.GONE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.VISIBLE);
            prepareSurfaceView(isSelf, isSelf ? curUid : remoteUid);
        }else{
            floatView.findViewById(R.id.layout_call_voice).setVisibility(View.VISIBLE);
            floatView.findViewById(R.id.layout_call_video).setVisibility(View.GONE);
        }
    }

    public SingleCallInfo getSingleCallInfo() {
        return singleCallInfo;
    }

    public void setCameraDirection(boolean isFront, boolean changeFlag) {
        if(singleCallInfo == null) {
            singleCallInfo = new SingleCallInfo();
        }
        singleCallInfo.isCameraFront = isFront;
        singleCallInfo.changeFlag = changeFlag;
    }

    public boolean isShowing() {
        if(callType == EaseCallType.CONFERENCE_CALL){
            return memberView != null;
        }else{
            return floatView != null;
        }
    }

    /**
     * For the single call, only the remote uid is returned
     * @return
     */
    public int getUid() {
        if(callType == EaseCallType.CONFERENCE_CALL && memberView != null) {
            return memberView.getUserId();
        }else if((callType == EaseCallType.SINGLE_VIDEO_CALL || callType == EaseCallType.SINGLE_VOICE_CALL) && singleCallInfo != null) {
            return singleCallInfo.remoteUid;
        }
        return -1;
    }

    /**
     * 停止悬浮窗
     */
    public void dismiss() {
        Log.i(TAG, "dismiss: ");
        if (windowManager != null && floatView != null) {
            stopCount();
            windowManager.removeView(floatView);
        }
        floatView = null;
        memberView = null;
        surfaceView = null;
        if(conferenceInfo != null) {
            conferenceInfo = null;
        }
        if(singleCallInfo != null) {
            singleCallInfo = null;
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
    
    public static class SingleCallInfo {
        /**
         * Current user's uid
         */
        public int curUid;
        /**
         * The other size of uid
         */
        public int remoteUid;
        /**
         * Camera direction: front or back
         */
        public boolean isCameraFront = true;
        /**
         * A tag used to mark the switch between local and remote video
         */
        public boolean changeFlag;
    }

    /**
     * Use to hold the conference info
     */
    public static class ConferenceInfo {
        public Map<Integer, ViewState> uidToViewList;
        public Map<String, Integer> userAccountToUidMap;
        public Map<Integer, EaseUserAccount> uidToUserAccountMap;

        /**
         * Hold the states of {@link EaseCallMemberView}
         */
        public static class ViewState {
            // video state
            public boolean isVideoOff;
            // audio state
            public boolean isAudioOff;
            // screen mode
            public boolean isFullScreenMode;
            // speak activate state
            public boolean speakActivated;
            // camera direction
            public boolean isCameraFront;
        }
    }
}
